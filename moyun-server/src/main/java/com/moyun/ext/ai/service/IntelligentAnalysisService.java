package com.moyun.ext.ai.service;

import com.moyun.ext.ai.vo.DataQueryResponse;

import java.util.List;
import java.util.Map;

/**
 * 智能分析服务接口
 *
 * @author laomao
 */
public interface IntelligentAnalysisService {

    /**
     * 自动分析数据
     *
     * @param data 查询结果数据
     * @param columns 列信息
     * @return 统计分析结果
     */
    Map<String, Object> autoAnalyze(List<Map<String, Object>> data, 
                                     List<DataQueryResponse.ColumnInfo> columns);

    /**
     * 推荐图表类型
     *
     * @param data 查询结果数据
     * @param columns 列信息
     * @return 图表推荐列表
     */
    List<DataQueryResponse.ChartRecommendation> recommendCharts(
            List<Map<String, Object>> data, 
            List<DataQueryResponse.ColumnInfo> columns);

    /**
     * 生成数据洞察
     *
     * @param data 查询结果数据
     * @param statistics 统计信息
     * @return 洞察列表
     */
    List<DataQueryResponse.DataInsight> generateInsights(
            List<Map<String, Object>> data,
            Map<String, Object> statistics);

    /**
     * AI生成分析文本
     *
     * @param data 查询结果数据
     * @param statistics 统计信息
     * @param query 原始查询
     * @return AI分析文本
     */
    String generateAnalysisText(List<Map<String, Object>> data,
                                Map<String, Object> statistics,
                                String query);
}
