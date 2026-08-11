package com.moyun.common.enums;

/**
 * 软删除标识枚举
 *
 * <p>对应 RuoYi 体系 sys_* 表的 {@code del_flag} 字段（char(1)）：
 * <ul>
 *   <li>{@link #EXIST}  - {@code "0"} 正常（未删除）</li>
 *   <li>{@link #DELETED} - {@code "2"} 已删除（软删）</li>
 * </ul>
 *
 * <p>使用建议：业务代码中应使用 {@code DelFlag.EXIST.getCode()}
 * 替代裸字符串 "0"，避免与 {@link CommonStatus#NORMAL}（也是 "0"）混淆。
 *
 * <p>注意：本项目 AI 模块的 9 张核心表使用 {@code deleted tinyint(1)} 字段
 * （0/1，由 {@code @TableLogic} 处理），不使用本枚举。本枚举仅用于 sys_* 表。
 *
 * <p>历史：RuoYi 框架的 del_flag 跳过 "1"（保留给"已禁用"语义），
 * 删除直接到 "2"，避免与 CommonStatus 混淆。
 *
 * @author moyun
 */
public enum DelFlag {

    EXIST("0", "正常"),
    DELETED("2", "删除");

    private final String code;
    private final String desc;

    DelFlag(String code, String desc) {
        this.code = code;
        this.desc = desc;
    }

    public String getCode() {
        return code;
    }

    public String getDesc() {
        return desc;
    }

    public static DelFlag fromCode(String code) {
        if (code == null) {
            return EXIST;
        }
        for (DelFlag flag : values()) {
            if (flag.code.equals(code)) {
                return flag;
            }
        }
        return EXIST;
    }
}
