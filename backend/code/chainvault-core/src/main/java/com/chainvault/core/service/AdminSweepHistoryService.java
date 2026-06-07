package com.chainvault.core.service;

import com.chainvault.common.result.PageResult;
import com.chainvault.core.domain.dto.SweepBatchQueryReq;
import com.chainvault.core.domain.dto.SweepRecordQueryReq;
import com.chainvault.core.domain.vo.SweepAddressSummaryVO;
import com.chainvault.core.domain.vo.SweepBatchVO;
import com.chainvault.core.domain.vo.SweepRecordVO;
import com.chainvault.core.domain.vo.SweepTriggerVO;

/**
 * Admin 归集历史业务接口
 */
public interface AdminSweepHistoryService {

    /**
     * 分页查询归集批次
     *
     * @param req 查询条件
     * @return 分页结果
     */
    PageResult<SweepBatchVO> listBatches(SweepBatchQueryReq req);

    /**
     * 查询批次详情
     *
     * @param batchNo 批次号
     * @return 批次视图
     */
    SweepBatchVO getBatch(String batchNo);

    /**
     * 分页查询归集明细
     *
     * @param req 查询条件
     * @return 分页结果
     */
    PageResult<SweepRecordVO> listRecords(SweepRecordQueryReq req);

    /**
     * 查询单条明细
     *
     * @param recordNo 明细号
     * @return 明细视图
     */
    SweepRecordVO getRecord(String recordNo);

    /**
     * 单条失败明细重试
     *
     * @param recordNo  明细号
     * @param triggerBy 操作人
     * @return 触发结果
     */
    SweepTriggerVO retryRecord(String recordNo, String triggerBy);

    /**
     * 批次内失败明细批量重试
     *
     * @param batchNo   批次号
     * @param triggerBy 操作人
     * @return 触发结果
     */
    SweepTriggerVO retryBatchFailed(String batchNo, String triggerBy);

    /**
     * 充值地址归集汇总
     *
     * @param chainCode 链标识
     * @param address   充值地址
     * @return 汇总视图
     */
    SweepAddressSummaryVO getAddressSummary(String chainCode, String address);

    /**
     * 充值地址归集历史分页
     *
     * @param chainCode 链标识
     * @param address   充值地址
     * @param page      页码
     * @param size      每页条数
     * @return 分页结果
     */
    PageResult<SweepRecordVO> listAddressRecords(String chainCode, String address, int page, int size);
}
