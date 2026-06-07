package com.chainvault.core.service.impl;

import com.chainvault.common.constants.QueueNames;
import com.chainvault.common.dto.WithdrawBroadcastMessage;
import com.chainvault.common.enums.TransactionStatus;
import com.chainvault.common.enums.WithdrawStatus;
import com.chainvault.common.exception.BusinessException;
import com.chainvault.common.redis.RedisMessageQueue;
import com.chainvault.core.domain.dto.WithdrawRejectReq;
import com.chainvault.core.domain.entity.TransactionRecord;
import com.chainvault.core.domain.entity.WithdrawOrder;
import com.chainvault.core.mapper.WithdrawOrderMapper;
import com.chainvault.core.service.HotWalletService;
import com.chainvault.core.service.TransactionRecordService;
import com.chainvault.core.service.WithdrawAuditService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
/**
 * 提币审核业务实现
 *
 * @author chainvault
 * @date 2026-06-05
 */
@Service
@RequiredArgsConstructor
public class WithdrawAuditServiceImpl implements WithdrawAuditService {

    private final WithdrawOrderMapper withdrawOrderMapper;
    private final TransactionRecordService transactionRecordService;
    private final HotWalletService hotWalletService;
    private final RedisMessageQueue messageQueue;

    /**
     * 审核通过待审提币单
     *
     * @param orderNo 提币单号
     */
    @Transactional(rollbackFor = Exception.class)
    @Override
    public void approve(String orderNo) {
        // 1. 加载并校验状态
        WithdrawOrder order = requirePendingOrder(orderNo);

        // 2. 更新为审核通过
        order.setStatus(WithdrawStatus.APPROVED.getCode());
        withdrawOrderMapper.updateById(order);

        TransactionRecord record = transactionRecordService.findByTradeId(order.getTradeId());
        if (record != null) {
            transactionRecordService.updateStatus(record.getId(), TransactionStatus.PROCESSING.getCode());
        }

        // 3. 事务提交后入广播队列
        enqueueAfterCommit(orderNo);
    }

    /**
     * 拒绝待审提币单
     *
     * @param orderNo 提币单号
     * @param req     拒绝原因
     */
    @Transactional(rollbackFor = Exception.class)
    @Override
    public void reject(String orderNo, WithdrawRejectReq req) {
        // 1. 加载并校验状态
        WithdrawOrder order = requirePendingOrder(orderNo);

        // 2. 解冻余额并标记拒绝
        hotWalletService.unfreezeBalance(order.getMerchantId(), order.getCoinType(), order.getAmount());
        order.setStatus(WithdrawStatus.REJECTED.getCode());
        withdrawOrderMapper.updateById(order);

        // 3. 更新交易记录
        TransactionRecord record = transactionRecordService.findByTradeId(order.getTradeId());
        if (record != null) {
            String reason = req != null ? req.getReason() : null;
            transactionRecordService.updateStatusAndRemark(
                    record.getId(), TransactionStatus.FAILED.getCode(), reason);
        }
    }

    // 加载待审提币单
    private WithdrawOrder requirePendingOrder(String orderNo) {
        WithdrawOrder order = withdrawOrderMapper.selectByOrderNo(orderNo);
        if (order == null) {
            throw new BusinessException("提币单不存在: " + orderNo);
        }
        if (order.getStatus() != WithdrawStatus.PENDING.getCode()) {
            throw new BusinessException("仅待审核提币单可操作，当前状态: " + order.getStatus());
        }
        return order;
    }

    // 事务提交后推送广播队列
    private void enqueueAfterCommit(String orderNo) {
        if (!TransactionSynchronizationManager.isSynchronizationActive()) {
            pushBroadcastQueue(orderNo);
            return;
        }
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                pushBroadcastQueue(orderNo);
            }
        });
    }

    // 推送提币广播队列
    private void pushBroadcastQueue(String orderNo) {
        WithdrawBroadcastMessage message = new WithdrawBroadcastMessage();
        message.setOrderNo(orderNo);
        messageQueue.push(QueueNames.WITHDRAW_BROADCAST, message);
    }
}
