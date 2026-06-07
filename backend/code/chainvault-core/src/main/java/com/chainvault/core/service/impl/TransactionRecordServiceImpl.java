package com.chainvault.core.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.chainvault.common.enums.TransactionStatus;
import com.chainvault.common.enums.TxType;
import java.math.BigDecimal;
import com.chainvault.core.domain.entity.TransactionRecord;
import com.chainvault.core.mapper.TransactionRecordMapper;
import com.chainvault.core.service.TransactionRecordService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * 交易记录业务实现
 *
 * @author chainvault
 * @date 2026-06-05
 */
@Service
@RequiredArgsConstructor
public class TransactionRecordServiceImpl implements TransactionRecordService {

    private final TransactionRecordMapper transactionRecordMapper;

    /**
     * 判断链上交易是否已存在
     *
     * @param chainCode 链标识
     * @param txHash    交易 Hash
     * @return 是否已存在
     */
    @Transactional(readOnly = true)
    @Override
    public boolean existsByChainAndTxHash(String chainCode, String txHash) {
        Long count = transactionRecordMapper.selectCount(
                new LambdaQueryWrapper<TransactionRecord>()
                        .eq(TransactionRecord::getChainCode, chainCode)
                        .eq(TransactionRecord::getTxHash, txHash));
        return count != null && count > 0;
    }

    /**
     * 保存充值记录
     *
     * @param record 交易记录
     * @return 保存后的记录
     */
    @Transactional(rollbackFor = Exception.class)
    @Override
    public TransactionRecord saveDeposit(TransactionRecord record) {
        transactionRecordMapper.insert(record);
        return record;
    }

    /**
     * 查询处理中的充值记录
     *
     * @param chainCode 链标识
     * @return 记录列表
     */
    @Transactional(readOnly = true)
    @Override
    public List<TransactionRecord> listProcessingDeposits(String chainCode) {
        return listProcessingByType(chainCode, TxType.DEPOSIT.getCode());
    }

    /**
     * 查询处理中的归集记录
     */
    @Transactional(readOnly = true)
    @Override
    public List<TransactionRecord> listProcessingSweeps(String chainCode) {
        return listProcessingByType(chainCode, TxType.SWEEP.getCode());
    }

    /**
     * 保存归集链上交易记录
     */
    @Transactional(rollbackFor = Exception.class)
    @Override
    public TransactionRecord saveSweep(TransactionRecord record) {
        transactionRecordMapper.insert(record);
        return record;
    }

    // 按链与类型查询处理中记录
    private List<TransactionRecord> listProcessingByType(String chainCode, int txType) {
        return transactionRecordMapper.selectList(
                new LambdaQueryWrapper<TransactionRecord>()
                        .eq(TransactionRecord::getChainCode, chainCode)
                        .eq(TransactionRecord::getTxType, txType)
                        .eq(TransactionRecord::getStatus, TransactionStatus.PROCESSING.getCode()));
    }

    /**
     * 更新确认数
     *
     * @param id       记录 ID
     * @param confirms 确认数
     */
    @Transactional(rollbackFor = Exception.class)
    @Override
    public void updateConfirms(Long id, int confirms) {
        transactionRecordMapper.update(null,
                new LambdaUpdateWrapper<TransactionRecord>()
                        .eq(TransactionRecord::getId, id)
                        .set(TransactionRecord::getConfirms, confirms));
    }

    /**
     * 更新交易状态
     *
     * @param id     记录 ID
     * @param status 目标状态
     */
    @Transactional(rollbackFor = Exception.class)
    @Override
    public void updateStatus(Long id, int status) {
        transactionRecordMapper.update(null,
                new LambdaUpdateWrapper<TransactionRecord>()
                        .eq(TransactionRecord::getId, id)
                        .set(TransactionRecord::getStatus, status));
    }

    /**
     * 更新交易状态与备注
     *
     * @param id     记录 ID
     * @param status 目标状态
     * @param remark 备注
     */
    @Transactional(rollbackFor = Exception.class)
    @Override
    public void updateStatusAndRemark(Long id, int status, String remark) {
        LambdaUpdateWrapper<TransactionRecord> wrapper = new LambdaUpdateWrapper<TransactionRecord>()
                .eq(TransactionRecord::getId, id)
                .set(TransactionRecord::getStatus, status);
        if (remark != null && !remark.isBlank()) {
            wrapper.set(TransactionRecord::getRemark, remark);
        }
        transactionRecordMapper.update(null, wrapper);
    }

    /**
     * 保存提币记录
     *
     * @param record 交易记录
     * @return 保存后的记录
     */
    @Transactional(rollbackFor = Exception.class)
    @Override
    public TransactionRecord saveWithdraw(TransactionRecord record) {
        transactionRecordMapper.insert(record);
        return record;
    }

    /**
     * 按 tradeId 查询
     *
     * @param tradeId 交易 ID
     * @return 交易记录
     */
    @Transactional(readOnly = true)
    @Override
    public TransactionRecord findByTradeId(String tradeId) {
        return transactionRecordMapper.selectOne(
                new LambdaQueryWrapper<TransactionRecord>()
                        .eq(TransactionRecord::getTradeId, tradeId));
    }

    /**
     * 更新链上 Hash 与状态
     *
     * @param tradeId 交易 ID
     * @param txHash  链上 Hash
     * @param status  目标状态
     */
    @Transactional(rollbackFor = Exception.class)
    @Override
    public void updateTxHashAndStatus(String tradeId, String txHash, int status) {
        transactionRecordMapper.update(null,
                new LambdaUpdateWrapper<TransactionRecord>()
                        .eq(TransactionRecord::getTradeId, tradeId)
                        .set(TransactionRecord::getTxHash, txHash)
                        .set(TransactionRecord::getStatus, status));
    }

    /**
     * 更新回调状态与重试次数
     *
     * @param id             记录 ID
     * @param callbackStatus 回调状态
     * @param callbackTimes  回调次数
     */
    @Transactional(rollbackFor = Exception.class)
    @Override
    public void updateCallbackStatus(Long id, int callbackStatus, int callbackTimes) {
        transactionRecordMapper.update(null,
                new LambdaUpdateWrapper<TransactionRecord>()
                        .eq(TransactionRecord::getId, id)
                        .set(TransactionRecord::getCallbackStatus, callbackStatus)
                        .set(TransactionRecord::getCallbackTimes, callbackTimes));
    }

    /**
     * 统计地址已确认充值总额
     *
     * @param merchantId 商户号
     * @param coinType   币种
     * @param toAddress  充值地址
     * @return 已确认充值总额
     */
    @Transactional(readOnly = true)
    @Override
    public BigDecimal sumConfirmedDepositsToAddress(String merchantId, String coinType, String toAddress) {
        BigDecimal sum = transactionRecordMapper.sumConfirmedDepositsToAddress(merchantId, coinType, toAddress);
        if (sum == null) {
            return BigDecimal.ZERO;
        }
        return sum;
    }
}
