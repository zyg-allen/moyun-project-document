package com.moyun.core.base.text;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.nio.ByteBuffer;
import java.nio.charset.Charset;
import java.text.NumberFormat;
import java.util.Set;

/**
 * 类型转换工具类
 *
 * @author moyun
 */
public class Convert {
    /**
     * 默认字符集
     */
    private static final String DEFAULT_CHARSET = "UTF-8";

    /**
     * 转换为字符串<br>
     * 如果给定的值为null，或者转换失败，返回默认值<br>
     * 转换失败不会报错
     *
     * @param value        被转换的值
     * @param defaultValue 转换错误时的默认值
     * @return 转换后的字符串
     */
    public static String toStr(Object value, String defaultValue) {
        if (null == value) {
            return defaultValue;
        }
        if (value instanceof String) {
            return (String) value;
        }
        return value.toString();
    }

    /**
     * 转换为字符串<br>
     * 如果给定的值为null，或者转换失败，返回空字符串<br>
     * 转换失败不会报错
     *
     * @param value 被转换的值
     * @return 转换后的字符串
     */
    public static String toStr(Object value) {
        return toStr(value, "");
    }

    /**
     * 转换为字符<br>
     * 如果给定的值为null，或者转换失败，返回默认值<br>
     * 转换失败不会报错
     *
     * @param value        被转换的值
     * @param defaultValue 转换错误时的默认值
     * @return 转换后的字符
     */
    public static Character toChar(Object value, Character defaultValue) {
        if (null == value) {
            return defaultValue;
        }
        if (value instanceof Character) {
            return (Character) value;
        }

        final String valueStr = toStr(value, null);
        return (null == valueStr) ? defaultValue : valueStr.charAt(0);
    }

    /**
     * 转换为字符<br>
     * 如果给定的值为null，或者转换失败，返回默认值<br>
     * 转换失败不会报错
     *
     * @param value 被转换的值
     * @return 转换后的字符
     */
    public static Character toChar(Object value) {
        return toChar(value, null);
    }

    /**
     * 转换为int<br>
     * 如果给定的值为null，或者转换失败，返回默认值<br>
     * 转换失败不会报错
     *
     * @param value        被转换的值
     * @param defaultValue 转换错误时的默认值
     * @return 转换后的int
     */
    public static Integer toInt(Object value, Integer defaultValue) {
        if (value == null) {
            return defaultValue;
        }
        if (value instanceof Integer) {
            return (Integer) value;
        }
        if (value instanceof Number) {
            return ((Number) value).intValue();
        }
        final String valueStr = toStr(value, null);
        if (null == valueStr) {
            return defaultValue;
        }
        try {
            return Integer.parseInt(valueStr.trim());
        } catch (Exception e) {
            return defaultValue;
        }
    }

    /**
     * 转换为int<br>
     * 如果给定的值为null，或者转换失败，返回0<br>
     * 转换失败不会报错
     *
     * @param value 被转换的值
     * @return 转换后的int
     */
    public static Integer toInt(Object value) {
        return toInt(value, 0);
    }

    /**
     * 转换为long<br>
     * 如果给定的值为null，或者转换失败，返回默认值<br>
     * 转换失败不会报错
     *
     * @param value        被转换的值
     * @param defaultValue 转换错误时的默认值
     * @return 转换后的long
     */
    public static Long toLong(Object value, Long defaultValue) {
        if (value == null) {
            return defaultValue;
        }
        if (value instanceof Long) {
            return (Long) value;
        }
        if (value instanceof Number) {
            return ((Number) value).longValue();
        }
        final String valueStr = toStr(value, null);
        if (null == valueStr) {
            return defaultValue;
        }
        try {
            return Long.parseLong(valueStr.trim());
        } catch (Exception e) {
            return defaultValue;
        }
    }

    /**
     * 转换为long<br>
     * 如果给定的值为null，或者转换失败，返回0<br>
     * 转换失败不会报错
     *
     * @param value 被转换的值
     * @return 转换后的long
     */
    public static Long toLong(Object value) {
        return toLong(value, 0L);
    }

    /**
     * 转换为double<br>
     * 如果给定的值为null，或者转换失败，返回默认值<br>
     * 转换失败不会报错
     *
     * @param value        被转换的值
     * @param defaultValue 转换错误时的默认值
     * @return 转换后的double
     */
    public static Double toDouble(Object value, Double defaultValue) {
        if (value == null) {
            return defaultValue;
        }
        if (value instanceof Double) {
            return (Double) value;
        }
        if (value instanceof Number) {
            return ((Number) value).doubleValue();
        }
        final String valueStr = toStr(value, null);
        if (null == valueStr) {
            return defaultValue;
        }
        try {
            return Double.parseDouble(valueStr.trim());
        } catch (Exception e) {
            return defaultValue;
        }
    }

    /**
     * 转换为double<br>
     * 如果给定的值为null，或者转换失败，返回0<br>
     * 转换失败不会报错
     *
     * @param value 被转换的值
     * @return 转换后的double
     */
    public static Double toDouble(Object value) {
        return toDouble(value, 0D);
    }

    /**
     * 转换为Float<br>
     * 如果给定的值为null，或者转换失败，返回默认值<br>
     * 转换失败不会报错
     *
     * @param value        被转换的值
     * @param defaultValue 转换错误时的默认值
     * @return 转换后的Float
     */
    public static Float toFloat(Object value, Float defaultValue) {
        if (value == null) {
            return defaultValue;
        }
        if (value instanceof Float) {
            return (Float) value;
        }
        if (value instanceof Number) {
            return ((Number) value).floatValue();
        }
        final String valueStr = toStr(value, null);
        if (null == valueStr) {
            return defaultValue;
        }
        try {
            return Float.parseFloat(valueStr.trim());
        } catch (Exception e) {
            return defaultValue;
        }
    }

    /**
     * 转换为Float<br>
     * 如果给定的值为null，或者转换失败，返回0<br>
     * 转换失败不会报错
     *
     * @param value 被转换的值
     * @return 转换后的Float
     */
    public static Float toFloat(Object value) {
        return toFloat(value, 0F);
    }

    /**
     * 转换为boolean<br>
     * String支持的值为：true、false、yes、ok、no，忽略大小写<br>
     * 如果给定的值为null，或者转换失败，返回默认值<br>
     * 转换失败不会报错
     *
     * @param value        被转换的值
     * @param defaultValue 转换错误时的默认值
     * @return 转换后的boolean
     */
    public static Boolean toBool(Object value, Boolean defaultValue) {
        if (value == null) {
            return defaultValue;
        }
        if (value instanceof Boolean) {
            return (Boolean) value;
        }
        String valueStr = toStr(value, null);
        if (null == valueStr) {
            return defaultValue;
        }
        valueStr = valueStr.trim().toLowerCase();
        return "true".equals(valueStr) || "yes".equals(valueStr) || "ok".equals(valueStr);
    }

    /**
     * 转换为boolean<br>
     * String支持的值为：true、false、yes、ok、no，忽略大小写<br>
     * 如果给定的值为null，或者转换失败，返回false<br>
     * 转换失败不会报错
     *
     * @param value 被转换的值
     * @return 转换后的boolean
     */
    public static Boolean toBool(Object value) {
        return toBool(value, false);
    }

    /**
     * 转换为Short<br>
     * 如果给定的值为null，或者转换失败，返回默认值<br>
     * 转换失败不会报错
     *
     * @param value        被转换的值
     * @param defaultValue 转换错误时的默认值
     * @return 转换后的Short
     */
    public static Short toShort(Object value, Short defaultValue) {
        if (value == null) {
            return defaultValue;
        }
        if (value instanceof Short) {
            return (Short) value;
        }
        if (value instanceof Number) {
            return ((Number) value).shortValue();
        }
        final String valueStr = toStr(value, null);
        if (null == valueStr) {
            return defaultValue;
        }
        try {
            return Short.parseShort(valueStr.trim());
        } catch (Exception e) {
            return defaultValue;
        }
    }

    /**
     * 转换为Short<br>
     * 如果给定的值为null，或者转换失败，返回0<br>
     * 转换失败不会报错
     *
     * @param value 被转换的值
     * @return 转换后的Short
     */
    public static Short toShort(Object value) {
        return toShort(value, (short) 0);
    }

    /**
     * 转换为BigDecimal<br>
     * 如果给定的值为null，或者转换失败，返回默认值<br>
     * 转换失败不会报错
     *
     * @param value        被转换的值
     * @param defaultValue 转换错误时的默认值
     * @return 转换后的BigDecimal
     */
    public static BigDecimal toBigDecimal(Object value, BigDecimal defaultValue) {
        if (value == null) {
            return defaultValue;
        }
        if (value instanceof BigDecimal) {
            return (BigDecimal) value;
        }
        if (value instanceof BigInteger) {
            return new BigDecimal((BigInteger) value);
        }
        if (value instanceof Number) {
            return BigDecimal.valueOf(((Number) value).doubleValue());
        }
        final String valueStr = toStr(value, null);
        if (null == valueStr) {
            return defaultValue;
        }
        try {
            return new BigDecimal(valueStr.trim());
        } catch (Exception e) {
            return defaultValue;
        }
    }

    /**
     * 转换为BigDecimal<br>
     * 如果给定的值为null，或者转换失败，返回0<br>
     * 转换失败不会报错
     *
     * @param value 被转换的值
     * @return 转换后的BigDecimal
     */
    public static BigDecimal toBigDecimal(Object value) {
        return toBigDecimal(value, BigDecimal.ZERO);
    }

    /**
     * 转换为BigInteger<br>
     * 如果给定的值为null，或者转换失败，返回默认值<br>
     * 转换失败不会报错
     *
     * @param value        被转换的值
     * @param defaultValue 转换错误时的默认值
     * @return 转换后的BigInteger
     */
    public static BigInteger toBigInteger(Object value, BigInteger defaultValue) {
        if (value == null) {
            return defaultValue;
        }
        if (value instanceof BigInteger) {
            return (BigInteger) value;
        }
        if (value instanceof BigDecimal) {
            return ((BigDecimal) value).toBigInteger();
        }
        if (value instanceof Number) {
            return BigInteger.valueOf(((Number) value).longValue());
        }
        final String valueStr = toStr(value, null);
        if (null == valueStr) {
            return defaultValue;
        }
        try {
            return new BigInteger(valueStr.trim());
        } catch (Exception e) {
            return defaultValue;
        }
    }

    /**
     * 转换为BigInteger<br>
     * 如果给定的值为null，或者转换失败，返回0<br>
     * 转换失败不会报错
     *
     * @param value 被转换的值
     * @return 转换后的BigInteger
     */
    public static BigInteger toBigInteger(Object value) {
        return toBigInteger(value, BigInteger.ZERO);
    }

    /**
     * 将对象转为字符串<br>
     * 1、Byte数组和ByteBuffer会被转换为对应字符串的数组
     * 2、对象数组会调用Arrays.toString方法
     *
     * @param obj     对象
     * @param charset 字符集
     * @return 字符串
     */
    public static String str(Object obj, String charset) {
        if (null == obj) {
            return null;
        }

        if (obj instanceof String) {
            return (String) obj;
        } else if (obj instanceof byte[] || obj instanceof Byte[]) {
            return str((byte[]) obj, charset);
        } else if (obj instanceof ByteBuffer) {
            return str((ByteBuffer) obj, charset);
        }

        return obj.toString();
    }

    /**
     * 将byte数组转为字符串
     *
     * @param bytes   byte数组
     * @param charset 字符集，如果此字段为空，则解码的结果取决于平台
     * @return 字符串
     */
    public static String str(byte[] bytes, String charset) {
        if (bytes == null) {
            return null;
        }
        if (null == charset || charset.length() == 0) {
            return new String(bytes);
        }
        return new String(bytes, Charset.forName(charset));
    }

    /**
     * 将ByteBuffer转为字符串
     *
     * @param byteBuffer ByteBuffer
     * @param charset    字符集，如果此字段为空，则解码的结果取决于平台
     * @return 字符串
     */
    public static String str(ByteBuffer byteBuffer, String charset) {
        if (byteBuffer == null) {
            return null;
        }
        return str(byteBuffer.array(), charset);
    }

    /**
     * 将对象转为字符串，使用默认字符集
     *
     * @param obj 对象
     * @return 字符串
     */
    public static String str(Object obj) {
        return str(obj, DEFAULT_CHARSET);
    }

    /**
     * 转换为字符串数组<br>
     *
     * @param obj 被转换的值
     * @return 字符串数组
     */
    public static String[] toStrArray(Object obj) {
        if (obj == null) {
            return null;
        }
        if (obj instanceof String[]) {
            return (String[]) obj;
        }
        if (obj instanceof String) {
            return new String[]{(String) obj};
        }
        if (obj instanceof Object[]) {
            Object[] objs = (Object[]) obj;
            String[] result = new String[objs.length];
            for (int i = 0; i < objs.length; i++) {
                result[i] = toStr(objs[i]);
            }
            return result;
        }
        return null;
    }

    /**
     * 转换为Integer数组<br>
     *
     * @param obj 被转换的值
     * @return Integer数组
     */
    public static Integer[] toIntArray(Object obj) {
        if (obj == null) {
            return null;
        }
        if (obj instanceof Integer[]) {
            return (Integer[]) obj;
        }
        if (obj instanceof int[]) {
            int[] array = (int[]) obj;
            Integer[] result = new Integer[array.length];
            for (int i = 0; i < array.length; i++) {
                result[i] = array[i];
            }
            return result;
        }
        String[] strs = toStrArray(obj);
        if (strs == null) {
            return null;
        }
        Integer[] result = new Integer[strs.length];
        for (int i = 0; i < strs.length; i++) {
            result[i] = toInt(strs[i], 0);
        }
        return result;
    }

    /**
     * 转换为Long数组<br>
     *
     * @param obj 被转换的值
     * @return Long数组
     */
    public static Long[] toLongArray(Object obj) {
        if (obj == null) {
            return null;
        }
        if (obj instanceof Long[]) {
            return (Long[]) obj;
        }
        if (obj instanceof long[]) {
            long[] array = (long[]) obj;
            Long[] result = new Long[array.length];
            for (int i = 0; i < array.length; i++) {
                result[i] = array[i];
            }
            return result;
        }
        String[] strs = toStrArray(obj);
        if (strs == null) {
            return null;
        }
        Long[] result = new Long[strs.length];
        for (int i = 0; i < strs.length; i++) {
            result[i] = toLong(strs[i], 0L);
        }
        return result;
    }

    /**
     * 转换为Long数组<br>
     *
     * @param str 被转换的值
     * @return Long数组
     */
    public static Long[] toLongArray(String str) {
        return toLongArray((Object) str);
    }

    /**
     * 转换为Integer数组<br>
     *
     * @param str 被转换的值
     * @return Integer数组
     */
    public static Integer[] toIntArray(String str) {
        return toIntArray((Object) str);
    }

    /**
     * 转换为String集合
     *
     * @param obj 被转换的值
     * @return String集合
     */
    @SuppressWarnings({"unchecked", "rawtypes"})
    public static Set<String> toSet(Object obj) {
        if (obj == null) {
            return null;
        }
        if (obj instanceof Set) {
            return (Set) obj;
        }
        String[] strs = toStrArray(obj);
        if (strs == null) {
            return null;
        }
        Set<String> set = new java.util.LinkedHashSet<String>();
        for (String str : strs) {
            set.add(str);
        }
        return set;
    }

    /**
     * 转换为Number数组<br>
     *
     * @param obj 被转换的值
     * @return Number数组
     */
    public static Number[] toNumberArray(Object obj) {
        if (obj == null) {
            return null;
        }
        if (obj instanceof Number[]) {
            return (Number[]) obj;
        }
        if (obj instanceof String[]) {
            String[] strs = (String[]) obj;
            Number[] numbers = new Number[strs.length];
            for (int i = 0; i < strs.length; i++) {
                numbers[i] = toBigDecimal(strs[i]);
            }
            return numbers;
        }
        return null;
    }

    /**
     * 将对象转成字符串，如果对象为null则返回空字符串
     *
     * @param obj 对象
     * @return 字符串
     */
    public static String utf8Str(Object obj) {
        return str(obj, "UTF-8");
    }

    /**
     * 将对象转成字符串，如果对象为null则返回空字符串
     *
     * @param obj 对象
     * @return 字符串
     */
    public static String utf8Str(Object obj, String defaultValue) {
        String result = str(obj, "UTF-8");
        return result == null ? defaultValue : result;
    }

    /**
     * 将字符串转换为Unicode字符串
     *
     * @param str 字符串
     * @return Unicode字符串
     */
    public static String strToUnicode(String str) {
        if (str == null) {
            return null;
        }
        StringBuilder sb = new StringBuilder();
        for (char c : str.toCharArray()) {
            sb.append("\\u").append(String.format("%04x", (int) c));
        }
        return sb.toString();
    }

    /**
     * 将Unicode字符串转换为字符串
     *
     * @param unicode Unicode字符串
     * @return 字符串
     */
    public static String unicodeToStr(String unicode) {
        if (unicode == null) {
            return null;
        }
        StringBuilder sb = new StringBuilder();
        int i = -1;
        int pos = 0;
        while ((i = unicode.indexOf("\\u", pos)) != -1) {
            sb.append(unicode.substring(pos, i));
            if (i + 5 < unicode.length()) {
                try {
                    sb.append((char) Integer.parseInt(unicode.substring(i + 2, i + 6), 16));
                    pos = i + 6;
                } catch (NumberFormatException e) {
                    sb.append(unicode.substring(i, i + 6));
                    pos = i + 6;
                }
            } else {
                sb.append(unicode.substring(i));
                pos = unicode.length();
            }
        }
        sb.append(unicode.substring(pos));
        return sb.toString();
    }

    /**
     * 将数字转换为格式化的字符串
     *
     * @param number 数字
     * @return 格式化的字符串
     */
    public static String numberToStr(Number number) {
        if (number == null) {
            return null;
        }
        return NumberFormat.getInstance().format(number);
    }

    /**
     * 将数字转换为格式化的字符串
     *
     * @param number    数字
     * @param minFractionDigits 最小小数位数
     * @param maxFractionDigits 最大小数位数
     * @return 格式化的字符串
     */
    public static String numberToStr(Number number, int minFractionDigits, int maxFractionDigits) {
        if (number == null) {
            return null;
        }
        NumberFormat format = NumberFormat.getInstance();
        format.setMinimumFractionDigits(minFractionDigits);
        format.setMaximumFractionDigits(maxFractionDigits);
        return format.format(number);
    }
}