package com.moyun.ext.ai.constant;

import java.util.List;

/**
 * 数据分析模块常量定义
 *
 * @author laomao
 */
public class DataAnalysisConstants {

    /**
     * 默认查询最大行数
     */
    public static final int DEFAULT_MAX_ROWS = 1000;

    /**
     * 查询超时时间(秒)
     */
    public static final int QUERY_TIMEOUT_SECONDS = 30;

    /**
     * 最大连接池大小
     */
    public static final int MAX_POOL_SIZE = 5;

    /**
     * 最小空闲连接数
     */
    public static final int MIN_IDLE = 1;

    /**
     * SQL安全 - 禁止的关键词
     */
    public static final List<String> FORBIDDEN_SQL_KEYWORDS = List.of(
        "DROP", "DELETE", "TRUNCATE", "UPDATE", "INSERT",
        "ALTER", "CREATE", "GRANT", "REVOKE", "EXEC", "EXECUTE"
    );

    /**
     * 数据源类型
     */
    public static class DataSourceType {
        public static final String MYSQL = "mysql";
        public static final String ELASTICSEARCH = "elasticsearch";
        public static final String MONGODB = "mongodb";
    }

    /**
     * 查询类型
     */
    public static class QueryType {
        public static final String SELECT = "select";
        public static final String AGGREGATE = "aggregate";
        public static final String JOIN = "join";
        public static final String RANKING = "ranking";
        public static final String ANALYSIS = "analysis";
    }

    /**
     * 字段类型
     */
    public static class FieldType {
        public static final String NUMERIC = "numeric";
        public static final String DATETIME = "datetime";
        public static final String CATEGORY = "category";
        public static final String TEXT = "text";
    }

    /**
     * 图表类型
     */
    public static class ChartType {
        public static final String LINE = "line";
        public static final String BAR = "bar";
        public static final String PIE = "pie";
        public static final String SCATTER = "scatter";
        public static final String RADAR = "radar";
        public static final String HEATMAP = "heatmap";
        public static final String TABLE = "table";
    }

    /**
     * 洞察类型
     */
    public static class InsightType {
        public static final String OVERVIEW = "overview";
        public static final String ANOMALY = "anomaly";
        public static final String TREND = "trend";
        public static final String CORRELATION = "correlation";
        public static final String PATTERN = "pattern";
    }

    /**
     * 严重程度
     */
    public static class Severity {
        public static final String LOW = "low";
        public static final String MEDIUM = "medium";
        public static final String HIGH = "high";
    }

    /**
     * 健康状态
     */
    public static class HealthStatus {
        public static final String HEALTHY = "healthy";
        public static final String UNHEALTHY = "unhealthy";
        public static final String UNKNOWN = "unknown";
    }

    /**
     * 查询状态
     */
    public static class QueryStatus {
        public static final String SUCCESS = "success";
        public static final String FAILED = "failed";
        public static final String TIMEOUT = "timeout";
    }
}
