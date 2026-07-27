package com.moyun.agent.service.impl;

import co.elastic.clients.elasticsearch.indices.GetMappingResponse;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.moyun.agent.entity.DataSourceConfig;
import com.moyun.agent.exception.BusinessException;
import com.moyun.agent.exception.ErrorCode;
import com.moyun.agent.entity.TableMetadata;
import com.moyun.agent.mapper.DataSourceConfigMapper;
import com.moyun.agent.mapper.TableMetadataMapper;
import com.moyun.agent.service.DataSourceService;
import com.moyun.agent.vo.TableInfoVO;
import com.moyun.agent.vo.TableSchemaVO;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

// Elasticsearch imports
import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch.indices.GetIndexResponse;
import co.elastic.clients.json.jackson.JacksonJsonpMapper;
import co.elastic.clients.transport.rest_client.RestClientTransport;
import org.apache.http.HttpHost;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.impl.client.BasicCredentialsProvider;
import org.elasticsearch.client.RestClient;

import javax.sql.DataSource;
import java.sql.*;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 数据源管理服务实现
 *
 * @author laomao
 */
@Slf4j
@Service
public class DataSourceServiceImpl extends ServiceImpl<DataSourceConfigMapper, DataSourceConfig> 
        implements DataSourceService {

    @Autowired
    private TableMetadataMapper tableMetadataMapper;

    @Autowired
    private ObjectMapper objectMapper;

    /**
     * 表结构缓存 (datasourceId:tableName -> TableSchemaVO)
     */
    private final Map<String, TableSchemaVO> schemaCache = new ConcurrentHashMap<>();
    
    /**
     * 缓存过期时间（毫秒）- 5分钟
     */
    private static final long CACHE_EXPIRE_TIME = 5 * 60 * 1000;
    
    /**
     * 缓存时间戳
     */
    private final Map<String, Long> cacheTimestamp = new ConcurrentHashMap<>();

    /**
     * 数据源连接池缓存
     */
    private final Map<Long, HikariDataSource> dataSourcePool = new ConcurrentHashMap<>();

    @Override
    public boolean testConnection(DataSourceConfig config) {
        if ("elasticsearch".equalsIgnoreCase(config.getType())) {
            return testElasticsearchConnection(config);
        } else {
            return testMySQLConnection(config);
        }
    }
    
    /**
     * 测试MySQL连接
     */
    private boolean testMySQLConnection(DataSourceConfig config) {
        try {
            DataSource ds = createDataSource(config);
            try (Connection conn = ds.getConnection()) {
                return conn.isValid(5);
            } finally {
                if (ds instanceof HikariDataSource) {
                    ((HikariDataSource) ds).close();
                }
            }
        } catch (Exception e) {
            log.error("测试MySQL连接失败: {}", e.getMessage());
            return false;
        }
    }
    
    /**
     * 测试Elasticsearch连接
     */
    private boolean testElasticsearchConnection(DataSourceConfig config) {
        ElasticsearchClient client = null;
        RestClient restClient = null;
        
        try {
            // 创建RestClient
            org.elasticsearch.client.RestClientBuilder builder = RestClient.builder(
                new HttpHost(config.getHost(), config.getPort(), "http")
            );
            
            // 如果配置了用户名和密码，添加认证
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
            
            // 创建传输层
            RestClientTransport transport = new RestClientTransport(
                restClient,
                new JacksonJsonpMapper()
            );
            
            // 创建客户端
            client = new ElasticsearchClient(transport);
            
            // 测试连接 - 获取集群信息
            var info = client.info();
            log.info("Elasticsearch连接成功: {}, 版本: {}", info.clusterName(), info.version().number());
            
            return true;
        } catch (Exception e) {
            log.error("测试Elasticsearch连接失败: {}", e.getMessage());
            return false;
        } finally {
            // 清理资源
            try {
                if (restClient != null) {
                    restClient.close();
                }
            } catch (Exception e) {
                log.error("关闭Elasticsearch连接失败", e);
            }
        }
    }

    @Override
    public List<String> listTables(Long datasourceId) {
        List<String> tables = new ArrayList<>();

        try {
            DataSourceConfig config = getById(datasourceId);
            
            if ("elasticsearch".equalsIgnoreCase(config.getType())) {
                return listElasticsearchIndices(config);
            } else {
                return listMySQLTables(config);
            }
        } catch (Exception e) {
            log.error("获取表/索引列表失败", e);
        }

        return tables;
    }
    
    /**
     * 获取MySQL表列表
     */
    private List<String> listMySQLTables(DataSourceConfig config) {
        List<String> tables = new ArrayList<>();
        
        try {
            DataSource ds = getOrCreateDataSource(config);
            try (Connection conn = ds.getConnection()) {
                DatabaseMetaData metaData = conn.getMetaData();
                ResultSet rs = metaData.getTables(
                    config.getDatabaseName(), 
                    null, 
                    "%", 
                    new String[]{"TABLE"}
                );

                while (rs.next()) {
                    tables.add(rs.getString("TABLE_NAME"));
                }
            }
        } catch (Exception e) {
            log.error("获取MySQL表列表失败", e);
        }
        
        return tables;
    }
    
    /**
     * 获取Elasticsearch索引详细信息
     */
    private List<TableInfoVO> listElasticsearchIndicesWithInfo(DataSourceConfig config) {
        List<TableInfoVO> indices = new ArrayList<>();
        RestClient restClient = null;
        
        try {
            // 创建RestClient
            org.elasticsearch.client.RestClientBuilder builder = RestClient.builder(
                new HttpHost(config.getHost(), config.getPort(), "http")
            );
            
            // 如果配置了认证
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
            
            // 获取索引列表（排除系统索引）
            String indexPattern = config.getDatabaseName() != null && !config.getDatabaseName().isEmpty() 
                ? config.getDatabaseName() 
                : "*";
            
            GetIndexResponse response = client.indices().get(g -> g.index(indexPattern));
            
            // 获取索引统计信息
            for (String indexName : response.result().keySet()) {
                // 过滤掉以点开头的系统索引
                if (indexName.startsWith(".")) {
                    continue;
                }
                
                try {
                    // 获取索引的统计信息（包含文档数量和存储大小）
                    var statsResponse = client.indices().stats(s -> s.index(indexName));
                    
                    long docCount = 0L;
                    long storeSize = 0L;
                    
                    // 获取文档数量和存储大小
                    var indexStats = statsResponse.indices().get(indexName);
                    if (indexStats != null) {
                        // 文档数量
                        if (indexStats.total() != null && indexStats.total().docs() != null) {
                            docCount = indexStats.total().docs().count();
                        }
                        // 存储大小（字节）
                        if (indexStats.total() != null && indexStats.total().store() != null) {
                            storeSize = indexStats.total().store().sizeInBytes();
                        }
                    }
                    
                    // 构建TableInfoVO
                    TableInfoVO tableInfo = TableInfoVO.builder()
                        .tableName(indexName)
                        .tableComment("Elasticsearch索引")
                        .rowCount(docCount)
                        .dataLength(storeSize)
                        .dataSizeFormatted(formatDataSize(storeSize))
                        .createTime(null)
                        .updateTime(null)
                        .build();
                    
                    indices.add(tableInfo);
                } catch (Exception e) {
                    log.warn("获取索引 {} 统计信息失败: {}", indexName, e.getMessage());
                    // 即使获取统计失败，也添加基本信息
                    TableInfoVO tableInfo = TableInfoVO.builder()
                        .tableName(indexName)
                        .tableComment("Elasticsearch索引")
                        .rowCount(0L)
                        .dataLength(0L)
                        .dataSizeFormatted("0B")
                        .build();
                    indices.add(tableInfo);
                }
            }
            
            // 按名称排序
            indices.sort((a, b) -> a.getTableName().compareTo(b.getTableName()));
            
            log.info("获取到 {} 个Elasticsearch索引详细信息", indices.size());
            
        } catch (Exception e) {
            log.error("获取Elasticsearch索引详细信息失败", e);
        } finally {
            try {
                if (restClient != null) {
                    restClient.close();
                }
            } catch (Exception e) {
                log.error("关闭Elasticsearch连接失败", e);
            }
        }
        
        return indices;
    }
    
    /**
     * 获取Elasticsearch索引的Mapping（字段结构）
     */
    private TableSchemaVO getElasticsearchIndexMapping(DataSourceConfig config, String indexName, String cacheKey) {
        RestClient restClient = null;
        
        try {
            // 创建RestClient
            org.elasticsearch.client.RestClientBuilder builder = RestClient.builder(
                new HttpHost(config.getHost(), config.getPort(), "http")
            );
            
            // 如果配置了认证
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
            
            // 获取索引mapping
            GetMappingResponse mappingResponse = client.indices().getMapping(m -> m.index(indexName));
            
            TableSchemaVO schema = new TableSchemaVO();
            schema.setTableName(indexName);
            schema.setTableComment("Elasticsearch索引");
            
            // 解析mapping字段
            List<TableSchemaVO.ColumnSchema> columns = new ArrayList<>();
            
            // 获取该索引的mapping
            var indexMapping = mappingResponse.get(indexName);
            if (indexMapping != null && indexMapping.mappings() != null) {
                var properties = indexMapping.mappings().properties();
                
                if (properties != null) {
                    for (Map.Entry<String, co.elastic.clients.elasticsearch._types.mapping.Property> entry : properties.entrySet()) {
                        String fieldName = entry.getKey();
                        co.elastic.clients.elasticsearch._types.mapping.Property property = entry.getValue();
                        
                        // 获取字段类型
                        String fieldType = getElasticsearchFieldType(property);
                        
                        TableSchemaVO.ColumnSchema column = TableSchemaVO.ColumnSchema.builder()
                            .columnName(fieldName)
                            .dataType(fieldType)
                            .comment("ES字段")
                            .columnComment("ES字段")
                            .nullable(true)  // ES字段默认可为空
                            .columnKey("")
                            .columnDefault(null)
                            .extra("")
                            .primaryKey(false)
                            .fieldType(mapEsTypeToFieldType(fieldType))
                            .build();
                        
                        columns.add(column);
                    }
                }
            }
            
            schema.setColumns(columns);
            schema.setPrimaryKeys(new ArrayList<>());  // ES没有主键概念
            
            // 获取文档数量
            try {
                var countResponse = client.count(c -> c.index(indexName));
                schema.setRowCount(countResponse.count());
            } catch (Exception e) {
                log.warn("获取索引文档数量失败: {}", e.getMessage());
                schema.setRowCount(0L);
            }
            
            // 保存到缓存
            schemaCache.put(cacheKey, schema);
            cacheTimestamp.put(cacheKey, System.currentTimeMillis());
            log.info("获取到Elasticsearch索引 {} 的mapping，共 {} 个字段", indexName, columns.size());
            
            return schema;
            
        } catch (Exception e) {
            log.error("获取Elasticsearch索引mapping失败", e);
            throw new BusinessException(ErrorCode.ES_QUERY_FAILED, "获取索引结构失败: " + e.getMessage(), e);
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
     * 获取Elasticsearch字段类型
     */
    private String getElasticsearchFieldType(co.elastic.clients.elasticsearch._types.mapping.Property property) {
        try {
            // 文本类型
            if (property.isText()) return "text";
            if (property.isKeyword()) return "keyword";
            if (property.isWildcard()) return "wildcard";
            if (property.isConstantKeyword()) return "constant_keyword";
            
            // 数值类型
            if (property.isLong()) return "long";
            if (property.isInteger()) return "integer";
            if (property.isShort()) return "short";
            if (property.isByte()) return "byte";
            if (property.isDouble()) return "double";
            if (property.isFloat()) return "float";
            if (property.isHalfFloat()) return "half_float";
            if (property.isScaledFloat()) return "scaled_float";
            if (property.isUnsignedLong()) return "unsigned_long";
            
            // 布尔类型
            if (property.isBoolean()) return "boolean";
            
            // 日期类型
            if (property.isDate()) return "date";
            if (property.isDateNanos()) return "date_nanos";
            
            // 对象类型
            if (property.isObject()) return "object";
            if (property.isNested()) return "nested";
            if (property.isFlattened()) return "flattened";
            
            // 地理类型
            if (property.isGeoPoint()) return "geo_point";
            if (property.isGeoShape()) return "geo_shape";
            
            // 网络类型
            if (property.isIp()) return "ip";
            if (property.isIpRange()) return "ip_range";
            
            // 向量类型（需要检查方法是否存在）
            try {
                if (property.isDenseVector()) return "dense_vector";
            } catch (Exception e) {
                // 方法可能不存在，忽略
            }
            try {
                if (property.isSparseVector()) return "sparse_vector";
            } catch (Exception e) {
                // 方法可能不存在，忽略
            }
            
            // 范围类型
            if (property.isIntegerRange()) return "integer_range";
            if (property.isFloatRange()) return "float_range";
            if (property.isLongRange()) return "long_range";
            if (property.isDoubleRange()) return "double_range";
            if (property.isDateRange()) return "date_range";
            
            // 特殊类型
            if (property.isBinary()) return "binary";
            if (property.isCompletion()) return "completion";
            if (property.isTokenCount()) return "token_count";
            if (property.isVersion()) return "version";
            if (property.isAlias()) return "alias";
            if (property.isRankFeature()) return "rank_feature";
            if (property.isRankFeatures()) return "rank_features";
            if (property.isSearchAsYouType()) return "search_as_you_type";
            if (property.isPercolator()) return "percolator";
            if (property.isHistogram()) return "histogram";
            
            // 尝试通过_kind()方法获取类型名称（fallback机制）
            try {
                String kind = property._kind().toString();
                if (kind != null && !kind.isEmpty()) {
                    log.debug("字段类型通过_kind()获取: {}", kind);
                    return kind;
                }
            } catch (Exception e) {
                // 忽略
            }
            
        } catch (Exception e) {
            log.warn("获取字段类型时发生异常: {}", e.getMessage());
        }
        
        // 未知类型
        return "unknown";
    }
    
    /**
     * 映射ES类型到通用字段类型
     */
    private String mapEsTypeToFieldType(String esType) {
        switch (esType.toLowerCase()) {
            // 文本类型
            case "text":
            case "keyword":
            case "wildcard":
            case "constant_keyword":
            case "search_as_you_type":
                return "string";
            
            // 整数类型
            case "long":
            case "integer":
            case "short":
            case "byte":
            case "unsigned_long":
            case "token_count":
                return "number";
            
            // 浮点数类型
            case "double":
            case "float":
            case "half_float":
            case "scaled_float":
                return "decimal";
            
            // 布尔类型
            case "boolean":
                return "boolean";
            
            // 日期类型
            case "date":
            case "date_nanos":
                return "datetime";
            
            // 对象类型
            case "object":
            case "nested":
            case "flattened":
                return "object";
            
            // 向量类型
            case "dense_vector":
            case "sparse_vector":
                return "vector";
            
            // 范围类型
            case "integer_range":
            case "float_range":
            case "long_range":
            case "double_range":
            case "date_range":
            case "ip_range":
                return "range";
            
            // 地理类型
            case "geo_point":
            case "geo_shape":
                return "geo";
            
            // 网络类型
            case "ip":
                return "ip";
            
            // 特殊类型
            case "binary":
                return "binary";
            case "completion":
                return "completion";
            case "version":
                return "version";
            case "alias":
                return "alias";
            case "rank_feature":
            case "rank_features":
                return "rank";
            case "percolator":
                return "percolator";
            case "histogram":
                return "histogram";
            
            // 默认
            default:
                return "string";
        }
    }
    
    /**
     * 获取Elasticsearch索引列表
     */
    private List<String> listElasticsearchIndices(DataSourceConfig config) {
        List<String> indices = new ArrayList<>();
        RestClient restClient = null;
        
        try {
            // 创建RestClient
            org.elasticsearch.client.RestClientBuilder builder = RestClient.builder(
                new HttpHost(config.getHost(), config.getPort(), "http")
            );
            
            // 如果配置了认证
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
            
            // 获取索引列表（排除系统索引）
            String indexPattern = config.getDatabaseName() != null && !config.getDatabaseName().isEmpty() 
                ? config.getDatabaseName() 
                : "*";
            
            GetIndexResponse response = client.indices().get(g -> g.index(indexPattern));
            
            // 过滤掉以点开头的系统索引
            indices = new ArrayList<>(response.result().keySet());
            indices.removeIf(idx -> idx.startsWith("."));
            indices.sort(String::compareTo);
            
            log.info("获取到 {} 个Elasticsearch索引", indices.size());
            
        } catch (Exception e) {
            log.error("获取Elasticsearch索引列表失败", e);
        } finally {
            try {
                if (restClient != null) {
                    restClient.close();
                }
            } catch (Exception e) {
                log.error("关闭Elasticsearch连接失败", e);
            }
        }
        
        return indices;
    }
    
    @Override
    public List<TableInfoVO> listTablesWithInfo(Long datasourceId) {
        List<TableInfoVO> tables = new ArrayList<>();

        try {
            DataSourceConfig config = getById(datasourceId);
            
            // 根据数据源类型调用不同的方法
            if ("elasticsearch".equalsIgnoreCase(config.getType())) {
                return listElasticsearchIndicesWithInfo(config);
            }
            
            // MySQL处理
            DataSource ds = getOrCreateDataSource(config);

            try (Connection conn = ds.getConnection()) {
                // 对于MySQL，可以从information_schema获取详细信息
                String sql = "SELECT " +
                        "    TABLE_NAME, " +
                        "    TABLE_COMMENT, " +
                        "    TABLE_ROWS, " +
                        "    DATA_LENGTH, " +
                        "    CREATE_TIME, " +
                        "    UPDATE_TIME " +
                        "FROM information_schema.TABLES " +
                        "WHERE TABLE_SCHEMA = ? " +
                        "  AND TABLE_TYPE = 'BASE TABLE' " +
                        "ORDER BY TABLE_NAME";
                
                try (PreparedStatement stmt = conn.prepareStatement(sql)) {
                    stmt.setString(1, config.getDatabaseName());
                    ResultSet rs = stmt.executeQuery();
                    
                    while (rs.next()) {
                        TableInfoVO tableInfo = TableInfoVO.builder()
                                .tableName(rs.getString("TABLE_NAME"))
                                .tableComment(rs.getString("TABLE_COMMENT"))
                                .rowCount(rs.getLong("TABLE_ROWS"))
                                .dataLength(rs.getLong("DATA_LENGTH"))
                                .dataSizeFormatted(formatDataSize(rs.getLong("DATA_LENGTH")))
                                .createTime(rs.getTimestamp("CREATE_TIME") != null ? 
                                        rs.getTimestamp("CREATE_TIME").toString() : null)
                                .updateTime(rs.getTimestamp("UPDATE_TIME") != null ? 
                                        rs.getTimestamp("UPDATE_TIME").toString() : null)
                                .build();
                        tables.add(tableInfo);
                    }
                }
            }
        } catch (Exception e) {
            log.error("获取表详细信息失败", e);
        }

        return tables;
    }
    
    /**
     * 格式化数据大小
     */
    private String formatDataSize(long bytes) {
        if (bytes < 1024) {
            return bytes + " B";
        } else if (bytes < 1024 * 1024) {
            return String.format("%.2f KB", bytes / 1024.0);
        } else if (bytes < 1024 * 1024 * 1024) {
            return String.format("%.2f MB", bytes / (1024.0 * 1024));
        } else {
            return String.format("%.2f GB", bytes / (1024.0 * 1024 * 1024));
        }
    }

    @Override
    public TableSchemaVO getTableSchema(Long datasourceId, String tableName) {
        // 检查缓存
        String cacheKey = datasourceId + ":" + tableName;
        Long timestamp = cacheTimestamp.get(cacheKey);
        if (timestamp != null && System.currentTimeMillis() - timestamp < CACHE_EXPIRE_TIME) {
            TableSchemaVO cached = schemaCache.get(cacheKey);
            if (cached != null) {
                log.debug("从缓存获取表结构: {}", cacheKey);
                return cached;
            }
        }
        
        try {
            DataSourceConfig config = getById(datasourceId);
            
            // 根据数据源类型调用不同的方法
            if ("elasticsearch".equalsIgnoreCase(config.getType())) {
                return getElasticsearchIndexMapping(config, tableName, cacheKey);
            }
            
            // MySQL处理
            DataSource ds = getOrCreateDataSource(config);

            try (Connection conn = ds.getConnection()) {
                DatabaseMetaData metaData = conn.getMetaData();

                TableSchemaVO schema = new TableSchemaVO();
                schema.setTableName(tableName);

                // 获取表注释
                ResultSet tableRs = metaData.getTables(
                    config.getDatabaseName(), null, tableName, new String[]{"TABLE"}
                );
                if (tableRs.next()) {
                    schema.setTableComment(tableRs.getString("REMARKS"));
                }

                // 获取列信息 (使用information_schema获取完整信息)
                List<TableSchemaVO.ColumnSchema> columns = new ArrayList<>();
                String columnSql = "SELECT COLUMN_NAME, DATA_TYPE, COLUMN_COMMENT, IS_NULLABLE, " +
                                 "COLUMN_KEY, COLUMN_DEFAULT, EXTRA " +
                                 "FROM information_schema.COLUMNS " +
                                 "WHERE TABLE_SCHEMA = ? AND TABLE_NAME = ? " +
                                 "ORDER BY ORDINAL_POSITION";
                
                try (PreparedStatement pstmt = conn.prepareStatement(columnSql)) {
                    pstmt.setString(1, config.getDatabaseName());
                    pstmt.setString(2, tableName);
                    
                    try (ResultSet columnRs = pstmt.executeQuery()) {
                        while (columnRs.next()) {
                            String columnName = columnRs.getString("COLUMN_NAME");
                            String dataType = columnRs.getString("DATA_TYPE");
                            String columnComment = columnRs.getString("COLUMN_COMMENT");
                            String isNullable = columnRs.getString("IS_NULLABLE");
                            String columnKey = columnRs.getString("COLUMN_KEY");
                            String columnDefault = columnRs.getString("COLUMN_DEFAULT");
                            String extra = columnRs.getString("EXTRA");
                            
                            TableSchemaVO.ColumnSchema column = TableSchemaVO.ColumnSchema.builder()
                                .columnName(columnName)
                                .dataType(dataType)
                                .comment(columnComment)
                                .columnComment(columnComment)  // 兼容前端
                                .nullable("YES".equals(isNullable))
                                .columnKey(columnKey)
                                .columnDefault(columnDefault)
                                .extra(extra)
                                .primaryKey("PRI".equals(columnKey))
                                .fieldType(determineFieldType(dataType))
                                .build();

                            columns.add(column);
                        }
                    }
                }
                schema.setColumns(columns);

                // 获取主键
                List<String> primaryKeys = new ArrayList<>();
                for (TableSchemaVO.ColumnSchema column : columns) {
                    if (Boolean.TRUE.equals(column.getPrimaryKey())) {
                        primaryKeys.add(column.getColumnName());
                    }
                }
                schema.setPrimaryKeys(primaryKeys);

                // 获取行数
                String countSql = String.format("SELECT COUNT(*) FROM `%s`", tableName);
                try (Statement stmt = conn.createStatement();
                     ResultSet rs = stmt.executeQuery(countSql)) {
                    if (rs.next()) {
                        schema.setRowCount(rs.getLong(1));
                    }
                }

                // 保存到缓存
                schemaCache.put(cacheKey, schema);
                cacheTimestamp.put(cacheKey, System.currentTimeMillis());
                log.debug("缓存表结构: {}", cacheKey);

                return schema;
            }
        } catch (Exception e) {
            log.error("获取表结构失败", e);
            throw new BusinessException(ErrorCode.SQL_EXECUTE_FAILED, "获取表结构失败: " + e.getMessage(), e);
        }
    }

    /**
     * 清理指定数据源的缓存
     */
    public void clearCache(Long datasourceId) {
        String prefix = datasourceId + ":";
        schemaCache.keySet().removeIf(key -> key.startsWith(prefix));
        cacheTimestamp.keySet().removeIf(key -> key.startsWith(prefix));
        log.info("清理数据源缓存: {}", datasourceId);
    }
    
    /**
     * 获取连接池状态
     */
    public com.moyun.agent.vo.DataSourcePoolStatus getPoolStatus(Long datasourceId) {
        DataSourceConfig config = getById(datasourceId);
        if (config == null) {
            return null;
        }
        
        HikariDataSource ds = dataSourcePool.get(datasourceId);
        if (ds == null) {
            return com.moyun.agent.vo.DataSourcePoolStatus.builder()
                .datasourceId(datasourceId)
                .datasourceName(config.getName())
                .status("not_initialized")
                .activeConnections(0)
                .idleConnections(0)
                .totalConnections(0)
                .build();
        }
        
        try {
            com.zaxxer.hikari.HikariPoolMXBean poolMXBean = ds.getHikariPoolMXBean();
            
            int active = poolMXBean.getActiveConnections();
            int idle = poolMXBean.getIdleConnections();
            int total = poolMXBean.getTotalConnections();
            int waiting = poolMXBean.getThreadsAwaitingConnection();
            int max = ds.getMaximumPoolSize();
            
            double usageRate = max > 0 ? (double) total / max * 100 : 0;
            
            // 判断状态
            String status;
            if (usageRate < 60) {
                status = "healthy";
            } else if (usageRate < 80) {
                status = "warning";
            } else {
                status = "critical";
            }
            
            return com.moyun.agent.vo.DataSourcePoolStatus.builder()
                .datasourceId(datasourceId)
                .datasourceName(config.getName())
                .activeConnections(active)
                .idleConnections(idle)
                .totalConnections(total)
                .maxConnections(max)
                .waitingThreads(waiting)
                .usageRate(usageRate)
                .status(status)
                .build();
                
        } catch (Exception e) {
            log.error("获取连接池状态失败", e);
            return null;
        }
    }
    
    /**
     * 清理所有过期缓存
     */
    public void clearExpiredCache() {
        long now = System.currentTimeMillis();
        List<String> expiredKeys = new ArrayList<>();
        
        cacheTimestamp.forEach((key, timestamp) -> {
            if (now - timestamp > CACHE_EXPIRE_TIME) {
                expiredKeys.add(key);
            }
        });
        
        expiredKeys.forEach(key -> {
            schemaCache.remove(key);
            cacheTimestamp.remove(key);
        });
        
        if (!expiredKeys.isEmpty()) {
            log.info("清理过期缓存 {} 个", expiredKeys.size());
        }
    }

    @Override
    public void syncTableMetadata(Long datasourceId) {
        log.info("开始同步数据源{}的元数据", datasourceId);

        try {
            List<String> tables = listTables(datasourceId);

            for (String tableName : tables) {
                try {
                    TableSchemaVO schema = getTableSchema(datasourceId, tableName);

                    // 保存或更新元数据
                    TableMetadata metadata = new TableMetadata();
                    metadata.setDatasourceId(datasourceId);
                    metadata.setTableName(tableName);
                    metadata.setTableComment(schema.getTableComment());
                    metadata.setTableSchema(convertSchemaToJson(schema));
                    metadata.setColumnCount(schema.getColumns().size());
                    metadata.setRowCount(schema.getRowCount());
                    metadata.setHasPrimaryKey(!schema.getPrimaryKeys().isEmpty());
                    
                    // 检测时间字段
                    detectTimeField(schema, metadata);
                    
                    // 分类字段
                    classifyFields(schema, metadata);
                    
                    metadata.setLastSyncTime(LocalDateTime.now());

                    // 检查是否已存在
                    TableMetadata existing = tableMetadataMapper.selectOne(
                        new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<TableMetadata>()
                            .eq(TableMetadata::getDatasourceId, datasourceId)
                            .eq(TableMetadata::getTableName, tableName)
                    );

                    if (existing != null) {
                        metadata.setId(existing.getId());
                        tableMetadataMapper.updateById(metadata);
                    } else {
                        tableMetadataMapper.insert(metadata);
                    }

                } catch (Exception e) {
                    log.error("同步表{}元数据失败", tableName, e);
                }
            }

            log.info("数据源{}元数据同步完成", datasourceId);

        } catch (Exception e) {
            log.error("同步元数据失败", e);
        }
    }

    @Override
    public String checkHealth(Long datasourceId) {
        try {
            DataSourceConfig config = getById(datasourceId);
            boolean healthy = testConnection(config);

            String status = healthy ? "healthy" : "unhealthy";

            // 更新健康状态
            config.setHealthStatus(status);
            config.setLastCheckTime(LocalDateTime.now());
            updateById(config);

            return status;
        } catch (Exception e) {
            log.error("健康检查失败", e);
            return "unhealthy";
        }
    }

    /**
     * 获取或创建数据源
     */
    public DataSource getOrCreateDataSource(DataSourceConfig config) {
        return dataSourcePool.computeIfAbsent(config.getId(), id -> {
            return createHikariDataSource(config);
        });
    }

    /**
     * 创建数据源
     */
    private DataSource createDataSource(DataSourceConfig config) {
        if ("mysql".equalsIgnoreCase(config.getType())) {
            return createHikariDataSource(config);
        }
        throw new UnsupportedOperationException("不支持的数据源类型: " + config.getType());
    }

    /**
     * 创建HikariCP数据源
     */
    private HikariDataSource createHikariDataSource(DataSourceConfig config) {
        HikariConfig hikari = new HikariConfig();

        String jdbcUrl = String.format(
            "jdbc:mysql://%s:%d/%s?useUnicode=true&characterEncoding=utf8&useSSL=false&serverTimezone=Asia/Shanghai",
            config.getHost(),
            config.getPort(),
            config.getDatabaseName()
        );

        hikari.setJdbcUrl(jdbcUrl);
        hikari.setUsername(config.getUsername());
        hikari.setPassword(config.getPassword());
        hikari.setDriverClassName("com.mysql.cj.jdbc.Driver");

        // 连接池配置
        hikari.setMaximumPoolSize(5);  // 限制连接数
        hikari.setMinimumIdle(1);
        hikari.setConnectionTimeout(10000);
        hikari.setIdleTimeout(600000);
        hikari.setMaxLifetime(1800000);
        hikari.setReadOnly(true);  // 只读模式,更安全

        return new HikariDataSource(hikari);
    }

    /**
     * 判断字段类型
     */
    private String determineFieldType(String dataType) {
        dataType = dataType.toUpperCase();

        if (dataType.contains("INT") || dataType.contains("DECIMAL") || 
            dataType.contains("FLOAT") || dataType.contains("DOUBLE")) {
            return "numeric";
        } else if (dataType.contains("DATE") || dataType.contains("TIME")) {
            return "datetime";
        } else if (dataType.contains("CHAR") || dataType.contains("TEXT")) {
            return "text";
        } else {
            return "text";
        }
    }

    /**
     * 检测时间字段
     */
    private void detectTimeField(TableSchemaVO schema, TableMetadata metadata) {
        for (TableSchemaVO.ColumnSchema column : schema.getColumns()) {
            if ("datetime".equals(column.getFieldType())) {
                metadata.setHasTimeField(true);
                metadata.setTimeFieldName(column.getColumnName());
                break;
            }
        }
    }

    /**
     * 分类字段
     */
    private void classifyFields(TableSchemaVO schema, TableMetadata metadata) {
        List<String> numericFields = new ArrayList<>();
        List<String> categoryFields = new ArrayList<>();

        for (TableSchemaVO.ColumnSchema column : schema.getColumns()) {
            if ("numeric".equals(column.getFieldType())) {
                numericFields.add(column.getColumnName());
            } else if ("text".equals(column.getFieldType())) {
                categoryFields.add(column.getColumnName());
            }
        }

        metadata.setNumericFields(String.join(",", numericFields));
        metadata.setCategoryFields(String.join(",", categoryFields));
    }

    /**
     * 转换Schema为JSON
     */
    private String convertSchemaToJson(TableSchemaVO schema) {
        try {
            return objectMapper.writeValueAsString(schema);
        } catch (Exception e) {
            log.error("转换Schema为JSON失败", e);
            return "{}";
        }
    }
}
