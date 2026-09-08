package com.moyun.ext.ai2.model.data;

import lombok.Data;

import java.util.List;

/**
 * 场景4：财务分析数据（scene = finance_analysis / ledger）
 *
 * @author laomao
 * @since 2026-09-09
 */
@Data
public class LedgerSceneData {

    /** 综述 */
    private String summary;

    /** 分类支出 */
    private List<CategoryExpense> categoryExpenses;

    /** 趋势数据 */
    private List<TrendPoint> trendData;

    /** 建议 */
    private String suggestion;

    /** 健康评分（0-100） */
    private Integer healthScore;

    /**
     * 分类支出
     */
    @Data
    public static class CategoryExpense {
        private String category;
        private Double amount;
        private Double ratio;
    }

    /**
     * 趋势点
     */
    @Data
    public static class TrendPoint {
        private String period;
        private Double value;
    }
}
