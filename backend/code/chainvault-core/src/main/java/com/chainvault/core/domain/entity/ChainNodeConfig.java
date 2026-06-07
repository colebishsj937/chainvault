package com.chainvault.core.domain.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.chainvault.common.enums.ChainNodeProvider;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 链节点 Provider 配置实体
 *
 * @author chainvault
 * @date 2026-06-05
 */
@Data
@TableName("chain_node_config")
public class ChainNodeConfig {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 链标识 */
    private String chainCode;

    /**
     * 节点服务商
     * @see ChainNodeProvider
     */
    private String provider;

    /** 自定义 RPC 完整地址 */
    private String rpcUrl;

    /** API Key（加密） */
    private String apiKeyEnc;

    /** HTTP API 根地址 */
    private String apiUrl;

    /** BTC RPC 用户名 */
    private String rpcUser;

    /** BTC RPC 密码（加密） */
    private String rpcPasswordEnc;

    /** 所需确认数 */
    private Integer requiredConfirms;

    /** 0=禁用 1=启用 */
    private Integer isEnabled;

    /** 备注 */
    private String remark;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;
}
