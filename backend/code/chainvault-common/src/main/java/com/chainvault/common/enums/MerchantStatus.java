package com.chainvault.common.enums;

import lombok.Getter;

/**
 * 商户状态枚举
 *
 * @author chainvault
 * @date 2026-06-05
 */
@Getter
public enum MerchantStatus {

    /**
     * 禁用
     */
    DISABLED(0, "禁用"),

    /**
     * 正常
     */
    ACTIVE(1, "正常"),

    /**
     * 冻结
     */
    FROZEN(2, "冻结");

    /** 状态码 */
    private final int code;

    /** 状态描述 */
    private final String desc;

    MerchantStatus(int code, String desc) {
        this.code = code;
        this.desc = desc;
    }
}
