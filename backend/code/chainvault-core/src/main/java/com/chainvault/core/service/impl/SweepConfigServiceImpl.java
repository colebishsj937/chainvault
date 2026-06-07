package com.chainvault.core.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.chainvault.common.exception.BusinessException;
import com.chainvault.core.config.CoreProperties;
import com.chainvault.core.domain.dto.SweepCoinThresholdUpdateReq;
import com.chainvault.core.domain.dto.SweepConfigUpdateReq;
import com.chainvault.core.domain.entity.CoinConfig;
import com.chainvault.core.domain.entity.SweepConfig;
import com.chainvault.core.domain.vo.SweepCoinThresholdVO;
import com.chainvault.core.domain.vo.SweepConfigVO;
import com.chainvault.core.mapper.CoinConfigMapper;
import com.chainvault.core.mapper.SweepConfigMapper;
import com.chainvault.core.service.SweepConfigService;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

/**
 * 归集配置业务实现
 *
 * @author chainvault
 * @date 2026-06-07
 */
@Service
@RequiredArgsConstructor
public class SweepConfigServiceImpl implements SweepConfigService {

    private static final long CONFIG_ID = 1L;
    private static final DateTimeFormatter DT_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    private static final String THRESHOLD_FORMULA = "归集阈值 = 最小充值(min_deposit) × 阈值倍数";

    private final SweepConfigMapper sweepConfigMapper;
    private final CoinConfigMapper coinConfigMapper;
    private final CoreProperties coreProperties;

    private volatile int cachedMultiplier = 5;
    private volatile boolean cachedSweepEnabled = true;

    /**
     * 启动时加载数据库配置到内存
     */
    @PostConstruct
    public void initCache() {
        refreshCache(loadOrInitConfig());
    }

    /**
     * 查询归集全局配置
     *
     * @return 配置视图
     */
    @Transactional(readOnly = true)
    @Override
    public SweepConfigVO getConfig() {
        return toConfigVO(loadOrInitConfig());
    }

    /**
     * 更新归集全局配置
     *
     * @param req 更新请求
     * @return 更新后的配置
     */
    @Transactional(rollbackFor = Exception.class)
    @Override
    public SweepConfigVO updateConfig(SweepConfigUpdateReq req) {
        // 1. 读取或初始化配置行
        SweepConfig record = loadOrInitConfig();

        // 2. 更新字段
        record.setSweepEnabled(req.getSweepEnabled());
        record.setThresholdMultiplier(req.getThresholdMultiplier());
        sweepConfigMapper.updateById(record);

        // 3. 刷新运行时缓存
        refreshCache(record);
        return toConfigVO(record);
    }

    /**
     * 查询各币种归集阈值
     *
     * @return 阈值列表
     */
    @Transactional(readOnly = true)
    @Override
    public List<SweepCoinThresholdVO> listCoinThresholds() {
        int multiplier = getThresholdMultiplier();
        List<CoinConfig> coins = coinConfigMapper.selectList(
                new LambdaQueryWrapper<CoinConfig>().orderByAsc(CoinConfig::getChainCode, CoinConfig::getCoinType));

        List<SweepCoinThresholdVO> result = new ArrayList<>();
        for (CoinConfig coin : coins) {
            result.add(toCoinThresholdVO(coin, multiplier));
        }
        return result;
    }

    /**
     * 更新币种最小充值
     *
     * @param coinType 币种标识
     * @param req      更新请求
     * @return 更新后的阈值视图
     */
    @Transactional(rollbackFor = Exception.class)
    @Override
    public SweepCoinThresholdVO updateCoinThreshold(String coinType, SweepCoinThresholdUpdateReq req) {
        if (!StringUtils.hasText(coinType)) {
            throw new BusinessException("币种标识不能为空");
        }

        // 1. 查询币种配置
        CoinConfig coin = coinConfigMapper.selectOne(
                new LambdaQueryWrapper<CoinConfig>().eq(CoinConfig::getCoinType, coinType));
        if (coin == null) {
            throw new BusinessException("币种配置不存在");
        }

        // 2. 更新最小充值
        coin.setMinDeposit(req.getMinDeposit());
        coinConfigMapper.updateById(coin);
        return toCoinThresholdVO(coin, getThresholdMultiplier());
    }

    /**
     * 运行时读取阈值倍数
     *
     * @return 倍数
     */
    @Override
    public int getThresholdMultiplier() {
        return cachedMultiplier;
    }

    /**
     * 运行时读取定时归集开关
     *
     * @return 是否启用
     */
    @Override
    public boolean isSweepEnabled() {
        return cachedSweepEnabled;
    }

    // 加载或初始化单行配置
    private SweepConfig loadOrInitConfig() {
        SweepConfig record = sweepConfigMapper.selectById(CONFIG_ID);
        if (record != null) {
            return record;
        }

        SweepConfig init = new SweepConfig();
        init.setId(CONFIG_ID);
        init.setSweepEnabled(coreProperties.isSweepEnabled() ? 1 : 0);
        init.setThresholdMultiplier(coreProperties.getSweepThresholdMultiplier());
        sweepConfigMapper.insert(init);
        return init;
    }

    // 刷新内存缓存
    private void refreshCache(SweepConfig record) {
        if (record.getThresholdMultiplier() != null) {
            cachedMultiplier = record.getThresholdMultiplier();
        } else {
            cachedMultiplier = coreProperties.getSweepThresholdMultiplier();
        }
        cachedSweepEnabled = record.getSweepEnabled() != null && record.getSweepEnabled() == 1;
    }

    // 转全局配置视图
    private SweepConfigVO toConfigVO(SweepConfig record) {
        SweepConfigVO vo = new SweepConfigVO();
        vo.setSweepEnabled(record.getSweepEnabled());
        vo.setThresholdMultiplier(record.getThresholdMultiplier());
        vo.setThresholdFormula(THRESHOLD_FORMULA);
        if (record.getUpdatedAt() != null) {
            vo.setUpdatedAt(record.getUpdatedAt().format(DT_FMT));
        }
        return vo;
    }

    // 转币种阈值视图
    private SweepCoinThresholdVO toCoinThresholdVO(CoinConfig coin, int multiplier) {
        SweepCoinThresholdVO vo = new SweepCoinThresholdVO();
        vo.setCoinType(coin.getCoinType());
        vo.setSymbol(coin.getSymbol());
        vo.setChainCode(coin.getChainCode());
        vo.setIsEnabled(coin.getIsEnabled());
        BigDecimal minDeposit = coin.getMinDeposit() == null ? BigDecimal.ZERO : coin.getMinDeposit();
        vo.setMinDeposit(minDeposit.stripTrailingZeros().toPlainString());
        vo.setSweepThreshold(minDeposit.multiply(BigDecimal.valueOf(multiplier)).stripTrailingZeros().toPlainString());
        return vo;
    }
}
