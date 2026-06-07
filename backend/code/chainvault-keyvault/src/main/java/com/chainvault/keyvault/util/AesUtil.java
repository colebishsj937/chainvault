package com.chainvault.keyvault.util;

import com.chainvault.common.exception.BusinessException;

import javax.crypto.Cipher;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.util.Base64;

/**
 * AES-256-GCM 加解密工具
 *
 * @author chainvault
 * @date 2026-06-05
 */
public final class AesUtil {

    private static final String ALGORITHM = "AES/GCM/NoPadding";
    private static final int IV_LENGTH = 12;
    private static final int TAG_LENGTH = 128;

    private AesUtil() {
    }

    /**
     * 加密明文
     *
     * @param plainText 明文
     * @param keyText   密钥字符串（取前 32 字节）
     * @return Base64 密文（IV + 密文）
     */
    public static String encrypt(String plainText, String keyText) {
        try {
            // 1. 准备密钥与 IV
            byte[] keyBytes = normalizeKey(keyText);
            byte[] iv = new byte[IV_LENGTH];
            new SecureRandom().nextBytes(iv);

            // 2. 执行加密
            Cipher cipher = Cipher.getInstance(ALGORITHM);
            cipher.init(Cipher.ENCRYPT_MODE, new SecretKeySpec(keyBytes, "AES"),
                    new GCMParameterSpec(TAG_LENGTH, iv));
            byte[] encrypted = cipher.doFinal(plainText.getBytes(StandardCharsets.UTF_8));

            // 3. 拼接 IV + 密文
            ByteBuffer buffer = ByteBuffer.allocate(iv.length + encrypted.length);
            buffer.put(iv);
            buffer.put(encrypted);
            return Base64.getEncoder().encodeToString(buffer.array());
        } catch (Exception e) {
            throw new BusinessException("助记词加密失败");
        }
    }

    /**
     * 解密密文
     *
     * @param cipherText Base64 密文
     * @param keyText    密钥字符串
     * @return 明文
     */
    public static String decrypt(String cipherText, String keyText) {
        try {
            // 1. 解析密文
            byte[] payload = Base64.getDecoder().decode(cipherText);
            ByteBuffer buffer = ByteBuffer.wrap(payload);
            byte[] iv = new byte[IV_LENGTH];
            buffer.get(iv);
            byte[] encrypted = new byte[buffer.remaining()];
            buffer.get(encrypted);

            // 2. 执行解密
            byte[] keyBytes = normalizeKey(keyText);
            Cipher cipher = Cipher.getInstance(ALGORITHM);
            cipher.init(Cipher.DECRYPT_MODE, new SecretKeySpec(keyBytes, "AES"),
                    new GCMParameterSpec(TAG_LENGTH, iv));
            byte[] plain = cipher.doFinal(encrypted);
            return new String(plain, StandardCharsets.UTF_8);
        } catch (Exception e) {
            throw new BusinessException("助记词解密失败");
        }
    }

    // 规范化密钥长度为 32 字节
    private static byte[] normalizeKey(String keyText) {
        byte[] raw = keyText.getBytes(StandardCharsets.UTF_8);
        byte[] key = new byte[32];
        System.arraycopy(raw, 0, key, 0, Math.min(raw.length, 32));
        return key;
    }
}
