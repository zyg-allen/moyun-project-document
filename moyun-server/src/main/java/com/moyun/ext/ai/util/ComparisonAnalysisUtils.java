package com.moyun.ext.ai.util;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.*;

/**
 * 同比环比分析工具类
 *
 * @author laomao
 */
public class ComparisonAnalysisUtils {

    /**
     * 计算同比增长率
     * Year-over-Year (YoY) Growth Rate
     *
     * @param currentValue 当前值
     * @param lastYearValue 去年同期值
     * @return 同比增长率（百分比）
     */
    public static Double calculateYoY(Double currentValue, Double lastYearValue) {
        if (lastYearValue == null || lastYearValue == 0) {
            return null;
        }
        return ((currentValue - lastYearValue) / lastYearValue) * 100;
    }

    /**
     * 计算环比增长率
     * Month-over-Month (MoM) Growth Rate
     *
     * @param currentValue 当前值
     * @param lastPeriodValue 上期值
     * @return 环比增长率（百分比）
     */
    public static Double calculateMoM(Double currentValue, Double lastPeriodValue) {
        if (lastPeriodValue == null || lastPeriodValue == 0) {
            return null;
        }
        return ((currentValue - lastPeriodValue) / lastPeriodValue) * 100;
    }

    /**
     * 生成同比对比报告
     *
     * @param data 时间序列数据（按时间排序）
     * @param timeField 时间字段名
     * @param valueField 数值字段名
     * @return 同比对比结果
     */
    public static ComparisonResult generateYoYComparison(List<Map<String, Object>> data,
                                                         String timeField,
                                                         String valueField) {
        ComparisonResult result = new ComparisonResult();
        result.setComparisonType("YoY");
        result.setTimeField(timeField);
        result.setValueField(valueField);

        List<ComparisonItem> items = new ArrayList<>();

        // 按时间分组
        Map<String, List<Map<String, Object>>> yearlyData = groupByYear(data, timeField);

        for (String year : yearlyData.keySet()) {
            int currentYear = Integer.parseInt(year);
            String lastYear = String.valueOf(currentYear - 1);

            if (yearlyData.containsKey(lastYear)) {
                double currentSum = sumValues(yearlyData.get(year), valueField);
                double lastYearSum = sumValues(yearlyData.get(lastYear), valueField);
                Double yoyRate = calculateYoY(currentSum, lastYearSum);

                ComparisonItem item = new ComparisonItem();
                item.setPeriod(year);
                item.setCurrentValue(currentSum);
                item.setCompareValue(lastYearSum);
                item.setGrowthRate(yoyRate);
                item.setGrowthAmount(currentSum - lastYearSum);

                items.add(item);
            }
        }

        result.setItems(items);
        result.setSummary(generateSummary(items, "同比"));

        return result;
    }

    /**
     * 生成环比对比报告
     *
     * @param data 时间序列数据（按时间排序）
     * @param timeField 时间字段名
     * @param valueField 数值字段名
     * @return 环比对比结果
     */
    public static ComparisonResult generateMoMComparison(List<Map<String, Object>> data,
                                                         String timeField,
                                                         String valueField) {
        ComparisonResult result = new ComparisonResult();
        result.setComparisonType("MoM");
        result.setTimeField(timeField);
        result.setValueField(valueField);

        List<ComparisonItem> items = new ArrayList<>();

        // 按月分组
        Map<String, List<Map<String, Object>>> monthlyData = groupByMonth(data, timeField);
        List<String> sortedMonths = new ArrayList<>(monthlyData.keySet());
        Collections.sort(sortedMonths);

        for (int i = 1; i < sortedMonths.size(); i++) {
            String currentMonth = sortedMonths.get(i);
            String lastMonth = sortedMonths.get(i - 1);

            double currentSum = sumValues(monthlyData.get(currentMonth), valueField);
            double lastMonthSum = sumValues(monthlyData.get(lastMonth), valueField);
            Double momRate = calculateMoM(currentSum, lastMonthSum);

            ComparisonItem item = new ComparisonItem();
            item.setPeriod(currentMonth);
            item.setCurrentValue(currentSum);
            item.setCompareValue(lastMonthSum);
            item.setGrowthRate(momRate);
            item.setGrowthAmount(currentSum - lastMonthSum);

            items.add(item);
        }

        result.setItems(items);
        result.setSummary(generateSummary(items, "环比"));

        return result;
    }

    /**
     * 按年分组数据
     */
    private static Map<String, List<Map<String, Object>>> groupByYear(List<Map<String, Object>> data,
                                                                       String timeField) {
        Map<String, List<Map<String, Object>>> grouped = new HashMap<>();

        for (Map<String, Object> row : data) {
            Object timeValue = row.get(timeField);
            if (timeValue != null) {
                String year = extractYear(timeValue);
                grouped.computeIfAbsent(year, k -> new ArrayList<>()).add(row);
            }
        }

        return grouped;
    }

    /**
     * 按月分组数据
     */
    private static Map<String, List<Map<String, Object>>> groupByMonth(List<Map<String, Object>> data,
                                                                        String timeField) {
        Map<String, List<Map<String, Object>>> grouped = new HashMap<>();

        for (Map<String, Object> row : data) {
            Object timeValue = row.get(timeField);
            if (timeValue != null) {
                String yearMonth = extractYearMonth(timeValue);
                grouped.computeIfAbsent(yearMonth, k -> new ArrayList<>()).add(row);
            }
        }

        return grouped;
    }

    /**
     * 提取年份
     */
    private static String extractYear(Object timeValue) {
        if (timeValue instanceof LocalDate) {
            return String.valueOf(((LocalDate) timeValue).getYear());
        } else if (timeValue instanceof LocalDateTime) {
            return String.valueOf(((LocalDateTime) timeValue).getYear());
        } else if (timeValue instanceof java.sql.Date) {
            return String.valueOf(((java.sql.Date) timeValue).toLocalDate().getYear());
        } else {
            String str = timeValue.toString();
            return str.substring(0, Math.min(4, str.length()));
        }
    }

    /**
     * 提取年月
     */
    private static String extractYearMonth(Object timeValue) {
        if (timeValue instanceof LocalDate) {
            LocalDate date = (LocalDate) timeValue;
            return String.format("%d-%02d", date.getYear(), date.getMonthValue());
        } else if (timeValue instanceof LocalDateTime) {
            LocalDateTime dateTime = (LocalDateTime) timeValue;
            return String.format("%d-%02d", dateTime.getYear(), dateTime.getMonthValue());
        } else if (timeValue instanceof java.sql.Date) {
            LocalDate date = ((java.sql.Date) timeValue).toLocalDate();
            return String.format("%d-%02d", date.getYear(), date.getMonthValue());
        } else {
            String str = timeValue.toString();
            return str.substring(0, Math.min(7, str.length()));
        }
    }

    /**
     * 求和
     */
    private static double sumValues(List<Map<String, Object>> data, String valueField) {
        return data.stream()
                .map(row -> row.get(valueField))
                .filter(Objects::nonNull)
                .filter(v -> v instanceof Number)
                .mapToDouble(v -> ((Number) v).doubleValue())
                .sum();
    }

    /**
     * 生成摘要
     */
    private static String generateSummary(List<ComparisonItem> items, String type) {
        if (items.isEmpty()) {
            return type + "对比: 无数据";
        }

        double avgGrowthRate = items.stream()
                .map(ComparisonItem::getGrowthRate)
                .filter(Objects::nonNull)
                .mapToDouble(Double::doubleValue)
                .average()
                .orElse(0);

        long positiveCount = items.stream()
                .map(ComparisonItem::getGrowthRate)
                .filter(Objects::nonNull)
                .filter(rate -> rate > 0)
                .count();

        return String.format("%s分析: 平均增长率%.2f%%, %d/%d期呈正增长",
                type, avgGrowthRate, positiveCount, items.size());
    }

    /**
     * 对比结果
     */
    public static class ComparisonResult {
        private String comparisonType;  // YoY/MoM
        private String timeField;
        private String valueField;
        private List<ComparisonItem> items;
        private String summary;

        public String getComparisonType() { return comparisonType; }
        public void setComparisonType(String comparisonType) { this.comparisonType = comparisonType; }

        public String getTimeField() { return timeField; }
        public void setTimeField(String timeField) { this.timeField = timeField; }

        public String getValueField() { return valueField; }
        public void setValueField(String valueField) { this.valueField = valueField; }

        public List<ComparisonItem> getItems() { return items; }
        public void setItems(List<ComparisonItem> items) { this.items = items; }

        public String getSummary() { return summary; }
        public void setSummary(String summary) { this.summary = summary; }
    }

    /**
     * 对比项
     */
    public static class ComparisonItem {
        private String period;          // 时期
        private Double currentValue;    // 当前值
        private Double compareValue;    // 对比值
        private Double growthRate;      // 增长率(%)
        private Double growthAmount;    // 增长量

        public String getPeriod() { return period; }
        public void setPeriod(String period) { this.period = period; }

        public Double getCurrentValue() { return currentValue; }
        public void setCurrentValue(Double currentValue) { this.currentValue = currentValue; }

        public Double getCompareValue() { return compareValue; }
        public void setCompareValue(Double compareValue) { this.compareValue = compareValue; }

        public Double getGrowthRate() { return growthRate; }
        public void setGrowthRate(Double growthRate) { this.growthRate = growthRate; }

        public Double getGrowthAmount() { return growthAmount; }
        public void setGrowthAmount(Double growthAmount) { this.growthAmount = growthAmount; }
    }
}
