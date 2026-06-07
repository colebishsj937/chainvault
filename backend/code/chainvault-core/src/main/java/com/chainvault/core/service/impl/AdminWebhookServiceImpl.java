package com.chainvault.core.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.chainvault.common.exception.BusinessException;
import com.chainvault.common.result.PageResult;
import com.chainvault.common.util.WebhookSignUtil;
import com.chainvault.core.domain.dto.AdminWebhookSaveReq;
import com.chainvault.core.domain.dto.AdminWebhookTestReq;
import com.chainvault.core.domain.dto.WebhookUpsertReq;
import com.chainvault.core.domain.entity.WebhookConfig;
import com.chainvault.core.domain.vo.AdminWebhookVO;
import com.chainvault.core.domain.vo.WebhookSecretVO;
import com.chainvault.core.domain.vo.WebhookTestResultVO;
import com.chainvault.core.mapper.WebhookConfigMapper;
import com.chainvault.core.service.AdminWebhookService;
import com.chainvault.core.service.WebhookConfigService;
import com.chainvault.core.util.SecretMaskUtil;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 运营后台 Webhook 管理业务实现
 *
 * @author chainvault
 * @date 2026-06-05
 */
@Service
@RequiredArgsConstructor
public class AdminWebhookServiceImpl implements AdminWebhookService {

    private static final int MAX_PAGE_SIZE = 100;
    private static final String DEFAULT_MERCHANT_ID = "300001";

    private final WebhookConfigMapper webhookConfigMapper;
    private final WebhookConfigService webhookConfigService;
    private final RestTemplate webhookRestTemplate;
    private final ObjectMapper objectMapper;

    /**
     * 分页查询 Webhook 配置
     *
     * @param page       页码
     * @param size       每页条数
     * @param merchantId 商户过滤
     * @return 分页结果
     */
    @Transactional(readOnly = true)
    @Override
    public PageResult<AdminWebhookVO> list(int page, int size, String merchantId) {
        int safePage = Math.max(page, 1);
        int safeSize = Math.min(Math.max(size, 1), MAX_PAGE_SIZE);

        // 1. 查询并聚合
        List<WebhookConfig> configs = loadConfigs(merchantId);
        List<AdminWebhookVO> grouped = groupConfigs(configs, true);

        // 2. 内存分页
        int from = (safePage - 1) * safeSize;
        if (from >= grouped.size()) {
            return PageResult.of(safePage, safeSize, grouped.size(), List.of());
        }
        int to = Math.min(from + safeSize, grouped.size());
        return PageResult.of(safePage, safeSize, grouped.size(), grouped.subList(from, to));
    }

    /**
     * 创建 Webhook
     *
     * @param req 创建请求
     * @return 聚合视图（含完整密钥）
     */
    @Transactional(rollbackFor = Exception.class)
    @Override
    public AdminWebhookVO create(AdminWebhookSaveReq req) {
        // 1. 校验参数
        String merchantId = resolveMerchantId(req.getMerchantId());
        validateSaveReq(req, true);

        // 2. 逐事件写入
        String plainSecret = null;
        for (String eventType : req.getEvents()) {
            WebhookUpsertReq upsertReq = buildUpsertReq(merchantId, eventType, req);
            WebhookSecretVO result = webhookConfigService.upsert(upsertReq);
            if (plainSecret == null && StringUtils.hasText(result.getSecretKey())) {
                plainSecret = result.getSecretKey();
            }
        }

        // 3. 返回聚合视图
        WebhookGroup group = resolveGroupByUrl(merchantId, req.getUrl());
        AdminWebhookVO vo = toVo(group, false);
        if (StringUtils.hasText(plainSecret)) {
            vo.setSecret(plainSecret);
        }
        return vo;
    }

    /**
     * 更新 Webhook
     *
     * @param webhookId 标识
     * @param req       更新请求
     * @return 聚合视图
     */
    @Transactional(rollbackFor = Exception.class)
    @Override
    public AdminWebhookVO update(String webhookId, AdminWebhookSaveReq req) {
        // 1. 定位现有分组
        WebhookGroup group = resolveGroupByWebhookId(webhookId);
        String merchantId = group.merchantId();
        String oldUrl = group.callbackUrl();

        // 2. 删除被移除的事件
        if (req.getEvents() != null) {
            List<String> targetEvents = normalizeEvents(req.getEvents());
            for (WebhookConfig config : group.configs()) {
                if (!targetEvents.contains(config.getEventType())) {
                    webhookConfigMapper.deleteById(config.getId());
                }
            }
            for (String eventType : targetEvents) {
                WebhookUpsertReq upsertReq = new WebhookUpsertReq();
                upsertReq.setMerchantId(merchantId);
                upsertReq.setEventType(eventType);
                upsertReq.setCallbackUrl(StringUtils.hasText(req.getUrl()) ? req.getUrl() : oldUrl);
                if (req.getEnabled() != null) {
                    upsertReq.setIsEnabled(req.getEnabled() ? 1 : 0);
                }
                if (StringUtils.hasText(req.getSecret())) {
                    upsertReq.setSecretKey(req.getSecret());
                }
                webhookConfigService.upsert(upsertReq);
            }
        } else if (StringUtils.hasText(req.getUrl()) || req.getEnabled() != null || StringUtils.hasText(req.getSecret())) {
            for (WebhookConfig config : group.configs()) {
                WebhookUpsertReq upsertReq = new WebhookUpsertReq();
                upsertReq.setMerchantId(merchantId);
                upsertReq.setEventType(config.getEventType());
                upsertReq.setCallbackUrl(StringUtils.hasText(req.getUrl()) ? req.getUrl() : config.getCallbackUrl());
                if (req.getEnabled() != null) {
                    upsertReq.setIsEnabled(req.getEnabled() ? 1 : 0);
                }
                if (StringUtils.hasText(req.getSecret())) {
                    upsertReq.setSecretKey(req.getSecret());
                }
                webhookConfigService.upsert(upsertReq);
            }
        }

        // 3. 返回最新聚合
        String url = StringUtils.hasText(req.getUrl()) ? req.getUrl() : oldUrl;
        return toVo(resolveGroupByUrl(merchantId, url), false);
    }

    /**
     * 删除 Webhook
     *
     * @param webhookId 标识
     */
    @Transactional(rollbackFor = Exception.class)
    @Override
    public void delete(String webhookId) {
        WebhookGroup group = resolveGroupByWebhookId(webhookId);
        for (WebhookConfig config : group.configs()) {
            webhookConfigMapper.deleteById(config.getId());
        }
    }

    /**
     * 同步测试推送
     *
     * @param req 测试请求
     * @return 推送结果
     */
    @Override
    public WebhookTestResultVO test(AdminWebhookTestReq req) {
        // 1. 解析配置
        WebhookGroup group = resolveGroupByWebhookId(req.getWebhookId());
        WebhookConfig config = group.configs().stream()
                .filter(item -> req.getEventType().equals(item.getEventType()))
                .findFirst()
                .orElseThrow(() -> new BusinessException("事件未配置: " + req.getEventType()));

        String body = StringUtils.hasText(req.getPayload())
                ? req.getPayload()
                : "{\"event\":\"" + req.getEventType() + "\",\"test\":true}";

        WebhookTestResultVO result = new WebhookTestResultVO();
        long start = System.currentTimeMillis();
        try {
            // 2. 签名并 POST
            String sign = WebhookSignUtil.sign(body, config.getSecretKey());
            ObjectNode root = (ObjectNode) objectMapper.readTree(body);
            root.put("sign", sign);
            String signedBody = objectMapper.writeValueAsString(root);

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            HttpEntity<String> entity = new HttpEntity<>(signedBody, headers);
            ResponseEntity<String> response = webhookRestTemplate.postForEntity(
                    config.getCallbackUrl(), entity, String.class);

            result.setDuration(System.currentTimeMillis() - start);
            result.setStatusCode(response.getStatusCode().value());
            result.setSuccess(response.getStatusCode().is2xxSuccessful());
            result.setResponseBody(truncate(response.getBody()));
        } catch (Exception e) {
            result.setDuration(System.currentTimeMillis() - start);
            result.setStatusCode(0);
            result.setSuccess(false);
            result.setResponseBody(truncate(e.getMessage()));
        }
        return result;
    }

    /**
     * Webhook 投递日志（当前返回空分页）
     *
     * @param page      页码
     * @param size      每页条数
     * @param webhookId 过滤标识
     * @return 空分页
     */
    @Transactional(readOnly = true)
    @Override
    public PageResult<Object> listLogs(int page, int size, String webhookId) {
        int safePage = Math.max(page, 1);
        int safeSize = Math.min(Math.max(size, 1), MAX_PAGE_SIZE);
        return PageResult.of(safePage, safeSize, 0, List.of());
    }

    // 查询配置列表
    private List<WebhookConfig> loadConfigs(String merchantId) {
        LambdaQueryWrapper<WebhookConfig> wrapper = new LambdaQueryWrapper<>();
        if (StringUtils.hasText(merchantId)) {
            wrapper.eq(WebhookConfig::getMerchantId, merchantId);
        }
        wrapper.orderByAsc(WebhookConfig::getMerchantId)
                .orderByAsc(WebhookConfig::getCallbackUrl)
                .orderByAsc(WebhookConfig::getEventType);
        return webhookConfigMapper.selectList(wrapper);
    }

    // 按 URL 聚合配置
    private List<AdminWebhookVO> groupConfigs(List<WebhookConfig> configs, boolean maskSecret) {
        Map<String, WebhookGroup> groups = new LinkedHashMap<>();
        for (WebhookConfig config : configs) {
            String key = config.getMerchantId() + "|" + config.getCallbackUrl();
            groups.computeIfAbsent(key, ignored -> new WebhookGroup(
                    config.getMerchantId(), config.getCallbackUrl(), new ArrayList<>()))
                    .configs().add(config);
        }
        return groups.values().stream()
                .map(group -> toVo(group, maskSecret))
                .sorted(Comparator.comparing(AdminWebhookVO::getMerchantId)
                        .thenComparing(AdminWebhookVO::getUrl))
                .collect(Collectors.toList());
    }

    // 转换为聚合视图
    private AdminWebhookVO toVo(WebhookGroup group, boolean maskSecret) {
        List<String> events = group.configs().stream()
                .map(WebhookConfig::getEventType)
                .sorted()
                .toList();
        WebhookConfig first = group.configs().stream()
                .min(Comparator.comparing(WebhookConfig::getCreatedAt))
                .orElse(group.configs().get(0));

        AdminWebhookVO vo = new AdminWebhookVO();
        vo.setWebhookId(buildWebhookId(group.merchantId(), events.get(0)));
        vo.setMerchantId(group.merchantId());
        vo.setUrl(group.callbackUrl());
        vo.setEvents(events);
        vo.setEnabled(group.configs().stream().allMatch(item -> item.getIsEnabled() == 1));
        vo.setCreatedAt(first.getCreatedAt() != null ? first.getCreatedAt() : LocalDateTime.now());
        String secret = first.getSecretKey();
        vo.setSecret(maskSecret ? SecretMaskUtil.maskWebhookSecret(secret) : secret);
        return vo;
    }

    // 解析 webhookId
    private WebhookGroup resolveGroupByWebhookId(String webhookId) {
        ParsedWebhookId parsed = parseWebhookId(webhookId);
        WebhookConfig anchor = webhookConfigMapper.selectByMerchantEvent(
                parsed.merchantId(), parsed.eventType());
        if (anchor == null) {
            throw new BusinessException("Webhook 不存在: " + webhookId);
        }
        return resolveGroupByUrl(parsed.merchantId(), anchor.getCallbackUrl());
    }

    // 按商户与 URL 加载分组
    private WebhookGroup resolveGroupByUrl(String merchantId, String callbackUrl) {
        List<WebhookConfig> configs = webhookConfigMapper.selectList(
                new LambdaQueryWrapper<WebhookConfig>()
                        .eq(WebhookConfig::getMerchantId, merchantId)
                        .eq(WebhookConfig::getCallbackUrl, callbackUrl));
        if (configs.isEmpty()) {
            throw new BusinessException("Webhook 不存在");
        }
        return new WebhookGroup(merchantId, callbackUrl, configs);
    }

    // 解析 webhookId 结构
    private ParsedWebhookId parseWebhookId(String webhookId) {
        if (!StringUtils.hasText(webhookId) || !webhookId.contains(":")) {
            throw new BusinessException("webhookId 格式无效");
        }
        int index = webhookId.indexOf(':');
        return new ParsedWebhookId(webhookId.substring(0, index), webhookId.substring(index + 1));
    }

    // 构建 webhookId
    private String buildWebhookId(String merchantId, String eventType) {
        return merchantId + ":" + eventType;
    }

    // 校验保存请求
    private void validateSaveReq(AdminWebhookSaveReq req, boolean creating) {
        if (creating) {
            if (!StringUtils.hasText(req.getUrl())) {
                throw new BusinessException("url 不能为空");
            }
            if (req.getEvents() == null || req.getEvents().isEmpty()) {
                throw new BusinessException("events 不能为空");
            }
        }
    }

    // 解析商户号
    private String resolveMerchantId(String merchantId) {
        if (StringUtils.hasText(merchantId)) {
            return merchantId;
        }
        return DEFAULT_MERCHANT_ID;
    }

    // 构建 upsert 请求
    private WebhookUpsertReq buildUpsertReq(String merchantId, String eventType, AdminWebhookSaveReq req) {
        WebhookUpsertReq upsertReq = new WebhookUpsertReq();
        upsertReq.setMerchantId(merchantId);
        upsertReq.setEventType(eventType);
        upsertReq.setCallbackUrl(req.getUrl());
        upsertReq.setSecretKey(req.getSecret());
        upsertReq.setIsEnabled(req.getEnabled() == null || req.getEnabled() ? 1 : 0);
        return upsertReq;
    }

    // 规范化事件列表
    private List<String> normalizeEvents(List<String> events) {
        return events.stream()
                .filter(StringUtils::hasText)
                .distinct()
                .sorted()
                .toList();
    }

    // 截断响应文本
    private String truncate(String value) {
        if (value == null) {
            return null;
        }
        if (value.length() <= 500) {
            return value;
        }
        return value.substring(0, 500);
    }

    private record WebhookGroup(String merchantId, String callbackUrl, List<WebhookConfig> configs) {
    }

    private record ParsedWebhookId(String merchantId, String eventType) {
    }
}
