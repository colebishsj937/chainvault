package com.chainvault.core.service.impl;

import com.chainvault.common.constants.QueueNames;
import com.chainvault.common.constants.WebhookConstants;
import com.chainvault.common.dto.WebhookQueueMessage;
import com.chainvault.common.enums.CallbackStatus;
import com.chainvault.common.enums.TransactionStatus;
import com.chainvault.common.redis.RedisMessageQueue;
import com.chainvault.common.util.WebhookSignUtil;
import com.chainvault.core.domain.entity.CoinConfig;
import com.chainvault.core.domain.entity.TransactionRecord;
import com.chainvault.core.domain.entity.WebhookConfig;
import com.chainvault.core.service.CoinConfigService;
import com.chainvault.core.service.TransactionRecordService;
import com.chainvault.core.service.WebhookConfigService;
import com.chainvault.core.service.WebhookDeliveryService;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;

import java.time.Instant;
import java.util.Set;

/**
 * Webhook 异步投递实现
 *
 * @author chainvault
 * @date 2026-06-05
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class WebhookDeliveryServiceImpl implements WebhookDeliveryService {

    private final RedisMessageQueue messageQueue;
    private final StringRedisTemplate redis;
    private final ObjectMapper objectMapper;
    private final WebhookConfigService webhookConfigService;
    private final TransactionRecordService transactionRecordService;
    private final CoinConfigService coinConfigService;
    private final RestTemplate webhookRestTemplate;

    /**
     * 消费主队列
     */
    @Override
    public void processPending() {
        String json = messageQueue.pop(QueueNames.WEBHOOK, 1);
        if (json == null || json.isBlank()) {
            return;
        }
        deliverSafely(json);
    }

    /**
     * 消费重试队列
     */
    @Override
    public void processRetries() {
        long now = Instant.now().getEpochSecond();
        Set<String> dueItems = redis.opsForZSet().rangeByScore(
                WebhookConstants.RETRY_QUEUE, 0, now, 0, 5);
        if (dueItems == null || dueItems.isEmpty()) {
            return;
        }
        for (String json : dueItems) {
            redis.opsForZSet().remove(WebhookConstants.RETRY_QUEUE, json);
            deliverSafely(json);
        }
    }

    // 安全投递包装
    private void deliverSafely(String json) {
        try {
            WebhookQueueMessage message = objectMapper.readValue(json, WebhookQueueMessage.class);
            deliver(message, json);
        } catch (Exception e) {
            log.error("Webhook 消息解析失败 payload={}", json, e);
        }
    }

    // 执行 HTTP 投递
    @Transactional(rollbackFor = Exception.class)
    protected void deliver(WebhookQueueMessage message, String rawJson) {
        // 1. 解析回调配置
        WebhookConfig config = webhookConfigService.resolveConfig(
                message.getMerchantId(), message.getEvent());
        if (config == null) {
            log.warn("Webhook 未配置 merchant={} event={}", message.getMerchantId(), message.getEvent());
            return;
        }

        int attempt = message.getAttempt() == null ? 0 : message.getAttempt();
        int maxRetry = Math.min(config.getRetryTimes(), WebhookConstants.MAX_RETRY);

        try {
            // 2. 构建回调 JSON 并签名
            TransactionRecord record = transactionRecordService.findByTradeId(message.getTradeId());
            String body = buildPayload(message, record);
            String sign = WebhookSignUtil.sign(body, config.getSecretKey());
            ObjectNode root = (ObjectNode) objectMapper.readTree(body);
            root.put("sign", sign);
            String signedBody = objectMapper.writeValueAsString(root);

            // 3. HTTP POST
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            HttpEntity<String> entity = new HttpEntity<>(signedBody, headers);
            ResponseEntity<String> response = webhookRestTemplate.postForEntity(
                    config.getCallbackUrl(), entity, String.class);

            if (response.getStatusCode().is2xxSuccessful()) {
                onDeliverSuccess(message, record, attempt);
                log.info("Webhook 投递成功 event={} tradeId={} url={}",
                        message.getEvent(), message.getTradeId(), config.getCallbackUrl());
            } else {
                scheduleRetry(message, rawJson, attempt, maxRetry,
                        "HTTP " + response.getStatusCode().value());
            }
        } catch (Exception e) {
            scheduleRetry(message, rawJson, attempt, maxRetry, e.getMessage());
        }
    }

    // 投递成功处理
    private void onDeliverSuccess(WebhookQueueMessage message,
                                  TransactionRecord record,
                                  int attempt) {
        if (record != null) {
            transactionRecordService.updateCallbackStatus(
                    record.getId(), CallbackStatus.SUCCESS.getCode(), attempt + 1);
            if (record.getStatus() == TransactionStatus.SUCCESS.getCode()) {
                transactionRecordService.updateStatus(record.getId(), TransactionStatus.NOTIFIED.getCode());
            }
        }
    }

    // 安排指数退避重试
    private void scheduleRetry(WebhookQueueMessage message,
                               String rawJson,
                               int attempt,
                               int maxRetry,
                               String reason) {
        int nextAttempt = attempt + 1;
        if (nextAttempt >= maxRetry) {
            TransactionRecord record = transactionRecordService.findByTradeId(message.getTradeId());
            if (record != null) {
                transactionRecordService.updateCallbackStatus(
                        record.getId(), CallbackStatus.FAILED.getCode(), nextAttempt);
            }
            log.error("Webhook 投递失败且已达最大重试 event={} tradeId={} reason={}",
                    message.getEvent(), message.getTradeId(), reason);
            return;
        }

        try {
            message.setAttempt(nextAttempt);
            String retryJson = objectMapper.writeValueAsString(message);
            long delaySeconds = 1L << nextAttempt;
            long executeAt = Instant.now().getEpochSecond() + delaySeconds;
            redis.opsForZSet().add(WebhookConstants.RETRY_QUEUE, retryJson, executeAt);
            log.warn("Webhook 投递失败，{}s 后第 {} 次重试 event={} tradeId={} reason={}",
                    delaySeconds, nextAttempt, message.getEvent(), message.getTradeId(), reason);
        } catch (Exception e) {
            log.error("Webhook 重试入队失败 tradeId={}", message.getTradeId(), e);
        }
    }

    // 构建不含 sign 的回调 JSON
    private String buildPayload(WebhookQueueMessage message, TransactionRecord record) throws Exception {
        ObjectNode node = objectMapper.createObjectNode();
        node.put("event", message.getEvent());
        node.put("tradeId", message.getTradeId());
        node.put("merchantId", message.getMerchantId());
        node.put("timestamp", Instant.now().getEpochSecond());

        if (record != null) {
            node.put("bizId", record.getBizId() == null ? "" : record.getBizId());
            node.put("address", record.getToAddress());
            node.put("amount", record.getAmount().toPlainString());
            node.put("rawAmount", record.getRawAmount());
            node.put("coinType", record.getCoinType());
            node.put("chain", record.getChainCode());
            if (record.getTxHash() != null) {
                node.put("txHash", record.getTxHash());
            }
            if (record.getBlockNumber() != null) {
                node.put("blockNumber", record.getBlockNumber());
            }
            node.put("confirms", record.getConfirms() == null ? 0 : record.getConfirms());
            node.put("requiredConfirms", record.getRequiredConfirms());
            if (record.getFee() != null) {
                node.put("fee", record.getFee().toPlainString());
            }
            if (record.getMemo() != null) {
                node.put("memo", record.getMemo());
            }
            CoinConfig coin = coinConfigService.getByCoinType(record.getCoinType());
            if (coin != null) {
                node.put("symbol", coin.getSymbol());
            }
        } else {
            node.put("bizId", message.getBizId() == null ? "" : message.getBizId());
            node.put("address", message.getToAddress() == null ? "" : message.getToAddress());
            node.put("amount", message.getAmount() == null ? "" : message.getAmount());
            node.put("coinType", message.getCoinType());
            node.put("chain", message.getChainCode());
            if (message.getTxHash() != null) {
                node.put("txHash", message.getTxHash());
            }
            node.put("confirms", message.getConfirms() == null ? 0 : message.getConfirms());
            node.put("requiredConfirms", message.getRequiredConfirms() == null ? 0 : message.getRequiredConfirms());
        }
        return objectMapper.writeValueAsString(node);
    }
}
