package com.moyun.common.enums;

/**
 * 审核状态枚举
 *
 * <p>统一审核动作的三态语义，适用于：
 * <ul>
 *   <li>文章审核（{@code PENDING} → {@code PUBLISHED} 或 {@code REJECTED}）</li>
 *   <li>话题审核（{@code PENDING} → {@code PUBLISHED} 或 {@code REJECTED}）</li>
 *   <li>面经审核（{@code PENDING} → {@code PUBLISHED} 或 {@code REJECTED}）</li>
 *   <li>创作者认证审核（pending → approved/rejected，"approved" 不在本枚举中，
 *       因认证是独立业务流程，使用 {@code PortalCreatorCertificationStatus}）</li>
 * </ul>
 *
 * <p>与 {@link ArticleStatus} 的差异：AuditStatus 仅含审核动作的三态，
 * 不包含 DRAFT/ARCHIVED 等内容生命周期状态。
 *
 * <p>使用建议：审核 Controller/Service 应使用 {@code AuditStatus.PUBLISHED.getCode()}
 * 替代裸字符串 "published"，避免拼写错误导致状态机错乱。
 *
 * @author moyun
 */
public enum AuditStatus {

    PENDING("pending", "待审核"),
    PUBLISHED("published", "审核通过"),
    REJECTED("rejected", "审核驳回");

    private final String code;
    private final String desc;

    AuditStatus(String code, String desc) {
        this.code = code;
        this.desc = desc;
    }

    public String getCode() {
        return code;
    }

    public String getDesc() {
        return desc;
    }

    public static AuditStatus fromCode(String code) {
        if (code == null) {
            return PENDING;
        }
        for (AuditStatus status : values()) {
            if (status.code.equals(code)) {
                return status;
            }
        }
        return PENDING;
    }
}
