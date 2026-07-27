package com.moyun.agent.util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * SQL工具类
 * 
 * @author laomao
 */
public class SqlUtils {
    
    private static final Logger log = LoggerFactory.getLogger(SqlUtils.class);
    
    /**
     * 清理SQL（去除markdown等格式）
     */
    public static String cleanSql(String sql) {
        if (sql == null) {
            return "";
        }
        
        // 移除markdown代码块标记
        sql = sql.replaceAll("```sql\\s*", "");
        sql = sql.replaceAll("```\\s*", "");
        
        // 移除注释
        sql = sql.replaceAll("--.*", "");
        sql = sql.replaceAll("/\\*.*?\\*/", "");
        
        // 移除多余空白
        sql = sql.replaceAll("\\s+", " ").trim();
        
        // 确保以分号结尾
        if (!sql.endsWith(";")) {
            sql += ";";
        }
        
        return sql;
    }
    
    /**
     * 确保SQL有LIMIT限制（对聚合查询除外）
     */
    public static String ensureLimit(String sql, int maxRows) {
        if (sql == null || sql.isEmpty()) {
            return sql;
        }
        
        String upperSql = sql.toUpperCase();
        
        // 检查是否是聚合查询（COUNT, SUM, AVG, MAX, MIN）
        // 如果SELECT后面直接是聚合函数，且没有GROUP BY，则不需要LIMIT
        if (isAggregateQuery(sql)) {
            return sql;
        }
        
        // 检查是否已经有LIMIT
        if (upperSql.contains("LIMIT")) {
            return sql;
        }
        
        // 添加LIMIT
        if (sql.endsWith(";")) {
            sql = sql.substring(0, sql.length() - 1) + " LIMIT " + maxRows + ";";
        } else {
            sql = sql + " LIMIT " + maxRows;
        }
        
        return sql;
    }
    
    /**
     * 判断是否是聚合查询或分析查询（不需要LIMIT）
     */
    private static boolean isAggregateQuery(String sql) {
        String upperSql = sql.toUpperCase().trim();
        
        // 1. 检查是否包含聚合函数
        Pattern aggregatePattern = Pattern.compile(
            "SELECT.*?(COUNT|SUM|AVG|MAX|MIN)\\s*\\(",
            Pattern.CASE_INSENSITIVE | Pattern.DOTALL
        );
        Matcher matcher = aggregatePattern.matcher(upperSql);
        
        if (matcher.find()) {
            return true; // 任何包含聚合函数的查询都不需要LIMIT
        }
        
        // 2. 检查是否有GROUP BY（分组统计，不需要LIMIT）
        if (upperSql.contains("GROUP BY")) {
            return true;
        }
        
        return false;
    }
    
    /**
     * 判断SQL是否安全
     * 
     * <p>使用增强的 SqlSecurityValidator 进行多层次安全检查</p>
     */
    public static boolean isSafeSql(String sql) {
        SqlSecurityValidator.ValidationResult result = SqlSecurityValidator.validate(sql);
        
        if (!result.isValid()) {
            log.warn("[SQL安全检查] 失败: {} (风险级别: {}), SQL: {}", 
                result.getMessage(), 
                result.getRiskLevel(),
                SqlSecurityValidator.sanitizeForLog(sql));
        }
        
        return result.isValid();
    }
    
    /**
     * 带表名白名单的 SQL 安全检查
     * 
     * @param sql SQL 语句
     * @param allowedTables 允许访问的表名集合
     * @return 是否安全
     */
    public static boolean isSafeSql(String sql, java.util.Set<String> allowedTables) {
        SqlSecurityValidator.ValidationResult result = SqlSecurityValidator.validate(sql, allowedTables);
        
        if (!result.isValid()) {
            log.warn("[SQL安全检查] 失败: {} (风险级别: {}), SQL: {}", 
                result.getMessage(), 
                result.getRiskLevel(),
                SqlSecurityValidator.sanitizeForLog(sql));
        }
        
        return result.isValid();
    }
    
    /**
     * 获取详细的 SQL 验证结果
     */
    public static SqlSecurityValidator.ValidationResult validateSql(String sql) {
        return SqlSecurityValidator.validate(sql);
    }
    
    /**
     * 统计子查询层级
     */
    private static int countSubQueries(String sql) {
        int maxDepth = 0;
        int currentDepth = 0;
        
        for (int i = 0; i < sql.length(); i++) {
            char c = sql.charAt(i);
            if (c == '(') {
                currentDepth++;
                maxDepth = Math.max(maxDepth, currentDepth);
            } else if (c == ')') {
                currentDepth--;
            }
        }
        
        return maxDepth;
    }
    
    /**
     * 解析查询类型
     */
    public static String parseQueryType(String sql) {
        if (sql == null || sql.isEmpty()) {
            return "UNKNOWN";
        }
        
        String upperSql = sql.toUpperCase().trim();
        
        if (upperSql.startsWith("SELECT")) {
            if (upperSql.contains("COUNT(") || upperSql.contains("SUM(") || 
                upperSql.contains("AVG(") || upperSql.contains("MAX(") || 
                upperSql.contains("MIN(")) {
                return "AGGREGATE";
            }
            return "SELECT";
        }
        
        return "UNKNOWN";
    }
}
