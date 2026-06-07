package com.chainvault.common.dto;

import lombok.Data;

/**
 * Webhook 投递队列消息体
 *
 * @author chainvault
 * @date 2026-06-05
 */
@Data
public class WebhookQueueMessage {

    /** 事件类型 */
    private String event;

    /** 商户号 */
    private String merchantId;

    /** 平台交易 ID */
    private String tradeId;

    /** 币种标识 */
    private String coinType;

    /** 链标识 */
    private String chainCode;

    /** 链上交易 Hash */
    private String txHash;

    /** 可读金额 */
    private String amount;

    /** 到账地址 */
    private String toAddress;

    /** 当前确认数 */
    private Integer confirms;

    /** 所需确认数 */
    private Integer requiredConfirms;

    /** 商户业务 ID */
    private String bizId;

    /** 当前重试次数 */
    private Integer attempt;
}
