package com.chainvault.core.domain.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * Webhook 测试推送请求
 *
 * @author chainvault
 * @date 2026-06-05
 */
@Data
public class AdminWebhookTestReq {

    /** Webhook 标识 */
    @NotBlank(message = "webhookId 不能为空")
    private String webhookId;

    /** 事件类型 */
    @NotBlank(message = "eventType 不能为空")
    private String eventType;

    /** 测试 JSON 载荷 */
    private String payload;
}
