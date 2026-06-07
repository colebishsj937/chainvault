package com.chainvault.chainnode.service;

import com.chainvault.chainnode.dto.BlockScanResult;
import com.chainvault.chainnode.dto.ChainTransferEvent;
import lombok.extern.slf4j.Slf4j;
import org.web3j.protocol.Web3j;
import org.web3j.protocol.core.DefaultBlockParameter;
import org.web3j.protocol.core.methods.request.EthFilter;
import org.web3j.protocol.core.methods.response.EthBlock;
import org.web3j.protocol.core.methods.response.EthLog;
import org.web3j.protocol.core.methods.response.Transaction;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;
import java.util.function.IntSupplier;
import java.util.function.Supplier;

/**
 * EVM 链区块扫描抽象实现
 *
 * @author chainvault
 * @date 2026-06-05
 */
@Slf4j
public abstract class AbstractEvmBlockScanner implements BlockScanner {

    private static final String TRANSFER_TOPIC =
            "0xddf252ad1be2c89b69c2b068fc378daa952ba7f163c4a11628f55a4df523b3ef";

    private final Supplier<Web3j> web3jSupplier;
    private final String chainCode;
    private final String nativeCoinType;
    private final IntSupplier requiredConfirmsSupplier;
    private final TokenContractProvider tokenContractProvider;

    protected AbstractEvmBlockScanner(Supplier<Web3j> web3jSupplier,
                                      String chainCode,
                                      String nativeCoinType,
                                      IntSupplier requiredConfirmsSupplier,
                                      TokenContractProvider tokenContractProvider) {
        this.web3jSupplier = web3jSupplier;
        this.chainCode = chainCode;
        this.nativeCoinType = nativeCoinType;
        this.requiredConfirmsSupplier = requiredConfirmsSupplier;
        this.tokenContractProvider = tokenContractProvider;
    }

    /**
     * 链标识
     *
     * @return 链标识
     */
    @Override
    public String chainCode() {
        return chainCode;
    }

    /**
     * 是否已配置并可扫描
     *
     * @return 是否启用
     */
    @Override
    public boolean enabled() {
        return web3jSupplier.get() != null;
    }

    /**
     * 所需确认数
     *
     * @return 确认数
     */
    @Override
    public int requiredConfirms() {
        return requiredConfirmsSupplier.getAsInt();
    }

    /**
     * 获取最新区块高度
     *
     * @return 最新区块高度
     */
    @Override
    public long latestBlockNumber() {
        try {
            return requireWeb3j().ethBlockNumber().send().getBlockNumber().longValue();
        } catch (Exception e) {
            throw new IllegalStateException("[" + chainCode + "] 获取最新区块失败", e);
        }
    }

    /**
     * 扫描区块区间
     *
     * @param fromBlock 起始区块
     * @param toBlock   结束区块
     * @return 扫描结果
     */
    @Override
    public BlockScanResult scanRange(long fromBlock, long toBlock) {
        BlockScanResult result = new BlockScanResult();
        List<ChainTransferEvent> events = new ArrayList<>();
        Web3j web3j = requireWeb3j();

        try {
            // 1. 遍历区块处理原生币与代币转账
            for (long blockNum = fromBlock; blockNum <= toBlock; blockNum++) {
                EthBlock.Block block = web3j.ethGetBlockByNumber(
                                DefaultBlockParameter.valueOf(BigInteger.valueOf(blockNum)), true)
                        .send().getBlock();
                if (block == null) {
                    continue;
                }
                events.addAll(parseNativeTransfers(block));
                events.addAll(parseTokenTransfers(web3j, blockNum));
                result.setLastScannedBlock(blockNum);
            }
        } catch (Exception e) {
            log.error("[{}] 扫块异常 from={} to={}", chainCode, fromBlock, toBlock, e);
        }

        result.setEvents(events);
        return result;
    }

    // 获取 Web3j 客户端
    private Web3j requireWeb3j() {
        Web3j web3j = web3jSupplier.get();
        if (web3j == null) {
            throw new IllegalStateException("[" + chainCode + "] Web3j 未配置");
        }
        return web3j;
    }

    // 解析区块内原生币转账
    private List<ChainTransferEvent> parseNativeTransfers(EthBlock.Block block) {
        List<ChainTransferEvent> events = new ArrayList<>();
        for (EthBlock.TransactionResult<?> txResult : block.getTransactions()) {
            Transaction tx = (Transaction) txResult.get();
            if (tx.getTo() == null || tx.getValue().compareTo(BigInteger.ZERO) == 0) {
                continue;
            }

            ChainTransferEvent event = new ChainTransferEvent();
            event.setChainCode(chainCode);
            event.setCoinType(nativeCoinType);
            event.setFromAddress(normalizeAddress(tx.getFrom()));
            event.setToAddress(normalizeAddress(tx.getTo()));
            event.setRawAmount(tx.getValue().toString());
            event.setTxHash(tx.getHash());
            event.setBlockNumber(block.getNumber().longValue());
            events.add(event);
        }
        return events;
    }

    // 解析区块内 ERC-20 Transfer 日志
    private List<ChainTransferEvent> parseTokenTransfers(Web3j web3j, long blockNum) throws Exception {
        List<ChainTransferEvent> events = new ArrayList<>();
        List<String> contracts = tokenContractProvider.listContracts(chainCode);
        if (contracts.isEmpty()) {
            return events;
        }

        DefaultBlockParameter blockParam = DefaultBlockParameter.valueOf(BigInteger.valueOf(blockNum));
        EthFilter filter = new EthFilter(blockParam, blockParam, contracts);
        filter.addSingleTopic(TRANSFER_TOPIC);
        EthLog ethLog = web3j.ethGetLogs(filter).send();

        for (EthLog.LogResult<?> logResult : ethLog.getLogs()) {
            org.web3j.protocol.core.methods.response.Log transferLog =
                    (org.web3j.protocol.core.methods.response.Log) logResult.get();
            List<String> topics = transferLog.getTopics();
            if (topics.size() < 3) {
                continue;
            }

            ChainTransferEvent event = new ChainTransferEvent();
            event.setChainCode(chainCode);
            event.setCoinType(transferLog.getAddress().toLowerCase());
            event.setFromAddress("0x" + topics.get(1).substring(26).toLowerCase());
            event.setToAddress("0x" + topics.get(2).substring(26).toLowerCase());
            event.setRawAmount(new BigInteger(transferLog.getData().substring(2), 16).toString());
            event.setTxHash(transferLog.getTransactionHash());
            event.setBlockNumber(blockNum);
            events.add(event);
        }
        return events;
    }

    // EVM 地址统一小写
    private String normalizeAddress(String address) {
        if (address == null) {
            return null;
        }
        return address.toLowerCase();
    }
}
