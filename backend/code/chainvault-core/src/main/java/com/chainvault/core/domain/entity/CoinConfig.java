package com.chainvault.core.domain.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 币种配置实体
 *
 * @author chainvault
 * @date 2026-06-05
 */
@Data
@TableName("coin_config")
public class CoinConfig {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 内部币种标识 */
    private String coinType;

    /** 显示符号 */
    private String symbol;

    /** 所属链 */
    private String chainCode;

    /** 合约地址 */
    private String contractAddr;

    /** 精度 */
    private Integer decimals;

    /** 最小充值 */
    private BigDecimal minDeposit;

    /** 最小提币 */
    private BigDecimal minWithdraw;

    /** 0=禁用 1=启用 */
    private Integer isEnabled;

    /** 1=开源版可用 */
    private Integer isOpen;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
}
