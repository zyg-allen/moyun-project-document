package com.moyun.ext.ai.engine.tool;

import java.util.Map;

/**
 * 工具执行器接口
 *
 * <p>所有内置工具都需要实现此接口</p>
 *
 * @author laomao
 */
public interface ToolExecutor {

    /**
     * 获取工具名称（唯一标识）
     *
     * @return 工具名称
     */
    String getName();

    /**
     * 获取工具描述（给LLM理解用）
     *
     * @return 工具描述
     */
    String getDescription();

    /**
     * 获取参数定义（JSON Schema格式）
     *
     * @return 参数定义JSON
     */
    String getParametersSchema();

    /**
     * 执行工具
     *
     * @param context 执行上下文
     * @param params 调用参数
     * @return 执行结果
     */
    ToolResult execute(ToolContext context, Map<String, Object> params);

    /**
     * 是否支持异步执行
     *
     * @return 默认false
     */
    default boolean isAsync() {
        return false;
    }

    /**
     * 获取超时时间（秒）
     *
     * @return 默认30秒
     */
    default int getTimeoutSeconds() {
        return 30;
    }

    /**
     * 从参数 Map 中安全地解析 int 值，支持 Number / 数字字符串 / null
     *
     * @param params       参数 Map
     * @param key          参数 key
     * @param defaultValue 解析失败或为空时的默认值
     * @return int 值
     */
    default int asInt(Map<String, Object> params, String key, int defaultValue) {
        Object v = params.get(key);
        if (v == null) return defaultValue;
        if (v instanceof Number) return ((Number) v).intValue();
        try {
            return Integer.parseInt(v.toString().trim());
        } catch (NumberFormatException e) {
            return defaultValue;
        }
    }

    /**
     * 从参数 Map 中安全地解析 long 值，支持 Number / 数字字符串 / null
     *
     * @param params       参数 Map
     * @param key          参数 key
     * @param defaultValue 解析失败或为空时的默认值
     * @return long 值
     */
    default long asLong(Map<String, Object> params, String key, long defaultValue) {
        Object v = params.get(key);
        if (v == null) return defaultValue;
        if (v instanceof Number) return ((Number) v).longValue();
        try {
            return Long.parseLong(v.toString().trim());
        } catch (NumberFormatException e) {
            return defaultValue;
        }
    }

    /**
     * 从参数 Map 中安全地获取 String 值
     *
     * @param params 参数 Map
     * @param key    参数 key
     * @return 字符串值，为 null 时返回 null
     */
    default String asString(Map<String, Object> params, String key) {
        Object v = params.get(key);
        return v == null ? null : v.toString();
    }
}
