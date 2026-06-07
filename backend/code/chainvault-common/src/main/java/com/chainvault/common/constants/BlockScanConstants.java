package com.chainvault.common.constants;

/**
 * 区块扫描 Redis 键常量
 *
 * @author chainvault
 * @date 2026-06-05
 */
public final class BlockScanConstants {

    /** 扫块断点：chainnode:{chainCode}:last_block */
    public static final String LAST_BLOCK_KEY_PREFIX = "chainnode:";

    /** 扫块断点键后缀 */
    public static final String LAST_BLOCK_KEY_SUFFIX = ":last_block";

    private BlockScanConstants() {
    }

    /**
     * 构建扫块断点 Redis 键
     *
     * @param chainCode 链标识
     * @return Redis 键
     */
    public static String lastBlockKey(String chainCode) {
        return LAST_BLOCK_KEY_PREFIX + chainCode + LAST_BLOCK_KEY_SUFFIX;
    }
}
