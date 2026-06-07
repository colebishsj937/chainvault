package com.chainvault.core.domain.dto;

import lombok.Data;

/**
 * 交易记录分页查询条件
 *
 * @author chainvault
 * @date 2026-06-05
 */
@Data
public class TransactionQueryReq {

    /** 页码，从 1 开始 */
    private int page = 1;

    /** 每页条数 */
    private int size = 20;

    /** 商户号 */
    private String merchantId;

    /** 币种标识 */
    private String coinType;

    /** 链标识 */
    private String chainCode;

    /**
     * 交易类型：1=充值 2=提币
     * @see com.chainvault.common.enums.TxType
     */
    private Integer txType;

    /**
     * 交易状态
     * @see com.chainvault.common.enums.TransactionStatus
     */
    private Integer status;

    /** 平台交易 ID */
    private String tradeId;

    /** 链上 Hash */
    private String txHash;

    /** 商户业务 ID */
    private String bizId;

    /** 开始日期 yyyy-MM-dd */
    private String startDate;

    /** 结束日期 yyyy-MM-dd */
    private String endDate;
}
