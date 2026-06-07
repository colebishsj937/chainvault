package com.chainvault.common.enums;

import lombok.Getter;

/**
 * 交易类型枚举
 *
 * @author chainvault
 * @date 2026-06-05
 */
@Getter
public enum TxType {

    /**
     * 充值
     */
    DEPOSIT(1, "充值"),

    /**
     * 提币
     */
    WITHDRAW(2, "提币"),

    /**
     * 资金归集
     */
    SWEEP(3, "归集");

    /** 类型码 */
    private final int code;

    /** 类型描述 */
    private final String desc;

    TxType(int code, String desc) {
        this.code = code;
        this.desc = desc;
    }
}
