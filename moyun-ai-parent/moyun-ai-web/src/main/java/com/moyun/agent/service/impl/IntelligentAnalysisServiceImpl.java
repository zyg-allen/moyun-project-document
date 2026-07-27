package com.moyun.agent.service.impl;

import com.moyun.agent.service.DataQualityCheckService;
import com.moyun.agent.service.IntelligentAnalysisService;
import com.moyun.agent.service.LLMService;
import com.moyun.agent.vo.DataQueryResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

/**
 * 智能分析服务实现
 *
 * @author laomao
 */
@Slf4j
@Service
public class IntelligentAnalysisServiceImpl implements IntelligentAnalysisService {

    @Autowired
    private LLMService llmService;

    @Autowired
    private DataQualityCheckService dataQualityCheckService;

    @Override
    public Map<String, Object> autoAnalyze(List<Map<String, Object>> data,
                                           List<DataQueryResponse.ColumnInfo> columns) {
        Map<String, Object> statistics = new HashMap<>();

        if (data == null || data.isEmpty()) {
            return statistics;
        }

        statistics.put("totalRows", data.size());

        // 分析数值型字段
        for (DataQueryResponse.ColumnInfo column : columns) {
            if (isNumericType(column.getDataType())) {
                Map<String, Object> numStats = analyzeNumericColumn(data, column.getColumnName());
                statistics.put(column.getColumnName(), numStats);
            }
        }

        // 添加数据质量检查结果
        try {
            DataQualityCheckService.DataQualityReport qualityReport = 
                dataQualityCheckService.checkDataQuality(data, columns);
            statistics.put("dataQuality", Map.of(
                "overallScore", qualityReport.getOverallQualityScore(),
                "issueCount", qualityReport.getIssues().size()
            ));
        } catch (Exception e) {
            log.warn("数据质量检查失败", e);
        }

        return statistics;
    }

    @Override
    public List<DataQueryResponse.ChartRecommendation> recommendCharts(
            List<Map<String, Object>> data,
            List<DataQueryResponse.ColumnInfo> columns) {

        List<DataQueryResponse.ChartRecommendation> recommendations = new ArrayList<>();

        if (data == null || data.isEmpty() || columns == null || columns.isEmpty()) {
            return recommendations;
        }

        // 检测数据特征
        boolean hasTimeField = hasTimeField(columns);
        boolean hasNumericField = hasNumericField(columns);
        boolean hasCategoryField = hasCategoryField(columns);
        int dataSize = data.size();

        // 规则1: 时间序列数据 → 折线图
        if (hasTimeField && hasNumericField) {
            recommendations.add(DataQueryResponse.ChartRecommendation.builder()
                .chartType("line")
                .priority(95)
                .reason("检测到时间字段和数值字段,最适合用折线图展示趋势")
                .config(generateLineChartConfig(data, columns))
                .build());
        }

        // 规则2: 分类数据 → 饼图或柱状图
        if (hasCategoryField && hasNumericField) {
            if (dataSize <= 6) {
                recommendations.add(DataQueryResponse.ChartRecommendation.builder()
                    .chartType("pie")
                    .priority(85)
                    .reason("少量分类数据,适合用饼图展示占比")
                    .config(generatePieChartConfig(data, columns))
                    .build());
            } else {
                recommendations.add(DataQueryResponse.ChartRecommendation.builder()
                    .chartType("bar")
                    .priority(90)
                    .reason("多分类数据,适合用柱状图对比")
                    .config(generateBarChartConfig(data, columns))
                    .build());
            }
        }

        // 规则3: 纯数值数据 → 表格
        if (hasNumericField && !hasTimeField && !hasCategoryField) {
            recommendations.add(DataQueryResponse.ChartRecommendation.builder()
                .chartType("table")
                .priority(70)
                .reason("数值数据,表格展示更清晰")
                .config(new HashMap<>())
                .build());
        }

        // 按优先级排序
        recommendations.sort(Comparator.comparing(
            DataQueryResponse.ChartRecommendation::getPriority).reversed());

        return recommendations;
    }

    @Override
    public List<DataQueryResponse.DataInsight> generateInsights(
            List<Map<String, Object>> data,
            Map<String, Object> statistics) {

        List<DataQueryResponse.DataInsight> insights = new ArrayList<>();

        if (data == null || data.isEmpty()) {
            return insights;
        }

        // 洞察1: 数据量级
        insights.add(DataQueryResponse.DataInsight.builder()
            .type("overview")
            .severity("medium")
            .title("数据概览")
            .description(String.format("共查询到 %d 条数据", data.size()))
            .recommendation("数据量适中,可以进行详细分析")
            .build());

        // 洞察2: 数值异常检测(简单实现)
        for (Map.Entry<String, Object> entry : statistics.entrySet()) {
            if (entry.getValue() instanceof Map) {
                @SuppressWarnings("unchecked")
                Map<String, Object> stats = (Map<String, Object>) entry.getValue();

                if (stats.containsKey("max") && stats.containsKey("avg")) {
                    double max = ((Number) stats.get("max")).doubleValue();
                    double avg = ((Number) stats.get("avg")).doubleValue();

                    if (max > avg * 3) {
                        insights.add(DataQueryResponse.DataInsight.builder()
                            .type("anomaly")
                            .severity("high")
                            .title("检测到异常值")
                            .description(String.format(
                                "字段 %s 的最大值(%.2f)明显高于平均值(%.2f)",
                                entry.getKey(), max, avg))
                            .recommendation("建议检查数据来源或业务异常")
                            .build());
                    }
                }
            }
        }

        // 洞察3: 数据质量洞察
        if (statistics.containsKey("dataQuality")) {
            @SuppressWarnings("unchecked")
            Map<String, Object> quality = (Map<String, Object>) statistics.get("dataQuality");
            Double score = (Double) quality.get("overallScore");
            Integer issueCount = (Integer) quality.get("issueCount");

            if (score < 80) {
                insights.add(DataQueryResponse.DataInsight.builder()
                    .type("quality")
                    .severity("medium")
                    .title("数据质量需要关注")
                    .description(String.format(
                        "数据质量评分%.1f分，发现%d个质量问题",
                        score, issueCount))
                    .recommendation("建议进行数据清洗和质量提升")
                    .build());
            }
        }

        return insights;
    }

    @Override
    public String generateAnalysisText(List<Map<String, Object>> data,
                                       Map<String, Object> statistics,
                                       String query) {
        try {
            // 构建分析上下文
            String context = buildAnalysisContext(data, statistics, query);

            // AI生成分析
            String prompt = String.format("""
                你是一个专业的数据分析师。根据以下查询结果,给出简洁的数据分析。
                
                # 查询问题:
                %s
                
                # 数据概况:
                %s
                
                # 要求:
                1. 用1-2段话总结关键发现
                2. 突出重要的数据特征
                3. 语言专业但易懂
                4. 不要重复数据,给出分析和洞察
                
                # 分析:
                """,
                query,
                context
            );

            // 调用LLM生成分析文本
            return llmService.generate(prompt);

        } catch (Exception e) {
            log.error("生成分析文本失败", e);
            return "数据查询成功,共 " + data.size() + " 条结果。";
        }
    }

    /**
     * 分析数值型列
     */
    private Map<String, Object> analyzeNumericColumn(List<Map<String, Object>> data, String columnName) {
        Map<String, Object> stats = new HashMap<>();

        List<Double> values = data.stream()
            .map(row -> row.get(columnName))
            .filter(Objects::nonNull)
            .map(v -> Double.parseDouble(v.toString()))
            .collect(Collectors.toList());

        if (!values.isEmpty()) {
            stats.put("count", values.size());
            stats.put("sum", values.stream().mapToDouble(Double::doubleValue).sum());
            stats.put("avg", values.stream().mapToDouble(Double::doubleValue).average().orElse(0));
            stats.put("max", values.stream().mapToDouble(Double::doubleValue).max().orElse(0));
            stats.put("min", values.stream().mapToDouble(Double::doubleValue).min().orElse(0));
        }

        return stats;
    }

    /**
     * 构建分析上下文
     */
    private String buildAnalysisContext(List<Map<String, Object>> data,
                                        Map<String, Object> statistics,
                                        String query) {
        StringBuilder context = new StringBuilder();

        context.append(String.format("总行数: %d\n", data.size()));

        // 统计信息
        if (statistics != null && !statistics.isEmpty()) {
            context.append("\n统计指标:\n");
            for (Map.Entry<String, Object> entry : statistics.entrySet()) {
                if (entry.getValue() instanceof Map) {
                    @SuppressWarnings("unchecked")
                    Map<String, Object> stats = (Map<String, Object>) entry.getValue();
                    context.append(String.format("- %s: %s\n", entry.getKey(), stats));
                }
            }
        }

        // 前3行数据样本
        if (data.size() > 0) {
            context.append("\n数据样本(前3行):\n");
            int limit = Math.min(3, data.size());
            for (int i = 0; i < limit; i++) {
                context.append(data.get(i)).append("\n");
            }
        }

        return context.toString();
    }

    /**
     * 生成折线图配置
     */
    private Map<String, Object> generateLineChartConfig(List<Map<String, Object>> data,
                                                         List<DataQueryResponse.ColumnInfo> columns) {
        Map<String, Object> config = new HashMap<>();

        // 找时间字段和数值字段
        String timeField = findTimeField(columns);
        String valueField = findNumericField(columns);

        if (timeField != null && valueField != null) {
            List<String> xData = data.stream()
                .map(row -> String.valueOf(row.get(timeField)))
                .collect(Collectors.toList());

            List<Object> yData = data.stream()
                .map(row -> row.get(valueField))
                .collect(Collectors.toList());

            config.put("xData", xData);
            config.put("yData", yData);
            config.put("xField", timeField);
            config.put("yField", valueField);
        }

        return config;
    }

    /**
     * 生成饼图配置
     */
    private Map<String, Object> generatePieChartConfig(List<Map<String, Object>> data,
                                                        List<DataQueryResponse.ColumnInfo> columns) {
        Map<String, Object> config = new HashMap<>();

        String categoryField = findCategoryField(columns);
        String valueField = findNumericField(columns);

        if (categoryField != null && valueField != null) {
            List<Map<String, Object>> pieData = data.stream()
                .map(row -> Map.of(
                    "name", row.get(categoryField),
                    "value", row.get(valueField)
                ))
                .collect(Collectors.toList());

            config.put("data", pieData);
            config.put("nameField", categoryField);
            config.put("valueField", valueField);
        }

        return config;
    }

    /**
     * 生成柱状图配置
     */
    private Map<String, Object> generateBarChartConfig(List<Map<String, Object>> data,
                                                        List<DataQueryResponse.ColumnInfo> columns) {
        // 与饼图类似,但展示方式不同
        return generatePieChartConfig(data, columns);
    }

    private boolean isNumericType(String dataType) {
        if (dataType == null) return false;
        String upper = dataType.toUpperCase();
        return upper.contains("INT") || upper.contains("DECIMAL") ||
               upper.contains("FLOAT") || upper.contains("DOUBLE") ||
               upper.contains("NUMBER");
    }

    private boolean hasTimeField(List<DataQueryResponse.ColumnInfo> columns) {
        return columns.stream().anyMatch(c -> 
            c.getDataType().toUpperCase().contains("DATE") ||
            c.getDataType().toUpperCase().contains("TIME"));
    }

    private boolean hasNumericField(List<DataQueryResponse.ColumnInfo> columns) {
        return columns.stream().anyMatch(c -> isNumericType(c.getDataType()));
    }

    private boolean hasCategoryField(List<DataQueryResponse.ColumnInfo> columns) {
        return columns.stream().anyMatch(c -> 
            c.getDataType().toUpperCase().contains("CHAR") ||
            c.getDataType().toUpperCase().contains("TEXT") ||
            c.getDataType().toUpperCase().contains("VARCHAR"));
    }

    private String findTimeField(List<DataQueryResponse.ColumnInfo> columns) {
        return columns.stream()
            .filter(c -> c.getDataType().toUpperCase().contains("DATE") ||
                        c.getDataType().toUpperCase().contains("TIME"))
            .findFirst()
            .map(DataQueryResponse.ColumnInfo::getColumnName)
            .orElse(null);
    }

    private String findNumericField(List<DataQueryResponse.ColumnInfo> columns) {
        return columns.stream()
            .filter(c -> isNumericType(c.getDataType()))
            .findFirst()
            .map(DataQueryResponse.ColumnInfo::getColumnName)
            .orElse(null);
    }

    private String findCategoryField(List<DataQueryResponse.ColumnInfo> columns) {
        return columns.stream()
            .filter(c -> c.getDataType().toUpperCase().contains("CHAR") ||
                        c.getDataType().toUpperCase().contains("TEXT"))
            .findFirst()
            .map(DataQueryResponse.ColumnInfo::getColumnName)
            .orElse(null);
    }
}
