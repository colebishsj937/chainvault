package com.chainvault.core.domain.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

import java.util.List;

/**
 * 运营后台 Webhook 创建/更新请求
 *
 * @author chainvault
 * @date 2026-06-05
 */
@Data
public class AdminWebhookSaveReq {

    /** 商户号 */
    private String merchantId;

    /** 回调地址 */
    private String url;

    /** 自定义密钥 */
    private String secret;

    /** 事件类型列表 */
    private List<String> events;

    /** 是否启用 */
    private Boolean enabled;
}
