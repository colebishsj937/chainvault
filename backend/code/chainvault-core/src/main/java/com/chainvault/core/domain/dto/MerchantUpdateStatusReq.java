package com.chainvault.core.domain.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * 商户状态更新请求
 *
 * @author chainvault
 * @date 2026-06-05
 */
@Data
public class MerchantUpdateStatusReq {

    /**
     * 商户状态：0=禁用 1=正常 2=冻结
     *
     * @see com.chainvault.common.enums.MerchantStatus
     */
    @NotNull(message = "状态不能为空")
    private Integer status;
}
