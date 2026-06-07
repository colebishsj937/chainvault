package com.chainvault.common.enums;

import lombok.Getter;

/**
 * 交易回调状态枚举
 *
 * @author chainvault
 * @date 2026-06-05
 */
@Getter
public enum CallbackStatus {

    /**
     * 未回调
     */
    PENDING(0, "未回调"),

    /**
     * 回调成功
     */
    SUCCESS(1, "成功"),

    /**
     * 回调失败（已达最大重试）
     */
    FAILED(2, "失败");

    /** 状态码 */
    private final int code;

    /** 状态描述 */
    private final String desc;

    CallbackStatus(int code, String desc) {
        this.code = code;
        this.desc = desc;
    }
}
