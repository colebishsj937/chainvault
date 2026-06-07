package com.chainvault.common.enums;

import lombok.Getter;

/**
 * 归集批次状态枚举
 */
@Getter
public enum SweepBatchStatus {

    /**
     * 已创建
     */
    CREATED(0, "已创建"),

    /**
     * 执行中
     */
    RUNNING(1, "执行中"),

    /**
     * 全部成功（至少一条成功且无失败）
     */
    COMPLETED(2, "完成"),

    /**
     * 部分失败
     */
    PARTIAL_FAILED(3, "部分失败"),

    /**
     * 全部失败
     */
    FAILED(4, "全部失败"),

    /**
     * 已取消
     */
    CANCELLED(5, "已取消");

    /** 状态码 */
    private final int code;

    /** 状态描述 */
    private final String desc;

    SweepBatchStatus(int code, String desc) {
        this.code = code;
        this.desc = desc;
    }
}
