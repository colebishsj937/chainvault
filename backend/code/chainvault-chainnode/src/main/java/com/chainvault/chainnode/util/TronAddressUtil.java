package com.chainvault.chainnode.util;

import org.bitcoinj.core.Base58;
import org.web3j.utils.Numeric;

/**
 * TRON 地址转换工具
 *
 * @author chainvault
 * @date 2026-06-05
 */
public final class TronAddressUtil {

    private TronAddressUtil() {
    }

    /**
     * 将 TRON 十六进制地址转为 Base58 地址
     *
     * @param hexAddress 十六进制地址（可带 41 前缀）
     * @return Base58 地址
     */
    public static String hexToBase58(String hexAddress) {
        if (hexAddress == null || hexAddress.isBlank()) {
            return hexAddress;
        }
        String normalized = hexAddress.startsWith("0x") ? hexAddress.substring(2) : hexAddress;
        byte[] bytes = Numeric.hexStringToByteArray(normalized);
        if (bytes.length == 21 && bytes[0] == 0x41) {
            byte[] address20 = java.util.Arrays.copyOfRange(bytes, 1, 21);
            return Base58.encodeChecked(0x41, address20);
        }
        return Base58.encodeChecked(0x41, bytes);
    }
}
