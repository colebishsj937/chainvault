package com.chainvault.core.domain.vo;

import com.chainvault.core.domain.entity.WebhookConfig;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * Webhook 配置视图
 *
 * @author chainvault
 * @date 2026-06-05
 */
@Data
public class WebhookVO {

    private Long id;
    private String merchantId;
    private String eventType;
    private String callbackUrl;
    private Integer isEnabled;
    private Integer retryTimes;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    /**
     * 从实体转换（不暴露 secretKey）
     *
     * @param config 配置实体
     * @return 视图对象
     */
    public static WebhookVO from(WebhookConfig config) {
        WebhookVO vo = new WebhookVO();
        vo.setId(config.getId());
        vo.setMerchantId(config.getMerchantId());
        vo.setEventType(config.getEventType());
        vo.setCallbackUrl(config.getCallbackUrl());
        vo.setIsEnabled(config.getIsEnabled());
        vo.setRetryTimes(config.getRetryTimes());
        vo.setCreatedAt(config.getCreatedAt());
        vo.setUpdatedAt(config.getUpdatedAt());
        return vo;
    }
}
