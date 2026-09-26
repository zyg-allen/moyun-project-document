package com.moyun.ledger.service;

import com.moyun.ledger.domain.vo.FinancialReportVO;

import java.time.LocalDate;
import java.util.Map;

/**
 * 记账报表服务接口（Phase 3：报表中心 + CSV 导出；v2 整改：+ 财务分析报告）
 *
 * @author moyun
 */
public interface ILedgerReportService {

    /**
     * 报表总览（年度）
     *
     * @param userId 门户用户ID
     * @param year    统计年份
     * @return monthlyTrend(12月收支) / yearIncome / yearExpense / categoryIncome / categoryExpense
     *         / netWorthTrend(近30天) / accountDistribution / liabilityOverview / budgetExec(当月)
     */
    Map<String, Object> overview(Long userId, int year);

    /**
     * 财务分析报告（记账财务分析整改方案 v2：确定性报表 + 6 比率 + 规则引擎评分）
     *
     * <p>核心指标（净资产/收入/支出/结余）+ 资产负债表 + 收支报表 + 关键比率 +
     * 健康评分（A-D 等级 + 兜底文案）+ 用户画像（AI 增强上下文）。全部规则计算，
     * 不依赖 AI；AI 有则解读增强、无则本报表即可用。
     *
     * @param userId 门户用户ID
     * @param period 期间（yyyy-MM；null=当月）
     * @return FinancialReportVO
     */
    FinancialReportVO financialReport(Long userId, String period);

    /**
     * 流水 CSV 导出（UTF-8 BOM，Excel 兼容）
     *
     * @param userId    门户用户ID
     * @param startDate 开始日期（null=不限）
     * @param endDate   结束日期（null=不限）
     * @return CSV 文本
     */
    String buildCsv(Long userId, LocalDate startDate, LocalDate endDate);
}
