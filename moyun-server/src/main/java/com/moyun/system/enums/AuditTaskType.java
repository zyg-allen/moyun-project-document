package com.moyun.system.enums;

/**
 * 统一审核任务类型枚举（v8.1）
 * <p>
 * 覆盖系统全部审核业务，对应 sys_audit_task.task_type 字段。
 * 每个类型绑定一个 {@code AuditBizHandler} 实现，处理 approve/reject/getBizDetail。
 *
 * @author moyun
 */
public enum AuditTaskType {

    /** 文章审核 */
    ARTICLE("article", "文章审核", "/cms/article", "cms:article:audit"),
    /** 专栏审核 */
    COLUMN("column", "专栏审核", "/cms/column", "cms:column:audit"),
    /** 话题审核 */
    TOPIC("topic", "话题审核", "/cms/topic", "cms:topic:audit"),
    /** 面经审核 */
    INTERVIEW_EXP("interview_exp", "面经审核", "/cms/interview/experience", "cms:interview:audit"),
    /** 面经评论审核 */
    INTERVIEW_COMMENT("interview_comment", "面经评论审核", "/cms/interview/comment", "cms:interview:comment:audit"),
    /** 创作者认证审核 */
    CERTIFICATION("certification", "创作者认证审核", "/certification/audit", "cms:certification:audit"),
    /** 意见反馈处理 */
    FEEDBACK("feedback", "意见反馈处理", "/cms/feedback", "cms:feedback:handle"),
    /** 举报处理 */
    REPORT("report", "举报处理", "/cms/report", "cms:report:handle");

    private final String code;
    private final String displayName;
    private final String defaultRoutePath;
    private final String requiredPermission;

    AuditTaskType(String code, String displayName, String defaultRoutePath, String requiredPermission) {
        this.code = code;
        this.displayName = displayName;
        this.defaultRoutePath = defaultRoutePath;
        this.requiredPermission = requiredPermission;
    }

    public String getCode() {
        return code;
    }

    public String getDisplayName() {
        return displayName;
    }

    public String getDefaultRoutePath() {
        return defaultRoutePath;
    }

    public String getRequiredPermission() {
        return requiredPermission;
    }

    /**
     * 根据 code 解析枚举，未匹配返回 null。
     */
    public static AuditTaskType fromCode(String code) {
        if (code == null) {
            return null;
        }
        for (AuditTaskType t : values()) {
            if (t.code.equals(code)) {
                return t;
            }
        }
        return null;
    }
}
