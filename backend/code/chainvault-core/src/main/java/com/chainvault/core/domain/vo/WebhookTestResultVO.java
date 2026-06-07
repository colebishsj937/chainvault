package com.chainvault.core.domain.vo;

import lombok.Data;

/**
 * Webhook 测试推送结果
 *
 * @author chainvault
 * @date 2026-06-05
 */
@Data
public class WebhookTestResultVO {

    /** 是否 HTTP 2xx */
    private boolean success;

    /** HTTP 状态码，失败时为 0 */
    private int statusCode;

    /** 响应体摘要 */
    private String responseBody;

    /** 耗时毫秒 */
    private long duration;
}
