package com.chainvault.admin.controller;

import com.chainvault.common.result.ApiResult;
import com.chainvault.common.result.PageResult;
import com.chainvault.core.domain.dto.AdminWebhookSaveReq;
import com.chainvault.core.domain.dto.AdminWebhookTestReq;
import com.chainvault.core.domain.vo.AdminWebhookVO;
import com.chainvault.core.domain.vo.WebhookTestResultVO;
import com.chainvault.core.service.AdminWebhookService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Webhook 管理 API（运营后台）
 *
 * @author chainvault
 * @date 2026-06-05
 */
@RestController
@RequestMapping("/admin/api/v1/webhooks")
@RequiredArgsConstructor
public class WebhookController {

    private final AdminWebhookService adminWebhookService;

    /**
     * Webhook 配置列表
     *
     * @param page       页码
     * @param size       每页条数
     * @param merchantId 商户号
     * @return 分页结果
     */
    @GetMapping
    public ApiResult<PageResult<AdminWebhookVO>> list(
            @RequestParam(value = "page", defaultValue = "1") int page,
            @RequestParam(value = "size", defaultValue = "20") int size,
            @RequestParam(value = "merchantId", required = false) String merchantId) {
        return ApiResult.ok(adminWebhookService.list(page, size, merchantId));
    }

    /**
     * 创建 Webhook
     *
     * @param req 创建请求
     * @return 配置视图
     */
    @PostMapping
    public ApiResult<AdminWebhookVO> create(@RequestBody AdminWebhookSaveReq req) {
        return ApiResult.ok(adminWebhookService.create(req));
    }

    /**
     * 更新 Webhook
     *
     * @param webhookId 标识
     * @param req       更新请求
     * @return 配置视图
     */
    @PutMapping("/{webhookId}")
    public ApiResult<AdminWebhookVO> update(
            @PathVariable("webhookId") String webhookId,
            @RequestBody AdminWebhookSaveReq req) {
        return ApiResult.ok(adminWebhookService.update(webhookId, req));
    }

    /**
     * 删除 Webhook
     *
     * @param webhookId 标识
     * @return 空结果
     */
    @DeleteMapping("/{webhookId}")
    public ApiResult<Void> delete(@PathVariable("webhookId") String webhookId) {
        adminWebhookService.delete(webhookId);
        return ApiResult.ok(null);
    }

    /**
     * 测试 Webhook
     *
     * @param req 测试请求
     * @return 推送结果
     */
    @PostMapping("/test")
    public ApiResult<WebhookTestResultVO> test(@Valid @RequestBody AdminWebhookTestReq req) {
        return ApiResult.ok(adminWebhookService.test(req));
    }

    /**
     * Webhook 投递日志
     *
     * @param page      页码
     * @param size      每页条数
     * @param webhookId 标识
     * @return 空分页
     */
    @GetMapping("/logs")
    public ApiResult<PageResult<Object>> logs(
            @RequestParam(value = "page", defaultValue = "1") int page,
            @RequestParam(value = "size", defaultValue = "20") int size,
            @RequestParam(value = "webhookId", required = false) String webhookId) {
        return ApiResult.ok(adminWebhookService.listLogs(page, size, webhookId));
    }
}
