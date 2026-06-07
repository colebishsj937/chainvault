package com.chainvault.chainnode.service;

import com.chainvault.chainnode.dto.BroadcastRequest;
import com.chainvault.chainnode.dto.BroadcastResult;

/**
 * 链上交易广播接口
 *
 * @author chainvault
 * @date 2026-06-05
 */
public interface TransactionBroadcaster {

    /**
     * 签名并广播提币交易
     *
     * @param request 广播请求
     * @return 广播结果
     */
    BroadcastResult broadcast(BroadcastRequest request);
}
