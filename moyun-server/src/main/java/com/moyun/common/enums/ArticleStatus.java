package com.moyun.common.enums;

/**
 * 文章状态枚举
 *
 * <p>对应数据库表 portal_article.status 字段，标识文章从草稿到归档的完整生命周期。
 * 与 {@link AuditStatus} 的差异：ArticleStatus 额外包含 DRAFT/ARCHIVED，
 * AuditStatus 仅含审核三态（PENDING/PUBLISHED/REJECTED），用于审核动作语义化。
 *
 * <p>使用建议：业务代码中应使用 {@code ArticleStatus.PUBLISHED.getCode()}
 * 替代裸字符串 "published"，避免拼写错误。
 *
 * @author moyun
 */
public enum ArticleStatus {

    DRAFT("draft", "草稿"),
    PENDING("pending", "待审核"),
    PUBLISHED("published", "已发布"),
    REJECTED("rejected", "已驳回"),
    ARCHIVED("archived", "已归档");

    private final String code;
    private final String desc;

    ArticleStatus(String code, String desc) {
        this.code = code;
        this.desc = desc;
    }

    public String getCode() {
        return code;
    }

    public String getDesc() {
        return desc;
    }

    public static ArticleStatus fromCode(String code) {
        if (code == null) {
            return DRAFT;
        }
        for (ArticleStatus status : values()) {
            if (status.code.equals(code)) {
                return status;
            }
        }
        return DRAFT;
    }
}
