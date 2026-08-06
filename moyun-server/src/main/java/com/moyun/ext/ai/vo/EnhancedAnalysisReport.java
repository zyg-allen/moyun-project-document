package com.moyun.ext.ai.vo;

import lombok.Data;
import java.util.List;

/**
 * 增强的数据分析报告
 * 
 * @author laomao
 */
@Data
public class EnhancedAnalysisReport {
    
    /**
     * 数据概况
     */
    private DataOverview overview;
    
    /**
     * 深度洞察
     */
    private List<Insight> insights;
    
    /**
     * 趋势分析
     */
    private TrendAnalysis trend;
    
    /**
     * 异常发现
     */
    private List<Anomaly> anomalies;
    
    /**
     * 行动建议
     */
    private List<String> recommendations;
    
    /**
     * 预测分析
     */
    private Prediction prediction;
    
    /**
     * 数据概况
     */
    @Data
    public static class DataOverview {
        private Integer totalCount;
        private String mainMetric;
        private String mainMetricValue;
        private String summary;
    }
    
    /**
     * 深度洞察
     */
    @Data
    public static class Insight {
        private String title;
        private String content;
        private String type; // info, warning, success
        private Double impact; // 影响度 0-1
    }
    
    /**
     * 趋势分析
     */
    @Data
    public static class TrendAnalysis {
        private String direction; // up, down, stable
        private Double change; // 变化幅度
        private String period; // 时间周期
        private String description;
    }
    
    /**
     * 异常
     */
    @Data
    public static class Anomaly {
        private String title;
        private String description;
        private String severity; // low, medium, high
        private String possibleCause;
    }
    
    /**
     * 预测
     */
    @Data
    public static class Prediction {
        private String metric;
        private Double predictedValue;
        private String timeframe;
        private Double confidence; // 置信度 0-1
        private String description;
    }
}
