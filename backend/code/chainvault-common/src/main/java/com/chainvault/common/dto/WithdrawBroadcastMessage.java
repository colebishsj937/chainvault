package com.chainvault.common.dto;

import lombok.Data;

/**
 * 提币广播队列消息
 *
 * @author chainvault
 * @date 2026-06-05
 */
@Data
public class WithdrawBroadcastMessage {

    /** 提币单号 */
    private String orderNo;
}
