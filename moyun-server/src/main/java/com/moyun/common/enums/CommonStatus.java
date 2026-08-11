package com.moyun.common.enums;

/**
 * 通用状态枚举（启用/停用）
 *
 * <p>对应 RuoYi 体系 sys_* 表的 {@code status} 字段（char(1)）：
 * <ul>
 *   <li>{@link #NORMAL}   - {@code "0"} 正常（启用）</li>
 *   <li>{@link #DISABLED} - {@code "1"} 停用（禁用）</li>
 * </ul>
 *
 * <p>使用建议：业务代码中应使用 {@code CommonStatus.NORMAL.getCode()}
 * 替代裸字符串 "0"，避免与 {@link DelFlag#EXIST}（也是 "0"）混淆。
 * 字段语义不同：{@code status} 控制运行时启用/停用，{@code del_flag} 控制逻辑删除。
 *
 * <p>注意：本项目 AI 模块的 agent / model_config 等表使用 {@code enabled tinyint(1)}
 * 字段（Boolean，true/false），不使用本枚举。本枚举仅用于 sys_* 表。
 *
 * <p>已弃用：{@link UserStatus#OK}/{@link UserStatus#DISABLE}/{@link UserStatus#DELETED}
 * 三态合并枚举（用户表特殊需求），普通业务表请使用本枚举 + {@link DelFlag} 二维拆分。
 *
 * @author moyun
 */
public enum CommonStatus {

    NORMAL("0", "正常"),
    DISABLED("1", "停用");

    private final String code;
    private final String desc;

    CommonStatus(String code, String desc) {
        this.code = code;
        this.desc = desc;
    }

    public String getCode() {
        return code;
    }

    public String getDesc() {
        return desc;
    }

    public static CommonStatus fromCode(String code) {
        if (code == null) {
            return NORMAL;
        }
        for (CommonStatus status : values()) {
            if (status.code.equals(code)) {
                return status;
            }
        }
        return NORMAL;
    }
}
