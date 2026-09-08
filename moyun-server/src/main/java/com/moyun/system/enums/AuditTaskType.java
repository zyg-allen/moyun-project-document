package com.moyun.system.enums;

/**
 * 统一审核任务类型枚举（v8.1）
 * <p>
 * 覆盖系统全部审核业务，对应 sys_audit_task.task_type 字段。
 * 每个类型绑定一个 {@code AuditBizHandler} 实现，处理 approve/reject/getBizDetail。
 * <p>
 * v11.35.1：新增 bizRoutePath（原业务管理页路由）。
 * defaultRoutePath 统一指向审核中心（首页待办跳转用），
 * 详情弹层「查看原业务」使用 bizRoutePath 直达业务管理菜单页。
 *
 * @author moyun
 */
public enum AuditTaskType {

    /** 文章审核 */
    ARTICLE("article", "文章审核", "/portal/audit-center", "cms:article:audit", "/cms/article"),
    /** 专栏审核 */
    COLUMN("column", "专栏审核", "/portal/audit-center", "cms:column:audit", "/cms/column"),
    /** 话题审核 */
    TOPIC("topic", "话题审核", "/portal/audit-center", "cms:topic:audit", "/cms/topic"),
    /** 面经审核 */
    INTERVIEW_EXP("interview_exp", "面经审核", "/portal/audit-center", "cms:interview:audit", "/interview/experienceTab"),
    /** 面经评论审核 */
    INTERVIEW_COMMENT("interview_comment", "面经评论审核", "/portal/audit-center", "cms:interview:comment:audit", "/interview/experienceTab"),
    /** 创作者认证审核 */
    CERTIFICATION("certification", "创作者认证审核", "/portal/audit-center", "cms:certification:audit", "/certification/audit"),
    /** 意见反馈处理 */
    FEEDBACK("feedback", "意见反馈处理", "/portal/audit-center", "cms:feedback:handle", "/cms/feedback"),
    /** 举报处理 */
    REPORT("report", "举报处理", "/portal/audit-center", "cms:report:handle", "/cms/report");

    private final String code;
    private final String displayName;
    private final String defaultRoutePath;
    private final String requiredPermission;
    /** 原业务管理页路由（审核中心详情弹层「查看原业务」跳转用） */
    private final String bizRoutePath;

    AuditTaskType(String code, String displayName, String defaultRoutePath, String requiredPermission, String bizRoutePath) {
        this.code = code;
        this.displayName = displayName;
        this.defaultRoutePath = defaultRoutePath;
        this.requiredPermission = requiredPermission;
        this.bizRoutePath = bizRoutePath;
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

    public String getBizRoutePath() {
        return bizRoutePath;
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