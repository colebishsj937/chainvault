package com.chainvault.core.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.chainvault.common.enums.MerchantStatus;
import com.chainvault.common.exception.BusinessException;
import com.chainvault.common.result.PageResult;
import com.chainvault.core.domain.dto.MerchantCreateReq;
import com.chainvault.core.domain.entity.Merchant;
import com.chainvault.core.domain.vo.MerchantCredentialVO;
import com.chainvault.core.domain.vo.MerchantVO;
import com.chainvault.core.mapper.MerchantMapper;
import com.chainvault.core.service.MerchantService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.util.List;
import java.util.UUID;

/**
 * 商户业务实现
 *
 * @author chainvault
 * @date 2026-06-05
 */
@Service
@RequiredArgsConstructor
public class MerchantServiceImpl implements MerchantService {

    private static final String MERCHANT_SEQ_KEY = "cv:seq:merchant";
    private static final long MERCHANT_ID_BASE = 300000L;

    private final MerchantMapper merchantMapper;
    private final StringRedisTemplate redis;

    /**
     * 根据 API Key 查询商户
     *
     * @param apiKey API Key
     * @return 商户信息
     */
    @Transactional(readOnly = true)
    @Override
    public Merchant getByApiKey(String apiKey) {
        // 1. 按 api_key 查询
        Merchant merchant = merchantMapper.selectOne(
                new LambdaQueryWrapper<Merchant>().eq(Merchant::getApiKey, apiKey));

        // 2. 校验存在性与状态
        if (merchant == null) {
            throw new BusinessException(401, "无效的 API Key");
        }
        if (merchant.getStatus() != MerchantStatus.ACTIVE.getCode()) {
            throw new BusinessException(403, "商户已被禁用或冻结");
        }
        return merchant;
    }

    /**
     * 获取商户签名密钥
     *
     * @param apiKey API Key
     * @return 签名密钥
     */
    @Transactional(readOnly = true)
    @Override
    public String getSecretKey(String apiKey) {
        Merchant merchant = getByApiKey(apiKey);
        return merchant.getSecretKey();
    }

    /**
     * 注册新商户
     *
     * @param req 注册请求
     * @return 商户凭证
     */
    @Transactional(rollbackFor = Exception.class)
    @Override
    public MerchantCredentialVO create(MerchantCreateReq req) {
        // 1. 生成商户号与凭证
        String merchantId = nextMerchantId();
        String apiKey = generateApiKey();
        String secretKey = generateSecretKey();

        // 2. 落库
        Merchant entity = new Merchant();
        entity.setMerchantId(merchantId);
        entity.setMerchantName(req.getMerchantName());
        entity.setApiKey(apiKey);
        entity.setSecretKey(secretKey);
        entity.setCallbackUrl(req.getCallbackUrl());
        entity.setIpWhitelist(req.getIpWhitelist());
        entity.setStatus(MerchantStatus.ACTIVE.getCode());
        entity.setTier(req.getTier() == null ? 0 : req.getTier());
        merchantMapper.insert(entity);

        // 3. 返回凭证（密钥仅展示一次）
        MerchantCredentialVO result = new MerchantCredentialVO();
        result.setMerchant(MerchantVO.from(entity));
        result.setSecretKey(secretKey);
        return result;
    }

    /**
     * 根据商户号查询
     *
     * @param merchantId 商户号
     * @return 商户信息
     */
    @Transactional(readOnly = true)
    @Override
    public MerchantVO getByMerchantId(String merchantId) {
        Merchant merchant = findByMerchantIdOrThrow(merchantId);
        return MerchantVO.from(merchant);
    }

    /**
     * 分页查询商户列表
     *
     * @param page 页码
     * @param size 每页条数
     * @return 分页结果
     */
    @Transactional(readOnly = true)
    @Override
    public PageResult<MerchantVO> list(int page, int size) {
        // 1. 分页查询
        Page<Merchant> pageResult = merchantMapper.selectPage(
                new Page<>(page, size),
                new LambdaQueryWrapper<Merchant>().orderByDesc(Merchant::getId));

        // 2. 转换视图
        List<MerchantVO> records = pageResult.getRecords().stream()
                .map(MerchantVO::from)
                .toList();
        return PageResult.of(page, size, pageResult.getTotal(), records);
    }

    /**
     * 更新商户状态
     *
     * @param merchantId 商户号
     * @param status     目标状态
     * @return 更新后的商户信息
     */
    @Transactional(rollbackFor = Exception.class)
    @Override
    public MerchantVO updateStatus(String merchantId, Integer status) {
        // 1. 校验状态枚举
        if (!isValidStatus(status)) {
            throw new BusinessException(400, "无效的商户状态");
        }

        // 2. 更新状态
        Merchant merchant = findByMerchantIdOrThrow(merchantId);
        merchant.setStatus(status);
        merchantMapper.updateById(merchant);
        return MerchantVO.from(merchant);
    }

    /**
     * 轮换商户签名密钥
     *
     * @param merchantId 商户号
     * @return 新凭证
     */
    @Transactional(rollbackFor = Exception.class)
    @Override
    public MerchantCredentialVO rotateSecret(String merchantId) {
        // 1. 生成新密钥
        Merchant merchant = findByMerchantIdOrThrow(merchantId);
        String newSecret = generateSecretKey();
        merchant.setSecretKey(newSecret);
        merchantMapper.updateById(merchant);

        // 2. 返回新凭证
        MerchantCredentialVO result = new MerchantCredentialVO();
        result.setMerchant(MerchantVO.from(merchant));
        result.setSecretKey(newSecret);
        return result;
    }

    // 按商户号查询，不存在则抛异常
    private Merchant findByMerchantIdOrThrow(String merchantId) {
        Merchant merchant = merchantMapper.selectOne(
                new LambdaQueryWrapper<Merchant>().eq(Merchant::getMerchantId, merchantId));
        if (merchant == null) {
            throw new BusinessException(404, "商户不存在");
        }
        return merchant;
    }

    // 校验状态值是否合法
    private boolean isValidStatus(Integer status) {
        if (status == null) {
            return false;
        }
        for (MerchantStatus item : MerchantStatus.values()) {
            if (item.getCode() == status) {
                return true;
            }
        }
        return false;
    }

    // 生成商户号（Redis 序号与库内最大商户号对齐，避免与种子数据冲突）
    private String nextMerchantId() {
        syncMerchantSeqIfNeeded();
        Long seq = redis.opsForValue().increment(MERCHANT_SEQ_KEY);
        long safeSeq = seq == null ? 1L : seq;
        return String.valueOf(MERCHANT_ID_BASE + safeSeq);
    }

    // 将 Redis 序号同步到不低于数据库当前最大商户号偏移
    private void syncMerchantSeqIfNeeded() {
        Merchant maxMerchant = merchantMapper.selectOne(
                new LambdaQueryWrapper<Merchant>()
                        .orderByDesc(Merchant::getMerchantId)
                        .last("LIMIT 1"));
        long dbOffset = 0L;
        if (maxMerchant != null && maxMerchant.getMerchantId() != null) {
            dbOffset = Long.parseLong(maxMerchant.getMerchantId()) - MERCHANT_ID_BASE;
        }

        String current = redis.opsForValue().get(MERCHANT_SEQ_KEY);
        if (current == null) {
            redis.opsForValue().setIfAbsent(MERCHANT_SEQ_KEY, String.valueOf(dbOffset));
            return;
        }

        long redisOffset = Long.parseLong(current);
        if (redisOffset < dbOffset) {
            redis.opsForValue().set(MERCHANT_SEQ_KEY, String.valueOf(dbOffset));
        }
    }

    // 生成 API Key
    private String generateApiKey() {
        return "cv_ak_" + UUID.randomUUID().toString().replace("-", "");
    }

    // 生成签名密钥
    private String generateSecretKey() {
        byte[] bytes = new byte[16];
        new SecureRandom().nextBytes(bytes);
        StringBuilder sb = new StringBuilder("cv_sk_");
        for (byte b : bytes) {
            sb.append(String.format("%02x", b));
        }
        return sb.toString();
    }
}
