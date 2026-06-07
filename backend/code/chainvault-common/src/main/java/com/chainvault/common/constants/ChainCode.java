package com.chainvault.common.constants;

/**
 * 链标识与 BIP44 coin_type 常量
 *
 * @author chainvault
 * @date 2026-06-05
 */
public final class ChainCode {

    public static final String ETH = "ETH";
    public static final String BNB = "BNB";
    public static final String TRON = "TRON";
    public static final String BTC = "BTC";

    /** BIP44 coin_type：ETH / BNB */
    public static final int BIP44_ETH = 60;

    /** BIP44 coin_type：BTC */
    public static final int BIP44_BTC = 0;

    /** BIP44 coin_type：TRON */
    public static final int BIP44_TRON = 195;

    private ChainCode() {
    }

    /**
     * 根据链标识获取 BIP44 coin_type
     *
     * @param chainCode 链标识
     * @return coin_type
     */
    public static int bip44CoinType(String chainCode) {
        return switch (chainCode.toUpperCase()) {
            case ETH, BNB -> BIP44_ETH;
            case BTC -> BIP44_BTC;
            case TRON -> BIP44_TRON;
            default -> throw new IllegalArgumentException("不支持的链: " + chainCode);
        };
    }
}
