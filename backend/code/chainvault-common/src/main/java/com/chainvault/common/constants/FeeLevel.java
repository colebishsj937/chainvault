package com.chainvault.common.constants;

/**
 * 矿工费档位常量
 *
 * @author chainvault
 * @date 2026-06-05
 */
public final class FeeLevel {

    /** 快速 */
    public static final String FAST = "fast";

    /** 标准 */
    public static final String NORMAL = "normal";

    /** 慢速 */
    public static final String SLOW = "slow";

    private FeeLevel() {
    }

    /**
     * 校验档位是否合法
     *
     * @param level 档位
     * @return 是否合法
     */
    public static boolean isValid(String level) {
        if (level == null) {
            return false;
        }
        return FAST.equals(level) || NORMAL.equals(level) || SLOW.equals(level);
    }
}
