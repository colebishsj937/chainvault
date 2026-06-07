package com.chainvault.core.domain.vo;

import lombok.Data;

/**
 * 充值地址归集汇总视图
 */
@Data
public class SweepAddressSummaryVO {

    private String merchantId;
    private String coinType;
    private String chainCode;
    private String address;
    private String totalDeposits;
    private String alreadySwept;
    private String pendingAmount;
    private Integer lastStatus;
    private String lastStatusLabel;
    private String lastRecordNo;
}
