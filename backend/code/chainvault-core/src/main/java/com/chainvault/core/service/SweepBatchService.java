package com.chainvault.core.service;

import com.chainvault.common.enums.SweepTriggerType;
import com.chainvault.core.domain.entity.SweepBatch;

/**
 * 归集批次业务接口
 */
public interface SweepBatchService {

    /**
     * 创建执行中的归集批次
     *
     * @param merchantId  商户号
     * @param chainCode   链标识
     * @param coinType    币种
     * @param triggerType 触发方式
     * @param triggerBy   触发人
     * @return 批次实体
     */
    SweepBatch createRunningBatch(String merchantId, String chainCode, String coinType,
                                  SweepTriggerType triggerType, String triggerBy);

    /**
     * 按批次号查询
     *
     * @param batchNo 批次号
     * @return 批次实体
     */
    SweepBatch findByBatchNo(String batchNo);

    /**
     * 按 ID 查询
     *
     * @param batchId 批次 ID
     * @return 批次实体
     */
    SweepBatch findById(Long batchId);

    /**
     * 刷新批次计数并尝试完结
     *
     * @param batchId 批次 ID
     */
    void refreshAndFinalize(Long batchId);

    /**
     * 累加扫描统计
     *
     * @param batchId 批次 ID
     * @param scanned 扫描数增量
     * @param queued  入队数增量
     * @param skipped 跳过数增量
     */
    void addScanStats(Long batchId, int scanned, int queued, int skipped);
}
