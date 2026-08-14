package com.moyun.ext.ai.util;

import javax.crypto.Cipher;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.util.Base64;

/**
 * API Key 加解密工具（AES-GCM）
 *
 * <p>用于 model_config.api_key 字段的透明加解密：
 * <ul>
 *   <li>密文格式：base64(iv | ciphertext | tag)，带前缀 "ENC:"</li>
 *   <li>支持识别明文/密文：明文 apiKey 会被加密；已经是 "ENC:" 前缀的不会被二次加密</li>
 *   <li>解密时若遇到非 "ENC:" 前缀的字符串，直接返回原值（兼容历史明文数据）</li>
 * </ul>
 *
 * <p>密钥来源：优先读取环境变量 {@code MOYUN_APIKEY_AES_KEY}，
 * 未配置则使用内置默认密钥（仅适用于开发环境，生产必须配置环境变量）。</p>
 *
 * @author laomao
 * @since 2025-12-12
 */
public final class ApiKeyCryptoUtils {

    /** 密文前缀，用于识别已加密的 apiKey */
    private static final String ENC_PREFIX = "ENC:";

    private static final String ALGORITHM = "AES";
    private static final String TRANSFORMATION = "AES/GCM/NoPadding";
    private static final int GCM_TAG_LENGTH_BIT = 128;
    private static final int IV_LENGTH_BYTE = 12;

    /**
     * 密钥（32 字节 AES-256）。
     * 优先从环境变量 MOYUN_APIKEY_AES_KEY 读取，未配置则用内置默认值。
     * 生产环境务必通过环境变量覆盖。
     */
    private static final byte[] KEY_BYTES;

    static {
        String envKey = System.getenv("MOYUN_APIKEY_AES_KEY");
        if (envKey != null && envKey.length() == 32) {
            KEY_BYTES = envKey.getBytes(StandardCharsets.UTF_8);
        } else {
            // 默认密钥（仅开发环境；生产请配置 MOYUN_APIKEY_AES_KEY 环境变量）
            KEY_BYTES = "moyun-ai-apikey-default-key-32b!".getBytes(StandardCharsets.UTF_8);
        }
    }

    private ApiKeyCryptoUtils() {}

    /**
     * 判断 apiKey 是否已被加密（带 ENC: 前缀）
     */
    public static boolean isEncrypted(String apiKey) {
        return apiKey != null && apiKey.startsWith(ENC_PREFIX);
    }

    /**
     * 加密 apiKey。若已经是密文（ENC: 前缀）则原样返回，避免重复加密。
     */
    public static String encrypt(String plain) {
        if (plain == null || plain.isEmpty() || isEncrypted(plain)) {
            return plain;
        }
        try {
            byte[] iv = new byte[IV_LENGTH_BYTE];
            new SecureRandom().nextBytes(iv);

            Cipher cipher = Cipher.getInstance(TRANSFORMATION);
            SecretKeySpec keySpec = new SecretKeySpec(KEY_BYTES, ALGORITHM);
            GCMParameterSpec gcmSpec = new GCMParameterSpec(GCM_TAG_LENGTH_BIT, iv);
            cipher.init(Cipher.ENCRYPT_MODE, keySpec, gcmSpec);

            byte[] cipherText = cipher.doFinal(plain.getBytes(StandardCharsets.UTF_8));

            // 拼接 iv + cipherText(含 tag)
            byte[] combined = new byte[iv.length + cipherText.length];
            System.arraycopy(iv, 0, combined, 0, iv.length);
            System.arraycopy(cipherText, 0, combined, iv.length, cipherText.length);

            return ENC_PREFIX + Base64.getEncoder().encodeToString(combined);
        } catch (Exception e) {
            throw new IllegalStateException("API Key 加密失败", e);
        }
    }

    /**
     * 解密 apiKey。若不是密文（无 ENC: 前缀）则原样返回（兼容历史明文数据）。
     */
    public static String decrypt(String stored) {
        if (stored == null || stored.isEmpty() || !isEncrypted(stored)) {
            return stored;
        }
        try {
            String base64Part = stored.substring(ENC_PREFIX.length());
            byte[] combined = Base64.getDecoder().decode(base64Part);

            byte[] iv = new byte[IV_LENGTH_BYTE];
            byte[] cipherText = new byte[combined.length - IV_LENGTH_BYTE];
            System.arraycopy(combined, 0, iv, 0, IV_LENGTH_BYTE);
            System.arraycopy(combined, IV_LENGTH_BYTE, cipherText, 0, cipherText.length);

            Cipher cipher = Cipher.getInstance(TRANSFORMATION);
            SecretKeySpec keySpec = new SecretKeySpec(KEY_BYTES, ALGORITHM);
            GCMParameterSpec gcmSpec = new GCMParameterSpec(GCM_TAG_LENGTH_BIT, iv);
            cipher.init(Cipher.DECRYPT_MODE, keySpec, gcmSpec);

            byte[] plain = cipher.doFinal(cipherText);
            return new String(plain, StandardCharsets.UTF_8);
        } catch (Exception e) {
            // 解密失败可能是密钥变更或数据损坏，返回原值让调用方报错而非泄漏
            throw new IllegalStateException("API Key 解密失败，请检查 MOYUN_APIKEY_AES_KEY 配置", e);
        }
    }
}
