package com.chainvault.core.domain.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * 链节点配置更新请求
 *
 * @author chainvault
 * @date 2026-06-05
 */
@Data
public class ChainNodeConfigUpdateReq {

    /**
     * 节点服务商
     * @see com.chainvault.common.enums.ChainNodeProvider
     */
    @NotBlank(message = "节点服务商不能为空")
    private String provider;

    /** 自定义 RPC 完整地址 */
    private String rpcUrl;

    /** API Key（留空表示不修改） */
    private String apiKey;

    /** TronGrid 等 API 根地址 */
    private String apiUrl;

    /** BTC RPC 用户名 */
    private String rpcUser;

    /** BTC RPC 密码（留空表示不修改） */
    private String rpcPassword;

    /** 所需确认数 */
    private Integer requiredConfirms;

    /** 是否启用：0/1 */
    private Integer isEnabled;

    /** 备注 */
    private String remark;
}
