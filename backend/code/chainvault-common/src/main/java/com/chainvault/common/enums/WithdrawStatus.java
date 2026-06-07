package com.chainvault.common.enums;

import lombok.Getter;

/**
 * 提币单状态枚举
 *
 * @author chainvault
 * @date 2026-06-05
 */
@Getter
public enum WithdrawStatus {

    /**
     * 待审核
     */
    PENDING(0, "待审核"),

    /**
     * 审核通过
     */
    APPROVED(1, "审核通过"),

    /**
     * 广播中
     */
    BROADCASTING(2, "广播中"),

    /**
     * 成功
     */
    SUCCESS(3, "成功"),

    /**
     * 失败
     */
    FAILED(4, "失败"),

    /**
     * 拒绝
     */
    REJECTED(5, "拒绝");

    /** 状态码 */
    private final int code;

    /** 状态描述 */
    private final String desc;

    WithdrawStatus(int code, String desc) {
        this.code = code;
        this.desc = desc;
    }
}
