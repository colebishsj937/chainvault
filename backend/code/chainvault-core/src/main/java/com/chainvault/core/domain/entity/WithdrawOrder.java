package com.chainvault.core.domain.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.chainvault.common.enums.WithdrawStatus;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 提币申请实体
 *
 * @author chainvault
 * @date 2026-06-05
 */
@Data
@TableName("withdraw_order")
public class WithdrawOrder {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 提币单号 */
    private String orderNo;

    /** 关联 transaction_record.trade_id */
    private String tradeId;

    /** 商户号 */
    private String merchantId;

    /** 商户幂等键 */
    private String bizId;

    /** 币种标识 */
    private String coinType;

    /** 链标识 */
    private String chainCode;

    /** 目标地址 */
    private String toAddress;

    /** XRP/EOS tag */
    private String memo;

    /** 提币金额 */
    private BigDecimal amount;

    /** 矿工费档位：fast/normal/slow */
    private String feeLevel;

    /**
     * 提币状态
     * @see WithdrawStatus
     */
    private Integer status;

    /** 0=无需审核 1=待多签 2=已通过 */
    private Integer auditStatus;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;
}
