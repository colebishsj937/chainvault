package com.chainvault.core.domain.dto;

import lombok.Data;

/**
 * 归集明细查询条件
 */
@Data
public class SweepRecordQueryReq {

    private int page = 1;
    private int size = 20;
    private String batchNo;
    private String merchantId;
    private String chainCode;
    private String coinType;
    private String fromAddress;
    private Integer status;
}
