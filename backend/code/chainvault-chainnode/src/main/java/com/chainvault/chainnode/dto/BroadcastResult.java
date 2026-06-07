package com.chainvault.chainnode.dto;

import lombok.Data;

/**
 * 链上广播结果
 *
 * @author chainvault
 * @date 2026-06-05
 */
@Data
public class BroadcastResult {

    /** 链上交易 Hash */
    private String txHash;

    /** 出款地址 */
    private String fromAddress;

    /** 是否模拟广播 */
    private boolean simulated;
}
