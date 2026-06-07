package com.chainvault.keyvault.service;

import com.chainvault.common.constants.ChainCode;
import org.bitcoinj.core.Address;
import org.bitcoinj.params.MainNetParams;
import org.springframework.stereotype.Component;

import java.util.regex.Pattern;

/**
 * 各链地址格式校验
 *
 * @author chainvault
 * @date 2026-06-05
 */
@Component
public class AddressValidator {

    private static final Pattern ETH_PATTERN = Pattern.compile("^0x[a-fA-F0-9]{40}$");
    private static final Pattern TRON_PATTERN = Pattern.compile("^T[1-9A-HJ-NP-Za-km-z]{33}$");

    /**
     * 校验地址格式是否合法
     *
     * @param chainCode 链标识
     * @param address   地址
     * @return true=格式合法
     */
    public boolean isValid(String chainCode, String address) {
        if (address == null || address.isBlank()) {
            return false;
        }

        String chain = chainCode.toUpperCase();
        return switch (chain) {
            case ChainCode.ETH, ChainCode.BNB -> ETH_PATTERN.matcher(address).matches();
            case ChainCode.TRON -> TRON_PATTERN.matcher(address).matches();
            case ChainCode.BTC -> isValidBtc(address);
            default -> false;
        };
    }

    // BTC 地址通过 bitcoinj 解析校验
    private boolean isValidBtc(String address) {
        try {
            Address.fromString(MainNetParams.get(), address);
            return true;
        } catch (Exception e) {
            return false;
        }
    }
}
