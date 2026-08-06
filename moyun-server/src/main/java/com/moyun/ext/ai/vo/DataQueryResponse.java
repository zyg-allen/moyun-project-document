package com.moyun.ext.ai.vo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;

/**
 * 数据查询响应VO
 *
 * @author laomao
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DataQueryResponse {

    /**
     * 查询ID
     */
    private Long queryId;

    /**
     * 生成的SQL
     */
    private String generatedSql;

    /**
     * 查询类型
     */
    private String queryType;

    /**
     * 执行时间(毫秒)
     */
    private Integer executionTime;

    /**
     * 结果数据
     */
    private List<Map<String, Object>> data;

    /**
     * 结果总数
     */
    private Integer totalCount;

    /**
     * 列信息
     */
    private List<ColumnInfo> columns;

    /**
     * 基础统计
     */
    private Map<String, Object> statistics;

    /**
     * 图表推荐
     */
    private List<ChartRecommendation> chartRecommendations;

    /**
     * AI分析洞察
     */
    private String analysis;

    /**
     * 数据洞察列表
     */
    private List<DataInsight> insights;

    /**
     * 是否成功
     */
    private Boolean success;

    /**
     * 错误信息
     */
    private String errorMessage;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ColumnInfo {
        private String columnName;
        private String dataType;
        private String comment;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ChartRecommendation {
        private String chartType;
        private Integer priority;
        private String reason;
        private Map<String, Object> config;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class DataInsight {
        private String type;
        private String severity;
        private String title;
        private String description;
        private String recommendation;
    }
}
