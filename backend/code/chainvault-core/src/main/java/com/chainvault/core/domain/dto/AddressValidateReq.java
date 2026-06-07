package com.chainvault.core.domain.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * 地址校验请求
 *
 * @author chainvault
 * @date 2026-06-05
 */
@Data
public class AddressValidateReq {

    /** 链标识 */
    @NotBlank(message = "chainCode 不能为空")
    private String chainCode;

    /** 待校验地址 */
    @NotBlank(message = "address 不能为空")
    private String address;
}
