package com.chainvault.core.domain.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 归集全局配置实体
 *
 * @author chainvault
 * @date 2026-06-07
 */
@Data
@TableName("sweep_config")
public class SweepConfig {

    /** 固定主键 1 */
    @TableId(type = IdType.INPUT)
    private Long id;

    /** 是否启用定时归集扫描：0=否 1=是 */
    private Integer sweepEnabled;

    /** 归集阈值倍数 */
    private Integer thresholdMultiplier;

    private LocalDateTime updatedAt;
}
