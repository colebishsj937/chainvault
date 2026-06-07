package com.chainvault.common.util;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.util.HexFormat;

/**
 * Webhook 回调 HMAC-SHA256 签名工具
 *
 * @author chainvault
 * @date 2026-06-05
 */
public final class WebhookSignUtil {

    private static final String HMAC_SHA256 = "HmacSHA256";

    private WebhookSignUtil() {
    }

    /**
     * 对回调 JSON 原文计算 HMAC-SHA256 签名（小写十六进制）
     *
     * @param payloadJson 不含 sign 字段的 JSON 字符串
     * @param secretKey   Webhook 密钥
     * @return 签名字符串
     */
    public static String sign(String payloadJson, String secretKey) {
        try {
            Mac mac = Mac.getInstance(HMAC_SHA256);
            SecretKeySpec keySpec = new SecretKeySpec(
                    secretKey.getBytes(StandardCharsets.UTF_8), HMAC_SHA256);
            mac.init(keySpec);
            byte[] hash = mac.doFinal(payloadJson.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hash);
        } catch (Exception e) {
            throw new IllegalStateException("Webhook 签名计算失败", e);
        }
    }
}
