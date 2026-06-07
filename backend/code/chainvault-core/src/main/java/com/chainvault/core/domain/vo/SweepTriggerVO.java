package com.chainvault.core.domain.vo;

import lombok.Data;

/**
 * 归集触发结果
 *
 * @author chainvault
 * @date 2026-06-05
 */
@Data
public class SweepTriggerVO {

    /** 扫描地址数 */
    private int scanned;

    /** 入队归集任务数 */
    private int queued;

    /** 跳过数 */
    private int skipped;

    /** 批次号 */
    private String batchNo;
}
