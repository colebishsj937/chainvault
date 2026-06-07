package com.chainvault.core.scheduler;

import com.chainvault.core.service.SweepBroadcastService;
import com.chainvault.core.service.SweepService;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * 资金归集定时任务
 *
 * @author chainvault
 * @date 2026-06-05
 */
@Component
@RequiredArgsConstructor
public class SweepScheduler {

    private final SweepService sweepService;
    private final SweepBroadcastService sweepBroadcastService;

    /**
     * 每 5 分钟扫描可归集地址
     */
    @Scheduled(fixedDelay = 300000)
    public void scanSweepCandidates() {
        sweepService.scheduledScan();
    }

    /**
     * 每 3 秒消费一笔归集广播
     */
    @Scheduled(fixedDelay = 3000)
    public void broadcastSweep() {
        sweepBroadcastService.processNext();
    }
}
