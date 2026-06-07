package com.chainvault.core.domain.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;

/**
 * 单笔提币请求
 *
 * @author chainvault
 * @date 2026-06-05
 */
@Data
public class WithdrawSubmitReq {

    /** 商户号 */
    @NotBlank(message = "merchantId 不能为空")
    private String merchantId;

    /** 商户业务幂等键 */
    @NotBlank(message = "bizId 不能为空")
    private String bizId;

    /** 币种标识 */
    @NotBlank(message = "coinType 不能为空")
    private String coinType;

    /** 目标地址 */
    @NotBlank(message = "toAddress 不能为空")
    private String toAddress;

    /** 提币金额 */
    @NotNull(message = "amount 不能为空")
    @DecimalMin(value = "0", inclusive = false, message = "amount 必须大于 0")
    private BigDecimal amount;

    /** 备注/tag */
    private String memo;

    /** 矿工费档位：fast/normal/slow */
    private String feeLevel;
}
