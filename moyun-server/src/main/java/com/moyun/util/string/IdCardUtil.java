package com.moyun.util.string;

import java.time.DateTimeException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.regex.Pattern;

/**
 * 身份证号校验工具
 *
 * <p>支持 18 位居民身份证号校验：
 * <ul>
 *   <li>长度 18 位，前 17 位为数字，第 18 位为数字或 X</li>
 *   <li>出生日期段（第 7-14 位）合法且不晚于当前日期</li>
 *   <li>校验位按 GB 11643-1999 / ISO 7064:1983 MOD 11-2 计算</li>
 * </ul>
 *
 * <p>不支持的旧 15 位身份证号（已停发）一律视为非法。
 *
 * @author moyun
 */
public final class IdCardUtil {

    private IdCardUtil() {}

    /** 18 位身份证号格式（前 17 位数字，末位数字或 X） */
    private static final Pattern ID_18_PATTERN = Pattern.compile("^\\d{17}[\\dXx]$");

    /** MOD 11-2 加权因子 */
    private static final int[] WEIGHT = {7, 9, 10, 5, 8, 4, 2, 1, 6, 3, 7, 9, 10, 5, 8, 4, 2};

    /** MOD 11-2 校验位字符表（按余数 0-10 索引） */
    private static final char[] CHECK_CODE = {'1', '0', 'X', '9', '8', '7', '6', '5', '4', '3', '2'};

    /** 出生日期解析格式（yyyyMMdd） */
    private static final DateTimeFormatter BIRTH_DATE_FORMAT = DateTimeFormatter.ofPattern("yyyyMMdd");

    /**
     * 校验身份证号合法性（非空 + 格式 + 生日 + 校验位）
     *
     * @param idCard 身份证号
     * @return true 合法；false 非法
     */
    public static boolean isValid(String idCard) {
        if (StringUtils.isBlank(idCard)) {
            return false;
        }
        if (!ID_18_PATTERN.matcher(idCard).matches()) {
            return false;
        }
        // 出生日期段校验
        if (!isValidBirthDate(idCard.substring(6, 14))) {
            return false;
        }
        // 校验位校验
        return checkCodeMatches(idCard);
    }

    /**
     * 校验身份证号合法性，非法时返回错误原因（用于业务层友好提示）
     *
     * @param idCard 身份证号
     * @return null 表示合法；非空字符串表示错误原因
     */
    public static String validate(String idCard) {
        if (StringUtils.isBlank(idCard)) {
            return "身份证号不能为空";
        }
        if (idCard.length() != 18) {
            return "身份证号长度必须为 18 位";
        }
        if (!ID_18_PATTERN.matcher(idCard).matches()) {
            return "身份证号格式错误：前 17 位必须为数字，第 18 位为数字或 X";
        }
        if (!isValidBirthDate(idCard.substring(6, 14))) {
            return "身份证号中的出生日期非法";
        }
        if (!checkCodeMatches(idCard)) {
            return "身份证号校验位错误";
        }
        return null;
    }

    /**
     * 从身份证号中解析出生日期（yyyyMMdd）
     *
     * @param idCard 身份证号
     * @return 出生日期；非法返回 null
     */
    public static LocalDate getBirthDate(String idCard) {
        if (!isValid(idCard)) {
            return null;
        }
        try {
            return LocalDate.parse(idCard.substring(6, 14), BIRTH_DATE_FORMAT);
        } catch (DateTimeException e) {
            return null;
        }
    }

    /**
     * 从身份证号中解析性别（奇数=男，偶数=女）
     *
     * @param idCard 身份证号
     * @return "M" 男 / "F" 女；非法返回 null
     */
    public static String getGender(String idCard) {
        if (!isValid(idCard)) {
            return null;
        }
        int genderDigit = Character.digit(idCard.charAt(16), 10);
        return (genderDigit & 1) == 1 ? "M" : "F";
    }

    /**
     * 计算校验位（前 17 位 → 第 18 位）
     */
    private static char calculateCheckCode(String idCard17) {
        int sum = 0;
        for (int i = 0; i < 17; i++) {
            sum += Character.digit(idCard17.charAt(i), 10) * WEIGHT[i];
        }
        return CHECK_CODE[sum % 11];
    }

    private static boolean checkCodeMatches(String idCard) {
        char expected = calculateCheckCode(idCard.substring(0, 17));
        char actual = Character.toUpperCase(idCard.charAt(17));
        return expected == actual;
    }

    private static boolean isValidBirthDate(String yyyymmdd) {
        try {
            LocalDate birth = LocalDate.parse(yyyymmdd, BIRTH_DATE_FORMAT);
            // 不允许未来日期
            return !birth.isAfter(LocalDate.now());
        } catch (DateTimeException e) {
            return false;
        }
    }
}
