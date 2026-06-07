package com.chainvault.core.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.chainvault.common.enums.SweepErrorCode;
import com.chainvault.common.enums.SweepRecordStatus;
import com.chainvault.common.util.TradeIdGenerator;
import com.chainvault.core.domain.entity.SweepRecord;
import com.chainvault.core.mapper.SweepRecordMapper;
import com.chainvault.core.service.SweepBatchService;
import com.chainvault.core.service.SweepRecordService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 归集明细业务实现
 */
@Service
@RequiredArgsConstructor
public class SweepRecordServiceImpl implements SweepRecordService {

    private final SweepRecordMapper sweepRecordMapper;
    private final SweepBatchService sweepBatchService;
    private final TradeIdGenerator tradeIdGenerator;

    /**
     * 统计地址已成功归集总额
     */
    @Transactional(readOnly = true)
    @Override
    public BigDecimal sumSucceededAmount(String merchantId, String coinType, String fromAddress) {
        BigDecimal sum = sweepRecordMapper.sumSucceededAmount(merchantId, coinType, fromAddress);
        if (sum == null) {
            return BigDecimal.ZERO;
        }
        return sum;
    }

    /**
     * 判断地址是否存在进行中的归集
     */
    @Transactional(readOnly = true)
    @Override
    public boolean hasInFlight(String chainCode, String fromAddress) {
        Long count = sweepRecordMapper.countInFlight(chainCode, fromAddress);
        return count != null && count > 0;
    }

    /**
     * 按明细号查询
     */
    @Transactional(readOnly = true)
    @Override
    public SweepRecord findByRecordNo(String recordNo) {
        return sweepRecordMapper.selectOne(
                new LambdaQueryWrapper<SweepRecord>()
                        .eq(SweepRecord::getRecordNo, recordNo));
    }

    /**
     * 按 ID 查询
     */
    @Transactional(readOnly = true)
    @Override
    public SweepRecord findById(Long recordId) {
        return sweepRecordMapper.selectById(recordId);
    }

    /**
     * 创建跳过明细
     */
    @Transactional(rollbackFor = Exception.class)
    @Override
    public SweepRecord createSkipped(SweepRecord record) {
        record.setRecordNo(tradeIdGenerator.next("SWR"));
        record.setStatus(SweepRecordStatus.SKIPPED.getCode());
        record.setRetrySeq(defaultZero(record.getRetrySeq()));
        record.setConfirms(0);
        sweepRecordMapper.insert(record);
        return record;
    }

    /**
     * 创建已入队明细
     */
    @Transactional(rollbackFor = Exception.class)
    @Override
    public SweepRecord createQueued(SweepRecord record) {
        record.setRecordNo(tradeIdGenerator.next("SWR"));
        record.setStatus(SweepRecordStatus.QUEUED.getCode());
        record.setRetrySeq(defaultZero(record.getRetrySeq()));
        record.setConfirms(0);
        record.setQueuedAt(LocalDateTime.now());
        sweepRecordMapper.insert(record);
        return record;
    }

    /**
     * 标记广播中
     */
    @Transactional(rollbackFor = Exception.class)
    @Override
    public void markBroadcasting(Long recordId) {
        sweepRecordMapper.update(null,
                new LambdaUpdateWrapper<SweepRecord>()
                        .eq(SweepRecord::getId, recordId)
                        .eq(SweepRecord::getStatus, SweepRecordStatus.QUEUED.getCode())
                        .set(SweepRecord::getStatus, SweepRecordStatus.BROADCASTING.getCode()));
    }

    /**
     * 标记确认中并关联链上交易
     */
    @Transactional(rollbackFor = Exception.class)
    @Override
    public void markConfirming(Long recordId, String tradeId, String txHash,
                               Long blockNumber, int requiredConfirms) {
        sweepRecordMapper.update(null,
                new LambdaUpdateWrapper<SweepRecord>()
                        .eq(SweepRecord::getId, recordId)
                        .set(SweepRecord::getStatus, SweepRecordStatus.CONFIRMING.getCode())
                        .set(SweepRecord::getTradeId, tradeId)
                        .set(SweepRecord::getTxHash, txHash)
                        .set(SweepRecord::getBlockNumber, blockNumber)
                        .set(SweepRecord::getRequiredConfirms, requiredConfirms)
                        .set(SweepRecord::getBroadcastAt, LocalDateTime.now()));
    }

    /**
     * 标记成功
     */
    @Transactional(rollbackFor = Exception.class)
    @Override
    public void markSuccess(Long recordId, int confirms) {
        SweepRecord record = sweepRecordMapper.selectById(recordId);
        if (record == null) {
            return;
        }
        if (record.getStatus() == SweepRecordStatus.SUCCESS.getCode()) {
            return;
        }

        sweepRecordMapper.update(null,
                new LambdaUpdateWrapper<SweepRecord>()
                        .eq(SweepRecord::getId, recordId)
                        .set(SweepRecord::getStatus, SweepRecordStatus.SUCCESS.getCode())
                        .set(SweepRecord::getConfirms, confirms)
                        .set(SweepRecord::getConfirmedAt, LocalDateTime.now()));

        sweepBatchService.refreshAndFinalize(record.getBatchId());
    }

    /**
     * 标记失败
     */
    @Transactional(rollbackFor = Exception.class)
    @Override
    public void markFailed(Long recordId, SweepErrorCode errorCode, String errorMessage) {
        SweepRecord record = sweepRecordMapper.selectById(recordId);
        if (record == null) {
            return;
        }
        if (record.getStatus() == SweepRecordStatus.SUCCESS.getCode()) {
            return;
        }

        String message = errorMessage;
        if (message == null || message.isBlank()) {
            message = errorCode.getDefaultMessage();
        }

        sweepRecordMapper.update(null,
                new LambdaUpdateWrapper<SweepRecord>()
                        .eq(SweepRecord::getId, recordId)
                        .set(SweepRecord::getStatus, SweepRecordStatus.FAILED.getCode())
                        .set(SweepRecord::getErrorCode, errorCode.getCode())
                        .set(SweepRecord::getErrorMessage, truncate(message, 512))
                        .set(SweepRecord::getFailedAt, LocalDateTime.now()));

        sweepBatchService.refreshAndFinalize(record.getBatchId());
    }

    /**
     * 更新确认数
     */
    @Transactional(rollbackFor = Exception.class)
    @Override
    public void updateConfirms(Long recordId, int confirms) {
        sweepRecordMapper.update(null,
                new LambdaUpdateWrapper<SweepRecord>()
                        .eq(SweepRecord::getId, recordId)
                        .set(SweepRecord::getConfirms, confirms));
    }

    /**
     * 查询地址最近一条明细
     */
    @Transactional(readOnly = true)
    @Override
    public SweepRecord findLatestByAddress(String chainCode, String address) {
        return sweepRecordMapper.selectOne(
                new LambdaQueryWrapper<SweepRecord>()
                        .eq(SweepRecord::getChainCode, chainCode)
                        .eq(SweepRecord::getFromAddress, address)
                        .orderByDesc(SweepRecord::getId)
                        .last("LIMIT 1"));
    }

    // 空值转零
    private int defaultZero(Integer value) {
        if (value == null) {
            return 0;
        }
        return value;
    }

    // 截断错误信息
    private String truncate(String value, int maxLen) {
        if (value.length() <= maxLen) {
            return value;
        }
        return value.substring(0, maxLen);
    }
}
