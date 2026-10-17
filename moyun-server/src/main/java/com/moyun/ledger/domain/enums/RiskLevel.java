package com.moyun.ledger.domain.enums;

/** 账户风险标记（应收/应付关注程度，与账户关联打标）。 */
public enum RiskLevel {
    NORMAL("normal", "正常"),
    ATTENTION("attention", "关注"),
    BAD_DEBT("bad_debt", "坏账");

    private final String value;
    private final String label;

    RiskLevel(String value, String label) { this.value = value; this.label = label; }
    public String value() { return value; }
    public String label() { return label; }
}