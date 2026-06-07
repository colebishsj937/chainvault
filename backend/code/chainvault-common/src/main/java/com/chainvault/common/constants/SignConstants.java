package com.chainvault.common.constants;

import java.time.Duration;

/**
 * API 签名相关常量
 *
 * @author chainvault
 * @date 2026-06-05
 */
public final class SignConstants {

    /** 请求头：API Key */
    public static final String HEADER_API_KEY = "X-Api-Key";

    /** 请求头：时间戳（秒） */
    public static final String HEADER_TIMESTAMP = "X-Timestamp";

    /** 请求头：随机串 */
    public static final String HEADER_NONCE = "X-Nonce";

    /** 请求头：签名 */
    public static final String HEADER_SIGN = "X-Sign";

    /** Redis nonce 键前缀 */
    public static final String NONCE_KEY_PREFIX = "cv:nonce:";

    /** 时间戳容差（秒） */
    public static final long TIMESTAMP_TOLERANCE_SECONDS = 300L;

    /** nonce 过期时间 */
    public static final Duration NONCE_TTL = Duration.ofMinutes(10);

    /** 请求属性：当前商户号 */
    public static final String ATTR_MERCHANT_ID = "merchantId";

    private SignConstants() {
    }
}
