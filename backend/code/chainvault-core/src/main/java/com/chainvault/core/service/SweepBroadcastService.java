package com.chainvault.core.service;

/**
 * 归集广播业务接口
 *
 * @author chainvault
 * @date 2026-06-05
 */
public interface SweepBroadcastService {

    /**
     * 消费归集队列并广播一笔
     */
    void processNext();
}
