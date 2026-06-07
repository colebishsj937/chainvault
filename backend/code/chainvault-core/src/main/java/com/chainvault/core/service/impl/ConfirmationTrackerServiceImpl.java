package com.chainvault.core.service.impl;

import com.chainvault.chainnode.service.BlockScanner;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.chainvault.common.constants.QueueNames;
import com.chainvault.common.constants.WebhookEvents;
import com.chainvault.common.dto.WebhookQueueMessage;
import com.chainvault.common.enums.TransactionStatus;
import com.chainvault.common.redis.RedisMessageQueue;
import com.chainvault.core.domain.entity.SweepRecord;
import com.chainvault.core.domain.entity.TransactionRecord;
import com.chainvault.core.mapper.SweepRecordMapper;
import com.chainvault.core.service.ConfirmationTrackerService;
import com.chainvault.core.service.HotWalletService;
import com.chainvault.core.service.SweepRecordService;
import com.chainvault.core.service.TransactionRecordService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 充值确认数追踪业务实现
 *
 * @author chainvault
 * @date 2026-06-05
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class ConfirmationTrackerServiceImpl implements ConfirmationTrackerService {

    private final List<BlockScanner> blockScanners;
    private final TransactionRecordService transactionRecordService;
    private final HotWalletService hotWalletService;
    private final RedisMessageQueue messageQueue;
    private final SweepRecordService sweepRecordService;
    private final SweepRecordMapper sweepRecordMapper;

    /**
     * 追踪所有链处理中充值的确认数
     */
    @Override
    public void trackAllChains() {
        Map<String, BlockScanner> scannerMap = new HashMap<>();
        for (BlockScanner scanner : blockScanners) {
            if (scanner.enabled()) {
                scannerMap.put(scanner.chainCode(), scanner);
            }
        }

        // 1. 逐链更新确认数
        for (Map.Entry<String, BlockScanner> entry : scannerMap.entrySet()) {
            trackChain(entry.getKey(), entry.getValue());
        }
    }

    // 追踪单链确认数
    private void trackChain(String chainCode, BlockScanner scanner) {
        try {
            long latestBlock = scanner.latestBlockNumber();

            // 1. 充值确认
            List<TransactionRecord> depositList =
                    transactionRecordService.listProcessingDeposits(chainCode);
            for (TransactionRecord record : depositList) {
                processDepositRecord(record, latestBlock);
            }

            // 2. 归集确认
            List<TransactionRecord> sweepList =
                    transactionRecordService.listProcessingSweeps(chainCode);
            for (TransactionRecord record : sweepList) {
                processSweepRecord(record, latestBlock);
            }
        } catch (Exception e) {
            log.error("[{}] 确认数追踪失败", chainCode, e);
        }
    }

    // 处理单笔充值确认
    @Transactional(rollbackFor = Exception.class)
    protected void processDepositRecord(TransactionRecord record, long latestBlock) {
        if (record.getBlockNumber() == null) {
            return;
        }

        // 1. 计算当前确认数
        int confirms = (int) (latestBlock - record.getBlockNumber() + 1);
        if (confirms < 0) {
            confirms = 0;
        }
        transactionRecordService.updateConfirms(record.getId(), confirms);

        // 2. 未达标则结束
        if (confirms < record.getRequiredConfirms()) {
            return;
        }

        // 3. 入账并标记成功
        hotWalletService.addBalance(record.getMerchantId(), record.getCoinType(), record.getAmount());
        transactionRecordService.updateStatus(record.getId(), TransactionStatus.SUCCESS.getCode());

        // 4. 推送 deposit.confirmed 事件
        WebhookQueueMessage message = new WebhookQueueMessage();
        message.setEvent(WebhookEvents.DEPOSIT_CONFIRMED);
        message.setMerchantId(record.getMerchantId());
        message.setTradeId(record.getTradeId());
        message.setCoinType(record.getCoinType());
        message.setChainCode(record.getChainCode());
        message.setTxHash(record.getTxHash());
        message.setAmount(record.getAmount().toPlainString());
        message.setToAddress(record.getToAddress());
        message.setConfirms(confirms);
        message.setRequiredConfirms(record.getRequiredConfirms());
        message.setBizId(record.getBizId());
        message.setAttempt(0);
        messageQueue.push(QueueNames.WEBHOOK, message);

        log.info("[{}] 充值确认 tradeId={} amount={} {}",
                record.getChainCode(), record.getTradeId(),
                record.getAmount(), record.getCoinType());
    }

    // 处理单笔归集确认
    @Transactional(rollbackFor = Exception.class)
    protected void processSweepRecord(TransactionRecord record, long latestBlock) {
        if (record.getBlockNumber() == null) {
            return;
        }

        // 1. 计算当前确认数
        int confirms = (int) (latestBlock - record.getBlockNumber() + 1);
        if (confirms < 0) {
            confirms = 0;
        }
        transactionRecordService.updateConfirms(record.getId(), confirms);

        SweepRecord sweepRecord = sweepRecordMapper.selectOne(
                new LambdaQueryWrapper<SweepRecord>()
                        .eq(SweepRecord::getTradeId, record.getTradeId())
                        .last("LIMIT 1"));
        if (sweepRecord != null) {
            sweepRecordService.updateConfirms(sweepRecord.getId(), confirms);
        }

        // 2. 未达标则结束
        if (confirms < record.getRequiredConfirms()) {
            return;
        }

        // 3. 标记链上交易与归集明细成功（归集不入账 hot_wallet，不推 Webhook）
        transactionRecordService.updateStatus(record.getId(), TransactionStatus.SUCCESS.getCode());
        if (sweepRecord != null) {
            sweepRecordService.markSuccess(sweepRecord.getId(), confirms);
        }

        log.info("[{}] 归集确认 tradeId={} amount={} {}",
                record.getChainCode(), record.getTradeId(),
                record.getAmount(), record.getCoinType());
    }
}
