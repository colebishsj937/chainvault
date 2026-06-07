package com.chainvault.common.constants;

/**
 * 限流相关常量
 *
 * @author chainvault
 * @date 2026-06-05
 */
public final class RateLimitConstants {

    /** Redis 限流键前缀 */
    public static final String RATE_LIMIT_KEY_PREFIX = "cv:ratelimit:";

    private RateLimitConstants() {
    }
}
