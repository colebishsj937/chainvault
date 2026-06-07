package com.chainvault.core.domain.vo;

import lombok.Data;

/**
 * 币种归集阈值视图
 *
 * @author chainvault
 * @date 2026-06-07
 */
@Data
public class SweepCoinThresholdVO {

    /** 币种标识 */
    private String coinType;

    /** 显示符号 */
    private String symbol;

    /** 所属链 */
    private String chainCode;

    /** 最小充值（阈值基数） */
    private String minDeposit;

    /** 归集阈值 = minDeposit × multiplier */
    private String sweepThreshold;

    /** 是否启用：0=否 1=是 */
    private Integer isEnabled;
}
