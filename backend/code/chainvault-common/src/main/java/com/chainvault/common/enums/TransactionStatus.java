package com.chainvault.common.enums;

import lombok.Getter;

/**
 * 交易状态枚举
 *
 * @author chainvault
 * @date 2026-06-05
 */
@Getter
public enum TransactionStatus {

    /**
     * 待处理
     */
    PENDING(0, "待处理"),

    /**
     * 处理中（等待链上确认）
     */
    PROCESSING(1, "处理中"),

    /**
     * 成功
     */
    SUCCESS(2, "成功"),

    /**
     * 失败
     */
    FAILED(3, "失败"),

    /**
     * 已回调
     */
    NOTIFIED(4, "已回调");

    /** 状态码 */
    private final int code;

    /** 状态描述 */
    private final String desc;

    TransactionStatus(int code, String desc) {
        this.code = code;
        this.desc = desc;
    }
}
