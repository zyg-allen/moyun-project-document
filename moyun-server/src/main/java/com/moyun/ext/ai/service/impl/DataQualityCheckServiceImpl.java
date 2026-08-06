package com.moyun.ext.ai.service.impl;

import com.moyun.ext.ai.service.DataQualityCheckService;
import com.moyun.ext.ai.vo.DataQueryResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

/**
 * 数据质量检查服务实现
 *
 * @author laomao
 */
@Slf4j
@Service
public class DataQualityCheckServiceImpl implements DataQualityCheckService {

    @Override
    public DataQualityReport checkDataQuality(List<Map<String, Object>> data,
                                             List<DataQueryResponse.ColumnInfo> columns) {
        DataQualityReport report = new DataQualityReport();
        report.setTotalRows(data.size());
        report.setTotalColumns(columns.size());

        List<ColumnQuality> columnQualities = new ArrayList<>();
        List<String> issues = new ArrayList<>();
        List<String> recommendations = new ArrayList<>();

        double totalScore = 0.0;

        for (DataQueryResponse.ColumnInfo column : columns) {
            ColumnQuality quality = new ColumnQuality();
            quality.setColumnName(column.getColumnName());
            quality.setDataType(column.getDataType());

            List<String> columnIssues = new ArrayList<>();

            // 检查空值
            NullValueCheck nullCheck = checkNullValues(data, column.getColumnName());
            quality.setNullCount(nullCheck.getNullCount());
            quality.setNullPercentage(nullCheck.getNullPercentage());

            if (nullCheck.getNullPercentage() > 10) {
                String issue = String.format("列'%s'空值比例%.1f%%,超过阈值10%%",
                        column.getColumnName(), nullCheck.getNullPercentage());
                columnIssues.add(issue);
                issues.add(issue);
            }

            // 检查重复值
            DuplicateCheck dupCheck = checkDuplicates(data, column.getColumnName());
            quality.setDuplicateCount(dupCheck.getDuplicateCount());

            // 检查异常值（仅数值型）
            if (isNumericColumn(column.getDataType())) {
                OutlierCheck outlierCheck = checkOutliers(data, column.getColumnName());
                quality.setOutlierCount(outlierCheck.getOutlierCount());

                if (outlierCheck.getOutlierCount() > 0) {
                    String issue = String.format("列'%s'发现%d个异常值",
                            column.getColumnName(), outlierCheck.getOutlierCount());
                    columnIssues.add(issue);
                    issues.add(issue);
                }
            }

            quality.setIssues(columnIssues);

            // 计算列质量分数
            double columnScore = calculateColumnScore(quality, data.size());
            quality.setQualityScore(columnScore);
            totalScore += columnScore;

            columnQualities.add(quality);
        }

        report.setColumnQualities(columnQualities);
        report.setIssues(issues);

        // 计算整体质量分数
        double overallScore = columns.isEmpty() ? 100.0 : totalScore / columns.size();
        report.setOverallQualityScore(overallScore);

        // 生成建议
        if (overallScore < 80) {
            recommendations.add("数据质量较低，建议进行数据清洗");
        }
        if (!issues.isEmpty()) {
            recommendations.add("发现" + issues.size() + "个数据质量问题，建议逐项处理");
        }
        if (recommendations.isEmpty()) {
            recommendations.add("数据质量良好，可以直接用于分析");
        }
        report.setRecommendations(recommendations);

        return report;
    }

    @Override
    public NullValueCheck checkNullValues(List<Map<String, Object>> data, String columnName) {
        NullValueCheck check = new NullValueCheck();
        check.setColumnName(columnName);

        int nullCount = 0;
        List<Integer> nullIndexes = new ArrayList<>();

        for (int i = 0; i < data.size(); i++) {
            Object value = data.get(i).get(columnName);
            if (value == null || value.toString().trim().isEmpty()) {
                nullCount++;
                if (nullIndexes.size() < 10) {  // 最多记录10个
                    nullIndexes.add(i);
                }
            }
        }

        check.setNullCount(nullCount);
        check.setNullPercentage(data.isEmpty() ? 0.0 : (nullCount * 100.0 / data.size()));
        check.setNullRowIndexes(nullIndexes);

        return check;
    }

    @Override
    public DuplicateCheck checkDuplicates(List<Map<String, Object>> data, String columnName) {
        DuplicateCheck check = new DuplicateCheck();
        check.setColumnName(columnName);

        Map<Object, Integer> valueCounts = new HashMap<>();

        for (Map<String, Object> row : data) {
            Object value = row.get(columnName);
            if (value != null) {
                valueCounts.put(value, valueCounts.getOrDefault(value, 0) + 1);
            }
        }

        int duplicates = valueCounts.values().stream()
                .filter(count -> count > 1)
                .mapToInt(count -> count - 1)
                .sum();

        check.setDuplicateCount(duplicates);
        check.setUniqueCount(valueCounts.size());
        check.setDuplicatePercentage(data.isEmpty() ? 0.0 : (duplicates * 100.0 / data.size()));
        check.setValueCounts(valueCounts);

        return check;
    }

    @Override
    public OutlierCheck checkOutliers(List<Map<String, Object>> data, String columnName) {
        OutlierCheck check = new OutlierCheck();
        check.setColumnName(columnName);
        check.setDetectionMethod("ZScore");

        List<Double> values = data.stream()
                .map(row -> row.get(columnName))
                .filter(Objects::nonNull)
                .filter(v -> v instanceof Number)
                .map(v -> ((Number) v).doubleValue())
                .collect(Collectors.toList());

        if (values.size() < 3) {
            check.setOutlierCount(0);
            check.setOutlierPercentage(0.0);
            check.setOutliers(new ArrayList<>());
            return check;
        }

        // 计算均值和标准差
        double mean = values.stream().mapToDouble(Double::doubleValue).average().orElse(0);
        double variance = values.stream()
                .mapToDouble(v -> Math.pow(v - mean, 2))
                .average().orElse(0);
        double stdDev = Math.sqrt(variance);

        List<OutlierInfo> outliers = new ArrayList<>();

        for (int i = 0; i < data.size(); i++) {
            Object value = data.get(i).get(columnName);
            if (value instanceof Number) {
                double numValue = ((Number) value).doubleValue();
                double zScore = stdDev == 0 ? 0 : (numValue - mean) / stdDev;

                if (Math.abs(zScore) > 3) {  // 3倍标准差
                    OutlierInfo outlier = new OutlierInfo();
                    outlier.setRowIndex(i);
                    outlier.setValue(value);
                    outlier.setZScore(zScore);
                    outlier.setReason("偏离均值超过3倍标准差");
                    outliers.add(outlier);
                }
            }
        }

        check.setOutlierCount(outliers.size());
        check.setOutlierPercentage(data.isEmpty() ? 0.0 : (outliers.size() * 100.0 / data.size()));
        check.setOutliers(outliers);

        return check;
    }

    /**
     * 计算列质量分数
     */
    private double calculateColumnScore(ColumnQuality quality, int totalRows) {
        double score = 100.0;

        // 空值扣分
        if (quality.getNullPercentage() != null) {
            score -= quality.getNullPercentage() * 0.5;  // 空值每1%扣0.5分
        }

        // 异常值扣分
        if (quality.getOutlierCount() != null && totalRows > 0) {
            double outlierPct = quality.getOutlierCount() * 100.0 / totalRows;
            score -= outlierPct * 0.3;  // 异常值每1%扣0.3分
        }

        return Math.max(0, Math.min(100, score));
    }

    /**
     * 判断是否为数值型列
     */
    private boolean isNumericColumn(String dataType) {
        if (dataType == null) return false;
        String upper = dataType.toUpperCase();
        return upper.contains("INT") || upper.contains("DECIMAL") ||
               upper.contains("FLOAT") || upper.contains("DOUBLE") ||
               upper.contains("NUMBER");
    }
}
