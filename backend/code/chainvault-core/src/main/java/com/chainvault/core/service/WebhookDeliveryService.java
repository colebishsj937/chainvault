package com.chainvault.core.service;

/**
 * Webhook 异步投递业务接口
 *
 * @author chainvault
 * @date 2026-06-05
 */
public interface WebhookDeliveryService {

    /**
     * 消费主队列中的一条消息
     */
    void processPending();

    /**
     * 消费到期重试队列
     */
    void processRetries();
}
