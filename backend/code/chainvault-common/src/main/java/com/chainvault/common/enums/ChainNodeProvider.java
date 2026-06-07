package com.chainvault.common.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * 链节点服务商枚举
 *
 * @author chainvault
 * @date 2026-06-05
 */
@Getter
@RequiredArgsConstructor
public enum ChainNodeProvider {

    /**
     * Alchemy，需填写 apiKey，按链自动拼接 RPC URL
     */
    ALCHEMY("ALCHEMY", "Alchemy"),

    /**
     * Infura，需填写 apiKey，按链自动拼接 RPC URL
     */
    INFURA("INFURA", "Infura"),

    /**
     * 自定义 RPC 完整地址（可含 Key）
     */
    CUSTOM("CUSTOM", "自定义 RPC"),

    /**
     * TronGrid HTTP API
     */
    TRONGRID("TRONGRID", "TronGrid"),

    /**
     * Bitcoin Core JSON-RPC
     */
    BITCOIN_CORE("BITCOIN_CORE", "Bitcoin Core");

    /** 编码 */
    private final String code;

    /** 描述 */
    private final String desc;

    /**
     * 根据编码解析枚举
     *
     * @param code 编码
     * @return 枚举值
     */
    public static ChainNodeProvider fromCode(String code) {
        for (ChainNodeProvider provider : values()) {
            if (provider.code.equalsIgnoreCase(code)) {
                return provider;
            }
        }
        throw new IllegalArgumentException("不支持的节点服务商: " + code);
    }
}
