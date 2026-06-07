package com.chainvault.core.service;

import com.chainvault.common.result.PageResult;
import com.chainvault.core.domain.dto.AdminWebhookSaveReq;
import com.chainvault.core.domain.dto.AdminWebhookTestReq;
import com.chainvault.core.domain.vo.AdminWebhookVO;
import com.chainvault.core.domain.vo.WebhookTestResultVO;

/**
 * 运营后台 Webhook 管理业务接口
 *
 * @author chainvault
 * @date 2026-06-05
 */
public interface AdminWebhookService {

    /**
     * 分页查询 Webhook 配置
     *
     * @param page       页码
     * @param size       每页条数
     * @param merchantId 商户过滤
     * @return 分页结果
     */
    PageResult<AdminWebhookVO> list(int page, int size, String merchantId);

    /**
     * 创建 Webhook
     *
     * @param req 创建请求
     * @return 聚合视图（含完整密钥）
     */
    AdminWebhookVO create(AdminWebhookSaveReq req);

    /**
     * 更新 Webhook
     *
     * @param webhookId 标识
     * @param req       更新请求
     * @return 聚合视图
     */
    AdminWebhookVO update(String webhookId, AdminWebhookSaveReq req);

    /**
     * 删除 Webhook
     *
     * @param webhookId 标识
     */
    void delete(String webhookId);

    /**
     * 同步测试推送
     *
     * @param req 测试请求
     * @return 推送结果
     */
    WebhookTestResultVO test(AdminWebhookTestReq req);

    /**
     * Webhook 投递日志（当前返回空分页）
     *
     * @param page      页码
     * @param size      每页条数
     * @param webhookId 过滤标识
     * @return 空分页
     */
    PageResult<Object> listLogs(int page, int size, String webhookId);
}
