package com.moyun.ext.ai.util;

import java.util.regex.Pattern;

/**
 * 数据脱敏工具类
 *
 * @author laomao
 */
public class DataMaskingUtils {

    /**
     * 手机号正则
     */
    private static final Pattern MOBILE_PATTERN = Pattern.compile("1[3-9]\\d{9}");

    /**
     * 身份证号正则
     */
    private static final Pattern ID_CARD_PATTERN = Pattern.compile("\\d{17}[0-9Xx]");

    /**
     * 邮箱正则
     */
    private static final Pattern EMAIL_PATTERN = Pattern.compile("[\\w.-]+@[\\w.-]+\\.\\w+");

    /**
     * 银行卡号正则
     */
    private static final Pattern BANK_CARD_PATTERN = Pattern.compile("\\d{16,19}");

    /**
     * 脱敏手机号
     * 138****1234
     *
     * @param mobile 手机号
     * @return 脱敏后的手机号
     */
    public static String maskMobile(String mobile) {
        if (mobile == null || mobile.length() != 11) {
            return mobile;
        }
        return mobile.substring(0, 3) + "****" + mobile.substring(7);
    }

    /**
     * 脱敏身份证号
     * 110***********1234
     *
     * @param idCard 身份证号
     * @return 脱敏后的身份证号
     */
    public static String maskIdCard(String idCard) {
        if (idCard == null || idCard.length() != 18) {
            return idCard;
        }
        return idCard.substring(0, 3) + "***********" + idCard.substring(14);
    }

    /**
     * 脱敏邮箱
     * u***@example.com
     *
     * @param email 邮箱
     * @return 脱敏后的邮箱
     */
    public static String maskEmail(String email) {
        if (email == null || !email.contains("@")) {
            return email;
        }
        int atIndex = email.indexOf("@");
        if (atIndex <= 1) {
            return email;
        }
        String username = email.substring(0, atIndex);
        String domain = email.substring(atIndex);

        if (username.length() <= 2) {
            return username.charAt(0) + "*" + domain;
        } else {
            return username.charAt(0) + "***" + username.charAt(username.length() - 1) + domain;
        }
    }

    /**
     * 脱敏银行卡号
     * 6222 **** **** 1234
     *
     * @param bankCard 银行卡号
     * @return 脱敏后的银行卡号
     */
    public static String maskBankCard(String bankCard) {
        if (bankCard == null || bankCard.length() < 16) {
            return bankCard;
        }
        return bankCard.substring(0, 4) + " **** **** " + bankCard.substring(bankCard.length() - 4);
    }

    /**
     * 脱敏姓名
     * 张三 -> 张*
     * 欧阳娜娜 -> 欧阳**
     *
     * @param name 姓名
     * @return 脱敏后的姓名
     */
    public static String maskName(String name) {
        if (name == null || name.isEmpty()) {
            return name;
        }

        int length = name.length();
        if (length == 1) {
            return name;
        } else if (length == 2) {
            return name.charAt(0) + "*";
        } else {
            // 保留姓氏,其他用*代替
            StringBuilder masked = new StringBuilder();
            masked.append(name.charAt(0));
            for (int i = 1; i < length; i++) {
                masked.append("*");
            }
            return masked.toString();
        }
    }

    /**
     * 脱敏地址
     * 保留省市,详细地址用***代替
     *
     * @param address 地址
     * @return 脱敏后的地址
     */
    public static String maskAddress(String address) {
        if (address == null || address.length() <= 10) {
            return address;
        }
        // 简单实现:保留前6个字符,后面用***代替
        return address.substring(0, Math.min(6, address.length())) + "***";
    }

    /**
     * 自动检测并脱敏
     * 根据字段名和值自动判断脱敏类型
     *
     * @param fieldName 字段名
     * @param value 字段值
     * @return 脱敏后的值
     */
    public static String autoMask(String fieldName, String value) {
        if (value == null || value.isEmpty()) {
            return value;
        }

        String lowerFieldName = fieldName.toLowerCase();

        // 根据字段名判断
        if (lowerFieldName.contains("mobile") || lowerFieldName.contains("phone") || 
            lowerFieldName.contains("tel")) {
            if (MOBILE_PATTERN.matcher(value).matches()) {
                return maskMobile(value);
            }
        }

        if (lowerFieldName.contains("idcard") || lowerFieldName.contains("id_card") ||
            lowerFieldName.contains("identity")) {
            if (ID_CARD_PATTERN.matcher(value).matches()) {
                return maskIdCard(value);
            }
        }

        if (lowerFieldName.contains("email") || lowerFieldName.contains("mail")) {
            if (EMAIL_PATTERN.matcher(value).matches()) {
                return maskEmail(value);
            }
        }

        if (lowerFieldName.contains("bank") || lowerFieldName.contains("card")) {
            if (BANK_CARD_PATTERN.matcher(value).matches()) {
                return maskBankCard(value);
            }
        }

        if (lowerFieldName.contains("name") && !lowerFieldName.contains("username")) {
            return maskName(value);
        }

        if (lowerFieldName.contains("address") || lowerFieldName.contains("addr")) {
            return maskAddress(value);
        }

        // 如果无法判断字段名,尝试根据值的格式判断
        if (MOBILE_PATTERN.matcher(value).matches()) {
            return maskMobile(value);
        }
        if (ID_CARD_PATTERN.matcher(value).matches()) {
            return maskIdCard(value);
        }
        if (EMAIL_PATTERN.matcher(value).matches()) {
            return maskEmail(value);
        }

        return value;
    }

    /**
     * 检查字段是否需要脱敏
     *
     * @param fieldName 字段名
     * @return 是否需要脱敏
     */
    public static boolean needsMasking(String fieldName) {
        if (fieldName == null) {
            return false;
        }

        String lower = fieldName.toLowerCase();
        return lower.contains("mobile") || lower.contains("phone") ||
               lower.contains("idcard") || lower.contains("id_card") ||
               lower.contains("email") || lower.contains("mail") ||
               lower.contains("bank") || lower.contains("card") ||
               (lower.contains("name") && !lower.contains("username")) ||
               lower.contains("address") || lower.contains("addr");
    }
}
