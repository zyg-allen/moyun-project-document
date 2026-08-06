package com.moyun.ext.ai.service;

import com.moyun.ext.ai.vo.DataQueryResponse;

import java.util.List;
import java.util.Map;

/**
 * 报告生成服务接口
 *
 * @author laomao
 */
public interface ReportGenerationService {

    /**
     * 生成Markdown格式报告
     *
     * @param queryResponse 查询响应数据
     * @param naturalQuery 自然语言查询
     * @return Markdown格式报告
     */
    String generateMarkdownReport(DataQueryResponse queryResponse, String naturalQuery);

    /**
     * 生成HTML格式报告
     *
     * @param queryResponse 查询响应数据
     * @param naturalQuery 自然语言查询
     * @return HTML格式报告
     */
    String generateHtmlReport(DataQueryResponse queryResponse, String naturalQuery);

    /**
     * 生成完整的分析报告
     *
     * @param queryResponses 多个查询结果
     * @param reportTitle 报告标题
     * @return 完整报告内容
     */
    String generateCompleteReport(List<DataQueryResponse> queryResponses, String reportTitle);

    /**
     * 导出为PDF
     *
     * @param markdownContent Markdown内容
     * @param outputPath 输出路径
     * @return 是否成功
     */
    boolean exportToPdf(String markdownContent, String outputPath);

    /**
     * 导出为Excel
     *
     * @param data 数据
     * @param columns 列信息
     * @param outputPath 输出路径
     * @return 是否成功
     */
    boolean exportToExcel(List<Map<String, Object>> data, 
                         List<DataQueryResponse.ColumnInfo> columns,
                         String outputPath);
}
