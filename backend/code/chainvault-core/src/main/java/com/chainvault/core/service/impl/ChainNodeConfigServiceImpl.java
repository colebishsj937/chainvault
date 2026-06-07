package com.chainvault.core.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.chainvault.chainnode.config.ChainNodeProperties;
import com.chainvault.chainnode.dto.ChainNodeSettings;
import com.chainvault.common.constants.ChainCode;
import com.chainvault.common.constants.ConfigConstants;
import com.chainvault.common.enums.ChainNodeProvider;
import com.chainvault.common.exception.BusinessException;
import com.chainvault.core.domain.dto.ChainNodeApiKeyAddReq;
import com.chainvault.core.domain.dto.ChainNodeConfigUpdateReq;
import com.chainvault.core.domain.entity.ChainNodeApiKey;
import com.chainvault.core.domain.entity.ChainNodeConfig;
import com.chainvault.core.domain.vo.ChainNodeApiKeyVO;
import com.chainvault.core.domain.vo.ChainNodeConfigVO;
import com.chainvault.core.mapper.ChainNodeApiKeyMapper;
import com.chainvault.core.mapper.ChainNodeConfigMapper;
import com.chainvault.core.service.ChainNodeConfigService;
import com.chainvault.core.util.ChainNodeUrlBuilder;
import com.chainvault.core.util.SecretMaskUtil;
import com.chainvault.keyvault.config.KeyVaultProperties;
import com.chainvault.keyvault.util.AesUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * 链节点 Provider 配置业务实现
 *
 * @author chainvault
 * @date 2026-06-05
 */
@Service
@RequiredArgsConstructor
public class ChainNodeConfigServiceImpl implements ChainNodeConfigService {

    private static final DateTimeFormatter DT_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private final ChainNodeConfigMapper chainNodeConfigMapper;
    private final ChainNodeApiKeyMapper chainNodeApiKeyMapper;
    private final ChainNodeProperties chainNodeProperties;
    private final KeyVaultProperties keyVaultProperties;
    private final StringRedisTemplate redis;

    /**
     * 查询全部链节点配置
     *
     * @return 配置列表
     */
    @Transactional(readOnly = true)
    @Override
    public List<ChainNodeConfigVO> listAll() {
        // 1. 查询数据库记录
        List<ChainNodeConfig> records = chainNodeConfigMapper.selectList(
                new LambdaQueryWrapper<ChainNodeConfig>().orderByAsc(ChainNodeConfig::getChainCode));

        // 2. 转换为脱敏视图
        List<ChainNodeConfigVO> result = new ArrayList<>();
        for (ChainNodeConfig record : records) {
            result.add(toVO(record));
        }
        return result;
    }

    /**
     * 查询单链配置
     *
     * @param chainCode 链标识
     * @return 配置视图
     */
    @Transactional(readOnly = true)
    @Override
    public ChainNodeConfigVO getByChainCode(String chainCode) {
        // 1. 查询记录
        ChainNodeConfig record = requireConfig(chainCode);
        return toVO(record);
    }

    /**
     * 更新链节点配置
     *
     * @param chainCode 链标识
     * @param req       更新请求
     * @return 更新后的配置
     */
    @Transactional(rollbackFor = Exception.class)
    @Override
    public ChainNodeConfigVO update(String chainCode, ChainNodeConfigUpdateReq req) {
        // 1. 校验服务商
        ChainNodeProvider provider = ChainNodeProvider.fromCode(req.getProvider());
        ChainNodeConfig record = requireConfig(chainCode);

        // 2. 更新基础字段
        record.setProvider(provider.getCode());
        if (req.getRpcUrl() != null) {
            record.setRpcUrl(req.getRpcUrl().trim());
        }
        if (req.getApiUrl() != null) {
            record.setApiUrl(req.getApiUrl().trim());
        }
        if (req.getRpcUser() != null) {
            record.setRpcUser(req.getRpcUser().trim());
        }
        if (req.getRequiredConfirms() != null) {
            record.setRequiredConfirms(req.getRequiredConfirms());
        }
        if (req.getIsEnabled() != null) {
            record.setIsEnabled(req.getIsEnabled());
        }
        if (req.getRemark() != null) {
            record.setRemark(req.getRemark().trim());
        }

        // 3. 按需更新密钥（留空表示保留原值；写入 Key 池而非覆盖单字段）
        if (StringUtils.hasText(req.getApiKey())) {
            insertApiKeyRecord(chainCode, req.getApiKey().trim(), "手动添加");
            record.setApiKeyEnc(null);
        }
        if (StringUtils.hasText(req.getRpcPassword())) {
            record.setRpcPasswordEnc(encrypt(req.getRpcPassword()));
        }

        // 4. 校验配置完整性
        validateConfig(record, provider, decryptRpcPassword(record));

        // 5. 持久化并通知 Gateway 刷新
        chainNodeConfigMapper.updateById(record);
        publishRefreshEvent();
        return toVO(record);
    }

    /**
     * 解析运行时有效配置
     *
     * @param chainCode 链标识
     * @return 运行时配置
     */
    @Transactional(readOnly = true)
    @Override
    public Optional<ChainNodeSettings> resolveRuntimeSettings(String chainCode) {
        // 1. 优先读取数据库配置
        ChainNodeConfig record = chainNodeConfigMapper.selectOne(
                new LambdaQueryWrapper<ChainNodeConfig>().eq(ChainNodeConfig::getChainCode, chainCode));
        if (record != null) {
            ChainNodeSettings settings = buildRuntimeSettings(record);
            if (settings.isEnabled() && isScanReady(settings)) {
                return Optional.of(settings);
            }
            if (settings.isEnabled()) {
                return Optional.of(settings);
            }
            return Optional.empty();
        }

        // 2. 回退到 application.yml 环境变量配置
        return buildFallbackSettings(chainCode);
    }

    /**
     * 刷新运行时配置
     */
    @Override
    public void refreshRuntime() {
        publishRefreshEvent();
    }

    /**
     * 查询链下全部 API Key
     *
     * @param chainCode 链标识
     * @return Key 列表
     */
    @Transactional(readOnly = true)
    @Override
    public List<ChainNodeApiKeyVO> listApiKeys(String chainCode) {
        requireConfig(chainCode);
        return loadApiKeyVOs(chainCode);
    }

    /**
     * 添加 API Key
     *
     * @param chainCode 链标识
     * @param req       添加请求
     * @return 新增 Key 视图
     */
    @Transactional(rollbackFor = Exception.class)
    @Override
    public ChainNodeApiKeyVO addApiKey(String chainCode, ChainNodeApiKeyAddReq req) {
        // 1. 校验链配置存在（Key 按链存储，不依赖当前已保存的服务商，便于先加 Key 再切换 Infura/Alchemy）
        ChainNodeConfig record = requireConfig(chainCode);

        // 2. 写入 Key 池并清理旧单字段
        ChainNodeApiKey saved = insertApiKeyRecord(chainCode, req.getApiKey().trim(), req.getLabel());
        record.setApiKeyEnc(null);
        chainNodeConfigMapper.updateById(record);
        publishRefreshEvent();
        return toApiKeyVO(saved, req.getApiKey().trim());
    }

    /**
     * 删除 API Key
     *
     * @param chainCode 链标识
     * @param keyId     Key 主键
     */
    @Transactional(rollbackFor = Exception.class)
    @Override
    public void deleteApiKey(String chainCode, Long keyId) {
        // 1. 校验记录归属
        ChainNodeConfig record = requireConfig(chainCode);
        ChainNodeApiKey apiKey = chainNodeApiKeyMapper.selectById(keyId);
        if (apiKey == null || !chainCode.equalsIgnoreCase(apiKey.getChainCode())) {
            throw new BusinessException(404, "API Key 不存在");
        }

        // 2. 删除后仍需满足最少一个 Key（若服务商要求）
        ChainNodeProvider provider = ChainNodeProvider.fromCode(record.getProvider());
        if (provider == ChainNodeProvider.ALCHEMY || provider == ChainNodeProvider.INFURA) {
            long remain = countEnabledApiKeys(chainCode)
                    - (apiKey.getIsEnabled() != null && apiKey.getIsEnabled() == 1 ? 1 : 0);
            if (remain <= 0 && !StringUtils.hasText(record.getApiKeyEnc())) {
                throw new BusinessException(400, "至少保留一个可用 API Key");
            }
        }

        chainNodeApiKeyMapper.deleteById(keyId);
        publishRefreshEvent();
    }

    // 查询配置，不存在则报错
    private ChainNodeConfig requireConfig(String chainCode) {
        ChainNodeConfig record = chainNodeConfigMapper.selectOne(
                new LambdaQueryWrapper<ChainNodeConfig>().eq(ChainNodeConfig::getChainCode, chainCode));
        if (record == null) {
            throw new BusinessException(404, "链节点配置不存在: " + chainCode);
        }
        return record;
    }

    // 实体转脱敏视图
    private ChainNodeConfigVO toVO(ChainNodeConfig record) {
        List<String> apiKeys = loadEnabledApiKeyPlainList(record.getChainCode(), record);
        String primaryKey = apiKeys.isEmpty() ? null : apiKeys.get(0);
        String rpcPassword = decryptRpcPassword(record);
        ChainNodeProvider provider = ChainNodeProvider.fromCode(record.getProvider());
        ChainNodeSettings runtime = buildRuntimeSettings(record);

        ChainNodeConfigVO vo = new ChainNodeConfigVO();
        vo.setChainCode(record.getChainCode());
        vo.setProvider(record.getProvider());
        vo.setRpcUrl(record.getRpcUrl());
        vo.setApiKeyConfigured(!apiKeys.isEmpty());
        vo.setApiKeyCount(apiKeys.size());
        vo.setApiKeys(loadApiKeyVOs(record.getChainCode()));
        vo.setApiKeyMasked(SecretMaskUtil.maskSecret(primaryKey));
        vo.setApiUrl(record.getApiUrl());
        vo.setRpcUser(record.getRpcUser());
        vo.setRpcPasswordConfigured(StringUtils.hasText(rpcPassword));
        vo.setRequiredConfirms(record.getRequiredConfirms());
        vo.setIsEnabled(record.getIsEnabled());
        vo.setRemark(record.getRemark());
        vo.setScanReady(isScanReady(runtime));
        vo.setEffectiveRpcUrlMasked(SecretMaskUtil.maskUrl(runtime.getRpcUrl()));
        if (record.getUpdatedAt() != null) {
            vo.setUpdatedAt(record.getUpdatedAt().format(DT_FMT));
        }
        return vo;
    }

    // 构建运行时配置
    private ChainNodeSettings buildRuntimeSettings(ChainNodeConfig record) {
        List<String> apiKeys = loadEnabledApiKeyPlainList(record.getChainCode(), record);
        String primaryKey = apiKeys.isEmpty() ? null : apiKeys.get(0);
        String rpcPassword = decryptRpcPassword(record);
        ChainNodeProvider provider = ChainNodeProvider.fromCode(record.getProvider());

        ChainNodeSettings settings = new ChainNodeSettings();
        settings.setChainCode(record.getChainCode());
        settings.setProvider(provider.getCode());
        settings.setApiKeys(apiKeys);
        settings.setApiKey(primaryKey);
        settings.setApiUrl(record.getApiUrl());
        settings.setRpcUser(record.getRpcUser());
        settings.setRpcPassword(rpcPassword);
        settings.setEnabled(record.getIsEnabled() != null && record.getIsEnabled() == 1);
        settings.setRequiredConfirms(resolveRequiredConfirms(record));
        settings.setRpcUrl(ChainNodeUrlBuilder.buildRpcUrl(
                record.getChainCode(), provider, primaryKey, record.getRpcUrl()));
        return settings;
    }

    // 回退 YAML 配置
    private Optional<ChainNodeSettings> buildFallbackSettings(String chainCode) {
        ChainNodeSettings settings = new ChainNodeSettings();
        settings.setChainCode(chainCode);
        settings.setProvider(ChainNodeProvider.CUSTOM.getCode());
        settings.setEnabled(true);

        if (ChainCode.ETH.equalsIgnoreCase(chainCode)) {
            settings.setRpcUrl(chainNodeProperties.getEth().getRpcUrl());
            settings.setRequiredConfirms(chainNodeProperties.getEth().getRequiredConfirms());
        } else if (ChainCode.BNB.equalsIgnoreCase(chainCode)) {
            settings.setRpcUrl(chainNodeProperties.getBnb().getRpcUrl());
            settings.setRequiredConfirms(chainNodeProperties.getBnb().getRequiredConfirms());
        } else if (ChainCode.TRON.equalsIgnoreCase(chainCode)) {
            settings.setProvider(ChainNodeProvider.TRONGRID.getCode());
            settings.setApiUrl(chainNodeProperties.getTron().getApiUrl());
            settings.setApiKey(chainNodeProperties.getTron().getApiKey());
            settings.setRequiredConfirms(chainNodeProperties.getTron().getRequiredConfirms());
        } else if (ChainCode.BTC.equalsIgnoreCase(chainCode)) {
            settings.setProvider(ChainNodeProvider.BITCOIN_CORE.getCode());
            settings.setRpcUrl(chainNodeProperties.getBtc().getRpcUrl());
            settings.setRpcUser(chainNodeProperties.getBtc().getRpcUser());
            settings.setRpcPassword(chainNodeProperties.getBtc().getRpcPassword());
            settings.setRequiredConfirms(chainNodeProperties.getBtc().getRequiredConfirms());
        } else {
            return Optional.empty();
        }

        if (!isScanReady(settings)) {
            return Optional.empty();
        }
        return Optional.of(settings);
    }

    // 判断是否满足扫块条件
    private boolean isScanReady(ChainNodeSettings settings) {
        if (!settings.isEnabled()) {
            return false;
        }
        if (ChainCode.TRON.equalsIgnoreCase(settings.getChainCode())) {
            return StringUtils.hasText(settings.getApiUrl());
        }
        return StringUtils.hasText(settings.getRpcUrl());
    }

    // 解析确认数
    private int resolveRequiredConfirms(ChainNodeConfig record) {
        if (record.getRequiredConfirms() != null) {
            return record.getRequiredConfirms();
        }
        return switch (record.getChainCode().toUpperCase()) {
            case ChainCode.ETH -> chainNodeProperties.getEth().getRequiredConfirms();
            case ChainCode.BNB -> chainNodeProperties.getBnb().getRequiredConfirms();
            case ChainCode.TRON -> chainNodeProperties.getTron().getRequiredConfirms();
            case ChainCode.BTC -> chainNodeProperties.getBtc().getRequiredConfirms();
            default -> 12;
        };
    }

    // 校验配置
    private void validateConfig(ChainNodeConfig record,
                                ChainNodeProvider provider,
                                String rpcPassword) {
        if (provider == ChainNodeProvider.ALCHEMY || provider == ChainNodeProvider.INFURA) {
            if (!hasAnyApiKey(record.getChainCode(), record)) {
                throw new BusinessException(400, provider.getDesc() + " 必须配置至少一个 API Key");
            }
        }
        if (provider == ChainNodeProvider.CUSTOM || provider == ChainNodeProvider.BITCOIN_CORE) {
            if (!StringUtils.hasText(record.getRpcUrl())) {
                throw new BusinessException(400, "请填写 RPC 地址");
            }
        }
        if (provider == ChainNodeProvider.TRONGRID) {
            if (!StringUtils.hasText(record.getApiUrl())) {
                throw new BusinessException(400, "请填写 TronGrid API 地址");
            }
        }
        if (provider == ChainNodeProvider.BITCOIN_CORE && StringUtils.hasText(record.getRpcUser())) {
            if (!StringUtils.hasText(rpcPassword)) {
                throw new BusinessException(400, "请填写 BTC RPC 密码");
            }
        }
    }

    // 发布 Redis 刷新事件
    private void publishRefreshEvent() {
        redis.convertAndSend(ConfigConstants.CHAIN_NODE_REFRESH_CHANNEL, "refresh");
    }

    // 加密敏感字段
    private String encrypt(String plain) {
        return AesUtil.encrypt(plain, keyVaultProperties.getEncryptKey());
    }

    // 解密 API Key
    private String decryptApiKey(ChainNodeConfig record) {
        if (!StringUtils.hasText(record.getApiKeyEnc())) {
            return null;
        }
        return AesUtil.decrypt(record.getApiKeyEnc(), keyVaultProperties.getEncryptKey());
    }

    // 解密 RPC 密码
    private String decryptRpcPassword(ChainNodeConfig record) {
        if (!StringUtils.hasText(record.getRpcPasswordEnc())) {
            return null;
        }
        return AesUtil.decrypt(record.getRpcPasswordEnc(), keyVaultProperties.getEncryptKey());
    }

    // 加载启用的 API Key 明文列表
    private List<String> loadEnabledApiKeyPlainList(String chainCode, ChainNodeConfig record) {
        List<ChainNodeApiKey> rows = chainNodeApiKeyMapper.selectList(
                new LambdaQueryWrapper<ChainNodeApiKey>()
                        .eq(ChainNodeApiKey::getChainCode, chainCode)
                        .eq(ChainNodeApiKey::getIsEnabled, 1)
                        .orderByAsc(ChainNodeApiKey::getSortOrder)
                        .orderByAsc(ChainNodeApiKey::getId));

        List<String> keys = new ArrayList<>();
        for (ChainNodeApiKey row : rows) {
            String plain = decryptApiKeyEnc(row.getApiKeyEnc());
            if (StringUtils.hasText(plain)) {
                keys.add(plain);
            }
        }

        // 兼容旧版单字段
        if (keys.isEmpty()) {
            String legacy = decryptApiKey(record);
            if (StringUtils.hasText(legacy)) {
                keys.add(legacy);
            }
        }
        return keys;
    }

    // 加载 API Key 脱敏视图列表
    private List<ChainNodeApiKeyVO> loadApiKeyVOs(String chainCode) {
        List<ChainNodeApiKey> rows = chainNodeApiKeyMapper.selectList(
                new LambdaQueryWrapper<ChainNodeApiKey>()
                        .eq(ChainNodeApiKey::getChainCode, chainCode)
                        .orderByAsc(ChainNodeApiKey::getSortOrder)
                        .orderByAsc(ChainNodeApiKey::getId));

        List<ChainNodeApiKeyVO> result = new ArrayList<>();
        for (ChainNodeApiKey row : rows) {
            String plain = decryptApiKeyEnc(row.getApiKeyEnc());
            result.add(toApiKeyVO(row, plain));
        }
        return result;
    }

    // Key 实体转脱敏视图
    private ChainNodeApiKeyVO toApiKeyVO(ChainNodeApiKey row, String plain) {
        ChainNodeApiKeyVO vo = new ChainNodeApiKeyVO();
        vo.setId(row.getId());
        vo.setApiKeyMasked(SecretMaskUtil.maskSecret(plain));
        vo.setLabel(row.getLabel());
        vo.setIsEnabled(row.getIsEnabled());
        vo.setSortOrder(row.getSortOrder());
        if (row.getCreatedAt() != null) {
            vo.setCreatedAt(row.getCreatedAt().format(DT_FMT));
        }
        return vo;
    }

    // 写入 API Key 记录
    private ChainNodeApiKey insertApiKeyRecord(String chainCode, String plainKey, String label) {
        ChainNodeApiKey entity = new ChainNodeApiKey();
        entity.setChainCode(chainCode.toUpperCase());
        entity.setApiKeyEnc(encrypt(plainKey));
        entity.setLabel(StringUtils.hasText(label) ? label.trim() : null);
        entity.setIsEnabled(1);
        entity.setSortOrder(nextSortOrder(chainCode));
        chainNodeApiKeyMapper.insert(entity);
        return entity;
    }

    // 计算下一个排序号
    private int nextSortOrder(String chainCode) {
        List<ChainNodeApiKey> rows = chainNodeApiKeyMapper.selectList(
                new LambdaQueryWrapper<ChainNodeApiKey>()
                        .eq(ChainNodeApiKey::getChainCode, chainCode)
                        .orderByDesc(ChainNodeApiKey::getSortOrder)
                        .last("LIMIT 1"));
        if (rows.isEmpty() || rows.get(0).getSortOrder() == null) {
            return 0;
        }
        return rows.get(0).getSortOrder() + 1;
    }

    // 统计启用的 API Key 数量
    private long countEnabledApiKeys(String chainCode) {
        return chainNodeApiKeyMapper.selectCount(
                new LambdaQueryWrapper<ChainNodeApiKey>()
                        .eq(ChainNodeApiKey::getChainCode, chainCode)
                        .eq(ChainNodeApiKey::getIsEnabled, 1));
    }

    // 判断是否存在可用 API Key
    private boolean hasAnyApiKey(String chainCode, ChainNodeConfig record) {
        if (countEnabledApiKeys(chainCode) > 0) {
            return true;
        }
        return StringUtils.hasText(decryptApiKey(record));
    }

    // 解密 API Key 密文
    private String decryptApiKeyEnc(String enc) {
        if (!StringUtils.hasText(enc)) {
            return null;
        }
        return AesUtil.decrypt(enc, keyVaultProperties.getEncryptKey());
    }
}
