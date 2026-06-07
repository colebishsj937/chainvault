package com.chainvault.core.service;

import com.chainvault.core.domain.entity.TransactionRecord;

import java.math.BigDecimal;
import java.util.List;

/**
 * 交易记录业务接口
 *
 * @author chainvault
 * @date 2026-06-05
 */
public interface TransactionRecordService {

    /**
     * 判断链上交易是否已存在
     *
     * @param chainCode 链标识
     * @param txHash    交易 Hash
     * @return 是否已存在
     */
    boolean existsByChainAndTxHash(String chainCode, String txHash);

    /**
     * 保存充值记录
     *
     * @param record 交易记录
     * @return 保存后的记录
     */
    TransactionRecord saveDeposit(TransactionRecord record);

    /**
     * 查询处理中的充值记录
     *
     * @param chainCode 链标识
     * @return 记录列表
     */
    List<TransactionRecord> listProcessingDeposits(String chainCode);

    /**
     * 查询处理中的归集记录
     *
     * @param chainCode 链标识
     * @return 记录列表
     */
    List<TransactionRecord> listProcessingSweeps(String chainCode);

    /**
     * 保存归集链上交易记录
     *
     * @param record 交易记录
     * @return 保存后的记录
     */
    TransactionRecord saveSweep(TransactionRecord record);

    /**
     * 更新确认数
     *
     * @param id       记录 ID
     * @param confirms 确认数
     */
    void updateConfirms(Long id, int confirms);

    /**
     * 更新交易状态
     *
     * @param id     记录 ID
     * @param status 目标状态
     */
    void updateStatus(Long id, int status);

    /**
     * 更新交易状态与备注
     *
     * @param id     记录 ID
     * @param status 目标状态
     * @param remark 备注
     */
    void updateStatusAndRemark(Long id, int status, String remark);

    /**
     * 保存提币记录
     *
     * @param record 交易记录
     * @return 保存后的记录
     */
    TransactionRecord saveWithdraw(TransactionRecord record);

    /**
     * 按 tradeId 查询
     *
     * @param tradeId 交易 ID
     * @return 交易记录
     */
    TransactionRecord findByTradeId(String tradeId);

    /**
     * 更新链上 Hash 与状态
     *
     * @param tradeId 交易 ID
     * @param txHash  链上 Hash
     * @param status  目标状态
     */
    void updateTxHashAndStatus(String tradeId, String txHash, int status);

    /**
     * 更新回调状态与重试次数
     *
     * @param id             记录 ID
     * @param callbackStatus 回调状态
     * @param callbackTimes  回调次数
     */
    void updateCallbackStatus(Long id, int callbackStatus, int callbackTimes);

    /**
     * 统计地址已确认充值总额
     *
     * @param merchantId 商户号
     * @param coinType   币种
     * @param toAddress  充值地址
     * @return 已确认充值总额
     */
    BigDecimal sumConfirmedDepositsToAddress(String merchantId, String coinType, String toAddress);
}
