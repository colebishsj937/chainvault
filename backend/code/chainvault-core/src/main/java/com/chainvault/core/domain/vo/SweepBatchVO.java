package com.chainvault.core.domain.vo;

import com.chainvault.core.domain.entity.SweepBatch;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 归集批次视图
 */
@Data
public class SweepBatchVO {

    private String batchNo;
    private String merchantId;
    private String chainCode;
    private String coinType;
    private Integer triggerType;
    private String triggerBy;
    private Integer status;
    private String statusLabel;
    private Integer scannedCount;
    private Integer queuedCount;
    private Integer successCount;
    private Integer failedCount;
    private Integer skippedCount;
    private LocalDateTime createdAt;
    private LocalDateTime completedAt;

    /**
     * 从实体转换
     *
     * @param batch 批次实体
     * @return 视图对象
     */
    public static SweepBatchVO from(SweepBatch batch) {
        SweepBatchVO vo = new SweepBatchVO();
        vo.setBatchNo(batch.getBatchNo());
        vo.setMerchantId(batch.getMerchantId());
        vo.setChainCode(batch.getChainCode());
        vo.setCoinType(batch.getCoinType());
        vo.setTriggerType(batch.getTriggerType());
        vo.setTriggerBy(batch.getTriggerBy());
        vo.setStatus(batch.getStatus());
        vo.setScannedCount(batch.getScannedCount());
        vo.setQueuedCount(batch.getQueuedCount());
        vo.setSuccessCount(batch.getSuccessCount());
        vo.setFailedCount(batch.getFailedCount());
        vo.setSkippedCount(batch.getSkippedCount());
        vo.setCreatedAt(batch.getCreatedAt());
        vo.setCompletedAt(batch.getCompletedAt());
        return vo;
    }
}
