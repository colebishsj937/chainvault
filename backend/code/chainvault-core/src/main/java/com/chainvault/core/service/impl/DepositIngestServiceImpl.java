package com.chainvault.core.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.chainvault.chainnode.config.ChainNodeProperties;
import com.chainvault.chainnode.dto.BlockScanResult;
import com.chainvault.chainnode.dto.ChainTransferEvent;
import com.chainvault.chainnode.service.BlockScanner;
import com.chainvault.common.constants.BlockScanConstants;
import com.chainvault.common.constants.ChainCode;
import com.chainvault.common.constants.QueueNames;
import com.chainvault.common.constants.WebhookEvents;
import com.chainvault.common.dto.WebhookQueueMessage;
import com.chainvault.common.enums.TransactionStatus;
import com.chainvault.common.enums.TxType;
import com.chainvault.common.redis.RedisMessageQueue;
import com.chainvault.common.util.TradeIdGenerator;
import com.chainvault.core.domain.entity.CoinConfig;
import com.chainvault.core.domain.entity.DepositAddress;
import com.chainvault.core.domain.entity.TransactionRecord;
import com.chainvault.core.mapper.DepositAddressMapper;
import com.chainvault.core.service.CoinConfigService;
import com.chainvault.core.service.DepositIngestService;
import com.chainvault.core.service.TransactionRecordService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * 充值入账业务实现
 *
 * @author chainvault
 * @date 2026-06-05
 */
@Service
@Slf4j
public class DepositIngestServiceImpl implements DepositIngestService {

    private final List<BlockScanner> blockScanners;
    private final StringRedisTemplate redis;
    private final ChainNodeProperties chainNodeProperties;
    private final DepositAddressMapper depositAddressMapper;
    private final CoinConfigService coinConfigService;
    private final TransactionRecordService transactionRecordService;
    private final RedisMessageQueue messageQueue;
    private final TradeIdGenerator tradeIdGenerator;
    private final ExecutorService chainScanExecutor;

    /** 链级扫块进行中标记，防止同链重叠调度 */
    private final ConcurrentHashMap<String, AtomicBoolean> chainScanRunning = new ConcurrentHashMap<>();

    /**
     * 构造注入扫块依赖与专用线程池
     *
     * @param blockScanners            各链扫块器
     * @param redis                    Redis
     * @param chainNodeProperties      链节点配置
     * @param depositAddressMapper     充值地址 Mapper
     * @param coinConfigService        币种配置服务
     * @param transactionRecordService 交易记录服务
     * @param messageQueue             Webhook 队列
     * @param tradeIdGenerator         交易号生成器
     * @param chainScanExecutor        链扫块线程池
     */
    public DepositIngestServiceImpl(List<BlockScanner> blockScanners,
                                    StringRedisTemplate redis,
                                    ChainNodeProperties chainNodeProperties,
                                    DepositAddressMapper depositAddressMapper,
                                    CoinConfigService coinConfigService,
                                    TransactionRecordService transactionRecordService,
                                    RedisMessageQueue messageQueue,
                                    TradeIdGenerator tradeIdGenerator,
                                    @Qualifier("chainScanExecutor") ExecutorService chainScanExecutor) {
        this.blockScanners = blockScanners;
        this.redis = redis;
        this.chainNodeProperties = chainNodeProperties;
        this.depositAddressMapper = depositAddressMapper;
        this.coinConfigService = coinConfigService;
        this.transactionRecordService = transactionRecordService;
        this.messageQueue = messageQueue;
        this.tradeIdGenerator = tradeIdGenerator;
        this.chainScanExecutor = chainScanExecutor;
    }

    /**
     * 扫描所有已启用链的新区块并入账
     */
    @Override
    public void scanAllChains() {
        if (!chainNodeProperties.isScanEnabled()) {
            log.debug("链上扫块已关闭，跳过本次 scanAllChains");
            return;
        }

        // 1. 按链调度扫块（并行模式下各链独立线程，互不阻塞）
        for (BlockScanner scanner : blockScanners) {
            if (!scanner.enabled()) {
                log.debug("[{}] 扫块器未启用，跳过", scanner.chainCode());
                continue;
            }
            if (chainNodeProperties.isScanParallelEnabled()) {
                submitChainScan(scanner);
            } else {
                scanSingleChain(scanner);
            }
        }
    }

    /**
     * 异步提交单链扫块任务；若该链上一批尚未结束则跳过，避免重叠扫同一区间
     *
     * @param scanner 链扫块器
     */
    private void submitChainScan(BlockScanner scanner) {
        String chainCode = scanner.chainCode();
        AtomicBoolean running = chainScanRunning.computeIfAbsent(chainCode, key -> new AtomicBoolean(false));
        if (!running.compareAndSet(false, true)) {
            log.debug("[{}] 上一批扫块进行中，跳过本次调度", chainCode);
            return;
        }

        chainScanExecutor.execute(() -> {
            try {
                scanSingleChain(scanner);
            } finally {
                running.set(false);
            }
        });
    }

    /**
     * 扫描单条链的一个批次区块并入账
     *
     * @param scanner 链扫块器
     */
    private void scanSingleChain(BlockScanner scanner) {
        try {
            String checkpointKey = BlockScanConstants.lastBlockKey(scanner.chainCode());
            long latest = scanner.latestBlockNumber();
            long safeLatest = Math.max(latest - scanner.requiredConfirms(), 0L);
            Long lastScanned = Optional.ofNullable(redis.opsForValue().get(checkpointKey))
                    .map(Long::parseLong)
                    .orElse(null);

            // 1. 计算扫块区间（无断点时从 safeLatest 向前回溯一批，避免 fromBlock > toBlock）
            long fromBlock;
            if (lastScanned == null) {
                fromBlock = Math.max(0L, safeLatest - chainNodeProperties.getScanBatchSize() + 1);
            } else {
                fromBlock = lastScanned + 1;
            }
            long toBlock = Math.min(fromBlock + chainNodeProperties.getScanBatchSize() - 1, safeLatest);

            if (fromBlock > toBlock) {
                log.debug("[{}] 暂无新区块可扫 latest={} safeLatest={} lastScanned={} requiredConfirms={}",
                        scanner.chainCode(), latest, safeLatest, lastScanned, scanner.requiredConfirms());
                return;
            }

            log.debug("[{}] 开始扫块 from={} to={} latest={} lastScanned={}",
                    scanner.chainCode(), fromBlock, toBlock, latest, lastScanned);

            BlockScanResult scanResult = scanner.scanRange(fromBlock, toBlock);
            int eventCount = scanResult.getEvents().size();
            for (ChainTransferEvent event : scanResult.getEvents()) {
                ingestTransfer(event, scanner.requiredConfirms());
            }

            if (scanResult.getLastScannedBlock() > 0) {
                redis.opsForValue().set(checkpointKey, String.valueOf(scanResult.getLastScannedBlock()));
            }

            log.info("[{}] 扫块完成 blocks={}-{} events={} checkpoint={}",
                    scanner.chainCode(), fromBlock, toBlock, eventCount, scanResult.getLastScannedBlock());
        } catch (Exception e) {
            log.error("[{}] 充值扫块失败", scanner.chainCode(), e);
        }
    }

    // 处理单笔链上转账
    @Transactional(rollbackFor = Exception.class)
    protected void ingestTransfer(ChainTransferEvent event, int requiredConfirms) {
        // 1. 幂等检查
        if (transactionRecordService.existsByChainAndTxHash(event.getChainCode(), event.getTxHash())) {
            log.debug("[{}] 跳过已入库 txHash={}", event.getChainCode(), event.getTxHash());
            return;
        }

        // 2. 匹配充值地址
        DepositAddress depositAddress = findDepositAddress(event.getChainCode(), event.getToAddress());
        if (depositAddress == null) {
            log.debug("[{}] 非充值地址 to={} txHash={} coin={}",
                    event.getChainCode(), event.getToAddress(), event.getTxHash(), event.getCoinType());
            return;
        }

        // 3. 解析币种配置
        CoinConfig coinConfig = resolveCoinConfig(event);
        if (coinConfig == null || coinConfig.getIsEnabled() != 1) {
            log.debug("[{}] 币种未配置或未启用 coin={} txHash={} to={}",
                    event.getChainCode(), event.getCoinType(), event.getTxHash(), event.getToAddress());
            return;
        }

        // 4. 金额换算与最小充值校验
        BigDecimal amount = new BigDecimal(event.getRawAmount())
                .movePointLeft(coinConfig.getDecimals());
        if (amount.compareTo(coinConfig.getMinDeposit()) < 0) {
            log.debug("[{}] 金额低于最小充值 to={} amount={} min={} txHash={} coin={}",
                    event.getChainCode(), event.getToAddress(), amount, coinConfig.getMinDeposit(),
                    event.getTxHash(), coinConfig.getCoinType());
            return;
        }

        // 5. 写入充值记录
        TransactionRecord record = new TransactionRecord();
        record.setTradeId(tradeIdGenerator.next("CV"));
        record.setMerchantId(depositAddress.getMerchantId());
        record.setBizId(depositAddress.getBizId());
        record.setTxType(TxType.DEPOSIT.getCode());
        record.setCoinType(coinConfig.getCoinType());
        record.setChainCode(event.getChainCode());
        record.setFromAddress(event.getFromAddress());
        record.setToAddress(event.getToAddress());
        record.setAmount(amount);
        record.setRawAmount(event.getRawAmount());
        record.setTxHash(event.getTxHash());
        record.setBlockNumber(event.getBlockNumber());
        record.setConfirms(0);
        record.setRequiredConfirms(requiredConfirms);
        record.setStatus(TransactionStatus.PROCESSING.getCode());
        record.setRiskLevel(0);
        record.setCallbackStatus(0);
        record.setCallbackTimes(0);
        transactionRecordService.saveDeposit(record);

        // 6. 标记地址已使用
        if (depositAddress.getIsUsed() == null || depositAddress.getIsUsed() == 0) {
            depositAddress.setIsUsed(1);
            depositAddressMapper.updateById(depositAddress);
        }

        // 7. 推送 deposit.pending 事件
        messageQueue.push(QueueNames.WEBHOOK, buildWebhookMessage(WebhookEvents.DEPOSIT_PENDING, record));

        log.info("[{}] 新充值入账 tradeId={} txHash={} amount={} {}",
                event.getChainCode(), record.getTradeId(), record.getTxHash(),
                record.getAmount(), record.getCoinType());
    }

    // 按链匹配充值地址
    private DepositAddress findDepositAddress(String chainCode, String toAddress) {
        if (ChainCode.ETH.equals(chainCode) || ChainCode.BNB.equals(chainCode)) {
            return depositAddressMapper.selectOne(
                    new LambdaQueryWrapper<DepositAddress>()
                            .eq(DepositAddress::getChainCode, chainCode)
                            .apply("LOWER(address) = {0}", toAddress.toLowerCase()));
        }
        return depositAddressMapper.selectOne(
                new LambdaQueryWrapper<DepositAddress>()
                        .eq(DepositAddress::getChainCode, chainCode)
                        .eq(DepositAddress::getAddress, toAddress));
    }

    // 根据事件解析币种配置
    private CoinConfig resolveCoinConfig(ChainTransferEvent event) {
        String coinTypeOrContract = event.getCoinType();
        if (coinTypeOrContract.startsWith("0x") || coinTypeOrContract.startsWith("T")) {
            return coinConfigService.getByChainAndContract(event.getChainCode(), coinTypeOrContract);
        }
        return coinConfigService.getByCoinType(coinTypeOrContract);
    }

    // 构建 Webhook 队列消息
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
