package com.chainvault.core.service.impl;

import com.chainvault.chainnode.dto.BroadcastRequest;
import com.chainvault.chainnode.dto.BroadcastResult;
import com.chainvault.chainnode.service.TransactionBroadcaster;
import com.chainvault.common.constants.QueueNames;
import com.chainvault.common.constants.WebhookEvents;
import com.chainvault.common.dto.WebhookQueueMessage;
import com.chainvault.common.enums.TransactionStatus;
import com.chainvault.common.enums.WithdrawStatus;
import com.chainvault.common.exception.BusinessException;
import com.chainvault.common.redis.RedisMessageQueue;
import com.chainvault.core.domain.entity.MerchantChainIndex;
import com.chainvault.core.domain.entity.TransactionRecord;
import com.chainvault.core.domain.entity.WithdrawOrder;
import com.chainvault.core.mapper.WithdrawOrderMapper;
import com.chainvault.core.service.HotWalletService;
import com.chainvault.core.service.MerchantChainIndexService;
import com.chainvault.core.service.TransactionRecordService;
import com.chainvault.core.service.WithdrawBroadcastService;
import com.chainvault.keyvault.service.KeyVaultService;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.chainvault.common.dto.WithdrawBroadcastMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 提币广播业务实现
 *
 * @author chainvault
 * @date 2026-06-05
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class WithdrawBroadcastServiceImpl implements WithdrawBroadcastService {

    private static final int HOT_WALLET_ADDRESS_INDEX = 0;

    private final RedisMessageQueue messageQueue;
    private final ObjectMapper objectMapper;
    private final WithdrawOrderMapper withdrawOrderMapper;
    private final TransactionRecordService transactionRecordService;
    private final HotWalletService hotWalletService;
    private final MerchantChainIndexService merchantChainIndexService;
    private final KeyVaultService keyVaultService;
    private final TransactionBroadcaster transactionBroadcaster;

    /**
     * 消费广播队列并处理一笔提币
     */
    @Override
    public void processNext() {
        // 1. 非阻塞出队
        String json = messageQueue.pop(QueueNames.WITHDRAW_BROADCAST, 1);
        if (json == null || json.isBlank()) {
            return;
        }

        try {
            WithdrawBroadcastMessage message = objectMapper.readValue(json, WithdrawBroadcastMessage.class);
            processOrder(message.getOrderNo());
        } catch (Exception e) {
            log.error("提币广播消息处理失败 payload={}", json, e);
        }
    }

    // 处理单笔提币广播
    @Transactional(rollbackFor = Exception.class)
    protected void processOrder(String orderNo) {
        // 1. 加载提币单
        WithdrawOrder order = withdrawOrderMapper.selectByOrderNo(orderNo);
        if (order == null) {
            log.warn("提币单不存在 orderNo={}", orderNo);
            return;
        }
        if (order.getStatus() != WithdrawStatus.APPROVED.getCode()) {
            return;
        }

        // 2. 标记广播中
        order.setStatus(WithdrawStatus.BROADCASTING.getCode());
        withdrawOrderMapper.updateById(order);

        TransactionRecord record = transactionRecordService.findByTradeId(order.getTradeId());
        if (record == null) {
            throw new BusinessException("关联交易记录不存在: " + order.getTradeId());
        }

        try {
            // 3. 组装广播请求并上链
            MerchantChainIndex chainIndex = merchantChainIndexService.getOrCreate(
                    order.getMerchantId(), order.getChainCode());
            String bip44Path = keyVaultService.buildBip44Path(
                    order.getChainCode(), chainIndex.getAccountIndex(), HOT_WALLET_ADDRESS_INDEX);

            BroadcastRequest request = new BroadcastRequest();
            request.setOrderNo(order.getOrderNo());
            request.setMerchantId(order.getMerchantId());
            request.setChainCode(order.getChainCode());
            request.setCoinType(order.getCoinType());
            request.setToAddress(order.getToAddress());
            request.setAmount(order.getAmount());
            request.setMemo(order.getMemo());
            request.setFeeLevel(order.getFeeLevel());
            request.setFromBip44Path(bip44Path);

            BroadcastResult result = transactionBroadcaster.broadcast(request);

            // 4. 广播成功：扣减冻结、更新状态
            hotWalletService.commitFrozen(order.getMerchantId(), order.getCoinType(), order.getAmount());
            order.setStatus(WithdrawStatus.SUCCESS.getCode());
            withdrawOrderMapper.updateById(order);

            transactionRecordService.updateTxHashAndStatus(
                    order.getTradeId(), result.getTxHash(), TransactionStatus.SUCCESS.getCode());
            if (result.getFromAddress() != null) {
                record.setFromAddress(result.getFromAddress());
            }
            record.setTxHash(result.getTxHash());
            record.setStatus(TransactionStatus.SUCCESS.getCode());

            messageQueue.push(QueueNames.WEBHOOK, buildWebhookMessage(WebhookEvents.WITHDRAW_SUCCESS, record));
            log.info("提币广播成功 orderNo={} txHash={}", orderNo, result.getTxHash());
        } catch (Exception e) {
            // 5. 广播失败：解冻并标记失败
            log.error("提币广播失败 orderNo={}", orderNo, e);
            hotWalletService.unfreezeBalance(order.getMerchantId(), order.getCoinType(), order.getAmount());
            order.setStatus(WithdrawStatus.FAILED.getCode());
            withdrawOrderMapper.updateById(order);
            transactionRecordService.updateStatus(record.getId(), TransactionStatus.FAILED.getCode());
            record.setStatus(TransactionStatus.FAILED.getCode());
            messageQueue.push(QueueNames.WEBHOOK, buildWebhookMessage(WebhookEvents.WITHDRAW_FAILED, record));
        }
    }

    // 构建 Webhook 消息
    private WebhookQueueMessage buildWebhookMessage(String eventType, TransactionRecord record) {
        WebhookQueueMessage message = new WebhookQueueMessage();
        message.setEvent(eventType);
        message.setMerchantId(record.getMerchantId());
        message.setTradeId(record.getTradeId());
        message.setCoinType(record.getCoinType());
        message.setChainCode(record.getChainCode());
        message.setTxHash(record.getTxHash());
        message.setAmount(record.getAmount().toPlainString());
        message.setToAddress(record.getToAddress());
        message.setConfirms(record.getConfirms());
        message.setRequiredConfirms(record.getRequiredConfirms());
        message.setBizId(record.getBizId());
        message.setAttempt(0);
        return message;
    }
}
