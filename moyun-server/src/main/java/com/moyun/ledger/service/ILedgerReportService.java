package com.moyun.ledger.service;

import java.time.LocalDate;
import java.util.Map;

/**
 * 记账报表服务接口（Phase 3：报表中心 + CSV 导出）
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
     * 流水 CSV 导出（UTF-8 BOM，Excel 兼容）
     *
     * @param userId    门户用户ID
     * @param startDate 开始日期（null=不限）
     * @param endDate   结束日期（null=不限）
     * @return CSV 文本
     */
    String buildCsv(Long userId, LocalDate startDate, LocalDate endDate);
}
