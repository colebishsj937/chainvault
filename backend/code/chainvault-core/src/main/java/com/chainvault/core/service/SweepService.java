package com.chainvault.core.service;

import com.chainvault.common.enums.SweepTriggerType;
import com.chainvault.core.domain.dto.SweepTriggerReq;
import com.chainvault.core.domain.vo.SweepTriggerVO;

/**
 * 资金归集业务接口
 */
public interface SweepService {

    /**
     * 扫描已用充值地址并入队归集任务
     *
     * @param req 触发请求
     * @return 扫描统计
     */
    SweepTriggerVO scanAndEnqueue(SweepTriggerReq req);

    /**
     * 定时扫描全部商户归集候选地址
     */
    void scheduledScan();

    /**
     * 按链触发归集扫描
     *
     * @param chainCode   链标识
     * @param merchantId  商户号，可为空表示全平台
     * @param coinType    币种标识，可为空表示该链全部币种
     * @param triggerType 触发方式
     * @param triggerBy   触发人
     * @return 扫描统计
     */
    SweepTriggerVO scanByChainCode(String chainCode, String merchantId, String coinType,
                                   SweepTriggerType triggerType, String triggerBy);

    /**
     * 单条失败明细重试
     *
     * @param recordNo  明细号
     * @param triggerBy 操作人
     * @return 触发结果
     */
    SweepTriggerVO retryFailedRecord(String recordNo, String triggerBy);

    /**
     * 批次内失败明细批量重试
     *
     * @param batchNo   批次号
     * @param triggerBy 操作人
     * @return 触发结果
     */
    SweepTriggerVO retryFailedBatch(String batchNo, String triggerBy);
}
