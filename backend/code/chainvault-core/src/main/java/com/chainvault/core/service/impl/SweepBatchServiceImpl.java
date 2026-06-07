package com.chainvault.core.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.chainvault.common.enums.SweepBatchStatus;
import com.chainvault.common.enums.SweepRecordStatus;
import com.chainvault.common.enums.SweepTriggerType;
import com.chainvault.common.util.TradeIdGenerator;
import com.chainvault.core.domain.entity.SweepBatch;
import com.chainvault.core.domain.entity.SweepRecord;
import com.chainvault.core.mapper.SweepBatchMapper;
import com.chainvault.core.mapper.SweepRecordMapper;
import com.chainvault.core.service.SweepBatchService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 归集批次业务实现
 */
@Service
@RequiredArgsConstructor
public class SweepBatchServiceImpl implements SweepBatchService {

    private final SweepBatchMapper sweepBatchMapper;
    private final SweepRecordMapper sweepRecordMapper;
    private final TradeIdGenerator tradeIdGenerator;

    /**
     * 创建执行中的归集批次
     */
    @Transactional(rollbackFor = Exception.class)
    @Override
    public SweepBatch createRunningBatch(String merchantId, String chainCode, String coinType,
                                         SweepTriggerType triggerType, String triggerBy) {
        SweepBatch batch = new SweepBatch();
        batch.setBatchNo(tradeIdGenerator.next("SWB"));
        batch.setMerchantId(merchantId);
        batch.setChainCode(chainCode);
        batch.setCoinType(coinType);
        batch.setTriggerType(triggerType.getCode());
        batch.setTriggerBy(triggerBy);
        batch.setStatus(SweepBatchStatus.RUNNING.getCode());
        batch.setScannedCount(0);
        batch.setQueuedCount(0);
        batch.setSuccessCount(0);
        batch.setFailedCount(0);
        batch.setSkippedCount(0);
        sweepBatchMapper.insert(batch);
        return batch;
    }

    /**
     * 按批次号查询
     */
    @Transactional(readOnly = true)
    @Override
    public SweepBatch findByBatchNo(String batchNo) {
        return sweepBatchMapper.selectOne(
                new LambdaQueryWrapper<SweepBatch>()
                        .eq(SweepBatch::getBatchNo, batchNo));
    }

    /**
     * 按 ID 查询
     */
    @Transactional(readOnly = true)
    @Override
    public SweepBatch findById(Long batchId) {
        return sweepBatchMapper.selectById(batchId);
    }

    /**
     * 刷新批次计数并尝试完结
     */
    @Transactional(rollbackFor = Exception.class)
    @Override
    public void refreshAndFinalize(Long batchId) {
        SweepBatch batch = sweepBatchMapper.selectById(batchId);
        if (batch == null) {
            return;
        }

        // 1. 按明细状态重新统计
        List<SweepRecord> records = sweepRecordMapper.selectList(
                new LambdaQueryWrapper<SweepRecord>()
                        .eq(SweepRecord::getBatchId, batchId));

        int queued = 0;
        int success = 0;
        int failed = 0;
        int skipped = 0;
        int inProgress = 0;

        for (SweepRecord record : records) {
            Integer status = record.getStatus();
            if (status == null) {
                continue;
            }
            if (status == SweepRecordStatus.QUEUED.getCode()
                    || status == SweepRecordStatus.BROADCASTING.getCode()
                    || status == SweepRecordStatus.CONFIRMING.getCode()) {
                inProgress++;
                if (status == SweepRecordStatus.QUEUED.getCode()) {
                    queued++;
                }
            } else if (status == SweepRecordStatus.SUCCESS.getCode()) {
                success++;
            } else if (status == SweepRecordStatus.FAILED.getCode()) {
                failed++;
            } else if (status == SweepRecordStatus.SKIPPED.getCode()) {
                skipped++;
            }
        }

        batch.setQueuedCount(queued);
        batch.setSuccessCount(success);
        batch.setFailedCount(failed);
        batch.setSkippedCount(skipped);

        // 2. 仍有进行中明细则保持 RUNNING
        if (inProgress > 0) {
            batch.setStatus(SweepBatchStatus.RUNNING.getCode());
            sweepBatchMapper.updateById(batch);
            return;
        }

        // 3. 全部终态则完结批次
        if (success > 0 && failed == 0) {
            batch.setStatus(SweepBatchStatus.COMPLETED.getCode());
        } else if (success > 0) {
            batch.setStatus(SweepBatchStatus.PARTIAL_FAILED.getCode());
        } else if (failed > 0) {
            batch.setStatus(SweepBatchStatus.FAILED.getCode());
        } else {
            batch.setStatus(SweepBatchStatus.COMPLETED.getCode());
        }
        batch.setCompletedAt(LocalDateTime.now());
        sweepBatchMapper.updateById(batch);
    }

    /**
     * 累加扫描统计
     */
    @Transactional(rollbackFor = Exception.class)
    @Override
    public void addScanStats(Long batchId, int scanned, int queued, int skipped) {
        SweepBatch batch = sweepBatchMapper.selectById(batchId);
        if (batch == null) {
            return;
        }
        batch.setScannedCount(defaultZero(batch.getScannedCount()) + scanned);
        batch.setQueuedCount(defaultZero(batch.getQueuedCount()) + queued);
        batch.setSkippedCount(defaultZero(batch.getSkippedCount()) + skipped);
        sweepBatchMapper.updateById(batch);
    }

    // 空值转零
    private int defaultZero(Integer value) {
        if (value == null) {
            return 0;
        }
        return value;
    }
}
