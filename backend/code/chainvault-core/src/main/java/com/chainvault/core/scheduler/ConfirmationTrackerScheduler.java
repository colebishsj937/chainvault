package com.chainvault.core.scheduler;

import com.chainvault.core.service.ConfirmationTrackerService;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * 充值确认数追踪定时任务
 *
 * @author chainvault
 * @date 2026-06-05
 */
@Component
@RequiredArgsConstructor
public class ConfirmationTrackerScheduler {

    private final ConfirmationTrackerService confirmationTrackerService;

    /**
     * 每 30 秒更新确认数并触发达标入账
     */
    @Scheduled(fixedDelay = 30_000)
    public void trackConfirmations() {
        confirmationTrackerService.trackAllChains();
    }
}
