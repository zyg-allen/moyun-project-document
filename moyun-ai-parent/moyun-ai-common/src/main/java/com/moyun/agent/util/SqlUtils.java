package com.moyun.agent.util;

import com.moyun.agent.constant.DataAnalysisConstants;

import java.util.regex.Pattern;

/**
 * SQL工具类
 *
 * @author laomao
 */
public class SqlUtils {

    /**
     * SQL注入检测模式
     */
    private static final Pattern[] INJECTION_PATTERNS = {
        Pattern.compile("(;|')\\s*(DROP|DELETE|UPDATE|INSERT)", Pattern.CASE_INSENSITIVE),
        Pattern.compile("--", Pattern.CASE_INSENSITIVE),
        Pattern.compile("/\\*.*\\*/", Pattern.CASE_INSENSITIVE),
        Pattern.compile("UNION\\s+SELECT", Pattern.CASE_INSENSITIVE),
        Pattern.compile("OR\\s+1\\s*=\\s*1", Pattern.CASE_INSENSITIVE)
    };

    /**
     * 检查SQL是否安全
     *
     * @param sql SQL语句
     * @return 是否安全
     */
    public static boolean isSafeSql(String sql) {
        if (sql == null || sql.trim().isEmpty()) {
            return false;
        }

        String upperSql = sql.toUpperCase();

        // 1. 必须是SELECT开头
        if (!upperSql.trim().startsWith("SELECT")) {
            return false;
        }

        // 2. 不能包含危险关键词
        for (String keyword : DataAnalysisConstants.FORBIDDEN_SQL_KEYWORDS) {
            if (upperSql.contains(keyword)) {
                return false;
            }
        }

        // 3. 检查注入模式
        for (Pattern pattern : INJECTION_PATTERNS) {
            if (pattern.matcher(sql).find()) {
                return false;
            }
        }

        return true;
    }

    /**
     * 清理SQL语句
     * 去除markdown代码块、注释等
     *
     * @param sql 原始SQL
     * @return 清理后的SQL
     */
    public static String cleanSql(String sql) {
        if (sql == null) {
            return "";
        }

        // 去除markdown代码块
        sql = sql.replaceAll("```sql\\s*", "");
        sql = sql.replaceAll("```\\s*", "");

        // 去除SQL: 前缀
        sql = sql.replaceAll("(?i)^SQL:\\s*", "");

        // 只保留第一个SQL语句(以分号结尾)
        int semicolonIndex = sql.indexOf(';');
        if (semicolonIndex > 0) {
            sql = sql.substring(0, semicolonIndex + 1);
        }

        return sql.trim();
    }

    /**
     * 确保SQL有LIMIT子句
     *
     * @param sql 原始SQL
     * @param maxRows 最大行数
     * @return 添加LIMIT后的SQL
     */
    public static String ensureLimit(String sql, int maxRows) {
        if (sql == null) {
            return "";
        }

        String upperSql = sql.toUpperCase();

        if (!upperSql.contains("LIMIT")) {
            // 移除末尾的分号
            sql = sql.replaceAll(";\\s*$", "");
            // 添加LIMIT
            sql += " LIMIT " + maxRows;
        }

        return sql;
    }

    /**
     * 解析SQL查询类型
     *
     * @param sql SQL语句
     * @return 查询类型
     */
    public static String parseQueryType(String sql) {
        if (sql == null) {
            return DataAnalysisConstants.QueryType.SELECT;
        }

        String upperSql = sql.toUpperCase();

        if (upperSql.contains("JOIN")) {
            return DataAnalysisConstants.QueryType.JOIN;
        } else if (upperSql.contains("GROUP BY") || 
                   upperSql.contains("COUNT") ||
                   upperSql.contains("SUM") || 
                   upperSql.contains("AVG")) {
            return DataAnalysisConstants.QueryType.AGGREGATE;
        } else if (upperSql.contains("ORDER BY") && upperSql.contains("LIMIT")) {
            return DataAnalysisConstants.QueryType.RANKING;
        } else {
            return DataAnalysisConstants.QueryType.SELECT;
        }
    }

    /**
     * 转义SQL标识符
     *
     * @param identifier 标识符(表名/字段名)
     * @return 转义后的标识符
     */
    public static String escapeIdentifier(String identifier) {
        if (identifier == null) {
            return null;
        }
        // MySQL使用反引号
        return "`" + identifier.replace("`", "``") + "`";
    }

    /**
     * 格式化SQL语句
     *
     * @param sql SQL语句
     * @return 格式化后的SQL
     */
    public static String formatSql(String sql) {
        if (sql == null) {
            return "";
        }

        // 移除多余的空格
        sql = sql.replaceAll("\\s+", " ").trim();

        // 关键词换行(简单实现)
        sql = sql.replaceAll("(?i)\\s+(FROM|WHERE|GROUP BY|ORDER BY|LIMIT)\\s+", "\n$1 ");

        return sql;
    }
}
