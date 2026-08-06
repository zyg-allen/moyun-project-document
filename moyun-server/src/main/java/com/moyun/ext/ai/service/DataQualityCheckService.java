package com.moyun.ext.ai.service;

import com.moyun.ext.ai.vo.DataQueryResponse;

import java.util.List;
import java.util.Map;

/**
 * 数据质量检查服务
 *
 * @author laomao
 */
public interface DataQualityCheckService {

    /**
     * 执行完整的数据质量检查
     *
     * @param data 数据
     * @param columns 列信息
     * @return 质量检查报告
     */
    DataQualityReport checkDataQuality(List<Map<String, Object>> data,
                                       List<DataQueryResponse.ColumnInfo> columns);

    /**
     * 检查空值
     *
     * @param data 数据
     * @param columnName 列名
     * @return 空值统计
     */
    NullValueCheck checkNullValues(List<Map<String, Object>> data, String columnName);

    /**
     * 检查重复值
     *
     * @param data 数据
     * @param columnName 列名
     * @return 重复值统计
     */
    DuplicateCheck checkDuplicates(List<Map<String, Object>> data, String columnName);

    /**
     * 检查异常值（数值型字段）
     *
     * @param data 数据
     * @param columnName 列名
     * @return 异常值检测结果
     */
    OutlierCheck checkOutliers(List<Map<String, Object>> data, String columnName);

    /**
     * 数据质量报告
     */
    class DataQualityReport {
        private Integer totalRows;                    // 总行数
        private Integer totalColumns;                 // 总列数
        private Double overallQualityScore;           // 整体质量分数(0-100)
        private List<ColumnQuality> columnQualities;  // 各列质量
        private List<String> issues;                  // 问题列表
        private List<String> recommendations;         // 改进建议

        // Getters and Setters
        public Integer getTotalRows() { return totalRows; }
        public void setTotalRows(Integer totalRows) { this.totalRows = totalRows; }

        public Integer getTotalColumns() { return totalColumns; }
        public void setTotalColumns(Integer totalColumns) { this.totalColumns = totalColumns; }

        public Double getOverallQualityScore() { return overallQualityScore; }
        public void setOverallQualityScore(Double overallQualityScore) { 
            this.overallQualityScore = overallQualityScore; 
        }

        public List<ColumnQuality> getColumnQualities() { return columnQualities; }
        public void setColumnQualities(List<ColumnQuality> columnQualities) { 
            this.columnQualities = columnQualities; 
        }

        public List<String> getIssues() { return issues; }
        public void setIssues(List<String> issues) { this.issues = issues; }

        public List<String> getRecommendations() { return recommendations; }
        public void setRecommendations(List<String> recommendations) { 
            this.recommendations = recommendations; 
        }
    }

    /**
     * 列质量信息
     */
    class ColumnQuality {
        private String columnName;
        private String dataType;
        private Double qualityScore;          // 质量分数(0-100)
        private Integer nullCount;            // 空值数量
        private Double nullPercentage;        // 空值比例
        private Integer duplicateCount;       // 重复值数量
        private Integer outlierCount;         // 异常值数量
        private List<String> issues;          // 问题列表

        // Getters and Setters
        public String getColumnName() { return columnName; }
        public void setColumnName(String columnName) { this.columnName = columnName; }

        public String getDataType() { return dataType; }
        public void setDataType(String dataType) { this.dataType = dataType; }

        public Double getQualityScore() { return qualityScore; }
        public void setQualityScore(Double qualityScore) { this.qualityScore = qualityScore; }

        public Integer getNullCount() { return nullCount; }
        public void setNullCount(Integer nullCount) { this.nullCount = nullCount; }

        public Double getNullPercentage() { return nullPercentage; }
        public void setNullPercentage(Double nullPercentage) { 
            this.nullPercentage = nullPercentage; 
        }

        public Integer getDuplicateCount() { return duplicateCount; }
        public void setDuplicateCount(Integer duplicateCount) { 
            this.duplicateCount = duplicateCount; 
        }

        public Integer getOutlierCount() { return outlierCount; }
        public void setOutlierCount(Integer outlierCount) { this.outlierCount = outlierCount; }

        public List<String> getIssues() { return issues; }
        public void setIssues(List<String> issues) { this.issues = issues; }
    }

    /**
     * 空值检查结果
     */
    class NullValueCheck {
        private String columnName;
        private Integer nullCount;
        private Double nullPercentage;
        private List<Integer> nullRowIndexes;

        public String getColumnName() { return columnName; }
        public void setColumnName(String columnName) { this.columnName = columnName; }

        public Integer getNullCount() { return nullCount; }
        public void setNullCount(Integer nullCount) { this.nullCount = nullCount; }

        public Double getNullPercentage() { return nullPercentage; }
        public void setNullPercentage(Double nullPercentage) { 
            this.nullPercentage = nullPercentage; 
        }

        public List<Integer> getNullRowIndexes() { return nullRowIndexes; }
        public void setNullRowIndexes(List<Integer> nullRowIndexes) { 
            this.nullRowIndexes = nullRowIndexes; 
        }
    }

    /**
     * 重复值检查结果
     */
    class DuplicateCheck {
        private String columnName;
        private Integer duplicateCount;
        private Integer uniqueCount;
        private Double duplicatePercentage;
        private Map<Object, Integer> valueCounts;

        public String getColumnName() { return columnName; }
        public void setColumnName(String columnName) { this.columnName = columnName; }

        public Integer getDuplicateCount() { return duplicateCount; }
        public void setDuplicateCount(Integer duplicateCount) { 
            this.duplicateCount = duplicateCount; 
        }

        public Integer getUniqueCount() { return uniqueCount; }
        public void setUniqueCount(Integer uniqueCount) { this.uniqueCount = uniqueCount; }

        public Double getDuplicatePercentage() { return duplicatePercentage; }
        public void setDuplicatePercentage(Double duplicatePercentage) { 
            this.duplicatePercentage = duplicatePercentage; 
        }

        public Map<Object, Integer> getValueCounts() { return valueCounts; }
        public void setValueCounts(Map<Object, Integer> valueCounts) { 
            this.valueCounts = valueCounts; 
        }
    }

    /**
     * 异常值检查结果
     */
    class OutlierCheck {
        private String columnName;
        private Integer outlierCount;
        private Double outlierPercentage;
        private List<OutlierInfo> outliers;
        private String detectionMethod;  // 检测方法: IQR/ZScore

        public String getColumnName() { return columnName; }
        public void setColumnName(String columnName) { this.columnName = columnName; }

        public Integer getOutlierCount() { return outlierCount; }
        public void setOutlierCount(Integer outlierCount) { this.outlierCount = outlierCount; }

        public Double getOutlierPercentage() { return outlierPercentage; }
        public void setOutlierPercentage(Double outlierPercentage) { 
            this.outlierPercentage = outlierPercentage; 
        }

        public List<OutlierInfo> getOutliers() { return outliers; }
        public void setOutliers(List<OutlierInfo> outliers) { this.outliers = outliers; }

        public String getDetectionMethod() { return detectionMethod; }
        public void setDetectionMethod(String detectionMethod) { 
            this.detectionMethod = detectionMethod; 
        }
    }

    /**
     * 异常值信息
     */
    class OutlierInfo {
        private Integer rowIndex;
        private Object value;
        private Double zScore;  // Z分数
        private String reason;  // 异常原因

        public Integer getRowIndex() { return rowIndex; }
        public void setRowIndex(Integer rowIndex) { this.rowIndex = rowIndex; }

        public Object getValue() { return value; }
        public void setValue(Object value) { this.value = value; }

        public Double getZScore() { return zScore; }
        public void setZScore(Double zScore) { this.zScore = zScore; }

        public String getReason() { return reason; }
        public void setReason(String reason) { this.reason = reason; }
    }
}
