package com.moyun.ext.ai.engine.tool.builtin;

import com.moyun.ext.ai.dto.DataQueryRequest;
import com.moyun.ext.ai.engine.tool.ToolContext;
import com.moyun.ext.ai.engine.tool.ToolExecutor;
import com.moyun.ext.ai.engine.tool.ToolResult;
import com.moyun.ext.ai.service.DataQueryService;
import com.moyun.ext.ai.vo.DataQueryResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * 数据库查询工具
 * 
 * 支持自然语言查询数据库,自动生成SQL并执行分析
 *
 * @author laomao
 */
@Slf4j
@Component
public class DatabaseQueryTool implements ToolExecutor {

    @Autowired
    private DataQueryService dataQueryService;

    @Override
    public String getName() {
        return "database_query";
    }

    @Override
    public String getDescription() {
        return """
            查询数据库并返回智能分析结果。
            支持MySQL和Elasticsearch。
            可以使用自然语言提问,系统会自动生成SQL并执行。
            返回查询结果、统计分析和图表推荐。
            
            使用场景:
            - 查询销售数据
            - 统计用户行为
            - 分析业务指标
            - 生成数据报告
            """;
    }

    @Override
    public String getParametersSchema() {
        return """
            {
                "type": "object",
                "properties": {
                    "datasource_id": {
                        "type": "integer",
                        "description": "数据源ID,必须是已配置的数据源"
                    },
                    "query": {
                        "type": "string",
                        "description": "自然语言查询问题,如: 查询上个月销售额最高的10个产品"
                    },
                    "need_analysis": {
                        "type": "boolean",
                        "description": "是否需要智能分析,默认true"
                    },
                    "need_chart": {
                        "type": "boolean",
                        "description": "是否需要图表推荐,默认true"
                    }
                },
                "required": ["datasource_id", "query"]
            }
            """;
    }

    @Override
    public ToolResult execute(ToolContext context, Map<String, Object> params) {
        try {
            log.info("执行数据库查询工具, 参数: {}", params);

            // 1. 解析参数
            Long datasourceId = getLong(params, "datasource_id");
            String query = getString(params, "query");
            Boolean needAnalysis = getBoolean(params, "need_analysis", true);
            Boolean needChart = getBoolean(params, "need_chart", true);

            // 2. 构建请求
            DataQueryRequest request = new DataQueryRequest();
            request.setDatasourceId(datasourceId);
            request.setQuery(query);
            request.setNeedAnalysis(needAnalysis);
            request.setNeedChart(needChart);
            String sessionId = context.getConversationId() != null ? 
                String.valueOf(context.getConversationId()) : 
                ("tool-" + System.currentTimeMillis());
            request.setSessionId(sessionId);

            // 3. 执行查询
            DataQueryResponse response = dataQueryService.intelligentQuery(request);

            // 4. 构建返回结果
            if (response.getSuccess()) {
                StringBuilder result = new StringBuilder();
                result.append("查询成功!\n\n");

                // 生成的SQL
                result.append("【生成的SQL】\n");
                result.append(response.getGeneratedSql()).append("\n\n");

                // 数据结果
                result.append(String.format("【查询结果】共 %d 条数据\n", response.getTotalCount()));

                // 显示前5条数据
                if (response.getData() != null && !response.getData().isEmpty()) {
                    result.append("前5条数据:\n");
                    int limit = Math.min(5, response.getData().size());
                    for (int i = 0; i < limit; i++) {
                        result.append(response.getData().get(i)).append("\n");
                    }
                    result.append("\n");
                }

                // AI分析
                if (response.getAnalysis() != null) {
                    result.append("【AI分析】\n");
                    result.append(response.getAnalysis()).append("\n\n");
                }

                // 数据洞察
                if (response.getInsights() != null && !response.getInsights().isEmpty()) {
                    result.append("【数据洞察】\n");
                    for (DataQueryResponse.DataInsight insight : response.getInsights()) {
                        result.append(String.format("- [%s] %s: %s\n",
                            insight.getSeverity(),
                            insight.getTitle(),
                            insight.getDescription()));
                    }
                    result.append("\n");
                }

                // 图表推荐
                if (response.getChartRecommendations() != null && !response.getChartRecommendations().isEmpty()) {
                    result.append("【图表推荐】\n");
                    for (DataQueryResponse.ChartRecommendation chart : response.getChartRecommendations()) {
                        result.append(String.format("- %s图表 (推荐度: %d%%) - %s\n",
                            chart.getChartType(),
                            chart.getPriority(),
                            chart.getReason()));
                    }
                }

                return ToolResult.success(result.toString());
            } else {
                return ToolResult.fail(response.getErrorMessage());
            }

        } catch (Exception e) {
            log.error("数据库查询工具执行失败", e);
            return ToolResult.fail("查询失败: " + e.getMessage());
        }
    }

    private Long getLong(Map<String, Object> params, String key) {
        Object value = params.get(key);
        if (value instanceof Number) {
            return ((Number) value).longValue();
        }
        return Long.parseLong(value.toString());
    }

    private String getString(Map<String, Object> params, String key) {
        Object value = params.get(key);
        return value != null ? value.toString() : null;
    }

    private Boolean getBoolean(Map<String, Object> params, String key, boolean defaultValue) {
        Object value = params.get(key);
        if (value instanceof Boolean) {
            return (Boolean) value;
        }
        return defaultValue;
    }
}
