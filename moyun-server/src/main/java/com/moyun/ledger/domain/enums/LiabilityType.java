package com.moyun.ledger.domain.enums;

/** 负债账户类型（第二层账户分类，参与计算）。 */
public enum LiabilityType {
    DEBT("debt", "普通负债"),
    CREDIT("credit", "赊账应付");

    private final String value;
    private final String label;

    LiabilityType(String value, String label) { this.value = value; this.label = label; }
    public String value() { return value; }
    public String label() { return label; }
}