package com.chainvault.core.service;

import com.chainvault.core.domain.dto.WebhookUpsertReq;
import com.chainvault.core.domain.entity.WebhookConfig;
import com.chainvault.core.domain.vo.WebhookSecretVO;
import com.chainvault.core.domain.vo.WebhookVO;

import java.util.List;

/**
 * Webhook 配置业务接口
 *
 * @author chainvault
 * @date 2026-06-05
 */
public interface WebhookConfigService {

    /**
     * 注册或更新 Webhook 配置
     *
     * @param req 请求
     * @return 配置视图（轮换密钥时附带 secret）
     */
    WebhookSecretVO upsert(WebhookUpsertReq req);

    /**
     * 查询商户全部 Webhook 配置
     *
     * @param merchantId 商户号
     * @return 配置列表
     */
    List<WebhookVO> listByMerchant(String merchantId);

    /**
     * 解析有效回调配置（事件级优先，否则商户默认回调）
     *
     * @param merchantId 商户号
     * @param eventType  事件类型
     * @return 配置，可能为 null
     */
    WebhookConfig resolveConfig(String merchantId, String eventType);
}
