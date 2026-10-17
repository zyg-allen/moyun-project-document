package com.moyun.ledger.domain.enums;

/** 资产账户类型（第二层账户分类，参与计算）。 */
public enum AccountType {
    FUND("fund", "资金账户"),
    RECEIVABLE("receivable", "应收款"),
    INVESTMENT("investment", "投资"),
    FIXED("fixed", "固定资产");

    private final String value;
    private final String label;

    AccountType(String value, String label) { this.value = value; this.label = label; }
    public String value() { return value; }
    public String label() { return label; }
}