package com.chainvault.core.scheduler;

import com.chainvault.core.service.WebhookDeliveryService;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Webhook 投递定时任务
 *
 * @author chainvault
 * @date 2026-06-05
 */
@Component
@RequiredArgsConstructor
public class WebhookDeliveryScheduler {

    private final WebhookDeliveryService webhookDeliveryService;

    /**
     * 每 2 秒消费一条 Webhook 主队列
     */
    @Scheduled(fixedDelay = 2000)
    public void deliverPending() {
        webhookDeliveryService.processPending();
    }

    /**
     * 每 5 秒处理到期重试
     */
    @Scheduled(fixedDelay = 5000)
    public void deliverRetries() {
        webhookDeliveryService.processRetries();
    }
}
