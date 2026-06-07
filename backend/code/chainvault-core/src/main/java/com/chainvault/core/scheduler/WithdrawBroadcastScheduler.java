package com.chainvault.core.scheduler;

import com.chainvault.core.service.WithdrawBroadcastService;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * 提币广播定时任务
 *
 * @author chainvault
 * @date 2026-06-05
 */
@Component
@RequiredArgsConstructor
public class WithdrawBroadcastScheduler {

    private final WithdrawBroadcastService withdrawBroadcastService;

    /**
     * 每 2 秒消费一笔提币广播队列
     */
    @Scheduled(fixedDelay = 2000)
    public void broadcastWithdraw() {
        withdrawBroadcastService.processNext();
    }
}
