package com.moyun.ledger.domain.enums;

/**
 * 负债账户风险标记（我欠别人的：是否按时还）。
 *
 * <p>与资产侧 {@link RiskLevel}（应收款的催收/坏账）区分：
 * 负债关注的是「我有没有按时还」，故用 逾期/违约 而非 关注/坏账。</p>
 */
public enum LiabilityRiskLevel {
    NORMAL("normal", "正常"),
    OVERDUE("overdue", "逾期"),
    DEFAULT("default", "违约");

    private final String value;
    private final String label;

    LiabilityRiskLevel(String value, String label) { this.value = value; this.label = label; }
    public String value() { return value; }
    public String label() { return label; }
}
