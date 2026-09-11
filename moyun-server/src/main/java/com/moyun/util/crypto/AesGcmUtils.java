package com.moyun.util.crypto;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.Base64;

import javax.crypto.Cipher;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;

/**
 * AES-GCM 加解密工具（用于证件号等敏感信息落库加密）
 *
 * <p>密文格式：{@code enc:v1:<base64(iv)>:<base64(cipherText)}。
 * 密钥由调用方传入任意口令字符串，内部经 SHA-256 派生 256 位 AES 密钥，
 * 避免要求调用方自行生成/管理定长密钥字节。</p>
 *
 * <p>GCM 参数：IV 12 字节（每次加密随机生成），Tag 128 位。
 * 同一明文每次加密产生不同密文，天然抗重放与彩虹比对。</p>
 *
 * @author moyun
 */
public final class AesGcmUtils {

    private AesGcmUtils() {}

    /** 密文版本前缀 */
    private static final String PREFIX = "enc:v1:";

    /** GCM IV 长度（字节），NIST 推荐值 */
    private static final int IV_LENGTH = 12;

    /** GCM 认证标签长度（位） */
    private static final int TAG_BITS = 128;

    private static final SecureRandom RANDOM = new SecureRandom();

    /**
     * 判断值是否为本工具产出的密文
     */
    public static boolean isEncrypted(String value) {
        return value != null && value.startsWith(PREFIX);
    }

    /**
     * AES-GCM 加密
     *
     * @param plain       明文
     * @param keyMaterial 密钥口令（任意非空字符串，内部 SHA-256 派生）
     * @return 密文 {@code enc:v1:iv:cipher}（base64）
     */
    public static String encrypt(String plain, String keyMaterial) {
        if (plain == null) {
            return null;
        }
        try {
            byte[] iv = new byte[IV_LENGTH];
            RANDOM.nextBytes(iv);
            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            cipher.init(Cipher.ENCRYPT_MODE, deriveKey(keyMaterial), new GCMParameterSpec(TAG_BITS, iv));
            byte[] cipherText = cipher.doFinal(plain.getBytes(StandardCharsets.UTF_8));
            return PREFIX
                    + Base64.getEncoder().encodeToString(iv) + ":"
                    + Base64.getEncoder().encodeToString(cipherText);
        } catch (Exception e) {
            throw new IllegalStateException("证件号加密失败：" + e.getMessage(), e);
        }
    }

    /**
     * AES-GCM 解密
     *
     * @param encrypted 密文（{@code enc:v1:iv:cipher}）
     * @param keyMaterial 密钥口令（须与加密时一致）
     * @return 明文；入参为空返回 null
     */
    public static String decrypt(String encrypted, String keyMaterial) {
        if (encrypted == null || encrypted.isEmpty()) {
            return null;
        }
        if (!isEncrypted(encrypted)) {
            // 兼容存量明文数据：非密文格式直接原样返回
            return encrypted;
        }
        try {
            String[] parts = encrypted.substring(PREFIX.length()).split(":");
            if (parts.length != 2) {
                throw new IllegalArgumentException("密文格式非法");
            }
            byte[] iv = Base64.getDecoder().decode(parts[0]);
            byte[] cipherText = Base64.getDecoder().decode(parts[1]);
            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            cipher.init(Cipher.DECRYPT_MODE, deriveKey(keyMaterial), new GCMParameterSpec(TAG_BITS, iv));
            return new String(cipher.doFinal(cipherText), StandardCharsets.UTF_8);
        } catch (Exception e) {
            throw new IllegalStateException("证件号解密失败（密钥是否变更？）：" + e.getMessage(), e);
        }
    }

    /**
     * 口令 → 256 位 AES 密钥（SHA-256 派生）
     */
    private static SecretKeySpec deriveKey(String keyMaterial) throws Exception {
        String material = (keyMaterial == null || keyMaterial.isEmpty()) ? "moyun-default" : keyMaterial;
        byte[] key = MessageDigest.getInstance("SHA-256")
                .digest(material.getBytes(StandardCharsets.UTF_8));
        return new SecretKeySpec(key, "AES");
    }
}
