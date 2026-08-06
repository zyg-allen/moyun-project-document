package com.moyun.ext.ai.service.impl;

import com.moyun.ext.ai.service.EnhancedAnalysisService;
import com.moyun.ext.ai.service.LLMService;
import com.moyun.ext.ai.vo.DataQueryResponse;
import com.moyun.ext.ai.vo.EnhancedAnalysisReport;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

/**
 * 增强的数据分析服务实现
 * 
 * @author laomao
 */
@Slf4j
@Service
public class EnhancedAnalysisServiceImpl implements EnhancedAnalysisService {
    
    @Autowired
    private LLMService llmService;
    
    @Override
    public EnhancedAnalysisReport generateEnhancedAnalysis(DataQueryResponse queryResponse, String naturalQuery) {
        log.info("开始生成增强分析报告，查询: {}", naturalQuery);
        
        EnhancedAnalysisReport report = new EnhancedAnalysisReport();
        
        try {
            // 1. 数据概况
            report.setOverview(generateOverview(queryResponse, naturalQuery));
            
            // 2. 深度洞察
            report.setInsights(generateInsights(queryResponse, naturalQuery));
            
            // 3. 趋势分析
            report.setTrend(analyzeTrend(queryResponse));
            
            // 4. 异常检测
            report.setAnomalies(detectAnomaliesAdvanced(queryResponse));
            
            // 5. 行动建议
            report.setRecommendations(generateRecommendations(queryResponse, naturalQuery));
            
            // 6. 预测分析
            report.setPrediction(generatePrediction(queryResponse));
            
            log.info("增强分析报告生成完成");
        } catch (Exception e) {
            log.error("生成增强分析报告失败", e);
        }
        
        return report;
    }
    
    /**
     * 生成数据概况
     */
    private EnhancedAnalysisReport.DataOverview generateOverview(DataQueryResponse response, String naturalQuery) {
        EnhancedAnalysisReport.DataOverview overview = new EnhancedAnalysisReport.DataOverview();
        overview.setTotalCount(response.getTotalCount());
        
        // 识别主要指标
        if (response.getData() != null && !response.getData().isEmpty()) {
            Map<String, Object> firstRow = response.getData().get(0);
            
            // 找到第一个数值型字段作为主要指标
            for (Map.Entry<String, Object> entry : firstRow.entrySet()) {
                if (entry.getValue() instanceof Number) {
                    overview.setMainMetric(entry.getKey());
                    overview.setMainMetricValue(String.valueOf(entry.getValue()));
                    break;
                }
            }
        }
        
        // 生成摘要
        String summary = String.format("共分析 %d 条数据", response.getTotalCount());
        if (overview.getMainMetric() != null) {
            summary += String.format("，主要指标 %s: %s", overview.getMainMetric(), overview.getMainMetricValue());
        }
        overview.setSummary(summary);
        
        return overview;
    }
    
    /**
     * 生成深度洞察
     */
    private List<EnhancedAnalysisReport.Insight> generateInsights(DataQueryResponse response, String naturalQuery) {
        List<EnhancedAnalysisReport.Insight> insights = new ArrayList<>();
        
        if (response.getData() == null || response.getData().isEmpty()) {
            return insights;
        }
        
        // 数据分布洞察
        Map<String, Object> firstRow = response.getData().get(0);
        for (Map.Entry<String, Object> entry : firstRow.entrySet()) {
            String columnName = entry.getKey();
            
            // 分析数值型字段
            if (isNumericColumn(response.getData(), columnName)) {
                List<Double> values = extractNumericValues(response.getData(), columnName);
                if (!values.isEmpty()) {
                    double avg = values.stream().mapToDouble(Double::doubleValue).average().orElse(0);
                    double max = values.stream().mapToDouble(Double::doubleValue).max().orElse(0);
                    double min = values.stream().mapToDouble(Double::doubleValue).min().orElse(0);
                    
                    EnhancedAnalysisReport.Insight insight = new EnhancedAnalysisReport.Insight();
                    insight.setTitle(columnName + " 分布特征");
                    insight.setContent(String.format("平均值: %.2f, 最大值: %.2f, 最小值: %.2f, 极差: %.2f", 
                        avg, max, min, max - min));
                    insight.setType("info");
                    insight.setImpact(0.6);
                    insights.add(insight);
                    
                    // 检查是否有明显偏差
                    if (max > avg * 2) {
                        EnhancedAnalysisReport.Insight outlierInsight = new EnhancedAnalysisReport.Insight();
                        outlierInsight.setTitle("发现数据偏差");
                        outlierInsight.setContent(String.format("%s 存在较大值（%.2f），是平均值的 %.1f 倍", 
                            columnName, max, max / avg));
                        outlierInsight.setType("warning");
                        outlierInsight.setImpact(0.8);
                        insights.add(outlierInsight);
                    }
                }
            }
            
            // 分析分类型字段
            if (response.getData().size() > 1) {
                Set<Object> uniqueValues = response.getData().stream()
                    .map(row -> row.get(columnName))
                    .filter(Objects::nonNull)
                    .collect(Collectors.toSet());
                
                if (uniqueValues.size() > 1 && uniqueValues.size() <= 10) {
                    EnhancedAnalysisReport.Insight insight = new EnhancedAnalysisReport.Insight();
                    insight.setTitle(columnName + " 分类分析");
                    insight.setContent(String.format("包含 %d 个不同类别: %s", 
                        uniqueValues.size(), 
                        uniqueValues.stream().limit(5).map(String::valueOf).collect(Collectors.joining(", "))));
                    insight.setType("info");
                    insight.setImpact(0.5);
                    insights.add(insight);
                }
            }
        }
        
        return insights.stream().limit(5).collect(Collectors.toList());
    }
    
    /**
     * 趋势分析
     */
    private EnhancedAnalysisReport.TrendAnalysis analyzeTrend(DataQueryResponse response) {
        if (response.getData() == null || response.getData().size() < 2) {
            return null;
        }
        
        EnhancedAnalysisReport.TrendAnalysis trend = new EnhancedAnalysisReport.TrendAnalysis();
        
        // 尝试找到数值型字段进行趋势分析
        Map<String, Object> firstRow = response.getData().get(0);
        for (String columnName : firstRow.keySet()) {
            if (isNumericColumn(response.getData(), columnName)) {
                List<Double> values = extractNumericValues(response.getData(), columnName);
                if (values.size() >= 2) {
                    double first = values.get(0);
                    double last = values.get(values.size() - 1);
                    double change = ((last - first) / first) * 100;
                    
                    trend.setChange(change);
                    trend.setDirection(change > 5 ? "up" : change < -5 ? "down" : "stable");
                    trend.setPeriod(String.format("%d 条数据", values.size()));
                    
                    String emoji = change > 0 ? "📈" : change < 0 ? "📉" : "➡️";
                    trend.setDescription(String.format("%s %s 从 %.2f 到 %.2f，变化 %.1f%%", 
                        emoji, columnName, first, last, change));
                    
                    return trend;
                }
            }
        }
        
        return null;
    }
    
    /**
     * 高级异常检测
     */
    private List<EnhancedAnalysisReport.Anomaly> detectAnomaliesAdvanced(DataQueryResponse response) {
        List<EnhancedAnalysisReport.Anomaly> anomalies = new ArrayList<>();
        
        if (response.getData() == null || response.getData().isEmpty()) {
            return anomalies;
        }
        
        // 检测空值异常
        Map<String, Object> firstRow = response.getData().get(0);
        for (String columnName : firstRow.keySet()) {
            long nullCount = response.getData().stream()
                .filter(row -> row.get(columnName) == null)
                .count();
            
            double nullRate = (double) nullCount / response.getData().size();
            if (nullRate > 0.1) {
                EnhancedAnalysisReport.Anomaly anomaly = new EnhancedAnalysisReport.Anomaly();
                anomaly.setTitle(columnName + " 存在大量空值");
                anomaly.setDescription(String.format("空值比例: %.1f%%（%d/%d）", 
                    nullRate * 100, nullCount, response.getData().size()));
                anomaly.setSeverity(nullRate > 0.3 ? "high" : nullRate > 0.2 ? "medium" : "low");
                anomaly.setPossibleCause("数据采集不完整或字段为可选项");
                anomalies.add(anomaly);
            }
        }
        
        // 检测数值异常
        for (String columnName : firstRow.keySet()) {
            if (isNumericColumn(response.getData(), columnName)) {
                List<Double> values = extractNumericValues(response.getData(), columnName);
                if (!values.isEmpty()) {
                    double avg = values.stream().mapToDouble(Double::doubleValue).average().orElse(0);
                    double stdDev = calculateStdDev(values, avg);
                    
                    long outliers = values.stream()
                        .filter(v -> Math.abs(v - avg) > 3 * stdDev)
                        .count();
                    
                    if (outliers > 0) {
                        EnhancedAnalysisReport.Anomaly anomaly = new EnhancedAnalysisReport.Anomaly();
                        anomaly.setTitle(columnName + " 存在异常值");
                        anomaly.setDescription(String.format("发现 %d 个离群点（偏离平均值超过3倍标准差）", outliers));
                        anomaly.setSeverity("medium");
                        anomaly.setPossibleCause("数据录入错误或存在特殊情况");
                        anomalies.add(anomaly);
                    }
                }
            }
        }
        
        return anomalies.stream().limit(3).collect(Collectors.toList());
    }
    
    /**
     * 生成预测
     */
    private EnhancedAnalysisReport.Prediction generatePrediction(DataQueryResponse response) {
        if (response.getData() == null || response.getData().size() < 3) {
            return null;
        }
        
        // 简单的线性预测
        Map<String, Object> firstRow = response.getData().get(0);
        for (String columnName : firstRow.keySet()) {
            if (isNumericColumn(response.getData(), columnName)) {
                List<Double> values = extractNumericValues(response.getData(), columnName);
                if (values.size() >= 3) {
                    // 计算简单趋势
                    double avg = values.stream().mapToDouble(Double::doubleValue).average().orElse(0);
                    double recentAvg = values.subList(Math.max(0, values.size() - 3), values.size())
                        .stream().mapToDouble(Double::doubleValue).average().orElse(0);
                    
                    double trend = recentAvg - avg;
                    double predictedValue = values.get(values.size() - 1) + trend;
                    
                    EnhancedAnalysisReport.Prediction prediction = new EnhancedAnalysisReport.Prediction();
                    prediction.setMetric(columnName);
                    prediction.setPredictedValue(predictedValue);
                    prediction.setTimeframe("下一期");
                    prediction.setConfidence(0.7);
                    prediction.setDescription(String.format("基于当前趋势，%s 预计为 %.2f", columnName, predictedValue));
                    
                    return prediction;
                }
            }
        }
        
        return null;
    }
    
    @Override
    public String detectAnomalies(List<Map<String, Object>> data, String columnName) {
        if (data == null || data.isEmpty()) {
            return "数据不足，无法检测异常";
        }
        
        if (isNumericColumn(data, columnName)) {
            List<Double> values = extractNumericValues(data, columnName);
            double avg = values.stream().mapToDouble(Double::doubleValue).average().orElse(0);
            double stdDev = calculateStdDev(values, avg);
            
            long outliers = values.stream()
                .filter(v -> Math.abs(v - avg) > 2 * stdDev)
                .count();
            
            return String.format("发现 %d 个异常值（偏离平均值超过2倍标准差）", outliers);
        }
        
        return "该字段不适合异常检测";
    }
    
    @Override
    public String predictTrend(List<Map<String, Object>> data, String columnName, int periods) {
        if (data == null || data.size() < 2) {
            return "数据不足，无法预测";
        }
        
        if (isNumericColumn(data, columnName)) {
            List<Double> values = extractNumericValues(data, columnName);
            double avgChange = 0;
            
            for (int i = 1; i < values.size(); i++) {
                avgChange += (values.get(i) - values.get(i - 1));
            }
            avgChange /= (values.size() - 1);
            
            double lastValue = values.get(values.size() - 1);
            double predicted = lastValue + avgChange * periods;
            
            return String.format("预测未来 %d 期的值为: %.2f", periods, predicted);
        }
        
        return "该字段不适合预测";
    }
    
    @Override
    public List<String> generateRecommendations(DataQueryResponse queryResponse, String naturalQuery) {
        List<String> recommendations = new ArrayList<>();
        
        if (queryResponse.getData() == null || queryResponse.getData().isEmpty()) {
            recommendations.add("💡 数据为空，建议检查查询条件或数据源");
            return recommendations;
        }
        
        // 基于数据量给建议
        if (queryResponse.getTotalCount() > 1000) {
            recommendations.add("📊 数据量较大，建议添加筛选条件以获得更精确的分析");
        }
        
        // 基于查询类型给建议
        if (naturalQuery.contains("统计") || naturalQuery.contains("分析")) {
            recommendations.add("📈 建议添加时间维度，查看趋势变化");
            recommendations.add("🔍 可以按不同维度分组，发现更多洞察");
        }
        
        // 基于异常给建议
        List<EnhancedAnalysisReport.Anomaly> anomalies = detectAnomaliesAdvanced(queryResponse);
        if (!anomalies.isEmpty()) {
            for (EnhancedAnalysisReport.Anomaly anomaly : anomalies) {
                if ("high".equals(anomaly.getSeverity())) {
                    recommendations.add("⚠️ " + anomaly.getTitle() + "，建议深入调查原因");
                }
            }
        }
        
        // 基于趋势给建议
        EnhancedAnalysisReport.TrendAnalysis trend = analyzeTrend(queryResponse);
        if (trend != null && "down".equals(trend.getDirection())) {
            recommendations.add("📉 数据呈下降趋势，建议分析下降原因并制定改进措施");
        } else if (trend != null && "up".equals(trend.getDirection())) {
            recommendations.add("📈 数据呈上升趋势，建议总结成功经验并推广");
        }
        
        // 通用建议
        recommendations.add("💾 重要分析结果建议导出保存");
        recommendations.add("🔄 定期监控关键指标，及时发现问题");
        
        return recommendations.stream().limit(5).collect(Collectors.toList());
    }
    
    // ==================== 辅助方法 ====================
    
    private boolean isNumericColumn(List<Map<String, Object>> data, String columnName) {
        return data.stream()
            .map(row -> row.get(columnName))
            .filter(Objects::nonNull)
            .limit(5)
            .allMatch(v -> v instanceof Number || isNumericString(v));
    }
    
    private boolean isNumericString(Object value) {
        try {
            Double.parseDouble(String.valueOf(value));
            return true;
        } catch (NumberFormatException e) {
            return false;
        }
    }
    
    private List<Double> extractNumericValues(List<Map<String, Object>> data, String columnName) {
        return data.stream()
            .map(row -> row.get(columnName))
            .filter(Objects::nonNull)
            .map(v -> {
                if (v instanceof Number) {
                    return ((Number) v).doubleValue();
                }
                try {
                    return Double.parseDouble(String.valueOf(v));
                } catch (NumberFormatException e) {
                    return null;
                }
            })
            .filter(Objects::nonNull)
            .collect(Collectors.toList());
    }
    
    private double calculateStdDev(List<Double> values, double avg) {
        double variance = values.stream()
            .mapToDouble(v -> Math.pow(v - avg, 2))
            .average()
            .orElse(0);
        return Math.sqrt(variance);
    }
}
