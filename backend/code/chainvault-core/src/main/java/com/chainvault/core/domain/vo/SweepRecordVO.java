package com.chainvault.core.domain.vo;

import com.chainvault.core.domain.entity.SweepRecord;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 归集明细视图
 */
@Data
public class SweepRecordVO {

    private String recordNo;
    private String batchNo;
    private String parentRecordNo;
    private Integer retrySeq;
    private String merchantId;
    private String coinType;
    private String chainCode;
    private String fromAddress;
    private String toAddress;
    private String amount;
    private String thresholdSnapshot;
    private String pendingSnapshot;
    private Integer status;
    private String statusLabel;
    private String tradeId;
    private String txHash;
    private Long blockNumber;
    private Integer confirms;
    private Integer requiredConfirms;
    private String errorCode;
    private String errorMessage;
    private LocalDateTime queuedAt;
    private LocalDateTime broadcastAt;
    private LocalDateTime confirmedAt;
    private LocalDateTime failedAt;
    private LocalDateTime createdAt;

    /**
     * 从实体转换
     *
     * @param record  明细实体
     * @param batchNo 批次号
     * @return 视图对象
     */
    public static SweepRecordVO from(SweepRecord record, String batchNo) {
        return from(record, batchNo, null);
    }

    /**
     * 从实体转换
     *
     * @param record         明细实体
     * @param batchNo        批次号
     * @param parentRecordNo 父明细号
     * @return 视图对象
     */
    public static SweepRecordVO from(SweepRecord record, String batchNo, String parentRecordNo) {
        SweepRecordVO vo = new SweepRecordVO();
        vo.setRecordNo(record.getRecordNo());
        vo.setBatchNo(batchNo);
        vo.setParentRecordNo(parentRecordNo);
        vo.setRetrySeq(record.getRetrySeq());
        vo.setMerchantId(record.getMerchantId());
        vo.setCoinType(record.getCoinType());
        vo.setChainCode(record.getChainCode());
        vo.setFromAddress(record.getFromAddress());
        vo.setToAddress(record.getToAddress());
        if (record.getAmount() != null) {
            vo.setAmount(record.getAmount().toPlainString());
        }
        if (record.getThresholdSnapshot() != null) {
            vo.setThresholdSnapshot(record.getThresholdSnapshot().toPlainString());
        }
        if (record.getPendingSnapshot() != null) {
            vo.setPendingSnapshot(record.getPendingSnapshot().toPlainString());
        }
        vo.setStatus(record.getStatus());
        vo.setTradeId(record.getTradeId());
        vo.setTxHash(record.getTxHash());
        vo.setBlockNumber(record.getBlockNumber());
        vo.setConfirms(record.getConfirms());
        vo.setRequiredConfirms(record.getRequiredConfirms());
        vo.setErrorCode(record.getErrorCode());
        vo.setErrorMessage(record.getErrorMessage());
        vo.setQueuedAt(record.getQueuedAt());
        vo.setBroadcastAt(record.getBroadcastAt());
        vo.setConfirmedAt(record.getConfirmedAt());
        vo.setFailedAt(record.getFailedAt());
        vo.setCreatedAt(record.getCreatedAt());
        return vo;
    }
}
