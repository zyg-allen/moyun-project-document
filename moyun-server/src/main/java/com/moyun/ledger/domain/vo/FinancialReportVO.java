package com.moyun.ledger.domain.vo;

import lombok.Data;

import java.math.BigDecimal;
import java.util.Map;

/**
 * 财务分析报告 VO（记账财务分析整改方案 v2）
 *
 * <p>结构：核心指标 + 资产负债表 + 收支报表 + 关键比率 + 规则引擎评分 + 画像。
 * 全部为确定性规则计算结果——AI 有则增强解读、无则本表即可用（兜底）。
 *
 * @author moyun
 */
@Data
public class FinancialReportVO {

    /** 报告期间（yyyy-MM） */
    private String period;

    /** 报告生成日期（资产负债表时点） */
    private String asOfDate;

    /** 用户画像（AI 个性化分析基础数据；字段缺失时仅含已有信息） */
    private Map<String, Object> profile;

    /** 资产负债表 */
    private BalanceSheetVO balanceSheet = new BalanceSheetVO();

    /** 收支报表 */
    private IncomeStatementVO incomeStatement = new IncomeStatementVO();

    /** 关键财务比率（6 个，确定性） */
    private RatiosVO ratios = new RatiosVO();

    /** 规则引擎评分（兜底核心：总分 0-100 + 等级 + 兜底文案） */
    private ScoreVO score = new ScoreVO();

    /** 资产负债表（时点数据，按账户类型汇总） */
    @Data
    public static class BalanceSheetVO {
        /** 流动资产（现金/储蓄/电子钱包/储值卡） */
        private BigDecimal currentAssets = BigDecimal.ZERO;
        /** 投资资产 */
        private BigDecimal investmentAssets = BigDecimal.ZERO;
        /** 自用资产（固定资产） */
        private BigDecimal selfUseAssets = BigDecimal.ZERO;
        /** 其他资产（债权等） */
        private BigDecimal otherAssets = BigDecimal.ZERO;
        /** 总资产 */
        private BigDecimal totalAssets = BigDecimal.ZERO;
        /** 短期负债 */
        private BigDecimal shortTermLiabilities = BigDecimal.ZERO;
        /** 长期负债 */
        private BigDecimal longTermLiabilities = BigDecimal.ZERO;
        /** 总负债 */
        private BigDecimal totalLiabilities = BigDecimal.ZERO;
        /** 净资产 = 总资产 - 总负债 */
        private BigDecimal netAssets = BigDecimal.ZERO;
    }

    /** 收支报表（期间数据，主动/被动收入、必要/弹性支出） */
    @Data
    public static class IncomeStatementVO {
        /** 主动收入（工资/劳务等劳动所得） */
        private BigDecimal activeIncome = BigDecimal.ZERO;
        /** 被动收入（利息/租金/分红/投资收益等） */
        private BigDecimal passiveIncome = BigDecimal.ZERO;
        /** 总收入 */
        private BigDecimal totalIncome = BigDecimal.ZERO;
        /** 必要支出（生活刚需） */
        private BigDecimal necessaryExpense = BigDecimal.ZERO;
        /** 弹性支出（可优化空间） */
        private BigDecimal flexibleExpense = BigDecimal.ZERO;
        /** 总支出 */
        private BigDecimal totalExpense = BigDecimal.ZERO;
        /** 结余 = 总收入 - 总支出 */
        private BigDecimal surplus = BigDecimal.ZERO;
        /** 期间月供合计（负债收入比口径） */
        private BigDecimal monthlyDebtPayment = BigDecimal.ZERO;
    }
}
