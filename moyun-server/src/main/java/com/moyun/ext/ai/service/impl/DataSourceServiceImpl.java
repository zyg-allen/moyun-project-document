package com.moyun.ext.ai.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.moyun.ext.ai.entity.DataSourceConfig;
import com.moyun.ext.ai.exception.BusinessException;
import com.moyun.ext.ai.exception.ErrorCode;
import com.moyun.ext.ai.entity.TableMetadata;
import com.moyun.ext.ai.mapper.DataSourceConfigMapper;
import com.moyun.ext.ai.mapper.TableMetadataMapper;
import com.moyun.ext.ai.service.DataSourceService;
import com.moyun.ext.ai.vo.TableInfoVO;
import com.moyun.ext.ai.vo.TableSchemaVO;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

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
        return testMySQLConnection(config);
    }

    /**
     * 重写 save，兜底设置时间戳（P0-1）。
     * <p>DataSourceConfig 经 {@code @RequestBody} Jackson 反序列化后，
     * MyBatis-Plus 的 strictInsertFill 可能不生效，需显式赋值防止
     * "Column 'create_time' cannot be null" 故障。</p>
     */
    @Override
    public boolean save(DataSourceConfig entity) {
        if (entity != null) {
            LocalDateTime now = LocalDateTime.now();
            if (entity.getCreateTime() == null) {
                entity.setCreateTime(now);
            }
            if (entity.getUpdateTime() == null) {
                entity.setUpdateTime(now);
            }
        }
        return super.save(entity);
    }

    /**
     * 重写 updateById，兜底设置时间戳（P0-1）。
     */
    @Override
    public boolean updateById(DataSourceConfig entity) {
        if (entity != null && entity.getUpdateTime() == null) {
            entity.setUpdateTime(LocalDateTime.now());
        }
        return super.updateById(entity);
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
    
    @Override
    public List<String> listTables(Long datasourceId) {
        List<String> tables = new ArrayList<>();

        try {
            DataSourceConfig config = getById(datasourceId);
            return listMySQLTables(config);
        } catch (Exception e) {
            log.error("获取表列表失败", e);
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
    
    @Override
    public List<TableInfoVO> listTablesWithInfo(Long datasourceId) {
        List<TableInfoVO> tables = new ArrayList<>();

        try {
            DataSourceConfig config = getById(datasourceId);

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
    public com.moyun.ext.ai.vo.DataSourcePoolStatus getPoolStatus(Long datasourceId) {
        DataSourceConfig config = getById(datasourceId);
        if (config == null) {
            return null;
        }
        
        HikariDataSource ds = dataSourcePool.get(datasourceId);
        if (ds == null) {
            return com.moyun.ext.ai.vo.DataSourcePoolStatus.builder()
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
            
            return com.moyun.ext.ai.vo.DataSourcePoolStatus.builder()
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
            "jdbc:mysql://%s:%d/%s?useUnicode=true&characterEncoding=utf8&useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=Asia/Shanghai",
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
