package com.moyun.ext.ai.enums;

/**
 * 工具类型枚举
 *
 * <p>对应数据库表 agent_tool.tool_type 字段，标识工具的实现方式：
 * <ul>
 *   <li>{@link #BUILTIN}   - 内置工具（Java 实现，如 current_time, calculator）</li>
 *   <li>{@link #HTTP}      - HTTP 工具（调用外部 REST API，如 weather_query）</li>
 *   <li>{@link #DATABASE}  - 数据库工具（执行 SQL 查询，如 database_query）</li>
 * </ul>
 *
 * @author moyun
 */
public enum ToolType {

    BUILTIN("builtin", "内置工具"),
    HTTP("http", "HTTP工具"),
    DATABASE("database", "数据库工具");

    private final String code;
    private final String desc;

    ToolType(String code, String desc) {
        this.code = code;
        this.desc = desc;
    }

    public String getCode() {
        return code;
    }

    public String getDesc() {
        return desc;
    }

    public static ToolType fromCode(String code) {
        if (code == null) {
            return BUILTIN;
        }
        for (ToolType type : values()) {
            if (type.code.equals(code)) {
                return type;
            }
        }
        return BUILTIN;
    }
}
