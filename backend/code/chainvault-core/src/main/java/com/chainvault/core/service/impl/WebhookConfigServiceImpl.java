package com.chainvault.core.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.chainvault.common.constants.WebhookConstants;
import com.chainvault.common.exception.BusinessException;
import com.chainvault.core.domain.dto.WebhookUpsertReq;
import com.chainvault.core.domain.entity.Merchant;
import com.chainvault.core.domain.entity.WebhookConfig;
import com.chainvault.core.domain.vo.WebhookSecretVO;
import com.chainvault.core.domain.vo.WebhookVO;
import com.chainvault.core.mapper.MerchantMapper;
import com.chainvault.core.mapper.WebhookConfigMapper;
import com.chainvault.core.service.WebhookConfigService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.security.SecureRandom;
import java.util.List;
import java.util.UUID;

/**
 * Webhook 配置业务实现
 *
 * @author chainvault
 * @date 2026-06-05
 */
@Service
@RequiredArgsConstructor
public class WebhookConfigServiceImpl implements WebhookConfigService {

    private final WebhookConfigMapper webhookConfigMapper;
    private final MerchantMapper merchantMapper;
    private final SecureRandom secureRandom = new SecureRandom();

    /**
     * 注册或更新 Webhook 配置
     *
     * @param req 请求
     * @return 配置结果
     */
    @Transactional(rollbackFor = Exception.class)
    @Override
    public WebhookSecretVO upsert(WebhookUpsertReq req) {
        // 1. 校验商户存在
        Merchant merchant = merchantMapper.selectOne(
                new LambdaQueryWrapper<Merchant>().eq(Merchant::getMerchantId, req.getMerchantId()));
        if (merchant == null) {
            throw new BusinessException("商户不存在: " + req.getMerchantId());
        }

        // 2. 查询已有配置
        WebhookConfig existing = webhookConfigMapper.selectByMerchantEvent(
                req.getMerchantId(), req.getEventType());

        String newSecret = null;
        if (existing == null) {
            // 3. 新建配置
            WebhookConfig created = new WebhookConfig();
            created.setMerchantId(req.getMerchantId());
            created.setEventType(req.getEventType());
            created.setCallbackUrl(req.getCallbackUrl());
            newSecret = StringUtils.hasText(req.getSecretKey())
                    ? req.getSecretKey() : generateSecret();
            created.setSecretKey(newSecret);
            created.setIsEnabled(req.getIsEnabled() == null ? 1 : req.getIsEnabled());
            created.setRetryTimes(resolveRetryTimes(req.getRetryTimes()));
            webhookConfigMapper.insert(created);
            return buildSecretVO(created, newSecret);
        }

        // 4. 更新配置
        existing.setCallbackUrl(req.getCallbackUrl());
        if (req.getIsEnabled() != null) {
            existing.setIsEnabled(req.getIsEnabled());
        }
        if (req.getRetryTimes() != null) {
            existing.setRetryTimes(resolveRetryTimes(req.getRetryTimes()));
        }
        if (Boolean.TRUE.equals(req.getRotateSecret())) {
            newSecret = generateSecret();
            existing.setSecretKey(newSecret);
        } else if (StringUtils.hasText(req.getSecretKey())) {
            newSecret = req.getSecretKey();
            existing.setSecretKey(newSecret);
        }
        webhookConfigMapper.updateById(existing);
        return buildSecretVO(existing, newSecret);
    }

    /**
     * 查询商户全部 Webhook 配置
     *
     * @param merchantId 商户号
     * @return 配置列表
     */
    @Transactional(readOnly = true)
    @Override
    public List<WebhookVO> listByMerchant(String merchantId) {
        List<WebhookConfig> list = webhookConfigMapper.selectList(
                new LambdaQueryWrapper<WebhookConfig>()
                        .eq(WebhookConfig::getMerchantId, merchantId)
                        .orderByAsc(WebhookConfig::getEventType));
        return list.stream().map(WebhookVO::from).toList();
    }

    /**
     * 解析有效回调配置
     *
     * @param merchantId 商户号
     * @param eventType  事件类型
     * @return 配置
     */
    @Transactional(readOnly = true)
    @Override
    public WebhookConfig resolveConfig(String merchantId, String eventType) {
        // 1. 事件级配置
        WebhookConfig config = webhookConfigMapper.selectByMerchantEvent(merchantId, eventType);
        if (config != null && config.getIsEnabled() == 1) {
            return config;
        }

        // 2. 回退商户默认回调
        Merchant merchant = merchantMapper.selectOne(
                new LambdaQueryWrapper<Merchant>().eq(Merchant::getMerchantId, merchantId));
        if (merchant == null || !StringUtils.hasText(merchant.getCallbackUrl())) {
            return null;
        }

        WebhookConfig fallback = new WebhookConfig();
        fallback.setMerchantId(merchantId);
        fallback.setEventType(eventType);
        fallback.setCallbackUrl(merchant.getCallbackUrl());
        fallback.setSecretKey(merchant.getSecretKey());
        fallback.setIsEnabled(1);
        fallback.setRetryTimes(WebhookConstants.MAX_RETRY);
        return fallback;
    }

    // 生成 Webhook 密钥
    private String generateSecret() {
        return "cv_wh_" + UUID.randomUUID().toString().replace("-", "")
                + Integer.toHexString(secureRandom.nextInt(0xFFFF));
    }

    // 解析重试次数上限
    private int resolveRetryTimes(Integer retryTimes) {
        if (retryTimes == null) {
            return WebhookConstants.MAX_RETRY;
        }
        return Math.min(Math.max(retryTimes, 1), WebhookConstants.MAX_RETRY);
    }

    // 组装密钥轮换结果
    private WebhookSecretVO buildSecretVO(WebhookConfig config, String secretKey) {
        WebhookSecretVO vo = new WebhookSecretVO();
        vo.setId(config.getId());
        vo.setMerchantId(config.getMerchantId());
        vo.setEventType(config.getEventType());
        vo.setSecretKey(secretKey);
        return vo;
    }
}
