package com.chainvault.core.service;

import com.chainvault.common.enums.SweepErrorCode;
import com.chainvault.core.domain.entity.SweepRecord;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 归集明细业务接口
 */
public interface SweepRecordService {

    /**
     * 统计地址已成功归集总额
     *
     * @param merchantId  商户号
     * @param coinType    币种
     * @param fromAddress 充值地址
     * @return 已成功归集总额
     */
    BigDecimal sumSucceededAmount(String merchantId, String coinType, String fromAddress);

    /**
     * 判断地址是否存在进行中的归集
     *
     * @param chainCode   链标识
     * @param fromAddress 充值地址
     * @return 是否存在进行中任务
     */
    boolean hasInFlight(String chainCode, String fromAddress);

    /**
     * 按明细号查询
     *
     * @param recordNo 明细号
     * @return 明细实体
     */
    SweepRecord findByRecordNo(String recordNo);

    /**
     * 按 ID 查询
     *
     * @param recordId 明细 ID
     * @return 明细实体
     */
    SweepRecord findById(Long recordId);

    /**
     * 创建跳过明细
     *
     * @param record 明细草稿
     * @return 持久化后的明细
     */
    SweepRecord createSkipped(SweepRecord record);

    /**
     * 创建已入队明细
     *
     * @param record 明细草稿
     * @return 持久化后的明细
     */
    SweepRecord createQueued(SweepRecord record);

    /**
     * 标记广播中
     *
     * @param recordId 明细 ID
     */
    void markBroadcasting(Long recordId);

    /**
     * 标记确认中并关联链上交易
     *
     * @param recordId         明细 ID
     * @param tradeId          关联交易 ID
     * @param txHash           链上 Hash
     * @param blockNumber      区块高度
     * @param requiredConfirms 所需确认数
     */
    void markConfirming(Long recordId, String tradeId, String txHash,
                        Long blockNumber, int requiredConfirms);

    /**
     * 标记成功
     *
     * @param recordId 明细 ID
     * @param confirms 确认数
     */
    void markSuccess(Long recordId, int confirms);

    /**
     * 标记失败
     *
     * @param recordId     明细 ID
     * @param errorCode    错误码
     * @param errorMessage 错误说明
     */
    void markFailed(Long recordId, SweepErrorCode errorCode, String errorMessage);

    /**
     * 更新确认数
     *
     * @param recordId 明细 ID
     * @param confirms 确认数
     */
    void updateConfirms(Long recordId, int confirms);

    /**
     * 查询地址最近一条明细
     *
     * @param chainCode 链标识
     * @param address   充值地址
     * @return 最近明细
     */
    SweepRecord findLatestByAddress(String chainCode, String address);
}
