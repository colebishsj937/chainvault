package com.chainvault.core.domain.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;

/**
 * 币种归集阈值基数更新请求
 *
 * @author chainvault
 * @date 2026-06-07
 */
@Data
public class SweepCoinThresholdUpdateReq {

    /**
     * 最小充值金额（归集阈值基数）
     */
    @NotNull(message = "最小充值金额不能为空")
    @DecimalMin(value = "0", inclusive = false, message = "最小充值金额必须大于 0")
    private BigDecimal minDeposit;
}
