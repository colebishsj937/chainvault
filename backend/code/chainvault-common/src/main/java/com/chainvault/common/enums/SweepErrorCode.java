package com.chainvault.common.enums;

import lombok.Getter;

/**
 * 归集错误码枚举
 */
@Getter
public enum SweepErrorCode {

    THRESHOLD_NOT_MET("SWEEP_001", "待归集余额低于阈值"),
    COIN_DISABLED("SWEEP_002", "币种未启用"),
    HOT_WALLET_DERIVE_FAIL("SWEEP_003", "热钱包地址派生失败"),
    INSUFFICIENT_GAS("SWEEP_004", "源地址 Gas/TRX 不足"),
    BROADCAST_REJECTED("SWEEP_005", "节点拒绝广播"),
    BROADCAST_TIMEOUT("SWEEP_006", "广播 RPC 超时"),
    TX_NOT_FOUND("SWEEP_007", "链上未找到交易"),
    TX_REVERTED("SWEEP_008", "链上执行 revert"),
    CONFIRM_TIMEOUT("SWEEP_009", "确认超时"),
    DUPLICATE_IN_FLIGHT("SWEEP_010", "同地址已有进行中的归集"),
    SIGN_FAIL("SWEEP_011", "签名失败"),
    UNKNOWN("SWEEP_999", "未知错误");

    /** 错误码 */
    private final String code;

    /** 默认说明 */
    private final String defaultMessage;

    SweepErrorCode(String code, String defaultMessage) {
        this.code = code;
        this.defaultMessage = defaultMessage;
    }
}
