package com.chainvault.chainnode.dto;

import lombok.Data;

/**
 * 链上转账事件 DTO
 *
 * @author chainvault
 * @date 2026-06-05
 */
@Data
public class ChainTransferEvent {

    /** 链标识 */
    private String chainCode;

    /** 币种标识 */
    private String coinType;

    /** 来源地址 */
    private String fromAddress;

    /** 目标地址 */
    private String toAddress;

    /** 原始链上金额 */
    private String rawAmount;

    /** 交易 Hash */
    private String txHash;

    /** 区块高度 */
    private long blockNumber;
}
