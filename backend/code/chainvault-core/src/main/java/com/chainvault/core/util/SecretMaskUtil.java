package com.chainvault.core.util;

import org.springframework.util.StringUtils;

/**
 * 密钥脱敏工具
 *
 * @author chainvault
 * @date 2026-06-05
 */
public final class SecretMaskUtil {

    private SecretMaskUtil() {
    }

    /**
     * 脱敏密钥，仅展示后四位
     *
     * @param secret 明文密钥
     * @return 脱敏结果
     */
    public static String maskSecret(String secret) {
        if (!StringUtils.hasText(secret)) {
            return null;
        }
        String value = secret.trim();
        if (value.length() <= 4) {
            return "****";
        }
        return "****" + value.substring(value.length() - 4);
    }

    /**
     * 脱敏 Webhook 密钥
     *
     * @param secret 明文密钥
     * @return 脱敏结果
     */
    public static String maskWebhookSecret(String secret) {
        if (!StringUtils.hasText(secret)) {
            return null;
        }
        String value = secret.trim();
        if (value.length() <= 8) {
            return "cv_wh_****";
        }
        if (value.startsWith("cv_wh_")) {
            return "cv_wh_****" + value.substring(value.length() - 4);
        }
        return maskSecret(value);
    }

    /**
     * 脱敏 URL 中的 API Key 片段
     *
     * @param url 完整 URL
     * @return 脱敏 URL
     */
    public static String maskUrl(String url) {
        if (!StringUtils.hasText(url)) {
            return null;
        }
        int lastSlash = url.lastIndexOf('/');
        if (lastSlash < 0 || lastSlash >= url.length() - 1) {
            return url;
        }
        String prefix = url.substring(0, lastSlash + 1);
        String tail = url.substring(lastSlash + 1);
        return prefix + maskSecret(tail);
    }
}
