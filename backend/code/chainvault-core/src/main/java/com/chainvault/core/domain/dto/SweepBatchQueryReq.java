package com.chainvault.core.domain.dto;

import lombok.Data;

/**
 * 归集批次查询条件
 */
@Data
public class SweepBatchQueryReq {

    private int page = 1;
    private int size = 20;
    private String merchantId;
    private String chainCode;
    private String coinType;
    private Integer status;
    private String startDate;
    private String endDate;
}
