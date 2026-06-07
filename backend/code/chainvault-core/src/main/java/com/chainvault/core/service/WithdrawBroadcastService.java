package com.chainvault.core.service;

/**
 * 提币广播业务接口
 *
 * @author chainvault
 * @date 2026-06-05
 */
public interface WithdrawBroadcastService {

    /**
     * 消费广播队列并处理一笔提币
     */
    void processNext();
}
