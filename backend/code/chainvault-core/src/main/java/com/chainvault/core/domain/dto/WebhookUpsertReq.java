package com.chainvault.core.domain.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * Webhook 注册/更新请求
 *
 * @author chainvault
 * @date 2026-06-05
 */
@Data
public class WebhookUpsertReq {

    /** 商户号 */
    @NotBlank(message = "merchantId 不能为空")
    private String merchantId;

    /** 事件类型 */
    @NotBlank(message = "eventType 不能为空")
    private String eventType;

    /** 回调地址 */
    @NotBlank(message = "callbackUrl 不能为空")
    private String callbackUrl;

    /** 自定义密钥（可选，不传则自动生成） */
    private String secretKey;

    /** 是否启用，默认 1 */
    private Integer isEnabled;

    /** 最大重试次数，默认 5 */
    private Integer retryTimes;

    /** 是否轮换密钥（更新时生效） */
    private Boolean rotateSecret;
}
