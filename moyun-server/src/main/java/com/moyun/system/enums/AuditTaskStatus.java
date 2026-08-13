package com.moyun.system.enums;

/**
 * 统一审核任务状态枚举（v8.1）
 * <p>
 * 对应 sys_audit_task.status 字段，统一各业务表不一致的通过态命名
 * （article: published / topic: active / certification: approved / feedback&report: resolved）。
 *
 * @author moyun
 */
public enum AuditTaskStatus {

    /** 待处理 */
    PENDING("pending", "待处理"),
    /** 已通过（同意） */
    APPROVED("approved", "已通过"),
    /** 已驳回（拒绝） */
    REJECTED("rejected", "已驳回");

    private final String code;
    private final String displayName;

    AuditTaskStatus(String code, String displayName) {
        this.code = code;
        this.displayName = displayName;
    }

    public String getCode() {
        return code;
    }

    public String getDisplayName() {
        return displayName;
    }

    /**
     * 是否终态（已处理）。
     */
    public boolean isFinal() {
        return this != PENDING;
    }

    public static AuditTaskStatus fromCode(String code) {
        if (code == null) {
            return PENDING;
        }
        for (AuditTaskStatus s : values()) {
            if (s.code.equals(code)) {
                return s;
            }
        }
        return PENDING;
    }
}
