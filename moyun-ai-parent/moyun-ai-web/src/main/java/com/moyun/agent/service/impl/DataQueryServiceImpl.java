package com.moyun.agent.service.impl;

import com.moyun.agent.dto.DataQueryRequest;
import com.moyun.agent.exception.BusinessException;
import com.moyun.agent.exception.ErrorCode;
import com.moyun.agent.entity.DataSourceConfig;
import com.moyun.agent.entity.QueryHistory;
import com.moyun.agent.mapper.QueryHistoryMapper;
import com.moyun.agent.service.*;
import com.moyun.agent.vo.DataQueryResponse;
import com.moyun.agent.vo.TableSchemaVO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;

// Elasticsearch imports
import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.json.jackson.JacksonJsonpMapper;
import co.elastic.clients.transport.rest_client.RestClientTransport;
import org.apache.http.HttpHost;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.impl.client.BasicCredentialsProvider;
import org.elasticsearch.client.RestClient;

import javax.sql.DataSource;
import java.sql.*;
import java.util.*;

/**
 * 数据查询服务实现 - 核心调度器
 *
 * @author laomao
 */
@Slf4j
@Service
public class DataQueryServiceImpl implements DataQueryService {

    @Autowired
    private DataSourceService dataSourceService;

    @Autowired
    private SQLGeneratorService sqlGeneratorService;

    @Autowired
    private IntelligentAnalysisService analysisService;
    
    @Autowired
    private EnhancedAnalysisService enhancedAnalysisService;
    
    @Autowired(required = false)
    @Lazy
    private DataAnalysisConversationService conversationService;
    
    @Autowired
    private LLMService llmService;
    
    @Autowired
    private TokenUsageService tokenUsageService;

    @Autowired
    private QueryHistoryMapper queryHistoryMapper;

    @Override
    public DataQueryResponse intelligentQuery(DataQueryRequest request) {
        long startTime = System.currentTimeMillis();

        try {
            log.info("开始智能查询: datasourceId={}, query={}, sessionId={}", 
                request.getDatasourceId(), request.getQuery(), request.getSessionId());
            
            // 如果有会话ID且对话服务可用，使用多轮对话功能
            if (request.getSessionId() != null && conversationService != null) {
                boolean isFollowUp = conversationService.isFollowUpQuery(
                    request.getQuery(), 
                    request.getSessionId()
                );
                
                if (isFollowUp) {
                    log.info("检测到追问查询，使用上下文处理");
                    return conversationService.queryWithContext(request);
                }
            }
            
            // 限流检查（使用数据源ID作为标识）
            String rateLimitKey = "ds_" + request.getDatasourceId();
            if (!com.moyun.agent.util.RateLimiter.tryAcquire(rateLimitKey, 20)) {
                int remaining = com.moyun.agent.util.RateLimiter.getRemainingCount(rateLimitKey, 20);
                return errorResponse("查询过于频繁，请稍后再试。剩余次数: " + remaining + "/20 (每分钟)");
            }

            // 1. 获取数据源配置
            DataSourceConfig dsConfig = dataSourceService.getById(request.getDatasourceId());
            if (dsConfig == null) {
                return errorResponse("数据源不存在");
            }
            
            // 1.5. 如果是Elasticsearch，使用专门的查询方法
            if ("elasticsearch".equalsIgnoreCase(dsConfig.getType())) {
                log.info("检测到Elasticsearch数据源，使用ES查询逻辑");
                return queryElasticsearch(request, dsConfig, startTime);
            }
            
            // 1.6. 安全检查：检测用户意图（MySQL），禁止修改操作
            QueryIntent userIntent = analyzeQueryIntent(request.getQuery());
            if ("MODIFICATION".equals(userIntent.getType())) {
                return errorResponse("⚠️ 安全限制：系统仅支持查询操作，不支持数据修改！请重新输入查询问题。");
            }

            // 2. 智能选择相关表结构（MySQL）
            List<String> tables = dataSourceService.listTables(request.getDatasourceId());
            List<TableSchemaVO> schemas = new ArrayList<>();
            
            // 智能选择：优先匹配表名相关的表
            List<String> relevantTables = selectRelevantTables(tables, request.getQuery());
            
            log.info("从{}个表中智能选择了{}个相关表", tables.size(), relevantTables.size());
            
            for (String table : relevantTables) {
                try {
                    TableSchemaVO schema = dataSourceService.getTableSchema(
                        request.getDatasourceId(), table);
                    schemas.add(schema);
                } catch (Exception e) {
                    log.warn("获取表{}结构失败", table, e);
                }
            }

            // 3. 生成SQL（使用预处理后的查询提升理解）
            String enhancedQuery = preprocessQuery(request.getQuery());
            String sql = sqlGeneratorService.generateSQL(
                enhancedQuery, 
                schemas, 
                request.getSessionId()
            );
            
            log.info("原始查询: {}", request.getQuery());
            log.info("增强查询: {}", enhancedQuery);

            // 4. SQL安全验证（使用增强的安全验证器）
            com.moyun.agent.util.SqlSecurityValidator.ValidationResult validationResult = 
                com.moyun.agent.util.SqlUtils.validateSql(sql);
            
            if (!validationResult.isValid()) {
                log.warn("🚫 SQL安全验证失败: {} (风险级别: {})", 
                    validationResult.getMessage(), validationResult.getRiskLevel());
                
                String errorMsg = String.format(
                    "⚠️ 安全检查未通过：%s\n\n" +
                    "风险级别：%s\n" +
                    "建议：请用更简单的自然语言重新描述您的查询需求。",
                    validationResult.getMessage(),
                    validationResult.getRiskLevel()
                );
                return errorResponse(errorMsg);
            }

            // 5. 执行查询
            DataQueryResponse response = executeQueryInternal(
                request.getDatasourceId(), 
                sql, 
                dsConfig,
                request.getMaxRows()
            );

            // 6. 智能分析(如果需要)
            if (Boolean.TRUE.equals(request.getNeedAnalysis()) && response.getSuccess()) {
                // 使用基础分析
                Map<String, Object> statistics = analysisService.autoAnalyze(
                    response.getData(), 
                    response.getColumns()
                );
                response.setStatistics(statistics);

                // AI生成分析文本
                String analysis = analysisService.generateAnalysisText(
                    response.getData(),
                    statistics,
                    request.getQuery()
                );
                response.setAnalysis(analysis);

                // 生成洞察
                List<DataQueryResponse.DataInsight> insights = analysisService.generateInsights(
                    response.getData(),
                    statistics
                );
                response.setInsights(insights);
                
                // 使用增强分析
                try {
                    com.moyun.agent.vo.EnhancedAnalysisReport enhancedReport = 
                        enhancedAnalysisService.generateEnhancedAnalysis(response, request.getQuery());
                    
                    // 将增强分析结果添加到响应中
                    log.info("增强分析完成，包含 {} 条洞察", 
                        enhancedReport.getInsights() != null ? enhancedReport.getInsights().size() : 0);
                    
                    // 添加行动建议到分析文本
                    if (enhancedReport.getRecommendations() != null && !enhancedReport.getRecommendations().isEmpty()) {
                        StringBuilder recommendations = new StringBuilder("\n\n💡 增强分析建议：\n");
                        for (String rec : enhancedReport.getRecommendations()) {
                            recommendations.append("• ").append(rec).append("\n");
                        }
                        response.setAnalysis(response.getAnalysis() + recommendations.toString());
                    }
                    
                    // 添加异常检测信息
                    if (enhancedReport.getAnomalies() != null && !enhancedReport.getAnomalies().isEmpty()) {
                        StringBuilder anomalies = new StringBuilder("\n\n⚠️ 异常检测：\n");
                        for (com.moyun.agent.vo.EnhancedAnalysisReport.Anomaly anomaly : enhancedReport.getAnomalies()) {
                            anomalies.append(String.format("• %s：%s\n", anomaly.getTitle(), anomaly.getDescription()));
                        }
                        response.setAnalysis(response.getAnalysis() + anomalies.toString());
                    }
                    
                    // 添加预测信息
                    if (enhancedReport.getPrediction() != null) {
                        String prediction = String.format("\n\n🔮 预测分析：\n• %s\n", 
                            enhancedReport.getPrediction().getDescription());
                        response.setAnalysis(response.getAnalysis() + prediction);
                    }
                } catch (Exception e) {
                    log.error("增强分析失败", e);
                    // 即使增强分析失败，也不影响基础查询结果
                }
            }

            // 7. 图表推荐(如果需要)
            if (Boolean.TRUE.equals(request.getNeedChart()) && response.getSuccess()) {
                List<DataQueryResponse.ChartRecommendation> charts = 
                    analysisService.recommendCharts(
                        response.getData(), 
                        response.getColumns()
                    );
                response.setChartRecommendations(charts);
            }

            // 8. 记录查询历史
            long executionTime = System.currentTimeMillis() - startTime;
            saveQueryHistory(request, response, sql, executionTime);

            response.setExecutionTime((int) executionTime);
            
            // 9. 保存到对话上下文（如果有sessionId）
            if (request.getSessionId() != null && conversationService != null && response.getSuccess()) {
                try {
                    // 手动创建上下文（因为不是通过conversationService查询的）
                    // 这里简化处理，只记录日志
                    log.info("查询成功，sessionId: {}, 可用于后续追问", request.getSessionId());
                } catch (Exception e) {
                    log.warn("保存对话上下文失败", e);
                }
            }
            
            log.info("智能查询完成,耗时: {}ms", executionTime);

            return response;

        } catch (Exception e) {
            log.error("智能查询失败", e);
            return errorResponse("查询失败: " + e.getMessage());
        }
    }

    @Override
    public DataQueryResponse executeSQL(Long datasourceId, String sql) {
        try {
            // 安全验证
            if (!sqlGeneratorService.validateSQL(sql)) {
                return errorResponse("SQL不安全,已拦截");
            }

            DataSourceConfig dsConfig = dataSourceService.getById(datasourceId);
            return executeQueryInternal(datasourceId, sql, dsConfig, 1000);

        } catch (Exception e) {
            log.error("执行SQL失败", e);
            return errorResponse("执行失败: " + e.getMessage());
        }
    }

    /**
     * 内部执行查询
     */
    private DataQueryResponse executeQueryInternal(Long datasourceId, 
                                                    String sql, 
                                                    DataSourceConfig dsConfig,
                                                    Integer maxRows) {
        try {
            // 安全验证：禁止非查询操作
            validateQuerySafety(sql);
            DataSource ds = ((DataSourceServiceImpl) dataSourceService)
                .getOrCreateDataSource(dsConfig);

            try (Connection conn = ds.getConnection();
                 Statement stmt = conn.createStatement()) {

                // 设置查询超时
                stmt.setQueryTimeout(30);

                // 执行查询（记录开始时间）
                long queryStartTime = System.currentTimeMillis();
                ResultSet rs = stmt.executeQuery(sql);

                // 获取元数据
                ResultSetMetaData metaData = rs.getMetaData();
                int columnCount = metaData.getColumnCount();

                // 获取表的字段注释
                Map<String, String> columnComments = getColumnComments(conn, metaData, columnCount, dsConfig.getDatabaseName());

                // 构建列信息
                List<DataQueryResponse.ColumnInfo> columns = new ArrayList<>();
                for (int i = 1; i <= columnCount; i++) {
                    String columnName = metaData.getColumnName(i);
                    String tableName = metaData.getTableName(i);
                    String key = tableName + "." + columnName;
                    
                    columns.add(DataQueryResponse.ColumnInfo.builder()
                        .columnName(columnName)
                        .dataType(metaData.getColumnTypeName(i))
                        .comment(columnComments.getOrDefault(key, columnName))
                        .build());
                }

                // 读取数据并应用脱敏
                List<Map<String, Object>> data = new ArrayList<>();
                int limit = maxRows != null ? Math.min(maxRows, 10000) : 1000;
                int rowCount = 0;
                while (rs.next() && data.size() < limit) {
                    rowCount++;
                    Map<String, Object> row = new LinkedHashMap<>();
                    for (int i = 1; i <= columnCount; i++) {
                        String columnName = metaData.getColumnName(i);
                        Object value = rs.getObject(i);

                        // 数据类型格式化
                        value = formatValue(value);

                        // 数据脱敏 - 已禁用，显示完整数据
                        // if (value instanceof String && DataMaskingUtils.needsMasking(columnName)) {
                        //     value = DataMaskingUtils.autoMask(columnName, (String) value);
                        // }

                        row.put(columnName, value);
                    }
                    data.add(row);
                }
                
                long queryTime = System.currentTimeMillis() - queryStartTime;
                log.info("SQL执行完成: 读取{}行数据, 耗时{}ms, SQL: {}", rowCount, queryTime, sql);
                
                // 慢查询告警（超过3秒）
                if (queryTime > 3000) {
                    log.warn("⚠️ 慢查询检测: SQL耗时{}ms, 建议优化。SQL: {}", queryTime, sql);
                }

                // 构建响应
                return DataQueryResponse.builder()
                    .generatedSql(sql)
                    .queryType(sqlGeneratorService.parseQueryType(sql))
                    .data(data)
                    .totalCount(data.size())
                    .columns(columns)
                    .success(true)
                    .build();
            }

        } catch (Exception e) {
            log.error("执行查询失败", e);
            return errorResponse("查询执行失败: " + e.getMessage());
        }
    }

    /**
     * 日期格式化器 - 使用ThreadLocal保证线程安全
     */
    private static final ThreadLocal<java.text.SimpleDateFormat> DATETIME_FORMATTER = 
        ThreadLocal.withInitial(() -> new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss"));
    
    private static final ThreadLocal<java.text.SimpleDateFormat> DATE_FORMATTER = 
        ThreadLocal.withInitial(() -> new java.text.SimpleDateFormat("yyyy-MM-dd"));
    
    private static final ThreadLocal<java.text.SimpleDateFormat> TIME_FORMATTER = 
        ThreadLocal.withInitial(() -> new java.text.SimpleDateFormat("HH:mm:ss"));

    /**
     * 格式化数据值
     */
    private Object formatValue(Object value) {
        if (value == null) {
            return null;
        }
        
        // 日期类型格式化（线程安全）
        if (value instanceof java.sql.Timestamp) {
            return DATETIME_FORMATTER.get().format((java.sql.Timestamp) value);
        } else if (value instanceof java.sql.Date) {
            return DATE_FORMATTER.get().format((java.sql.Date) value);
        } else if (value instanceof java.sql.Time) {
            return TIME_FORMATTER.get().format((java.sql.Time) value);
        }
        
        // BigDecimal类型格式化
        if (value instanceof java.math.BigDecimal) {
            java.math.BigDecimal bd = (java.math.BigDecimal) value;
            // 保留2位小数，超过2位的保留原样
            if (bd.scale() > 2) {
                return bd.setScale(2, java.math.RoundingMode.HALF_UP);
            }
            return bd;
        }
        
        // 二进制数据处理
        if (value instanceof byte[]) {
            byte[] bytes = (byte[]) value;
            if (bytes.length > 100) {
                return "[BINARY DATA: " + bytes.length + " bytes]";
            }
            return "[BINARY DATA]";
        }
        
        return value;
    }
    
    /**
     * 获取字段注释
     */
    private Map<String, String> getColumnComments(Connection conn, ResultSetMetaData metaData, 
                                                   int columnCount, String databaseName) {
        Map<String, String> comments = new HashMap<>();
        
        try {
            // 收集需要查询注释的表名
            Set<String> tables = new HashSet<>();
            for (int i = 1; i <= columnCount; i++) {
                String tableName = metaData.getTableName(i);
                if (tableName != null && !tableName.isEmpty()) {
                    tables.add(tableName);
                }
            }
            
            // 查询每个表的字段注释
            for (String tableName : tables) {
                String sql = "SELECT COLUMN_NAME, COLUMN_COMMENT FROM information_schema.COLUMNS " +
                           "WHERE TABLE_SCHEMA = ? AND TABLE_NAME = ?";
                
                try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
                    pstmt.setString(1, databaseName);
                    pstmt.setString(2, tableName);
                    
                    try (ResultSet rs = pstmt.executeQuery()) {
                        while (rs.next()) {
                            String columnName = rs.getString("COLUMN_NAME");
                            String comment = rs.getString("COLUMN_COMMENT");
                            if (comment != null && !comment.isEmpty()) {
                                comments.put(tableName + "." + columnName, comment);
                            }
                        }
                    }
                }
            }
        } catch (Exception e) {
            log.warn("获取字段注释失败", e);
        }
        
        return comments;
    }

    /**
     * 智能选择相关表
     * 根据查询语句中的关键词匹配表名和表注释
     */
    private List<String> selectRelevantTables(List<String> allTables, String query) {
        if (allTables == null || allTables.isEmpty()) {
            return new ArrayList<>();
        }
        
        // 如果查询为空或表很少，返回所有表
        if (query == null || query.trim().isEmpty() || allTables.size() <= 3) {
            return allTables;
        }
        
        String queryLower = query.toLowerCase();
        List<String> relevantTables = new ArrayList<>();
        
        // 常见的业务关键词映射
        Map<String, List<String>> keywordMapping = new HashMap<>();
        keywordMapping.put("用户", Arrays.asList("user", "member", "customer", "account"));
        keywordMapping.put("订单", Arrays.asList("order", "sales"));
        keywordMapping.put("商品", Arrays.asList("product", "goods", "item"));
        keywordMapping.put("支付", Arrays.asList("payment", "pay", "transaction"));
        keywordMapping.put("员工", Arrays.asList("employee", "staff", "personnel"));
        keywordMapping.put("部门", Arrays.asList("department", "dept"));
        
        // 1. 精确匹配：查询中直接包含表名
        for (String table : allTables) {
            String tableLower = table.toLowerCase();
            if (queryLower.contains(tableLower)) {
                relevantTables.add(table);
                log.debug("精确匹配表: {}", table);
            }
        }
        
        // 2. 关键词匹配：通过业务关键词映射
        for (Map.Entry<String, List<String>> entry : keywordMapping.entrySet()) {
            if (queryLower.contains(entry.getKey())) {
                for (String keyword : entry.getValue()) {
                    for (String table : allTables) {
                        if (table.toLowerCase().contains(keyword) && !relevantTables.contains(table)) {
                            relevantTables.add(table);
                            log.debug("关键词匹配表: {} (关键词: {})", table, entry.getKey());
                        }
                    }
                }
            }
        }
        
        // 3. 如果没有匹配到任何表，返回所有表（兜底）
        if (relevantTables.isEmpty()) {
            log.debug("未匹配到相关表，返回所有表");
            return allTables;
        }
        
        // 4. 限制最多返回5个表（避免太多表影响性能）
        if (relevantTables.size() > 5) {
            relevantTables = relevantTables.subList(0, 5);
            log.debug("限制返回前5个表");
        }
        
        return relevantTables;
    }
    
    /**
     * Elasticsearch查询
     */
    private DataQueryResponse queryElasticsearch(DataQueryRequest request, 
                                                  DataSourceConfig dsConfig,
                                                  long startTime) {
        try {
            // 1. 安全检查：检测用户意图，禁止修改操作
            QueryIntent intent = analyzeQueryIntent(request.getQuery());
            if ("MODIFICATION".equals(intent.getType())) {
                return errorResponse("⚠️ 安全限制：系统仅支持查询操作，不支持数据修改！请重新输入查询问题。");
            }
            
            // 2. 获取所有索引列表
            List<String> indices = dataSourceService.listTables(request.getDatasourceId());
            log.info("Elasticsearch数据源有{}个索引", indices.size());
            
            // 3. 使用LLM生成Elasticsearch DSL查询（增强版）
            String dslQuery = generateElasticsearchDSL(
                request.getQuery(), 
                request.getDatasourceId(), 
                dsConfig
            );
            log.info("生成的ES DSL: {}", dslQuery);
            
            // 3. 执行Elasticsearch查询
            DataQueryResponse response = executeElasticsearchQuery(dsConfig, dslQuery, request.getMaxRows());
            
            // 4. 智能分析(如果需要)
            if (Boolean.TRUE.equals(request.getNeedAnalysis()) && response.getSuccess()) {
                applyIntelligentAnalysis(request, response);
            }
            
            // 5. 记录查询历史
            long executionTime = System.currentTimeMillis() - startTime;
            saveQueryHistory(request, response, dslQuery, executionTime);
            
            response.setExecutionTime((int) executionTime);
            log.info("Elasticsearch查询完成，耗时: {}ms", executionTime);
            
            return response;
            
        } catch (Exception e) {
            log.error("Elasticsearch查询失败", e);
            return errorResponse("Elasticsearch查询失败: " + e.getMessage());
        }
    }
    
    /**
     * 生成Elasticsearch DSL查询（增强版 - 带字段描述和数据采样）
     */
    private String generateElasticsearchDSL(String naturalQuery, 
                                              Long datasourceId,
                                              DataSourceConfig dsConfig) {
        try {
            // 1. 获取索引详细信息
            List<com.moyun.agent.vo.TableInfoVO> indicesInfo = 
                dataSourceService.listTablesWithInfo(datasourceId);
            
            if (indicesInfo == null || indicesInfo.isEmpty()) {
                throw new BusinessException(ErrorCode.ES_QUERY_FAILED, "未找到可用的索引");
            }
            
            // 限制最多处理5个索引（避免Prompt过长）
            // 智能选择最相关的索引
            if (indicesInfo.size() > 5) {
                indicesInfo = selectRelevantIndices(indicesInfo, naturalQuery, 5);
            }
            
            // 2. 构建增强的Prompt
            StringBuilder prompt = new StringBuilder();
            prompt.append("你是Elasticsearch查询专家。根据用户问题生成精确的DSL查询。\n\n");
            prompt.append("=== 可用索引及字段信息 ===\n\n");
            
            // 3. 为每个索引构建详细信息
            for (com.moyun.agent.vo.TableInfoVO indexInfo : indicesInfo) {
                String indexName = indexInfo.getTableName();
                
                // 智能推断索引用途
                String indexPurpose = inferIndexPurpose(indexName);
                
                prompt.append("【索引：").append(indexName).append("】\n");
                if (indexPurpose != null) {
                    prompt.append("用途：").append(indexPurpose).append("\n");
                }
                prompt.append("文档数：").append(formatNumber(indexInfo.getRowCount())).append("\n");
                prompt.append("存储大小：").append(indexInfo.getDataSizeFormatted()).append("\n\n");
                
                try {
                    // 获取字段mapping
                    com.moyun.agent.vo.TableSchemaVO schema = 
                        dataSourceService.getTableSchema(datasourceId, indexName);
                    
                    // 采样数据
                    Map<String, List<String>> samples = sampleElasticsearchData(dsConfig, indexName);
                    
                    prompt.append("字段列表：\n");
                    int fieldNum = 1;
                    for (com.moyun.agent.vo.TableSchemaVO.ColumnSchema field : schema.getColumns()) {
                        String fieldName = field.getColumnName();
                        String fieldType = field.getDataType();
                        
                        // 智能推断字段描述
                        String description = inferFieldDescription(fieldName, fieldType);
                        
                        prompt.append(fieldNum++).append(". ").append(fieldName)
                              .append(" (").append(fieldType).append(")\n");
                        prompt.append("   描述：").append(description).append("\n");
                        
                        // 添加示例数据和值分析
                        List<String> fieldSamples = samples.get(fieldName);
                        if (fieldSamples != null && !fieldSamples.isEmpty()) {
                            prompt.append("   示例：");
                            int sampleCount = Math.min(3, fieldSamples.size());
                            for (int i = 0; i < sampleCount; i++) {
                                if (i > 0) prompt.append(", ");
                                prompt.append("\"").append(fieldSamples.get(i)).append("\"");
                            }
                            prompt.append("\n");
                            
                            // 分析字段值类型
                            String valueAnalysis = analyzeFieldValues(fieldSamples, fieldType);
                            if (valueAnalysis != null) {
                                prompt.append("   特征：").append(valueAnalysis).append("\n");
                            }
                        }
                        
                        // 查询提示
                        prompt.append("   提示：");
                        if ("dense_vector".equals(fieldType) || "sparse_vector".equals(fieldType)) {
                            prompt.append("向量字段，用于相似度搜索，不用于普通文本查询");
                        } else if ("text".equals(fieldType)) {
                            prompt.append("全文搜索字段，使用match查询");
                        } else if ("keyword".equals(fieldType)) {
                            prompt.append("精确匹配字段，使用term查询");
                        } else if ("date".equals(fieldType) || "date_nanos".equals(fieldType)) {
                            prompt.append("日期字段，使用range查询");
                        } else {
                            prompt.append("使用对应类型的查询");
                        }
                        prompt.append("\n\n");
                    }
                    
                } catch (Exception e) {
                    log.warn("获取索引{}的字段信息失败: {}", indexName, e.getMessage());
                    prompt.append("（字段信息获取失败）\n\n");
                }
            }
            
            // 4. 智能识别查询意图
            QueryIntent intent = analyzeQueryIntent(naturalQuery);
            
            // 5. 添加用户问题和意图分析（使用预处理后的查询）
            String enhancedQuery = preprocessQuery(naturalQuery);
            prompt.append("=== 用户问题 ===\n");
            prompt.append("原始问题：").append(naturalQuery).append("\n");
            if (!enhancedQuery.equals(naturalQuery)) {
                prompt.append("语义增强：").append(enhancedQuery).append("\n");
            }
            prompt.append("查询意图：").append(intent.getEnhancedDescription()).append("\n");
            
            // 添加上下文提示
            QueryContext ctx = intent.getContext();
            if (ctx != null) {
                if (ctx.getLimit() != null) {
                    prompt.append("返回数量：").append(ctx.getLimit()).append("条\n");
                }
                if (ctx.getTimeRange() != null) {
                    prompt.append("时间范围：").append(ctx.getTimeRange()).append("\n");
                }
                if (ctx.getSortOrder() != null) {
                    prompt.append("排序方式：").append(ctx.getSortOrder()).append("\n");
                }
                if (!ctx.getKeywords().isEmpty()) {
                    prompt.append("关键概念：").append(String.join(", ", ctx.getKeywords())).append("\n");
                }
            }
            prompt.append("\n");
            
            // 6. 根据意图生成示例
            String queryExample = generateQueryExample(intent, indicesInfo);
            if (queryExample != null) {
                prompt.append("=== 参考示例 ===\n");
                prompt.append(queryExample).append("\n\n");
            }
            
            // 7. 生成要求
            prompt.append("=== 生成要求 ===\n\n");
            
            prompt.append("**语义理解增强**:\n");
            prompt.append("- 理解同义词：数量/总数/个数 → count聚合\n");
            prompt.append("- 理解时间：今天/昨天/最近N天 → range查询时间字段\n");
            prompt.append("- 理解排序：最大/最高/最多 → sort降序，最小/最低/最少 → sort升序\n");
            prompt.append("- 理解模糊：搜索/查找/包含 → match查询（text字段）\n");
            prompt.append("- 理解精确：等于/是/为 → term查询（keyword字段）\n\n");
            
            prompt.append("**核心要求**:\n");
            prompt.append("⚠️ 安全要求：只能生成查询（search）操作，严禁生成任何修改、删除、新增数据的操作！\n");
            prompt.append("1. 只返回JSON对象，格式：{\"index\": \"索引名\", \"query\": {...DSL...}, \"size\": 数量}\n");
            prompt.append("2. text字段用match查询（模糊匹配），keyword字段用term查询（精确匹配）\n");
            prompt.append("3. 向量字段不用于普通查询\n");
            prompt.append("4. ").append(intent.getQueryTip()).append("\n");
            prompt.append("5. 根据用户意图智能选择聚合、排序、范围等查询方式\n");
            prompt.append("6. 只返回JSON，不要任何解释说明\n");
            
// ...
            log.debug("增强Prompt长度: {} 字符", prompt.length());
            
            // 5. 调用LLM生成DSL
            String promptText = prompt.toString();
            String dsl = llmService.generate(promptText);
            
            // 6. 统计Token使用
            try {
                int inputTokens = tokenUsageService.estimateTokens(promptText);
                int outputTokens = tokenUsageService.estimateTokens(dsl);
                
                // 记录使用情况（agentId使用null，代表系统功能）
                tokenUsageService.recordUsageAsync(
                    null, // conversationId - AI数据分析不属于对话
                    null, // agentId - 系统功能
                    "default", // modelName - 使用默认模型
                    "system", // modelProvider
                    inputTokens,
                    outputTokens,
                    "AI数据分析-ES" // requestType
                );
                
                log.info("ES DSL生成Token统计: 输入={}, 输出={}, 总计={}", 
                    inputTokens, outputTokens, inputTokens + outputTokens);
            } catch (Exception e) {
                log.warn("Token统计失败", e);
            }
            
            // 清理返回结果
            dsl = dsl.replace("```json", "").replace("```", "").trim();
            
            return dsl;
            
        } catch (Exception e) {
            log.error("生成Elasticsearch DSL失败", e);
            throw new BusinessException(ErrorCode.ES_QUERY_FAILED, "生成查询失败: " + e.getMessage(), e);
        }
    }
    
    /**
     * 查询意图分析（增强版 - 更精确的意图识别）
     */
    private QueryIntent analyzeQueryIntent(String query) {
        String lowerQuery = query.toLowerCase();
        
        // 提取查询上下文信息
        QueryContext context = extractQueryContext(lowerQuery);
        
        // 1. 统计类查询（优先级最高）
        if (lowerQuery.matches(".*(有多少|总共有|一共有|总计|总数|共有|数量是|count|total).*") ||
            lowerQuery.matches(".*(统计.*数量|统计.*总数|计算.*总数).*") ||
            (lowerQuery.matches(".*(多少).*") && !lowerQuery.matches(".*(查看|显示|列出|哪些).*"))) {
            return new QueryIntent(
                "STATISTICS",
                "统计查询",
                "使用聚合查询(aggregations)并设置size为0，只返回统计结果",
                context
            );
        }
        
        // 2. 聚合分析类（分组、分类）
        if (lowerQuery.matches(".*(按.*分组|按.*分类|按.*统计|group\\s*by|分组统计).*") ||
            lowerQuery.matches(".*(各.*数量|每.*数量|.*的分布|.*占比|.*比例).*")) {
            return new QueryIntent(
                "AGGREGATION",
                "聚合分析",
                "使用terms或其他聚合查询，分组统计数据",
                context
            );
        }
        
        // 3. 时间范围查询（识别时间相关的查询）
        if (lowerQuery.matches(".*(最近|今天|昨天|本周|本月|今年|past|recent|last).*") ||
            lowerQuery.matches(".*(.*天内|.*小时内|.*分钟内|.*周内|.*月内).*")) {
            return new QueryIntent(
                "TIME_RANGE",
                "时间范围查询",
                "识别时间字段并使用range查询，自动计算时间范围",
                context
            );
        }
        
        // 4. 排序查询类（Top N、最值）
        if (lowerQuery.matches(".*(最大|最小|最高|最低|最多|最少|最贵|最便宜).*") ||
            lowerQuery.matches(".*(top\\s*\\d+|前\\d+|前.*名|前.*个|排名|排行).*") ||
            lowerQuery.matches(".*(最新|最早|最近.*条|最后.*条).*")) {
            return new QueryIntent(
                "SORT",
                "排序查询",
                "使用sort排序，根据问题自动识别排序字段和方向，并设置适当的size限制",
                context
            );
        }
        
        // 5. 数值范围查询
        if (lowerQuery.matches(".*(大于|小于|超过|不足|低于|高于|多于|少于).*\\d+.*") ||
            lowerQuery.matches(".*\\d+\\s*(到|至|-|~)\\s*\\d+.*") ||
            lowerQuery.matches(".*(\\d+.*以上|\\d+.*以下|\\d+.*之间).*")) {
            return new QueryIntent(
                "RANGE",
                "范围查询",
                "使用range查询设置gte/lte/gt/lt条件，自动识别数值字段",
                context
            );
        }
        
        // 6. 精确匹配类（状态、类型等枚举值）
        if (lowerQuery.matches(".*(状态为|状态是|类型为|类型是|等于|等同于).*") ||
            lowerQuery.matches(".*(是.*的|为.*的|属于).*") ||
            (lowerQuery.matches(".*(是|为)\\s*[\"'].*[\"'].*") && !lowerQuery.contains("不是"))) {
            return new QueryIntent(
                "EXACT",
                "精确匹配",
                "使用term查询进行精确匹配，优先匹配keyword类型字段",
                context
            );
        }
        
        // 7. 模糊搜索类（包含、搜索）
        if (lowerQuery.matches(".*(搜索|查找|检索|包含|含有|带有|匹配|search|find|contains).*") ||
            lowerQuery.matches(".*(.*中包含|.*里包含|.*包含了).*")) {
            return new QueryIntent(
                "SEARCH",
                "模糊搜索",
                "使用match查询进行全文搜索，优先匹配text类型字段",
                context
            );
        }
        
        // 8. 存在性查询
        if (lowerQuery.matches(".*(有没有|是否有|是否存在|存在.*字段|有.*字段的).*") ||
            lowerQuery.matches(".*(哪些.*有|哪些.*存在).*")) {
            return new QueryIntent(
                "EXISTS",
                "存在性查询",
                "使用exists查询检查字段是否存在或不为空",
                context
            );
        }
        
        // 9. 更新/修改类（识别但返回错误提示）
        if (lowerQuery.matches(".*(更新|修改|删除|新增|插入|update|delete|insert).*")) {
            return new QueryIntent(
                "MODIFICATION",
                "数据修改操作（不支持）",
                "系统仅支持查询操作，不支持数据修改",
                context
            );
        }
        
        // 10. 列举查询（查看、显示、列出）
        if (lowerQuery.matches(".*(查看|显示|列出|展示|show|list|view).*")) {
            return new QueryIntent(
                "LIST",
                "列举查询",
                "返回符合条件的文档列表，设置适当的size",
                context
            );
        }
        
        // 默认：智能浏览查询
        return new QueryIntent(
            "BROWSE",
            "数据浏览",
            "根据问题内容智能选择查询方式，返回相关数据",
            context
        );
    }
    
    /**
     * 同义词映射 - 提升语义理解
     */
    private static final Map<String, List<String>> SYNONYM_MAP = new HashMap<>();
    
    static {
        // 数量/统计相关
        SYNONYM_MAP.put("数量", Arrays.asList("总数", "个数", "条数", "多少", "几个", "几条", "count"));
        SYNONYM_MAP.put("总计", Arrays.asList("合计", "总和", "累计", "sum", "total"));
        SYNONYM_MAP.put("平均", Arrays.asList("均值", "平均值", "avg", "average"));
        
        // 时间相关
        SYNONYM_MAP.put("今天", Arrays.asList("当天", "本日", "今日", "today"));
        SYNONYM_MAP.put("昨天", Arrays.asList("昨日", "前一天", "yesterday"));
        SYNONYM_MAP.put("最近", Arrays.asList("近期", "最新", "最新的", "recent", "latest"));
        SYNONYM_MAP.put("本周", Arrays.asList("这周", "本星期", "这星期", "this week"));
        SYNONYM_MAP.put("本月", Arrays.asList("这月", "这个月", "当月", "this month"));
        
        // 排序相关
        SYNONYM_MAP.put("最大", Arrays.asList("最高", "最多", "最贵", "最长", "max", "maximum"));
        SYNONYM_MAP.put("最小", Arrays.asList("最低", "最少", "最便宜", "最短", "min", "minimum"));
        SYNONYM_MAP.put("排序", Arrays.asList("排行", "排名", "顺序", "sort", "order"));
        
        // 比较相关
        SYNONYM_MAP.put("大于", Arrays.asList("超过", "多于", "高于", "大过", "greater than", ">"));
        SYNONYM_MAP.put("小于", Arrays.asList("少于", "低于", "不足", "less than", "<"));
        SYNONYM_MAP.put("等于", Arrays.asList("是", "为", "equals", "="));
        
        // 操作相关
        SYNONYM_MAP.put("查询", Arrays.asList("查找", "搜索", "检索", "查看", "显示", "列出", "select", "search", "find"));
        SYNONYM_MAP.put("分组", Arrays.asList("分类", "按...统计", "group by", "分组统计"));
        SYNONYM_MAP.put("筛选", Arrays.asList("过滤", "条件", "where", "filter"));
        
        // 业务实体
        SYNONYM_MAP.put("用户", Arrays.asList("客户", "会员", "人员", "账号", "user", "customer", "member"));
        SYNONYM_MAP.put("订单", Arrays.asList("工单", "单据", "order", "transaction"));
        SYNONYM_MAP.put("商品", Arrays.asList("产品", "货品", "物品", "product", "goods", "item"));
        SYNONYM_MAP.put("价格", Arrays.asList("金额", "费用", "钱", "price", "amount", "cost"));
        
        // 状态相关
        SYNONYM_MAP.put("状态", Arrays.asList("情况", "状况", "status", "state"));
        SYNONYM_MAP.put("类型", Arrays.asList("种类", "分类", "type", "category"));
        SYNONYM_MAP.put("完成", Arrays.asList("已完成", "成功", "完结", "completed", "finished", "done"));
        SYNONYM_MAP.put("待处理", Arrays.asList("未处理", "处理中", "pending", "in progress"));
    }
    
    /**
     * 查询预处理 - 同义词扩展和语义增强
     */
    private String preprocessQuery(String query) {
        if (query == null || query.trim().isEmpty()) {
            return query;
        }
        
        String processed = query;
        
        // 1. 同义词扩展 - 提升理解能力
        for (Map.Entry<String, List<String>> entry : SYNONYM_MAP.entrySet()) {
            String canonical = entry.getKey();
            for (String synonym : entry.getValue()) {
                // 如果包含同义词，添加标准词到查询中（保留原词）
                if (processed.toLowerCase().contains(synonym.toLowerCase())) {
                    // 标记同义词关系，帮助AI理解
                    if (!processed.contains(canonical)) {
                        processed = processed + " (" + canonical + ")";
                    }
                    break;
                }
            }
        }
        
        // 2. 常见口语化表达转换
        processed = processed.replaceAll("有多少个?", "数量")
                           .replaceAll("一共有?", "总数")
                           .replaceAll("帮我", "")
                           .replaceAll("请", "")
                           .replaceAll("能不能", "")
                           .replaceAll("可以", "");
        
        // 3. 时间表达规范化
        processed = processed.replaceAll("([0-9]+)天前", "最近$1天")
                           .replaceAll("上个?月", "上月")
                           .replaceAll("去年", "上一年");
        
        log.debug("查询预处理: {} -> {}", query, processed);
        return processed;
    }
    
    /**
     * 提取查询上下文信息（增强版 - 更智能的提取）
     */
    private QueryContext extractQueryContext(String query) {
        // 先预处理查询
        String processedQuery = preprocessQuery(query);
        
        QueryContext context = new QueryContext();
        
        // 提取数量限制
        java.util.regex.Pattern numberPattern = java.util.regex.Pattern.compile("(前|top|最近|最新|最后)?\\s*(\\d+)\\s*(条|个|名|行)?");
        java.util.regex.Matcher matcher = numberPattern.matcher(query);
        if (matcher.find()) {
            try {
                context.setLimit(Integer.parseInt(matcher.group(2)));
            } catch (Exception e) {
                // 忽略
            }
        }
        
        // 提取时间范围
        if (query.contains("今天")) {
            context.setTimeRange("today");
        } else if (query.contains("昨天")) {
            context.setTimeRange("yesterday");
        } else if (query.contains("本周") || query.contains("这周")) {
            context.setTimeRange("this_week");
        } else if (query.contains("本月") || query.contains("这个月")) {
            context.setTimeRange("this_month");
        } else if (query.matches(".*(\\d+)\\s*天.*")) {
            java.util.regex.Matcher dayMatcher = java.util.regex.Pattern.compile("(\\d+)\\s*天").matcher(query);
            if (dayMatcher.find()) {
                context.setTimeRange("last_" + dayMatcher.group(1) + "_days");
            }
        }
        
        // 提取排序方向
        if (query.matches(".*(最大|最高|最多|最贵|降序|desc).*")) {
            context.setSortOrder("desc");
        } else if (query.matches(".*(最小|最低|最少|最便宜|升序|asc).*")) {
            context.setSortOrder("asc");
        }
        
        // 提取关键字段名称
        if (query.matches(".*(用户|user).*")) {
            context.addKeyword("user");
        }
        if (query.matches(".*(订单|order).*")) {
            context.addKeyword("order");
        }
        if (query.matches(".*(商品|产品|product).*")) {
            context.addKeyword("product");
        }
        if (query.matches(".*(价格|金额|price|amount).*")) {
            context.addKeyword("price");
        }
        if (query.matches(".*(时间|日期|date|time).*")) {
            context.addKeyword("time");
        }
        
        return context;
    }
    
    /**
     * 查询上下文类
     */
    private static class QueryContext {
        private Integer limit;
        private String timeRange;
        private String sortOrder;
        private List<String> keywords = new ArrayList<>();
        
        public Integer getLimit() { return limit; }
        public void setLimit(Integer limit) { this.limit = limit; }
        public String getTimeRange() { return timeRange; }
        public void setTimeRange(String timeRange) { this.timeRange = timeRange; }
        public String getSortOrder() { return sortOrder; }
        public void setSortOrder(String sortOrder) { this.sortOrder = sortOrder; }
        public List<String> getKeywords() { return keywords; }
        public void addKeyword(String keyword) { this.keywords.add(keyword); }
    }
    
    /**
     * 根据意图生成查询示例
     */
    private String generateQueryExample(QueryIntent intent, List<com.moyun.agent.vo.TableInfoVO> indices) {
        if (indices == null || indices.isEmpty()) {
            return null;
        }
        
        String indexName = indices.get(0).getTableName();
        
        switch (intent.getType()) {
            case "STATISTICS":
                return String.format(
                    "示例（统计文档总数）：\n" +
                    "{\n" +
                    "  \"index\": \"%s\",\n" +
                    "  \"query\": { \"match_all\": {} },\n" +
                    "  \"size\": 0,\n" +
                    "  \"aggs\": {\n" +
                    "    \"total_count\": { \"value_count\": { \"field\": \"_id\" } }\n" +
                    "  }\n" +
                    "}",
                    indexName
                );
                
            case "AGGREGATION":
                return String.format(
                    "示例（按字段分组统计）：\n" +
                    "{\n" +
                    "  \"index\": \"%s\",\n" +
                    "  \"query\": { \"match_all\": {} },\n" +
                    "  \"size\": 0,\n" +
                    "  \"aggs\": {\n" +
                    "    \"group_by_field\": {\n" +
                    "      \"terms\": { \"field\": \"字段名.keyword\" }\n" +
                    "    }\n" +
                    "  }\n" +
                    "}",
                    indexName
                );
                
            case "SORT":
                return String.format(
                    "示例（排序查询）：\n" +
                    "{\n" +
                    "  \"index\": \"%s\",\n" +
                    "  \"query\": { \"match_all\": {} },\n" +
                    "  \"sort\": [ { \"字段名\": \"desc\" } ],\n" +
                    "  \"size\": 10\n" +
                    "}",
                    indexName
                );
                
            case "RANGE":
                return String.format(
                    "示例（范围查询）：\n" +
                    "{\n" +
                    "  \"index\": \"%s\",\n" +
                    "  \"query\": {\n" +
                    "    \"range\": {\n" +
                    "      \"字段名\": { \"gte\": 起始值, \"lte\": 结束值 }\n" +
                    "    }\n" +
                    "  },\n" +
                    "  \"size\": 100\n" +
                    "}",
                    indexName
                );
                
            case "EXACT":
                return String.format(
                    "示例（精确匹配）：\n" +
                    "{\n" +
                    "  \"index\": \"%s\",\n" +
                    "  \"query\": {\n" +
                    "    \"term\": { \"字段名.keyword\": \"精确值\" }\n" +
                    "  },\n" +
                    "  \"size\": 100\n" +
                    "}",
                    indexName
                );
                
            case "SEARCH":
                return String.format(
                    "示例（全文搜索）：\n" +
                    "{\n" +
                    "  \"index\": \"%s\",\n" +
                    "  \"query\": {\n" +
                    "    \"match\": { \"字段名\": \"搜索词\" }\n" +
                    "  },\n" +
                    "  \"size\": 100\n" +
                    "}",
                    indexName
                );
                
            default:
                return null;
        }
    }
    
    /**
     * 查询意图类（增强版 - 包含上下文信息）
     */
    private static class QueryIntent {
        private String type;
        private String description;
        private String queryTip;
        private QueryContext context;
        
        QueryIntent(String type, String description, String queryTip, QueryContext context) {
            this.type = type;
            this.description = description;
            this.queryTip = queryTip;
            this.context = context;
        }
        
        public String getType() { return type; }
        public String getDescription() { return description; }
        public String getQueryTip() { return queryTip; }
        public QueryContext getContext() { return context; }
        
        public String getEnhancedDescription() {
            StringBuilder desc = new StringBuilder(description);
            if (context != null) {
                if (context.getLimit() != null) {
                    desc.append("（限制").append(context.getLimit()).append("条）");
                }
                if (context.getTimeRange() != null) {
                    desc.append("（时间范围：").append(context.getTimeRange()).append("）");
                }
                if (context.getSortOrder() != null) {
                    desc.append("（排序：").append(context.getSortOrder()).append("）");
                }
            }
            return desc.toString();
        }
    }
    
    /**
     * 智能选择最相关的索引
     */
    private List<com.moyun.agent.vo.TableInfoVO> selectRelevantIndices(
        List<com.moyun.agent.vo.TableInfoVO> allIndices,
        String query,
        int maxCount
    ) {
        // 计算每个索引的相关性分数
        List<IndexScore> scores = new ArrayList<>();
        String lowerQuery = query.toLowerCase();
        
        for (com.moyun.agent.vo.TableInfoVO index : allIndices) {
            String indexName = index.getTableName().toLowerCase();
            int score = 0;
            
            // 基础分数：文档数量（优先有数据的索引）
            if (index.getRowCount() > 0) {
                score += 10;
            }
            
            // 名称匹配分数
            String[] queryWords = lowerQuery.split("\\s+");
            for (String word : queryWords) {
                if (word.length() < 2) continue;
                
                if (indexName.contains(word)) {
                    score += 20;  // 完全匹配
                } else if (indexName.contains(word.substring(0, Math.min(3, word.length())))) {
                    score += 10;  // 部分匹配
                }
            }
            
            // 关键词匹配
            if (lowerQuery.contains("用户") && indexName.contains("user")) score += 15;
            if (lowerQuery.contains("日志") && indexName.contains("log")) score += 15;
            if (lowerQuery.contains("订单") && indexName.contains("order")) score += 15;
            if (lowerQuery.contains("商品") && indexName.contains("product")) score += 15;
            if (lowerQuery.contains("文档") && indexName.contains("doc")) score += 15;
            if (lowerQuery.contains("向量") && indexName.contains("vector")) score += 15;
            if (lowerQuery.contains("搜索") && indexName.contains("search")) score += 15;
            
            // 时间范围索引（如果查询提到时间）
            if (lowerQuery.matches(".*\\d{4}.*") && indexName.matches(".*\\d{4}.*")) {
                score += 15;
            }
            
            scores.add(new IndexScore(index, score));
        }
        
        // 按分数排序并返回前N个
        return scores.stream()
            .sorted((a, b) -> Integer.compare(b.score, a.score))
            .limit(maxCount)
            .map(s -> s.index)
            .collect(java.util.stream.Collectors.toList());
    }
    
    /**
     * 索引评分辅助类
     */
    private static class IndexScore {
        com.moyun.agent.vo.TableInfoVO index;
        int score;
        
        IndexScore(com.moyun.agent.vo.TableInfoVO index, int score) {
            this.index = index;
            this.score = score;
        }
    }
    
    /**
     * 智能推断索引用途
     */
    private String inferIndexPurpose(String indexName) {
        String lowerName = indexName.toLowerCase();
        
        // 用户相关
        if (lowerName.contains("user") || lowerName.contains("member") || lowerName.contains("account")) {
            return "用户数据存储";
        }
        
        // 日志相关
        if (lowerName.contains("log") || lowerName.contains("audit") || lowerName.contains("trace")) {
            return "日志记录";
        }
        
        // 订单相关
        if (lowerName.contains("order") || lowerName.contains("transaction") || lowerName.contains("payment")) {
            return "订单交易数据";
        }
        
        // 商品相关
        if (lowerName.contains("product") || lowerName.contains("goods") || lowerName.contains("item")) {
            return "商品信息";
        }
        
        // 文档相关
        if (lowerName.contains("doc") || lowerName.contains("article") || lowerName.contains("content")) {
            return "文档内容";
        }
        
        // AI/向量相关
        if (lowerName.contains("vector") || lowerName.contains("embedding") || lowerName.contains("ai")) {
            return "AI向量存储";
        }
        
        // 搜索相关
        if (lowerName.contains("search") || lowerName.contains("query")) {
            return "搜索数据";
        }
        
        // 消息相关
        if (lowerName.contains("message") || lowerName.contains("msg") || lowerName.contains("notification")) {
            return "消息通知";
        }
        
        // 事件相关
        if (lowerName.contains("event") || lowerName.contains("activity")) {
            return "事件活动记录";
        }
        
        // 指标相关
        if (lowerName.contains("metric") || lowerName.contains("stat") || lowerName.contains("analytics")) {
            return "统计分析数据";
        }
        
        return null;  // 无法推断
    }
    
    /**
     * 分析字段值特征
     */
    private String analyzeFieldValues(List<String> samples, String fieldType) {
        if (samples == null || samples.isEmpty()) {
            return null;
        }
        
        // 检查是否为枚举值（样本数量少且重复）
        Set<String> uniqueValues = new HashSet<>(samples);
        if (uniqueValues.size() <= 5 && samples.size() >= 3) {
            // 可能是枚举字段
            return "可能的枚举值：" + String.join(", ", uniqueValues);
        }
        
        // 检查是否为数值类型
        if ("integer".equals(fieldType) || "long".equals(fieldType) || 
            "float".equals(fieldType) || "double".equals(fieldType)) {
            try {
                List<Double> numbers = new ArrayList<>();
                for (String sample : samples) {
                    numbers.add(Double.parseDouble(sample));
                }
                if (!numbers.isEmpty()) {
                    double min = numbers.stream().min(Double::compare).orElse(0.0);
                    double max = numbers.stream().max(Double::compare).orElse(0.0);
                    return String.format("数值范围：%.1f ~ %.1f", min, max);
                }
            } catch (Exception e) {
                // 忽略解析错误
            }
        }
        
        // 检查文本长度
        if ("text".equals(fieldType)) {
            int avgLength = (int) samples.stream()
                .mapToInt(String::length)
                .average()
                .orElse(0.0);
            if (avgLength > 100) {
                return "长文本字段，平均长度" + avgLength + "字符";
            } else if (avgLength > 20) {
                return "短文本字段";
            }
        }
        
        // 检查是否为时间格式
        if ("keyword".equals(fieldType)) {
            String firstSample = samples.get(0);
            if (firstSample.matches("\\d{4}-\\d{2}-\\d{2}.*") || 
                firstSample.matches("\\d{13}")) {
                return "时间格式数据";
            }
        }
        
        return null;
    }
    
    /**
     * 智能推断字段描述
     */
    private String inferFieldDescription(String fieldName, String fieldType) {
        // 常见字段映射
        Map<String, String> descMap = new HashMap<>();
        // 身份标识
        descMap.put("id", "唯一标识");
        descMap.put("user_id", "用户ID");
        descMap.put("doc_id", "文档ID");
        descMap.put("order_id", "订单ID");
        descMap.put("product_id", "商品ID");
        
        // 用户信息
        descMap.put("username", "用户名");
        descMap.put("email", "邮箱地址");
        descMap.put("phone", "手机号");
        descMap.put("name", "姓名");
        descMap.put("age", "年龄");
        descMap.put("gender", "性别");
        
        // 内容字段
        descMap.put("text", "文本内容");
        descMap.put("content", "内容");
        descMap.put("title", "标题");
        descMap.put("description", "描述");
        descMap.put("summary", "摘要");
        descMap.put("body", "正文");
        descMap.put("message", "消息");
        descMap.put("comment", "评论");
        
        // AI相关
        descMap.put("embedding", "向量嵌入");
        descMap.put("vector", "向量");
        descMap.put("embeddings", "向量数据");
        
        // 元数据
        descMap.put("metadata", "元数据");
        descMap.put("tags", "标签");
        descMap.put("category", "分类");
        descMap.put("status", "状态");
        descMap.put("type", "类型");
        descMap.put("level", "等级");
        descMap.put("priority", "优先级");
        
        // 时间
        descMap.put("created_at", "创建时间");
        descMap.put("updated_at", "更新时间");
        descMap.put("timestamp", "时间戳");
        descMap.put("date", "日期");
        descMap.put("time", "时间");
        descMap.put("datetime", "日期时间");
        
        // 行为
        descMap.put("action", "操作");
        descMap.put("event", "事件");
        descMap.put("activity", "活动");
        descMap.put("behavior", "行为");
        
        // 网络
        descMap.put("ip", "IP地址");
        descMap.put("url", "网址");
        descMap.put("domain", "域名");
        descMap.put("path", "路径");
        
        // 数值
        descMap.put("count", "计数");
        descMap.put("total", "总数");
        descMap.put("amount", "金额");
        descMap.put("price", "价格");
        descMap.put("score", "分数");
        descMap.put("rating", "评分");
        
        // 位置
        descMap.put("location", "位置");
        descMap.put("address", "地址");
        descMap.put("city", "城市");
        descMap.put("country", "国家");
        
        // 直接匹配
        String desc = descMap.get(fieldName.toLowerCase());
        if (desc != null) {
            return desc;
        }
        
        // 处理下划线命名（如：user_name）
        if (fieldName.contains("_")) {
            String[] parts = fieldName.split("_");
            StringBuilder result = new StringBuilder();
            for (String part : parts) {
                String partDesc = descMap.get(part.toLowerCase());
                if (partDesc != null) {
                    result.append(partDesc);
                } else {
                    result.append(part);
                }
            }
            if (result.length() > 0) {
                return result.toString();
            }
        }
        
        // 根据类型推断
        switch (fieldType) {
            case "date":
            case "date_nanos":
                return "时间字段";
            case "ip":
                return "IP地址";
            case "geo_point":
                return "地理坐标";
            case "geo_shape":
                return "地理形状";
            case "dense_vector":
            case "sparse_vector":
                return "向量字段";
            case "object":
                return "对象字段";
            case "nested":
                return "嵌套对象";
            default:
                return fieldName;  // 默认返回字段名
        }
    }
    
    /**
     * 采样Elasticsearch数据
     */
    private Map<String, List<String>> sampleElasticsearchData(
        DataSourceConfig config, 
        String indexName
    ) {
        RestClient restClient = null;
        try {
            // 创建ES客户端
            org.elasticsearch.client.RestClientBuilder builder = RestClient.builder(
                new HttpHost(config.getHost(), config.getPort(), "http")
            );
            
            if (config.getUsername() != null && !config.getUsername().isEmpty()) {
                BasicCredentialsProvider credsProv = new BasicCredentialsProvider();
                credsProv.setCredentials(
                    AuthScope.ANY,
                    new UsernamePasswordCredentials(config.getUsername(), config.getPassword())
                );
                builder.setHttpClientConfigCallback(httpClientBuilder ->
                    httpClientBuilder.setDefaultCredentialsProvider(credsProv)
                );
            }
            
            restClient = builder.build();
            RestClientTransport transport = new RestClientTransport(
                restClient,
                new JacksonJsonpMapper()
            );
            ElasticsearchClient client = new ElasticsearchClient(transport);
            
            // 采样5条数据
            var response = client.search(s -> s
                .index(indexName)
                .size(5)
                .query(q -> q.matchAll(m -> m))
            , Map.class);
            
            Map<String, Set<String>> fieldSamples = new HashMap<>();
            
            for (var hit : response.hits().hits()) {
                Map<String, Object> source = hit.source();
                if (source != null) {
                    for (Map.Entry<String, Object> entry : source.entrySet()) {
                        Object value = entry.getValue();
                        if (value != null) {
                            String sampleValue = formatSampleValue(value);
                            if (sampleValue != null) {
                                fieldSamples.computeIfAbsent(
                                    entry.getKey(), 
                                    k -> new LinkedHashSet<>()
                                ).add(sampleValue);
                            }
                        }
                    }
                }
            }
            
            // 转换为List
            Map<String, List<String>> result = new HashMap<>();
            for (Map.Entry<String, Set<String>> entry : fieldSamples.entrySet()) {
                result.put(entry.getKey(), new ArrayList<>(entry.getValue()));
            }
            
            return result;
            
        } catch (Exception e) {
            log.warn("采样索引{}数据失败: {}", indexName, e.getMessage());
            return new HashMap<>();
        } finally {
            try {
                if (restClient != null) {
                    restClient.close();
                }
            } catch (Exception e) {
                log.error("关闭ES连接失败", e);
            }
        }
    }
    
    /**
     * 格式化采样值
     */
    private String formatSampleValue(Object value) {
        if (value == null) {
            return null;
        }
        
        String str = value.toString();
        
        // 跳过向量数据（通常很长）
        if (str.startsWith("[") && str.length() > 100) {
            return "[向量数据]";
        }
        
        // 限制长度
        if (str.length() > 50) {
            return str.substring(0, 47) + "...";
        }
        
        return str;
    }
    
    /**
     * 格式化数字显示
     */
    private String formatNumber(long number) {
        if (number < 1000) {
            return String.valueOf(number);
        } else if (number < 1000000) {
            return String.format("%.1fK", number / 1000.0);
        } else {
            return String.format("%.1fM", number / 1000000.0);
        }
    }
    
    /**
     * 执行Elasticsearch查询
     */
    private DataQueryResponse executeElasticsearchQuery(DataSourceConfig config, 
                                                          String dslJson,
                                                          Integer maxRows) {
        RestClient restClient = null;
        
        try {
            // 解析DSL JSON
            com.fasterxml.jackson.databind.ObjectMapper mapper = new com.fasterxml.jackson.databind.ObjectMapper();
            @SuppressWarnings("unchecked")
            Map<String, Object> dslMap = mapper.readValue(dslJson, Map.class);
            
            String indexName = (String) dslMap.get("index");
            @SuppressWarnings("unchecked")
            Map<String, Object> queryBody = (Map<String, Object>) dslMap.get("query");
            Integer size = dslMap.containsKey("size") ? ((Number) dslMap.get("size")).intValue() : 
                          (maxRows != null ? maxRows : 1000);
            
            // 创建Elasticsearch客户端
            org.elasticsearch.client.RestClientBuilder builder = RestClient.builder(
                new HttpHost(config.getHost(), config.getPort(), "http")
            );
            
            if (config.getUsername() != null && !config.getUsername().isEmpty()) {
                BasicCredentialsProvider credsProv = new BasicCredentialsProvider();
                credsProv.setCredentials(
                    AuthScope.ANY,
                    new UsernamePasswordCredentials(config.getUsername(), config.getPassword())
                );
                builder.setHttpClientConfigCallback(httpClientBuilder ->
                    httpClientBuilder.setDefaultCredentialsProvider(credsProv)
                );
            }
            
            restClient = builder.build();
            RestClientTransport transport = new RestClientTransport(
                restClient,
                new JacksonJsonpMapper()
            );
            ElasticsearchClient client = new ElasticsearchClient(transport);
            
            // 构建查询请求
            co.elastic.clients.elasticsearch.core.SearchRequest.Builder searchBuilder = 
                new co.elastic.clients.elasticsearch.core.SearchRequest.Builder();
            searchBuilder.index(indexName);
            searchBuilder.size(size);
            
            // 将查询DSL转换为SearchRequest
            String queryJson = mapper.writeValueAsString(queryBody);
            java.io.Reader queryReader = new java.io.StringReader(queryJson);
            searchBuilder.query(q -> q.withJson(queryReader));
            
            // 执行查询
            co.elastic.clients.elasticsearch.core.SearchResponse<Map> searchResponse = 
                client.search(searchBuilder.build(), Map.class);
            
            // 转换结果
            List<Map<String, Object>> resultData = new ArrayList<>();
            List<DataQueryResponse.ColumnInfo> columns = new ArrayList<>();
            Set<String> columnNames = new LinkedHashSet<>();
            
            // 处理查询结果
            for (co.elastic.clients.elasticsearch.core.search.Hit<Map> hit : searchResponse.hits().hits()) {
                Map<String, Object> source = hit.source();
                if (source != null) {
                    resultData.add(source);
                    columnNames.addAll(source.keySet());
                }
            }
            
            // 生成列信息
            for (String columnName : columnNames) {
                DataQueryResponse.ColumnInfo column = new DataQueryResponse.ColumnInfo();
                column.setColumnName(columnName);
                column.setComment(columnName);
                // ES字段不需要设置columnType
                columns.add(column);
            }
            
            // 构建响应
            DataQueryResponse response = DataQueryResponse.builder()
                .success(true)
                .data(resultData)
                .columns(columns)
                .totalCount(resultData.size())
                .generatedSql(dslJson)  // 保存DSL作为"SQL"
                .queryType("Elasticsearch查询")
                .build();
            
            log.info("Elasticsearch查询成功，返回{}条数据", resultData.size());
            return response;
            
        } catch (Exception e) {
            log.error("执行Elasticsearch查询失败", e);
            return errorResponse("执行查询失败: " + e.getMessage());
        } finally {
            try {
                if (restClient != null) {
                    restClient.close();
                }
            } catch (Exception e) {
                log.error("关闭Elasticsearch连接失败", e);
            }
        }
    }
    
    /**
     * 应用智能分析
     */
    private void applyIntelligentAnalysis(DataQueryRequest request, DataQueryResponse response) {
        try {
            // 使用基础分析
            Map<String, Object> statistics = analysisService.autoAnalyze(
                response.getData(), 
                response.getColumns()
            );
            response.setStatistics(statistics);
            
            // AI生成分析文本
            String analysis = analysisService.generateAnalysisText(
                response.getData(),
                statistics,
                request.getQuery()
            );
            response.setAnalysis(analysis);
            
            // 生成洞察
            List<DataQueryResponse.DataInsight> insights = analysisService.generateInsights(
                response.getData(),
                statistics
            );
            response.setInsights(insights);
            
            // 使用增强分析服务
            if (enhancedAnalysisService != null) {
                try {
                    com.moyun.agent.vo.EnhancedAnalysisReport enhancedReport = 
                        enhancedAnalysisService.generateEnhancedAnalysis(
                            response,
                            request.getQuery()
                        );
                    
                    // 追加增强分析内容
                    StringBuilder enhancedAnalysis = new StringBuilder(response.getAnalysis());
                    
                    if (enhancedReport.getRecommendations() != null && !enhancedReport.getRecommendations().isEmpty()) {
                        enhancedAnalysis.append("\n\n💡 行动建议：\n");
                        for (String recommendation : enhancedReport.getRecommendations()) {
                            enhancedAnalysis.append("• ").append(recommendation).append("\n");
                        }
                    }
                    
                    if (enhancedReport.getAnomalies() != null && !enhancedReport.getAnomalies().isEmpty()) {
                        enhancedAnalysis.append("\n\n⚠️ 异常检测：\n");
                        for (com.moyun.agent.vo.EnhancedAnalysisReport.Anomaly anomaly : enhancedReport.getAnomalies()) {
                            enhancedAnalysis.append(String.format("• %s：%s\n", anomaly.getTitle(), anomaly.getDescription()));
                        }
                    }
                    
                    if (enhancedReport.getPrediction() != null) {
                        enhancedAnalysis.append(String.format("\n\n🔮 预测分析：\n• %s\n", 
                            enhancedReport.getPrediction().getDescription()));
                    }
                    
                    response.setAnalysis(enhancedAnalysis.toString());
                } catch (Exception e) {
                    log.error("增强分析失败", e);
                }
            }
            
        } catch (Exception e) {
            log.error("智能分析失败", e);
        }
    }
    
    /**
     * 保存查询历史
     */
    private void saveQueryHistory(DataQueryRequest request, 
                                   DataQueryResponse response, 
                                   String sql,
                                   long executionTime) {
        try {
            QueryHistory history = new QueryHistory();
            history.setDatasourceId(request.getDatasourceId());
            history.setSessionId(request.getSessionId());
            history.setNaturalQuery(request.getQuery());
            history.setGeneratedSql(sql);
            history.setQueryType(response.getQueryType());
            history.setResultCount(response.getTotalCount());
            history.setExecutionTime((int) executionTime);
            history.setStatus(response.getSuccess() ? "success" : "failed");
            history.setErrorMessage(response.getErrorMessage());
            history.setHasInsight(response.getInsights() != null && !response.getInsights().isEmpty());

            queryHistoryMapper.insert(history);

            response.setQueryId(history.getId());

        } catch (Exception e) {
            log.error("保存查询历史失败", e);
        }
    }

    /**
     * 构建错误响应
     */
    private DataQueryResponse errorResponse(String message) {
        return DataQueryResponse.builder()
            .success(false)
            .errorMessage(message)
            .data(new ArrayList<>())
            .columns(new ArrayList<>())
            .build();
    }
    
    /**
     * SQL安全验证 - 确保只能执行SELECT查询
     * 禁止任何DML（INSERT/UPDATE/DELETE）和DDL（CREATE/DROP/ALTER）操作
     */
    private void validateQuerySafety(String sql) {
        if (sql == null || sql.trim().isEmpty()) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "SQL语句不能为空");
        }
        
        // 转换为小写并移除多余空格用于检查
        String normalizedSql = sql.toLowerCase().trim();
        
        // 移除注释（简单处理）
        normalizedSql = normalizedSql.replaceAll("--.*?(\r?\n|$)", " ");
        normalizedSql = normalizedSql.replaceAll("/\\*.*?\\*/", " ");
        normalizedSql = normalizedSql.replaceAll("\\s+", " ").trim();
        
        // 1. 检查是否以SELECT开头（允许WITH子句）
        if (!normalizedSql.startsWith("select") && !normalizedSql.startsWith("with")) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "安全限制：系统仅支持SELECT查询，禁止执行非查询操作");
        }
        
        // 2. 禁止的DML操作（数据修改）
        String[] dmlKeywords = {
            "insert ", "insert(", "insert\t",
            "update ", "update(", "update\t",
            "delete ", "delete(", "delete\t",
            "replace ", "replace(", "replace\t",
            "merge ", "merge(", "merge\t",
            "truncate ", "truncate(", "truncate\t"
        };
        
        for (String keyword : dmlKeywords) {
            if (normalizedSql.contains(keyword)) {
                throw new BusinessException(ErrorCode.PARAM_ERROR, "安全限制：禁止执行数据修改操作（" + keyword.trim().toUpperCase() + "）");
            }
        }
        
        // 3. 禁止的DDL操作（结构修改）
        String[] ddlKeywords = {
            "create ", "create(", "create\t",
            "drop ", "drop(", "drop\t",
            "alter ", "alter(", "alter\t",
            "rename ", "rename(", "rename\t",
            "truncate ", "truncate(", "truncate\t"
        };
        
        for (String keyword : ddlKeywords) {
            if (normalizedSql.contains(keyword)) {
                throw new BusinessException(ErrorCode.PARAM_ERROR, "安全限制：禁止执行表结构修改操作（" + keyword.trim().toUpperCase() + "）");
            }
        }
        
        // 4. 禁止的危险操作
        String[] dangerousKeywords = {
            "grant ", "grant(", "grant\t",
            "revoke ", "revoke(", "revoke\t",
            "exec ", "exec(", "exec\t",
            "execute ", "execute(", "execute\t",
            "call ", "call(", "call\t",
            "load_file", "into outfile", "into dumpfile"
        };
        
        for (String keyword : dangerousKeywords) {
            if (normalizedSql.contains(keyword)) {
                throw new BusinessException(ErrorCode.PARAM_ERROR, "安全限制：禁止执行危险操作（" + keyword.trim().toUpperCase() + "）");
            }
        }
        
        // 5. 检查多语句执行（分号）
        String[] statements = normalizedSql.split(";");
        if (statements.length > 1) {
            // 允许最后一个空语句
            boolean hasMultipleStatements = false;
            for (int i = 0; i < statements.length; i++) {
                String stmt = statements[i].trim();
                if (!stmt.isEmpty() && i < statements.length - 1) {
                    hasMultipleStatements = true;
                    break;
                }
            }
            if (hasMultipleStatements) {
                throw new BusinessException(ErrorCode.PARAM_ERROR, "安全限制：禁止执行多条SQL语句");
            }
        }
        
        log.debug("SQL安全验证通过: {}", sql);
    }
    
    /**
     * 智能字段匹配 - 模糊匹配字段名
     */
    private List<String> findSimilarFields(String userInput, List<String> availableFields) {
        List<String> matches = new ArrayList<>();
        String lowerInput = userInput.toLowerCase();
        
        for (String field : availableFields) {
            String lowerField = field.toLowerCase();
            
            // 1. 精确匹配
            if (lowerField.equals(lowerInput) || lowerField.contains(lowerInput) || lowerInput.contains(lowerField)) {
                matches.add(field);
                continue;
            }
            
            // 2. 同义词匹配
            for (Map.Entry<String, List<String>> entry : SYNONYM_MAP.entrySet()) {
                if (lowerInput.contains(entry.getKey().toLowerCase())) {
                    for (String synonym : entry.getValue()) {
                        if (lowerField.contains(synonym.toLowerCase())) {
                            matches.add(field);
                            break;
                        }
                    }
                }
            }
            
            // 3. 拼音首字母匹配（简单实现）
            if (matches.size() < 3 && calculateSimilarity(lowerInput, lowerField) > 0.6) {
                matches.add(field);
            }
        }
        
        return matches;
    }
    
    /**
     * 计算字符串相似度（简单版）
     */
    private double calculateSimilarity(String s1, String s2) {
        if (s1 == null || s2 == null) {
            return 0.0;
        }
        
        int maxLen = Math.max(s1.length(), s2.length());
        if (maxLen == 0) {
            return 1.0;
        }
        
        int commonChars = 0;
        for (char c : s1.toCharArray()) {
            if (s2.indexOf(c) >= 0) {
                commonChars++;
            }
        }
        
        return (double) commonChars / maxLen;
    }
}
