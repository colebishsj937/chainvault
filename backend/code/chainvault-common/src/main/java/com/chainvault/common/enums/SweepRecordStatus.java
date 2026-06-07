package com.chainvault.common.enums;

import lombok.Getter;

/**
 * 归集明细状态枚举
 */
@Getter
public enum SweepRecordStatus {

    /**
     * 已入 Redis 队列
     */
    QUEUED(1, "已入队"),

    /**
     * 广播进行中
     */
    BROADCASTING(2, "广播中"),

    /**
     * 已广播，等待链上确认
     */
    CONFIRMING(3, "确认中"),

    /**
     * 确认完成，计入已归集
     */
    SUCCESS(4, "成功"),

    /**
     * 广播或确认失败
     */
    FAILED(5, "失败"),

    /**
     * 未达阈值等原因跳过
     */
    SKIPPED(6, "跳过");

    /** 状态码 */
    private final int code;

    /** 状态描述 */
    private final String desc;

    SweepRecordStatus(int code, String desc) {
        this.code = code;
        this.desc = desc;
    }
}
