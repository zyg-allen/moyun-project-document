package com.moyun.ext.ai.enums;

/**
 * 工具分类枚举
 *
 * <p>对应数据库表 agent_tool.category 字段，用于工具市场的分组展示：
 * <ul>
 *   <li>{@link #GENERAL}      - 通用工具</li>
 *   <li>{@link #INFORMATION}  - 信息查询类（天气、搜索、网页读取）</li>
 *   <li>{@link #UTILITY}      - 实用工具类（计算器、翻译、时间）</li>
 *   <li>{@link #ACTION}        - 动作执行类（邮件、通知）</li>
 *   <li>{@link #DATA}          - 数据处理类（数据库查询）</li>
 * </ul>
 *
 * @author moyun
 */
public enum ToolCategory {

    GENERAL("general", "通用"),
    INFORMATION("information", "信息查询"),
    UTILITY("utility", "实用工具"),
    ACTION("action", "动作执行"),
    DATA("data", "数据处理");

    private final String code;
    private final String desc;

    ToolCategory(String code, String desc) {
        this.code = code;
        this.desc = desc;
    }

    public String getCode() {
        return code;
    }

    public String getDesc() {
        return desc;
    }

    public static ToolCategory fromCode(String code) {
        if (code == null) {
            return GENERAL;
        }
        for (ToolCategory category : values()) {
            if (category.code.equals(code)) {
                return category;
            }
        }
        return GENERAL;
    }
}
