package com.chainvault.core.service.impl;

import com.chainvault.chainnode.dto.BroadcastRequest;
import com.chainvault.chainnode.dto.BroadcastResult;
import com.chainvault.chainnode.service.BlockScanner;
import com.chainvault.chainnode.service.TransactionBroadcaster;
import com.chainvault.common.constants.QueueNames;
import com.chainvault.common.dto.SweepQueueMessage;
import com.chainvault.common.enums.SweepErrorCode;
import com.chainvault.common.enums.SweepRecordStatus;
import com.chainvault.common.enums.TransactionStatus;
import com.chainvault.common.enums.TxType;
import com.chainvault.common.exception.BusinessException;
import com.chainvault.common.redis.RedisMessageQueue;
import com.chainvault.common.util.TradeIdGenerator;
import com.chainvault.core.domain.entity.CoinConfig;
import com.chainvault.core.domain.entity.SweepRecord;
import com.chainvault.core.domain.entity.TransactionRecord;
import com.chainvault.core.service.CoinConfigService;
import com.chainvault.core.service.SweepRecordService;
import com.chainvault.core.service.SweepBroadcastService;
import com.chainvault.core.service.TransactionRecordService;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 归集广播实现
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class SweepBroadcastServiceImpl implements SweepBroadcastService {

    private final RedisMessageQueue messageQueue;
    private final ObjectMapper objectMapper;
    private final TransactionBroadcaster transactionBroadcaster;
    private final SweepRecordService sweepRecordService;
    private final TransactionRecordService transactionRecordService;
    private final CoinConfigService coinConfigService;
    private final TradeIdGenerator tradeIdGenerator;
    private final List<BlockScanner> blockScanners;

    /**
     * 消费归集队列
     */
    @Override
    public void processNext() {
        String json = messageQueue.pop(QueueNames.SWEEP, 1);
        if (json == null || json.isBlank()) {
            return;
        }
        try {
            SweepQueueMessage message = objectMapper.readValue(json, SweepQueueMessage.class);
            broadcast(message);
        } catch (Exception e) {
            log.error("归集消息处理失败 payload={}", json, e);
        }
    }

    // 广播归集交易并更新明细状态
    @Transactional(rollbackFor = Exception.class)
    protected void broadcast(SweepQueueMessage message) {
        if (message.getRecordId() == null) {
            log.warn("归集消息缺少 recordId，跳过");
            return;
        }

        SweepRecord record = sweepRecordService.findById(message.getRecordId());
        if (record == null) {
            log.warn("归集明细不存在 recordId={}", message.getRecordId());
            return;
        }
        if (record.getStatus() != SweepRecordStatus.QUEUED.getCode()) {
            return;
        }

        sweepRecordService.markBroadcasting(record.getId());

        String orderNo = record.getRecordNo();
        BroadcastRequest request = new BroadcastRequest();
        request.setOrderNo(orderNo);
        request.setMerchantId(message.getMerchantId());
        request.setChainCode(message.getChainCode());
        request.setCoinType(message.getCoinType());
        request.setToAddress(message.getToAddress());
        request.setAmount(message.getAmount());
        request.setFromBip44Path(message.getBip44Path());

        try {
            BroadcastResult result = transactionBroadcaster.broadcast(request);
            int requiredConfirms = result.isSimulated() ? 0 : resolveRequiredConfirms(message.getChainCode());
            Long blockNumber = resolveLatestBlock(message.getChainCode());

            String tradeId = tradeIdGenerator.next("CV");
            TransactionRecord txRecord = buildSweepTransaction(message, record, tradeId, result, blockNumber, requiredConfirms);
            transactionRecordService.saveSweep(txRecord);

            sweepRecordService.markConfirming(record.getId(), tradeId, result.getTxHash(), blockNumber, requiredConfirms);

            // 模拟广播：确认数要求为 0，立即成功
            if (result.isSimulated()) {
                transactionRecordService.updateStatus(txRecord.getId(), TransactionStatus.SUCCESS.getCode());
                transactionRecordService.updateConfirms(txRecord.getId(), requiredConfirms);
                sweepRecordService.markSuccess(record.getId(), requiredConfirms);
            }

            log.info("归集广播完成 recordNo={} from={} amount={} txHash={} simulated={}",
                    record.getRecordNo(), message.getFromAddress(), message.getAmount(),
                    result.getTxHash(), result.isSimulated());
        } catch (BusinessException e) {
            sweepRecordService.markFailed(record.getId(), SweepErrorCode.BROADCAST_REJECTED, e.getMessage());
            log.error("归集广播失败 recordNo={}", record.getRecordNo(), e);
        } catch (Exception e) {
            sweepRecordService.markFailed(record.getId(), SweepErrorCode.UNKNOWN, e.getMessage());
            log.error("归集广播异常 recordNo={}", record.getRecordNo(), e);
        }
    }

    // 构建归集链上交易记录
    private TransactionRecord buildSweepTransaction(SweepQueueMessage message, SweepRecord record,
                                                    String tradeId, BroadcastResult result,
                                                    Long blockNumber, int requiredConfirms) {
        CoinConfig coin = coinConfigService.getByCoinType(message.getCoinType());
        int decimals = coin != null && coin.getDecimals() != null ? coin.getDecimals() : 18;

        TransactionRecord txRecord = new TransactionRecord();
        txRecord.setTradeId(tradeId);
        txRecord.setMerchantId(message.getMerchantId());
        txRecord.setTxType(TxType.SWEEP.getCode());
        txRecord.setCoinType(message.getCoinType());
        txRecord.setChainCode(message.getChainCode());
        txRecord.setFromAddress(message.getFromAddress());
        txRecord.setToAddress(message.getToAddress());
        txRecord.setAmount(message.getAmount());
        txRecord.setRawAmount(message.getAmount().movePointRight(decimals).toPlainString());
        txRecord.setTxHash(result.getTxHash());
        txRecord.setBlockNumber(blockNumber);
        txRecord.setConfirms(0);
        txRecord.setRequiredConfirms(requiredConfirms);
        txRecord.setStatus(TransactionStatus.PROCESSING.getCode());
        txRecord.setRiskLevel(0);
        txRecord.setCallbackStatus(0);
        txRecord.setCallbackTimes(0);
        txRecord.setRemark("sweep:" + record.getRecordNo());
        return txRecord;
    }

    // 解析链所需确认数
    private int resolveRequiredConfirms(String chainCode) {
        BlockScanner scanner = scannerMap().get(chainCode);
        if (scanner != null) {
            return scanner.requiredConfirms();
        }
        return 12;
    }

    // 解析最新区块高度
    private Long resolveLatestBlock(String chainCode) {
        BlockScanner scanner = scannerMap().get(chainCode);
        if (scanner == null) {
            return 1L;
        }
        try {
            return scanner.latestBlockNumber();
        } catch (Exception e) {
            return 1L;
        }
    }

    // 构建扫块器映射
    private Map<String, BlockScanner> scannerMap() {
        Map<String, BlockScanner> map = new HashMap<>();
        for (BlockScanner scanner : blockScanners) {
            if (scanner.enabled()) {
                map.put(scanner.chainCode(), scanner);
            }
        }
        return map;
    }
}
