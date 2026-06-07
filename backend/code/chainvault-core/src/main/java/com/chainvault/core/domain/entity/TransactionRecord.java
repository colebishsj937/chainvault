package com.chainvault.core.domain.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.chainvault.common.enums.TransactionStatus;
import com.chainvault.common.enums.TxType;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 充提交易记录实体
 *
 * @author chainvault
 * @date 2026-06-05
 */
@Data
@TableName("transaction_record")
public class TransactionRecord {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 平台唯一交易 ID */
    private String tradeId;

    /** 商户号 */
    private String merchantId;

    /** 商户业务 ID */
    private String bizId;

    /**
     * 交易类型
     * @see TxType
     */
    private Integer txType;

    /** 币种标识 */
    private String coinType;

    /** 链标识 */
    private String chainCode;

    /** 来源地址 */
    private String fromAddress;

    /** 目标地址 */
    private String toAddress;

    /** 可读金额 */
    private BigDecimal amount;

    /** 原始链上金额 */
    private String rawAmount;

    /** 矿工费 */
    private BigDecimal fee;

    /** XRP/EOS tag */
    private String memo;

    /** 链上交易 Hash */
    private String txHash;

    /** 区块高度 */
    private Long blockNumber;

    /** 当前确认数 */
    private Integer confirms;

    /** 所需确认数 */
    private Integer requiredConfirms;

    /**
     * 交易状态
     * @see TransactionStatus
     */
    private Integer status;

    /** 0=正常 1=风控拦截 2=人工审核 */
    private Integer riskLevel;

    /** 0=未回调 1=成功 2=失败 */
    private Integer callbackStatus;

    /** 回调重试次数 */
    private Integer callbackTimes;

    /** 备注 */
    private String remark;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;
}
