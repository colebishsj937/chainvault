package com.chainvault.core.domain.vo;

import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 运营后台 Webhook 配置视图（按 URL 聚合）
 *
 * @author chainvault
 * @date 2026-06-05
 */
@Data
public class AdminWebhookVO {

    /** 稳定标识，格式 merchantId:eventType */
    private String webhookId;

    /** 商户号 */
    private String merchantId;

    /** 回调地址 */
    private String url;

    /** 脱敏密钥预览 */
    private String secret;

    /** 订阅事件列表 */
    private List<String> events;

    /** 是否启用 */
    private boolean enabled;

    /** 最早创建时间 */
    private LocalDateTime createdAt;
}
