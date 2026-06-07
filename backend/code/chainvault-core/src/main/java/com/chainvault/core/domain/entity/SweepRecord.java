package com.chainvault.core.domain.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.chainvault.common.enums.SweepRecordStatus;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 归集明细实体
 */
@Data
@TableName("sweep_record")
public class SweepRecord {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 明细号 */
    private String recordNo;

    /** 所属批次 ID */
    private Long batchId;

    /** 重试来源明细 ID */
    private Long parentRecordId;

    /** 重试序号，0=首次 */
    private Integer retrySeq;

    private String merchantId;
    private String coinType;
    private String chainCode;

    /** 充值地址表 ID */
    private Long depositAddressId;

    private String fromAddress;
    private String toAddress;
    private String bip44Path;

    private BigDecimal amount;
    private BigDecimal thresholdSnapshot;
    private BigDecimal pendingSnapshot;

    /**
     * 明细状态
     * @see SweepRecordStatus
     */
    private Integer status;

    /** 关联 transaction_record.trade_id */
    private String tradeId;

    private String txHash;
    private Long blockNumber;
    private Integer confirms;
    private Integer requiredConfirms;

    private String errorCode;
    private String errorMessage;

    private LocalDateTime queuedAt;
    private LocalDateTime broadcastAt;
    private LocalDateTime confirmedAt;
    private LocalDateTime failedAt;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;
}
