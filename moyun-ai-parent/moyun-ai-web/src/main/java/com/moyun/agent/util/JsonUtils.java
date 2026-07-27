package com.moyun.agent.util;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import lombok.extern.slf4j.Slf4j;

/**
 * JSON工具类
 *
 * <p>提供JSON字符串处理的通用方法，包括：</p>
 * <ul>
 *     <li>对象序列化/反序列化</li>
 *     <li>JSON字符串转义</li>
 *     <li>JSON键值对构建</li>
 *     <li>支持Java 8日期时间类型（LocalDateTime等）</li>
 * </ul>
 * 
 * <p>线程安全：ObjectMapper实例是线程安全的，可以在多线程环境下使用</p>
 *
 * @author laomao
 * @since 2025-11-30
 */
@Slf4j
public final class JsonUtils {

    /**
     * 全局共享的ObjectMapper实例（线程安全）
     * 配置为线程安全且高性能的单例模式，支持Java 8日期时间API
     */
    private static final ObjectMapper OBJECT_MAPPER;
    
    static {
        OBJECT_MAPPER = new ObjectMapper();
        // 注册JavaTimeModule以支持Java 8日期时间类型（LocalDateTime等）
        OBJECT_MAPPER.registerModule(new JavaTimeModule());
        // 禁用将日期写为时间戳的默认行为，使用ISO-8601格式
        OBJECT_MAPPER.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
    }

    private JsonUtils() {
        // 工具类禁止实例化
    }

    /**
     * JSON字符串转义
     *
     * <p>将字符串中的特殊字符转义为JSON安全格式</p>
     *
     * @param str 原始字符串
     * @return 转义后的字符串
     */
    public static String escapeJson(String str) {
        if (str == null) {
            return "";
        }
        StringBuilder sb = new StringBuilder();
        for (char c : str.toCharArray()) {
            switch (c) {
                case '"':
                    sb.append("\\\"");
                    break;
                case '\\':
                    sb.append("\\\\");
                    break;
                case '\b':
                    sb.append("\\b");
                    break;
                case '\f':
                    sb.append("\\f");
                    break;
                case '\n':
                    sb.append("\\n");
                    break;
                case '\r':
                    sb.append("\\r");
                    break;
                case '\t':
                    sb.append("\\t");
                    break;
                default:
                    if (c < ' ') {
                        sb.append(String.format("\\u%04x", (int) c));
                    } else {
                        sb.append(c);
                    }
            }
        }
        return sb.toString();
    }

    /**
     * 安全获取字符串值（用于JSON构建）
     *
     * <p>如果值为null，返回空字符串</p>
     *
     * @param value 原始值
     * @return 安全的字符串值
     */
    public static String safeString(String value) {
        return value != null ? value : "";
    }

    /**
     * 安全获取字符串值并转义
     *
     * @param value 原始值
     * @return 转义后的安全字符串
     */
    public static String safeEscapedString(String value) {
        return escapeJson(safeString(value));
    }

    /**
     * 构建JSON键值对
     *
     * @param key 键名
     * @param value 字符串值
     * @return JSON键值对字符串
     */
    public static String jsonPair(String key, String value) {
        return "\"" + key + "\":\"" + escapeJson(value) + "\"";
    }

    /**
     * 构建JSON键值对（数值类型）
     *
     * @param key 键名
     * @param value 数值
     * @return JSON键值对字符串
     */
    public static String jsonPair(String key, Number value) {
        return "\"" + key + "\":" + (value != null ? value : "null");
    }

    /**
     * 构建JSON键值对（布尔类型）
     *
     * @param key 键名
     * @param value 布尔值
     * @return JSON键值对字符串
     */
    public static String jsonPair(String key, boolean value) {
        return "\"" + key + "\":" + value;
    }
    
    // ==================== 序列化/反序列化方法 ====================
    
    /**
     * 对象转JSON字符串
     * 
     * <p>线程安全的序列化方法，将Java对象转换为JSON字符串</p>
     * 
     * @param obj 要序列化的对象
     * @return JSON字符串，序列化失败返回null
     */
    public static String toJson(Object obj) {
        if (obj == null) {
            return null;
        }
        try {
            return OBJECT_MAPPER.writeValueAsString(obj);
        } catch (JsonProcessingException e) {
            log.error("❌ JSON序列化失败: {}", obj.getClass().getSimpleName(), e);
            return null;
        }
    }
    
    /**
     * 对象转JSON字符串（带默认值）
     * 
     * <p>序列化失败时返回指定的默认值</p>
     * 
     * @param obj 要序列化的对象
     * @param defaultValue 默认值
     * @return JSON字符串，序列化失败返回defaultValue
     */
    public static String toJson(Object obj, String defaultValue) {
        if (obj == null) {
            return defaultValue;
        }
        try {
            return OBJECT_MAPPER.writeValueAsString(obj);
        } catch (JsonProcessingException e) {
            log.warn("⚠️  JSON序列化失败，使用默认值: {}", obj.getClass().getSimpleName());
            return defaultValue;
        }
    }
    
    /**
     * 对象转格式化的JSON字符串
     * 
     * <p>带缩进的美化JSON，便于阅读和调试</p>
     * 
     * @param obj 要序列化的对象
     * @return 格式化的JSON字符串
     */
    public static String toPrettyJson(Object obj) {
        if (obj == null) {
            return null;
        }
        try {
            return OBJECT_MAPPER.writerWithDefaultPrettyPrinter().writeValueAsString(obj);
        } catch (JsonProcessingException e) {
            log.error("❌ JSON格式化失败: {}", obj.getClass().getSimpleName(), e);
            return null;
        }
    }
    
    /**
     * JSON字符串转对象
     * 
     * <p>线程安全的反序列化方法</p>
     * 
     * @param json JSON字符串
     * @param clazz 目标类型
     * @param <T> 泛型类型
     * @return 反序列化后的对象，失败返回null
     */
    public static <T> T fromJson(String json, Class<T> clazz) {
        if (json == null || json.trim().isEmpty()) {
            return null;
        }
        try {
            return OBJECT_MAPPER.readValue(json, clazz);
        } catch (JsonProcessingException e) {
            log.error("❌ JSON反序列化失败: target={}, json={}", clazz.getSimpleName(), 
                    json.length() > 100 ? json.substring(0, 100) + "..." : json, e);
            return null;
        }
    }
    
    /**
     * JSON字符串转对象（支持泛型）
     * 
     * <p>用于复杂泛型类型的反序列化，例如：List&lt;User&gt;、Map&lt;String, Object&gt;</p>
     * 
     * @param json JSON字符串
     * @param typeReference 类型引用
     * @param <T> 泛型类型
     * @return 反序列化后的对象，失败返回null
     */
    public static <T> T fromJson(String json, TypeReference<T> typeReference) {
        if (json == null || json.trim().isEmpty()) {
            return null;
        }
        try {
            return OBJECT_MAPPER.readValue(json, typeReference);
        } catch (JsonProcessingException e) {
            log.error("❌ JSON反序列化失败: json={}", 
                    json.length() > 100 ? json.substring(0, 100) + "..." : json, e);
            return null;
        }
    }
    
    /**
     * 对象深拷贝
     * 
     * <p>通过JSON序列化和反序列化实现对象深拷贝</p>
     * <p>注意：此方法性能较低，仅适用于不频繁调用的场景</p>
     * 
     * @param obj 源对象
     * @param clazz 目标类型
     * @param <T> 泛型类型
     * @return 深拷贝后的对象，失败返回null
     */
    public static <T> T deepCopy(Object obj, Class<T> clazz) {
        if (obj == null) {
            return null;
        }
        try {
            String json = OBJECT_MAPPER.writeValueAsString(obj);
            return OBJECT_MAPPER.readValue(json, clazz);
        } catch (JsonProcessingException e) {
            log.error("❌ 对象深拷贝失败: {}", obj.getClass().getSimpleName(), e);
            return null;
        }
    }
    
    /**
     * 获取ObjectMapper实例
     * 
     * <p>提供全局ObjectMapper访问，用于特殊场景</p>
     * 
     * @return ObjectMapper实例
     */
    public static ObjectMapper getObjectMapper() {
        return OBJECT_MAPPER;
    }
}
