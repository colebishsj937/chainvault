package com.chainvault.core.scheduler;

import com.chainvault.core.service.DepositIngestService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * 充值扫块定时任务
 *
 * @author chainvault
 * @date 2026-06-05
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class DepositScanScheduler {

    private final DepositIngestService depositIngestService;

    /**
     * 每 5 秒扫描各链新区块
     */
    @Scheduled(fixedDelay = 5000)
    public void scanDeposits() {
        depositIngestService.scanAllChains();
    }
}
