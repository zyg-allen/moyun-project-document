package com.moyun.common.enums;

/**
 * 智能体启用状态枚举
 *
 * <p>对应数据库表 agent.enabled 字段（tinyint(1)，存储为 Boolean）：
 * <ul>
 *   <li>{@link #ENABLED}  - {@code Boolean.TRUE} 启用：可被前台调用</li>
 *   <li>{@link #DISABLED} - {@code Boolean.FALSE} 停用：仅创建者可见，不被调度</li>
 * </ul>
 *
 * <p>使用建议：业务代码中应使用 {@code AgentStatus.ENABLED.getCode()}
 * 替代裸 {@code true}/{@code false}，使状态语义自解释。
 *
 * <p>注意：本枚举使用 Boolean 作为 code 类型，与 {@link CommonStatus}（String "0"/"1"）
 * 不同。原因：agent 表的 enabled 字段是 {@code tinyint(1)} 映射为 Boolean，
 * 而非 sys_* 表的 {@code char(1)} status 字段。
 *
 * <p>扩展：智能体还有 apiEnabled / publishEnabled 等独立 Boolean 开关字段，
 * 各自承载不同语义（API 调用权限/发布权限），不与本枚举的"主启用开关"混用。
 *
 * @author moyun
 */
public enum AgentStatus {

    ENABLED(Boolean.TRUE, "启用"),
    DISABLED(Boolean.FALSE, "停用");

    private final Boolean code;
    private final String desc;

    AgentStatus(Boolean code, String desc) {
        this.code = code;
        this.desc = desc;
    }

    public Boolean getCode() {
        return code;
    }

    public String getDesc() {
        return desc;
    }

    /**
     * 字符串转枚举（兼容前端传 "true"/"false" 字符串的场景）
     */
    public static AgentStatus fromCode(String code) {
        if (code == null) {
            return DISABLED;
        }
        if ("true".equalsIgnoreCase(code) || "1".equals(code)) {
            return ENABLED;
        }
        if ("false".equalsIgnoreCase(code) || "0".equals(code)) {
            return DISABLED;
        }
        return DISABLED;
    }

    /**
     * Boolean 转枚举
     */
    public static AgentStatus fromCode(Boolean code) {
        return Boolean.TRUE.equals(code) ? ENABLED : DISABLED;
    }
}
