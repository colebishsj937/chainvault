package com.chainvault.chainnode.service.impl;

import com.chainvault.chainnode.config.RpcHttpClientFactory;
import com.chainvault.chainnode.dto.BlockScanResult;
import com.chainvault.chainnode.dto.ChainNodeSettings;
import com.chainvault.chainnode.dto.ChainTransferEvent;
import com.chainvault.chainnode.service.BlockScanner;
import com.chainvault.chainnode.service.ChainNodeSettingsProvider;
import com.chainvault.chainnode.service.TokenContractProvider;
import com.chainvault.chainnode.util.ChainNodeApiKeyRotator;
import com.chainvault.chainnode.util.TronAddressUtil;
import com.chainvault.common.constants.ChainCode;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.math.BigInteger;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * TRON 链区块扫描器（TronGrid API）
 *
 * @author chainvault
 * @date 2026-06-05
 */
@Component
@Slf4j
public class TronBlockScanner implements BlockScanner {

    private final ChainNodeSettingsProvider chainNodeSettingsProvider;
    private final ChainNodeApiKeyRotator chainNodeApiKeyRotator;
    private final TokenContractProvider tokenContractProvider;
    private final ObjectMapper objectMapper;
    private final HttpClient httpClient;

    public TronBlockScanner(ChainNodeSettingsProvider chainNodeSettingsProvider,
                            ChainNodeApiKeyRotator chainNodeApiKeyRotator,
                            TokenContractProvider tokenContractProvider,
                            ObjectMapper objectMapper,
                            RpcHttpClientFactory rpcHttpClientFactory) {
        this.chainNodeSettingsProvider = chainNodeSettingsProvider;
        this.chainNodeApiKeyRotator = chainNodeApiKeyRotator;
        this.tokenContractProvider = tokenContractProvider;
        this.objectMapper = objectMapper;
        this.httpClient = rpcHttpClientFactory.createJavaHttpClient();
    }

    @Override
    public String chainCode() {
        return ChainCode.TRON;
    }

    @Override
    public boolean enabled() {
        return currentSettings()
                .map(settings -> settings.getApiUrl() != null && !settings.getApiUrl().isBlank())
                .orElse(false);
    }

    @Override
    public int requiredConfirms() {
        return currentSettings().map(ChainNodeSettings::getRequiredConfirms).orElse(20);
    }

    @Override
    public long latestBlockNumber() {
        try {
            JsonNode root = postJson("/wallet/getnowblock", Map.of());
            return root.path("block_header").path("raw_data").path("number").asLong();
        } catch (Exception e) {
            throw new IllegalStateException("[TRON] 获取最新区块失败", e);
        }
    }

    @Override
    public BlockScanResult scanRange(long fromBlock, long toBlock) {
        BlockScanResult result = new BlockScanResult();
        List<ChainTransferEvent> events = new ArrayList<>();

        try {
            // 1. 遍历区块解析原生 TRX 与 TRC-20 转账
            for (long blockNum = fromBlock; blockNum <= toBlock; blockNum++) {
                events.addAll(parseTrxTransfers(blockNum));
                for (String contract : tokenContractProvider.listContracts(ChainCode.TRON)) {
                    events.addAll(parseTrc20Transfers(contract, blockNum));
                }
                result.setLastScannedBlock(blockNum);
            }
        } catch (Exception e) {
            log.error("[TRON] 扫块异常 from={} to={}", fromBlock, toBlock, e);
        }

        result.setEvents(events);
        return result;
    }

    // 解析区块内 TRX 原生转账
    private List<ChainTransferEvent> parseTrxTransfers(long blockNum) throws Exception {
        List<ChainTransferEvent> events = new ArrayList<>();
        JsonNode root = postJson("/wallet/getblockbynum", Map.of("num", blockNum));
        JsonNode transactions = root.path("transactions");
        if (!transactions.isArray()) {
            return events;
        }

        for (JsonNode tx : transactions) {
            JsonNode contract = tx.path("raw_data").path("contract").path(0);
            if (!"TransferContract".equals(contract.path("type").asText())) {
                continue;
            }
            JsonNode value = contract.path("parameter").path("value");
            String toHex = value.path("to_address").asText();
            long amount = value.path("amount").asLong();
            if (amount <= 0) {
                continue;
            }

            ChainTransferEvent event = new ChainTransferEvent();
            event.setChainCode(ChainCode.TRON);
            event.setCoinType("TRX");
            event.setFromAddress(TronAddressUtil.hexToBase58(value.path("owner_address").asText()));
            event.setToAddress(TronAddressUtil.hexToBase58(toHex));
            event.setRawAmount(String.valueOf(amount));
            event.setTxHash(tx.path("txID").asText());
            event.setBlockNumber(blockNum);
            events.add(event);
        }
        return events;
    }

    // 解析 TRC-20 Transfer 事件
    private List<ChainTransferEvent> parseTrc20Transfers(String contract, long blockNum) throws Exception {
        List<ChainTransferEvent> events = new ArrayList<>();
        String url = requireSettings().getApiUrl()
                + "/v1/contracts/" + contract + "/events"
                + "?event_name=Transfer&block_number=" + blockNum + "&limit=200";
        JsonNode root = getJson(url);
        JsonNode data = root.path("data");
        if (!data.isArray()) {
            return events;
        }

        for (JsonNode item : data) {
            JsonNode result = item.path("result");
            ChainTransferEvent event = new ChainTransferEvent();
            event.setChainCode(ChainCode.TRON);
            event.setCoinType(contract);
            // TronGrid TRC-20 事件返回 hex 地址，需转为 Base58 与 deposit_address 一致
            event.setFromAddress(TronAddressUtil.hexToBase58(result.path("from").asText()));
            event.setToAddress(TronAddressUtil.hexToBase58(result.path("to").asText()));
            event.setRawAmount(new BigInteger(result.path("value").asText()).toString());
            event.setTxHash(item.path("transaction_id").asText());
            event.setBlockNumber(blockNum);
            events.add(event);
        }
        return events;
    }

    // 发送 POST JSON 请求
    private JsonNode postJson(String path, Map<String, Object> body) throws Exception {
        String json = objectMapper.writeValueAsString(body);
        HttpRequest.Builder builder = HttpRequest.newBuilder()
                .uri(URI.create(requireSettings().getApiUrl() + path))
                .timeout(Duration.ofSeconds(15))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(json));
        addApiKeyHeader(builder);
        HttpResponse<String> response = httpClient.send(builder.build(), HttpResponse.BodyHandlers.ofString());
        return objectMapper.readTree(response.body());
    }

    // 发送 GET 请求
    private JsonNode getJson(String url) throws Exception {
        HttpRequest.Builder builder = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .timeout(Duration.ofSeconds(15))
                .GET();
        addApiKeyHeader(builder);
        HttpResponse<String> response = httpClient.send(builder.build(), HttpResponse.BodyHandlers.ofString());
        return objectMapper.readTree(response.body());
    }

    // 附加 TronGrid API Key（轮询）
    private void addApiKeyHeader(HttpRequest.Builder builder) {
        ChainNodeSettings settings = currentSettings().orElse(null);
        String apiKey = chainNodeApiKeyRotator.nextApiKey(settings);
        if (apiKey != null && !apiKey.isBlank()) {
            builder.header("TRON-PRO-API-KEY", apiKey);
        }
    }

    // 获取当前 TRON 配置
    private java.util.Optional<ChainNodeSettings> currentSettings() {
        return chainNodeSettingsProvider.getSettings(ChainCode.TRON);
    }

    // 获取当前 TRON 配置（必须存在）
    private ChainNodeSettings requireSettings() {
        return currentSettings().orElseThrow(() -> new IllegalStateException("[TRON] 节点未配置"));
    }
}
