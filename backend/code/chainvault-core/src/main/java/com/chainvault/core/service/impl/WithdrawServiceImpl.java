package com.chainvault.core.service.impl;

import com.chainvault.common.constants.FeeLevel;
import com.chainvault.common.constants.QueueNames;
import com.chainvault.common.constants.WebhookEvents;
import com.chainvault.common.dto.WebhookQueueMessage;
import com.chainvault.common.enums.TransactionStatus;
import com.chainvault.common.enums.TxType;
import com.chainvault.common.enums.WithdrawStatus;
import com.chainvault.common.exception.BusinessException;
import com.chainvault.common.redis.RedisMessageQueue;
import com.chainvault.common.util.TradeIdGenerator;
import com.chainvault.core.domain.dto.WithdrawBatchItem;
import com.chainvault.core.domain.dto.WithdrawBatchReq;
import com.chainvault.core.domain.dto.WithdrawSubmitReq;
import com.chainvault.core.domain.entity.CoinConfig;
import com.chainvault.core.domain.entity.MerchantChainIndex;
import com.chainvault.core.domain.entity.TransactionRecord;
import com.chainvault.core.domain.entity.WithdrawOrder;
import com.chainvault.core.domain.vo.WithdrawVO;
import com.chainvault.core.mapper.WithdrawOrderMapper;
import com.chainvault.core.service.CoinConfigService;
import com.chainvault.core.service.HotWalletService;
import com.chainvault.core.service.MerchantChainIndexService;
import com.chainvault.core.service.TransactionRecordService;
import com.chainvault.core.service.WithdrawService;
import com.chainvault.keyvault.service.KeyVaultService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

/**
 * 提币业务实现
 *
 * @author chainvault
 * @date 2026-06-05
 */
@Service
@RequiredArgsConstructor
public class WithdrawServiceImpl implements WithdrawService {

    private static final int HOT_WALLET_ADDRESS_INDEX = 0;

    private final WithdrawOrderMapper withdrawOrderMapper;
    private final HotWalletService hotWalletService;
    private final CoinConfigService coinConfigService;
    private final TransactionRecordService transactionRecordService;
    private final MerchantChainIndexService merchantChainIndexService;
    private final KeyVaultService keyVaultService;
    private final TradeIdGenerator tradeIdGenerator;
    private final RedisMessageQueue messageQueue;

    /**
     * 提交单笔提币
     *
     * @param req 提币请求
     * @return 提币单信息
     */
    @Transactional(rollbackFor = Exception.class)
    @Override
    public WithdrawVO submit(WithdrawSubmitReq req) {
        return createWithdraw(
                req.getMerchantId(),
                req.getBizId(),
                req.getCoinType(),
                req.getToAddress(),
                req.getAmount(),
                req.getMemo(),
                req.getFeeLevel());
    }

    /**
     * 批量提币
     *
     * @param req 批量请求
     * @return 提币单列表
     */
    @Transactional(rollbackFor = Exception.class)
    @Override
    public List<WithdrawVO> submitBatch(WithdrawBatchReq req) {
        List<WithdrawVO> result = new ArrayList<>();
        for (WithdrawBatchItem item : req.getItems()) {
            result.add(createWithdraw(
                    req.getMerchantId(),
                    item.getBizId(),
                    item.getCoinType(),
                    item.getToAddress(),
                    item.getAmount(),
                    item.getMemo(),
                    item.getFeeLevel()));
        }
        return result;
    }

    // 创建单笔提币单
    private WithdrawVO createWithdraw(String merchantId,
                                      String bizId,
                                      String coinType,
                                      String toAddress,
                                      BigDecimal amount,
                                      String memo,
                                      String feeLevel) {
        // 1. 幂等：已存在则直接返回
        WithdrawOrder existing = withdrawOrderMapper.selectByMerchantBiz(merchantId, bizId);
        if (existing != null) {
            return WithdrawVO.from(existing);
        }

        // 2. 校验币种与地址
        CoinConfig coin = coinConfigService.getByCoinType(coinType);
        if (coin == null || coin.getIsEnabled() != 1) {
            throw new BusinessException("不支持的币种: " + coinType);
        }
        if (amount.compareTo(coin.getMinWithdraw()) < 0) {
            throw new BusinessException("提币金额低于最小限额: " + coin.getMinWithdraw());
        }
        if (!keyVaultService.validateAddress(coin.getChainCode(), toAddress)) {
            throw new BusinessException("目标地址格式不合法");
        }

        String resolvedFeeLevel = FeeLevel.isValid(feeLevel) ? feeLevel : FeeLevel.NORMAL;

        // 3. 冻结余额
        hotWalletService.freezeBalance(merchantId, coinType, amount);

        // 4. 创建提币单与交易记录
        String orderNo = tradeIdGenerator.next("WD");
        String tradeId = tradeIdGenerator.next("CV");
        MerchantChainIndex chainIndex = merchantChainIndexService.getOrCreate(merchantId, coin.getChainCode());

        WithdrawOrder order = new WithdrawOrder();
        order.setOrderNo(orderNo);
        order.setTradeId(tradeId);
        order.setMerchantId(merchantId);
        order.setBizId(bizId);
        order.setCoinType(coinType);
        order.setChainCode(coin.getChainCode());
        order.setToAddress(toAddress);
        order.setMemo(memo);
        order.setAmount(amount);
        order.setFeeLevel(resolvedFeeLevel);
        order.setStatus(WithdrawStatus.PENDING.getCode());
        order.setAuditStatus(0);
        withdrawOrderMapper.insert(order);

        TransactionRecord record = new TransactionRecord();
        record.setTradeId(tradeId);
        record.setMerchantId(merchantId);
        record.setBizId(bizId);
        record.setTxType(TxType.WITHDRAW.getCode());
        record.setCoinType(coinType);
        record.setChainCode(coin.getChainCode());
        record.setFromAddress(keyVaultService.deriveAddress(
                coin.getChainCode(), chainIndex.getAccountIndex(), HOT_WALLET_ADDRESS_INDEX).getAddress());
        record.setToAddress(toAddress);
        record.setAmount(amount);
        record.setRawAmount(amount.movePointRight(coin.getDecimals()).toBigInteger().toString());
        record.setMemo(memo);
        record.setConfirms(0);
        record.setRequiredConfirms(1);
        record.setStatus(TransactionStatus.PENDING.getCode());
        record.setRiskLevel(0);
        record.setCallbackStatus(0);
        record.setCallbackTimes(0);
        transactionRecordService.saveWithdraw(record);

        // 5. 推送待审核 Webhook（广播在运营审核通过后入队）
        messageQueue.push(QueueNames.WEBHOOK, buildWebhookMessage(WebhookEvents.WITHDRAW_PENDING, record));

        return WithdrawVO.from(order);
    }

    // 构建 Webhook 消息
    private WebhookQueueMessage buildWebhookMessage(String eventType, TransactionRecord record) {
        WebhookQueueMessage message = new WebhookQueueMessage();
        message.setEvent(eventType);
        message.setMerchantId(record.getMerchantId());
        message.setTradeId(record.getTradeId());
        message.setCoinType(record.getCoinType());
        message.setChainCode(record.getChainCode());
        message.setAmount(record.getAmount().toPlainString());
        message.setToAddress(record.getToAddress());
        message.setConfirms(record.getConfirms());
        message.setRequiredConfirms(record.getRequiredConfirms());
        message.setBizId(record.getBizId());
        message.setAttempt(0);
        return message;
    }
}
