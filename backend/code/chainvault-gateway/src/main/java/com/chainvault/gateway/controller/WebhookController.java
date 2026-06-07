package com.chainvault.gateway.controller;

import com.chainvault.common.result.ApiResult;
import com.chainvault.core.domain.dto.WebhookUpsertReq;
import com.chainvault.core.domain.vo.WebhookSecretVO;
import com.chainvault.core.domain.vo.WebhookVO;
import com.chainvault.core.service.WebhookConfigService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * Webhook 配置 API
 *
 * @author chainvault
 * @date 2026-06-05
 */
@RestController
@RequestMapping("/api/v1/webhooks")
@RequiredArgsConstructor
public class WebhookController {

    private final WebhookConfigService webhookConfigService;

    /**
     * 注册或更新 Webhook（支持密钥轮换）
     *
     * @param req 配置请求
     * @return 配置结果（新建/轮换时返回 secretKey）
     */
    @PostMapping
    public ApiResult<WebhookSecretVO> upsert(@Valid @RequestBody WebhookUpsertReq req) {
        return ApiResult.ok(webhookConfigService.upsert(req));
    }

    /**
     * 查询商户 Webhook 列表
     *
     * @param merchantId 商户号
     * @return 配置列表
     */
    @GetMapping
    public ApiResult<List<WebhookVO>> list(@RequestParam String merchantId) {
        return ApiResult.ok(webhookConfigService.listByMerchant(merchantId));
    }
}
