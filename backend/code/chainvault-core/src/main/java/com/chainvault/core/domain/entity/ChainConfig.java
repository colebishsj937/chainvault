package com.chainvault.core.domain.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 区块链配置实体
 *
 * @author chainvault
 * @date 2026-06-05
 */
@Data
@TableName("chain_config")
public class ChainConfig {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 链标识：ETH/BTC/TRON */
    private String chainCode;

    /** 链名称 */
    private String chainName;

    /** 原生币精度 */
    private Integer decimals;

    /** 所需确认数 */
    private Integer confirmNum;

    /** 0=禁用 1=启用 */
    private Integer isEnabled;

    /** 1=开源版 0=商业版 */
    private Integer isOpen;

    /** 默认 RPC 地址 */
    private String rpcUrl;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;
}
