package com.chainvault.common.util;

import org.springframework.util.DigestUtils;

import java.nio.charset.StandardCharsets;

/**
 * MD5 请求签名工具
 *
 * @author chainvault
 * @date 2026-06-05
 */
public final class SignUtil {

    private SignUtil() {
    }

    /**
     * 计算请求签名
     * 算法：MD5(body + "&" + timestamp + "&" + nonce + "&" + secretKey)，小写十六进制
     *
     * @param body      请求体原文（GET 为空字符串）
     * @param timestamp 时间戳（秒）
     * @param nonce     随机串
     * @param secretKey 商户密钥
     * @return 签名字符串
     */
    public static String sign(String body, String timestamp, String nonce, String secretKey) {
        String raw = body + "&" + timestamp + "&" + nonce + "&" + secretKey;
        return DigestUtils.md5DigestAsHex(raw.getBytes(StandardCharsets.UTF_8));
    }
}
