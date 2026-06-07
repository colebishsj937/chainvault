package com.chainvault.chainnode.dto;

import lombok.Data;

import java.math.BigDecimal;

/**
 * 链上广播请求
 *
 * @author chainvault
 * @date 2026-06-05
 */
@Data
public class BroadcastRequest {

    /** 提币单号 */
    private String orderNo;

    /** 商户号 */
    private String merchantId;

    /** 链标识 */
    private String chainCode;

    /** 币种 */
    private String coinType;

    /** 目标地址 */
    private String toAddress;

    /** 金额 */
    private BigDecimal amount;

    /** 备注/tag */
    private String memo;

    /** 矿工费档位 */
    private String feeLevel;

    /** 热钱包 BIP44 路径 */
    private String fromBip44Path;
}
