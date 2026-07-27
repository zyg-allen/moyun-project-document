package com.moyun.agent.service;

import com.moyun.agent.vo.DataQueryResponse;
import com.moyun.agent.vo.EnhancedAnalysisReport;

/**
 * 增强的数据分析服务接口
 * 提供深度数据洞察、预测、异常检测和行动建议
 * 
 * @author laomao
 */
public interface EnhancedAnalysisService {
    
    /**
     * 生成增强的数据分析报告
     * 包括：统计分析、趋势分析、异常检测、预测分析、行动建议
     * 
     * @param queryResponse 查询结果
     * @param naturalQuery 原始查询
     * @return 增强分析报告
     */
    EnhancedAnalysisReport generateEnhancedAnalysis(DataQueryResponse queryResponse, String naturalQuery);
    
    /**
     * 检测数据异常
     * 
     * @param data 数据列表
     * @param columnName 列名
     * @return 异常描述
     */
    String detectAnomalies(java.util.List<java.util.Map<String, Object>> data, String columnName);
    
    /**
     * 预测未来趋势
     * 
     * @param data 历史数据
     * @param columnName 列名
     * @param periods 预测期数
     * @return 预测结果
     */
    String predictTrend(java.util.List<java.util.Map<String, Object>> data, String columnName, int periods);
    
    /**
     * 生成行动建议
     * 
     * @param queryResponse 查询结果
     * @param naturalQuery 原始查询
     * @return 建议列表
     */
    java.util.List<String> generateRecommendations(DataQueryResponse queryResponse, String naturalQuery);
}
