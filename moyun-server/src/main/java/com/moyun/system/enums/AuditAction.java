package com.moyun.system.enums;

/**
 * 审核操作类型枚举（v8.1）
 * <p>
 * 对应 sys_audit_task.audit_action 字段，记录处理人执行的具体操作。
 *
 * @author moyun
 */
public enum AuditAction {

    /** 同意（通过） */
    APPROVE("approve", "同意"),
    /** 驳回（拒绝） */
    REJECT("reject", "驳回");

    private final String code;
    private final String displayName;

    AuditAction(String code, String displayName) {
        this.code = code;
        this.displayName = displayName;
    }

    public String getCode() {
        return code;
    }

    public String getDisplayName() {
        return displayName;
    }

    public static AuditAction fromCode(String code) {
        if (code == null) {
            return null;
        }
        for (AuditAction a : values()) {
            if (a.code.equals(code)) {
                return a;
            }
        }
        return null;
    }
}
