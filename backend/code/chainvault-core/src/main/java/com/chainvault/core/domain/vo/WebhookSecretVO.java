package com.chainvault.core.domain.vo;

import lombok.Data;

/**
 * Webhook 密钥轮换结果
 *
 * @author chainvault
 * @date 2026-06-05
 */
@Data
public class WebhookSecretVO {

    /** 配置 ID */
    private Long id;

    /** 商户号 */
    private String merchantId;

    /** 事件类型 */
    private String eventType;

    /** 新密钥（仅轮换时返回一次） */
    private String secretKey;
}
