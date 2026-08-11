package com.moyun.ext.ai.enums;

/**
 * 工作流状态枚举
 *
 * <p>对应数据库表 workflow.status 字段，标识工作流的生命周期阶段：
 * <ul>
 *   <li>{@link #DRAFT}     - 草稿：编辑中，未发布，仅创建者可见</li>
 *   <li>{@link #PUBLISHED} - 已发布：可被智能体引用并执行</li>
 *   <li>{@link #DISABLED}  - 已禁用：保留数据但停止调度</li>
 * </ul>
 *
 * <p>使用建议：业务代码中应使用 {@code WorkflowStatus.PUBLISHED.getCode()} 替代裸字符串 "published"，
 * 避免拼写错误导致状态匹配失败。
 *
 * @author moyun
 */
public enum WorkflowStatus {

    DRAFT("draft", "草稿"),
    PUBLISHED("published", "已发布"),
    DISABLED("disabled", "已禁用");

    private final String code;
    private final String desc;

    WorkflowStatus(String code, String desc) {
        this.code = code;
        this.desc = desc;
    }

    public String getCode() {
        return code;
    }

    public String getDesc() {
        return desc;
    }

    public static WorkflowStatus fromCode(String code) {
        if (code == null) {
            return DRAFT;
        }
        for (WorkflowStatus status : values()) {
            if (status.code.equals(code)) {
                return status;
            }
        }
        return DRAFT;
    }
}
