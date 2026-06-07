package com.chainvault.chainnode.service.impl;

import com.chainvault.chainnode.config.RpcHttpClientFactory;
import com.chainvault.chainnode.dto.BlockScanResult;
import com.chainvault.chainnode.dto.ChainNodeSettings;
import com.chainvault.chainnode.dto.ChainTransferEvent;
import com.chainvault.chainnode.service.BlockScanner;
import com.chainvault.chainnode.service.ChainNodeSettingsProvider;
import com.chainvault.common.constants.ChainCode;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Base64;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;

/**
 * BTC 链区块扫描器（Bitcoin Core RPC）
 *
 * @author chainvault
 * @date 2026-06-05
 */
@Component
@Slf4j
public class BtcBlockScanner implements BlockScanner {

    private final ChainNodeSettingsProvider chainNodeSettingsProvider;
    private final ObjectMapper objectMapper;
    private final HttpClient httpClient;
    private final AtomicLong requestId = new AtomicLong(1);

    public BtcBlockScanner(ChainNodeSettingsProvider chainNodeSettingsProvider,
                           ObjectMapper objectMapper,
                           RpcHttpClientFactory rpcHttpClientFactory) {
        this.chainNodeSettingsProvider = chainNodeSettingsProvider;
        this.objectMapper = objectMapper;
        this.httpClient = rpcHttpClientFactory.createJavaHttpClient();
    }

    @Override
    public String chainCode() {
        return ChainCode.BTC;
    }

    @Override
    public boolean enabled() {
        return currentSettings()
                .map(settings -> settings.getRpcUrl() != null && !settings.getRpcUrl().isBlank())
                .orElse(false);
    }

    @Override
    public int requiredConfirms() {
        return currentSettings().map(ChainNodeSettings::getRequiredConfirms).orElse(6);
    }

    @Override
    public long latestBlockNumber() {
        try {
            JsonNode result = rpcCall("getblockcount", List.of());
            return result.asLong();
        } catch (Exception e) {
            throw new IllegalStateException("[BTC] 获取最新区块失败", e);
        }
    }

    @Override
    public BlockScanResult scanRange(long fromBlock, long toBlock) {
        BlockScanResult result = new BlockScanResult();
        List<ChainTransferEvent> events = new ArrayList<>();

        try {
            // 1. 遍历区块解析 vout 转账
            for (long blockNum = fromBlock; blockNum <= toBlock; blockNum++) {
                String blockHash = rpcCall("getblockhash", List.of(blockNum)).asText();
                JsonNode block = rpcCall("getblock", List.of(blockHash, 2));
                JsonNode transactions = block.path("tx");
                if (transactions.isArray()) {
                    for (JsonNode tx : transactions) {
                        events.addAll(parseBtcOutputs(tx, blockNum));
                    }
                }
                result.setLastScannedBlock(blockNum);
            }
        } catch (Exception e) {
            log.error("[BTC] 扫块异常 from={} to={}", fromBlock, toBlock, e);
        }

        result.setEvents(events);
        return result;
    }

    // 解析 BTC 交易输出
    private List<ChainTransferEvent> parseBtcOutputs(JsonNode tx, long blockNum) {
        List<ChainTransferEvent> events = new ArrayList<>();
        String txHash = tx.path("txid").asText();
        JsonNode vin = tx.path("vin");
        String fromAddress = "";
        if (vin.isArray() && !vin.isEmpty()) {
            fromAddress = vin.get(0).path("prevout").path("scriptPubKey").path("address").asText("");
        }

        JsonNode vout = tx.path("vout");
        if (!vout.isArray()) {
            return events;
        }

        for (JsonNode output : vout) {
            JsonNode script = output.path("scriptPubKey");
            String address = script.path("address").asText("");
            if (address.isBlank()) {
                continue;
            }
            BigDecimal btcAmount = BigDecimal.valueOf(output.path("value").asDouble());
            if (btcAmount.compareTo(BigDecimal.ZERO) <= 0) {
                continue;
            }

            ChainTransferEvent event = new ChainTransferEvent();
            event.setChainCode(ChainCode.BTC);
            event.setCoinType("BTC");
            event.setFromAddress(fromAddress);
            event.setToAddress(address);
            event.setRawAmount(btcAmount.movePointRight(8).toBigInteger().toString());
            event.setTxHash(txHash);
            event.setBlockNumber(blockNum);
            events.add(event);
        }
        return events;
    }

    // 调用 Bitcoin RPC
    private JsonNode rpcCall(String method, List<Object> params) throws Exception {
        Map<String, Object> payload = new HashMap<>();
        payload.put("jsonrpc", "1.0");
        payload.put("id", requestId.getAndIncrement());
        payload.put("method", method);
        payload.put("params", params);

        ChainNodeSettings settings = requireSettings();
        HttpRequest.Builder builder = HttpRequest.newBuilder()
                .uri(URI.create(settings.getRpcUrl()))
                .timeout(Duration.ofSeconds(20))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(objectMapper.writeValueAsString(payload)));

        String user = settings.getRpcUser();
        String password = settings.getRpcPassword();
        if (user != null && password != null) {
            String auth = Base64.getEncoder().encodeToString((user + ":" + password).getBytes());
            builder.header("Authorization", "Basic " + auth);
        }

        HttpResponse<String> response = httpClient.send(builder.build(), HttpResponse.BodyHandlers.ofString());
        JsonNode root = objectMapper.readTree(response.body());
        if (root.has("error") && !root.get("error").isNull()) {
            throw new IllegalStateException(root.get("error").toString());
        }
        return root.path("result");
    }

    // 获取当前 BTC 配置
    private java.util.Optional<ChainNodeSettings> currentSettings() {
        return chainNodeSettingsProvider.getSettings(ChainCode.BTC);
    }

    // 获取当前 BTC 配置（必须存在）
    private ChainNodeSettings requireSettings() {
        return currentSettings().orElseThrow(() -> new IllegalStateException("[BTC] 节点未配置"));
    }
}
