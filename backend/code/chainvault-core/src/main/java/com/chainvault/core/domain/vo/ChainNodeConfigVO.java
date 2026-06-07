package com.chainvault.core.domain.vo;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

/**
 * 链节点配置视图（密钥脱敏）
 *
 * @author chainvault
 * @date 2026-06-05
 */
@Data
public class ChainNodeConfigVO {

    /** 链标识 */
    private String chainCode;

    /**
     * 节点服务商
     * @see com.chainvault.common.enums.ChainNodeProvider
     */
    private String provider;

    /** 自定义 RPC 地址 */
    private String rpcUrl;

    /** 脱敏后的 API Key */
    private String apiKeyMasked;

    /** 是否已配置 API Key */
    private boolean apiKeyConfigured;

    /** 已配置的 API Key 数量 */
    private int apiKeyCount;

    /** API Key 列表（脱敏） */
    private List<ChainNodeApiKeyVO> apiKeys = new ArrayList<>();

    /** HTTP API 根地址 */
    private String apiUrl;

    /** BTC RPC 用户名 */
    private String rpcUser;

    /** 是否已配置 BTC 密码 */
    private boolean rpcPasswordConfigured;

    /** 有效 RPC URL（预览，密钥脱敏） */
    private String effectiveRpcUrlMasked;

    /** 所需确认数 */
    private Integer requiredConfirms;

    /** 是否启用 */
    private Integer isEnabled;

    /** 是否可用于扫块 */
    private boolean scanReady;

    /** 备注 */
    private String remark;

    /** 更新时间 */
    private String updatedAt;
}
