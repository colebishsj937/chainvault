package com.chainvault.chainnode.dto;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

/**
 * 链节点运行时配置（解密后的有效值）
 *
 * @author chainvault
 * @date 2026-06-05
 */
@Data
public class ChainNodeSettings {

    /** 链标识 */
    private String chainCode;

    /** 节点服务商编码 */
    private String provider;

    /** 有效 RPC URL（EVM/BTC，主 Key 对应地址，兼容展示） */
    private String rpcUrl;

    /** HTTP API 根地址（TRON） */
    private String apiUrl;

    /** API Key 明文（兼容单 Key，取 apiKeys 首个） */
    private String apiKey;

    /** 可用 API Key 列表（轮询使用） */
    private List<String> apiKeys = new ArrayList<>();

    /** BTC RPC 用户名 */
    private String rpcUser;

    /** BTC RPC 密码明文 */
    private String rpcPassword;

    /** 所需确认数 */
    private int requiredConfirms;

    /** 是否启用 */
    private boolean enabled;
}
