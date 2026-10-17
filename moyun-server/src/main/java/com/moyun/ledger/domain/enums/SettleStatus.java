package com.moyun.ledger.domain.enums;

/** 应收/应付结算状态。 */
public enum SettleStatus {
    PENDING("pending", "未结"),
    PARTIAL("partial", "部分结"),
    SETTLED("settled", "已结清");

    private final String value;
    private final String label;

    SettleStatus(String value, String label) { this.value = value; this.label = label; }
    public String value() { return value; }
    public String label() { return label; }
}