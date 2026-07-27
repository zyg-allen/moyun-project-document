package com.moyun.agent.service.impl;

import com.moyun.agent.service.ReportGenerationService;
import com.moyun.agent.vo.DataQueryResponse;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Service;

import java.io.FileOutputStream;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;

/**
 * 报告生成服务实现
 *
 * @author laomao
 */
@Slf4j
@Service
public class ReportGenerationServiceImpl implements ReportGenerationService {

    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    @Override
    public String generateMarkdownReport(DataQueryResponse queryResponse, String naturalQuery) {
        StringBuilder report = new StringBuilder();

        // 报告标题
        report.append("# 数据分析报告\n\n");
        report.append("**生成时间**: ").append(LocalDateTime.now().format(FORMATTER)).append("\n\n");

        // 查询信息
        report.append("## 📋 查询信息\n\n");
        report.append("**查询问题**: ").append(naturalQuery).append("\n\n");
        report.append("**生成的SQL**:\n");
        report.append("```sql\n");
        report.append(queryResponse.getGeneratedSql()).append("\n");
        report.append("```\n\n");
        report.append("**查询类型**: ").append(queryResponse.getQueryType()).append("\n\n");
        report.append("**执行时间**: ").append(queryResponse.getExecutionTime()).append("ms\n\n");
        report.append("**结果数量**: ").append(queryResponse.getTotalCount()).append("条\n\n");

        // 数据概览
        if (queryResponse.getData() != null && !queryResponse.getData().isEmpty()) {
            report.append("## 📊 数据概览\n\n");
            report.append("前5条数据:\n\n");

            // 表格标题
            if (!queryResponse.getColumns().isEmpty()) {
                report.append("| ");
                for (DataQueryResponse.ColumnInfo col : queryResponse.getColumns()) {
                    report.append(col.getColumnName()).append(" | ");
                }
                report.append("\n");

                // 分隔符
                report.append("| ");
                for (int i = 0; i < queryResponse.getColumns().size(); i++) {
                    report.append("--- | ");
                }
                report.append("\n");

                // 数据行
                int limit = Math.min(5, queryResponse.getData().size());
                for (int i = 0; i < limit; i++) {
                    Map<String, Object> row = queryResponse.getData().get(i);
                    report.append("| ");
                    for (DataQueryResponse.ColumnInfo col : queryResponse.getColumns()) {
                        Object value = row.get(col.getColumnName());
                        report.append(value != null ? value.toString() : "null").append(" | ");
                    }
                    report.append("\n");
                }
                report.append("\n");
            }
        }

        // AI分析
        if (queryResponse.getAnalysis() != null && !queryResponse.getAnalysis().isEmpty()) {
            report.append("## 🧠 AI分析\n\n");
            report.append(queryResponse.getAnalysis()).append("\n\n");
        }

        // 统计信息
        if (queryResponse.getStatistics() != null && !queryResponse.getStatistics().isEmpty()) {
            report.append("## 📈 统计信息\n\n");
            for (Map.Entry<String, Object> entry : queryResponse.getStatistics().entrySet()) {
                if (entry.getValue() instanceof Map) {
                    report.append("**").append(entry.getKey()).append("**:\n");
                    @SuppressWarnings("unchecked")
                    Map<String, Object> stats = (Map<String, Object>) entry.getValue();
                    for (Map.Entry<String, Object> stat : stats.entrySet()) {
                        report.append("- ").append(stat.getKey()).append(": ")
                              .append(stat.getValue()).append("\n");
                    }
                    report.append("\n");
                }
            }
        }

        // 数据洞察
        if (queryResponse.getInsights() != null && !queryResponse.getInsights().isEmpty()) {
            report.append("## 💡 数据洞察\n\n");
            for (DataQueryResponse.DataInsight insight : queryResponse.getInsights()) {
                report.append("### ").append(insight.getTitle()).append("\n\n");
                report.append("**严重程度**: ").append(insight.getSeverity()).append("\n\n");
                report.append("**描述**: ").append(insight.getDescription()).append("\n\n");
                if (insight.getRecommendation() != null) {
                    report.append("**建议**: ").append(insight.getRecommendation()).append("\n\n");
                }
            }
        }

        // 图表推荐
        if (queryResponse.getChartRecommendations() != null && 
            !queryResponse.getChartRecommendations().isEmpty()) {
            report.append("## 📊 图表推荐\n\n");
            for (DataQueryResponse.ChartRecommendation chart : queryResponse.getChartRecommendations()) {
                report.append("- **").append(getChartTypeName(chart.getChartType()))
                      .append("** (推荐度: ").append(chart.getPriority()).append("%) - ")
                      .append(chart.getReason()).append("\n");
            }
            report.append("\n");
        }

        // 报告结尾
        report.append("---\n\n");
        report.append("*本报告由AI数据分析系统自动生成*\n");

        return report.toString();
    }

    @Override
    public String generateHtmlReport(DataQueryResponse queryResponse, String naturalQuery) {
        StringBuilder html = new StringBuilder();

        html.append("<!DOCTYPE html>\n");
        html.append("<html lang=\"zh-CN\">\n");
        html.append("<head>\n");
        html.append("    <meta charset=\"UTF-8\">\n");
        html.append("    <meta name=\"viewport\" content=\"width=device-width, initial-scale=1.0\">\n");
        html.append("    <title>数据分析报告</title>\n");
        html.append("    <style>\n");
        html.append("        body { font-family: Arial, 'Microsoft YaHei', sans-serif; padding: 20px; max-width: 1200px; margin: 0 auto; }\n");
        html.append("        h1 { color: #333; border-bottom: 2px solid #409eff; padding-bottom: 10px; }\n");
        html.append("        h2 { color: #409eff; margin-top: 30px; }\n");
        html.append("        table { border-collapse: collapse; width: 100%; margin: 20px 0; }\n");
        html.append("        th, td { border: 1px solid #ddd; padding: 12px; text-align: left; }\n");
        html.append("        th { background-color: #409eff; color: white; }\n");
        html.append("        tr:nth-child(even) { background-color: #f2f2f2; }\n");
        html.append("        .info { background-color: #f0f9ff; padding: 15px; border-radius: 5px; margin: 10px 0; }\n");
        html.append("        .insight { background-color: #fff7e6; padding: 15px; border-radius: 5px; margin: 10px 0; border-left: 4px solid #fa8c16; }\n");
        html.append("        code { background-color: #f5f5f5; padding: 2px 5px; border-radius: 3px; }\n");
        html.append("        pre { background-color: #f5f5f5; padding: 15px; border-radius: 5px; overflow-x: auto; }\n");
        html.append("    </style>\n");
        html.append("</head>\n");
        html.append("<body>\n");

        // 标题
        html.append("    <h1>📊 数据分析报告</h1>\n");
        html.append("    <div class=\"info\">\n");
        html.append("        <strong>生成时间:</strong> ").append(LocalDateTime.now().format(FORMATTER)).append("<br>\n");
        html.append("        <strong>查询问题:</strong> ").append(escapeHtml(naturalQuery)).append("\n");
        html.append("    </div>\n");

        // SQL信息
        html.append("    <h2>📋 查询信息</h2>\n");
        html.append("    <div class=\"info\">\n");
        html.append("        <strong>查询类型:</strong> ").append(queryResponse.getQueryType()).append("<br>\n");
        html.append("        <strong>执行时间:</strong> ").append(queryResponse.getExecutionTime()).append("ms<br>\n");
        html.append("        <strong>结果数量:</strong> ").append(queryResponse.getTotalCount()).append("条\n");
        html.append("    </div>\n");
        html.append("    <pre><code>").append(escapeHtml(queryResponse.getGeneratedSql())).append("</code></pre>\n");

        // 数据表格
        if (queryResponse.getData() != null && !queryResponse.getData().isEmpty()) {
            html.append("    <h2>📊 数据结果</h2>\n");
            html.append("    <table>\n");
            html.append("        <thead><tr>\n");
            for (DataQueryResponse.ColumnInfo col : queryResponse.getColumns()) {
                html.append("            <th>").append(escapeHtml(col.getColumnName())).append("</th>\n");
            }
            html.append("        </tr></thead>\n");
            html.append("        <tbody>\n");

            int limit = Math.min(10, queryResponse.getData().size());
            for (int i = 0; i < limit; i++) {
                html.append("        <tr>\n");
                Map<String, Object> row = queryResponse.getData().get(i);
                for (DataQueryResponse.ColumnInfo col : queryResponse.getColumns()) {
                    Object value = row.get(col.getColumnName());
                    html.append("            <td>")
                        .append(value != null ? escapeHtml(value.toString()) : "null")
                        .append("</td>\n");
                }
                html.append("        </tr>\n");
            }
            html.append("        </tbody>\n");
            html.append("    </table>\n");
        }

        // AI分析
        if (queryResponse.getAnalysis() != null) {
            html.append("    <h2>🧠 AI分析</h2>\n");
            html.append("    <div class=\"info\">\n");
            html.append("        ").append(escapeHtml(queryResponse.getAnalysis()).replace("\n", "<br>")).append("\n");
            html.append("    </div>\n");
        }

        // 数据洞察
        if (queryResponse.getInsights() != null && !queryResponse.getInsights().isEmpty()) {
            html.append("    <h2>💡 数据洞察</h2>\n");
            for (DataQueryResponse.DataInsight insight : queryResponse.getInsights()) {
                html.append("    <div class=\"insight\">\n");
                html.append("        <strong>").append(escapeHtml(insight.getTitle())).append("</strong><br>\n");
                html.append("        ").append(escapeHtml(insight.getDescription())).append("\n");
                if (insight.getRecommendation() != null) {
                    html.append("        <br><em>建议: ").append(escapeHtml(insight.getRecommendation())).append("</em>\n");
                }
                html.append("    </div>\n");
            }
        }

        html.append("    <hr>\n");
        html.append("    <p style=\"text-align: center; color: #999;\"><em>本报告由AI数据分析系统自动生成</em></p>\n");
        html.append("</body>\n");
        html.append("</html>\n");

        return html.toString();
    }

    @Override
    public String generateCompleteReport(List<DataQueryResponse> queryResponses, String reportTitle) {
        StringBuilder report = new StringBuilder();

        report.append("# ").append(reportTitle).append("\n\n");
        report.append("**生成时间**: ").append(LocalDateTime.now().format(FORMATTER)).append("\n\n");
        report.append("**分析项数**: ").append(queryResponses.size()).append("\n\n");
        report.append("---\n\n");

        for (int i = 0; i < queryResponses.size(); i++) {
            DataQueryResponse response = queryResponses.get(i);
            report.append("## 分析 ").append(i + 1).append("\n\n");
            report.append(generateMarkdownReport(response, "查询" + (i + 1)));
            report.append("\n---\n\n");
        }

        return report.toString();
    }

    @Override
    public boolean exportToPdf(String markdownContent, String outputPath) {
        // PDF导出功能 - 需要引入PDF库(如iText)
        // 这里提供基础框架
        log.warn("PDF导出功能需要额外的PDF库支持,当前仅保存为文本");
        try {
            java.nio.file.Files.write(
                java.nio.file.Paths.get(outputPath.replace(".pdf", ".md")),
                markdownContent.getBytes()
            );
            return true;
        } catch (Exception e) {
            log.error("导出失败", e);
            return false;
        }
    }

    @Override
    public boolean exportToExcel(List<Map<String, Object>> data,
                                 List<DataQueryResponse.ColumnInfo> columns,
                                 String outputPath) {
        try (Workbook workbook = new XSSFWorkbook()) {
            Sheet sheet = workbook.createSheet("数据分析结果");

            // 创建标题样式
            CellStyle headerStyle = workbook.createCellStyle();
            Font headerFont = workbook.createFont();
            headerFont.setBold(true);
            headerStyle.setFont(headerFont);
            headerStyle.setFillForegroundColor(IndexedColors.LIGHT_BLUE.getIndex());
            headerStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);

            // 创建标题行
            Row headerRow = sheet.createRow(0);
            for (int i = 0; i < columns.size(); i++) {
                Cell cell = headerRow.createCell(i);
                cell.setCellValue(columns.get(i).getColumnName());
                cell.setCellStyle(headerStyle);
            }

            // 填充数据
            for (int i = 0; i < data.size(); i++) {
                Row row = sheet.createRow(i + 1);
                Map<String, Object> rowData = data.get(i);

                for (int j = 0; j < columns.size(); j++) {
                    Cell cell = row.createCell(j);
                    Object value = rowData.get(columns.get(j).getColumnName());

                    if (value == null) {
                        cell.setCellValue("");
                    } else if (value instanceof Number) {
                        cell.setCellValue(((Number) value).doubleValue());
                    } else {
                        cell.setCellValue(value.toString());
                    }
                }
            }

            // 自动调整列宽
            for (int i = 0; i < columns.size(); i++) {
                sheet.autoSizeColumn(i);
            }

            // 写入文件
            try (FileOutputStream fileOut = new FileOutputStream(outputPath)) {
                workbook.write(fileOut);
            }

            log.info("Excel导出成功: {}", outputPath);
            return true;

        } catch (Exception e) {
            log.error("Excel导出失败", e);
            return false;
        }
    }

    /**
     * HTML转义
     */
    private String escapeHtml(String text) {
        if (text == null) {
            return "";
        }
        return text.replace("&", "&amp;")
                   .replace("<", "&lt;")
                   .replace(">", "&gt;")
                   .replace("\"", "&quot;")
                   .replace("'", "&#39;");
    }

    /**
     * 获取图表类型中文名
     */
    private String getChartTypeName(String type) {
        return switch (type) {
            case "line" -> "折线图";
            case "bar" -> "柱状图";
            case "pie" -> "饼图";
            case "scatter" -> "散点图";
            case "radar" -> "雷达图";
            case "table" -> "表格";
            default -> type;
        };
    }
}
