package com.chainvault.common.dto;

import lombok.Data;

import java.math.BigDecimal;

/**
 * 资金归集队列消息
 *
 * @author chainvault
 * @date 2026-06-05
 */
@Data
public class SweepQueueMessage {

    /** 商户号 */
    private String merchantId;

    /** 币种 */
    private String coinType;

    /** 链标识 */
    private String chainCode;

    /** 充值地址 */
    private String fromAddress;

    /** 热钱包地址 */
    private String toAddress;

    /** BIP44 路径 */
    private String bip44Path;

    /** 归集金额 */
    private BigDecimal amount;

    /** 归集明细 ID */
    private Long recordId;

    /** 归集明细号 */
    private String recordNo;

    /** 所属批次 ID */
    private Long batchId;
}
