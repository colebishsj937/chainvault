package com.chainvault.core.domain.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 商户注册请求
 *
 * @author chainvault
 * @date 2026-06-05
 */
@Data
public class MerchantCreateReq {

    /** 商户名称 */
    @NotBlank(message = "商户名称不能为空")
    @Size(max = 100, message = "商户名称最长 100 字符")
    private String merchantName;

    /** 默认回调地址 */
    @Size(max = 512, message = "回调地址最长 512 字符")
    private String callbackUrl;

    /** IP 白名单，逗号分隔 */
    private String ipWhitelist;

    /** 0=开源版 1=商业版 */
    private Integer tier;
}
