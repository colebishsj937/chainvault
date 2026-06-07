package com.chainvault.core.domain.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.chainvault.common.enums.SweepBatchStatus;
import com.chainvault.common.enums.SweepTriggerType;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 归集批次实体
 */
@Data
@TableName("sweep_batch")
public class SweepBatch {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 批次号 */
    private String batchNo;

    /** 商户号，NULL 表示全平台 */
    private String merchantId;

    /** 链标识 */
    private String chainCode;

    /** 币种标识 */
    private String coinType;

    /**
     * 触发方式
     * @see SweepTriggerType
     */
    private Integer triggerType;

    /** 触发人 */
    private String triggerBy;

    /**
     * 批次状态
     * @see SweepBatchStatus
     */
    private Integer status;

    private Integer scannedCount;
    private Integer queuedCount;
    private Integer successCount;
    private Integer failedCount;
    private Integer skippedCount;

    private String remark;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;

    private LocalDateTime completedAt;
}
