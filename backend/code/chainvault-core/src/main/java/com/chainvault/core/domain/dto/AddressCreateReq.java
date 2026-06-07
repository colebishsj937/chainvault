package com.chainvault.core.domain.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import lombok.Data;

import java.util.List;

/**
 * 批量生成充值地址请求
 *
 * @author chainvault
 * @date 2026-06-05
 */
@Data
public class AddressCreateReq {

    /** 商户号 */
    @NotBlank(message = "merchantId 不能为空")
    private String merchantId;

    /** 币种标识 */
    @NotBlank(message = "coinType 不能为空")
    private String coinType;

    /** 商户业务 ID 列表 */
    @NotEmpty(message = "bizIds 不能为空")
    private List<String> bizIds;

    /** 可选回调地址 */
    private String callbackUrl;
}
