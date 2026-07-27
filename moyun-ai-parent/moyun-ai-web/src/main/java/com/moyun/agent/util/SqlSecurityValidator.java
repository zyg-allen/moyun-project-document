package com.moyun.agent.util;

import lombok.extern.slf4j.Slf4j;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * SQL 安全验证器
 * 
 * <p>提供多层次的 SQL 安全检查，防止 SQL 注入和危险操作</p>
 * 
 * <p>检查层级：</p>
 * <ul>
 *   <li>1. 基础语法检查：只允许 SELECT 语句</li>
 *   <li>2. 危险关键字检查：DDL/DML/系统命令</li>
 *   <li>3. 注入攻击检测：注释、堆叠查询、编码绕过</li>
 *   <li>4. 复杂度限制：子查询层级、语句长度</li>
 *   <li>5. 表名白名单：可选的表访问控制</li>
 * </ul>
 *
 * @author laomao
 * @since 2025-12-12
 */
@Slf4j
public class SqlSecurityValidator {

    /** 最大 SQL 长度 */
    private static final int MAX_SQL_LENGTH = 10000;

    /** 最大子查询层级 */
    private static final int MAX_SUBQUERY_DEPTH = 3;

    /** 最大 JOIN 数量 */
    private static final int MAX_JOIN_COUNT = 5;

    /** 危险关键字（DDL） */
    private static final Set<String> DDL_KEYWORDS = Set.of(
        "CREATE", "ALTER", "DROP", "TRUNCATE", "RENAME", "COMMENT"
    );

    /** 危险关键字（DML - 修改数据） */
    private static final Set<String> DML_KEYWORDS = Set.of(
        "INSERT", "UPDATE", "DELETE", "REPLACE", "MERGE"
    );

    /** 危险关键字（系统命令） */
    private static final Set<String> SYSTEM_KEYWORDS = Set.of(
        "GRANT", "REVOKE", "EXEC", "EXECUTE", "CALL", "PREPARE",
        "DEALLOCATE", "SET", "SHOW", "DESCRIBE", "EXPLAIN", "USE",
        "LOCK", "UNLOCK", "FLUSH", "RESET", "PURGE", "HANDLER",
        "LOAD", "OUTFILE", "DUMPFILE", "LOAD_FILE", "BENCHMARK",
        "SLEEP", "WAITFOR", "SHUTDOWN", "KILL"
    );

    /** 危险关键字（数据导出） */
    private static final Set<String> EXPORT_KEYWORDS = Set.of(
        "INTO OUTFILE", "INTO DUMPFILE", "LOAD_FILE", "LOAD DATA"
    );

    /** SQL 注入特征模式 */
    private static final List<Pattern> INJECTION_PATTERNS = Arrays.asList(
        // 注释注入: -- 或 /* */
        Pattern.compile("--[\\s\\S]*$", Pattern.MULTILINE),
        Pattern.compile("/\\*[\\s\\S]*?\\*/"),
        
        // 堆叠查询: ; 后跟其他语句
        Pattern.compile(";\\s*(?:SELECT|INSERT|UPDATE|DELETE|DROP|CREATE|ALTER|EXEC)", Pattern.CASE_INSENSITIVE),
        
        // 十六进制编码绕过
        Pattern.compile("0x[0-9a-fA-F]+"),
        
        // CHAR 函数注入
        Pattern.compile("CHAR\\s*\\([\\d,\\s]+\\)", Pattern.CASE_INSENSITIVE),
        
        // 条件永真: OR 1=1, OR 'a'='a'
        Pattern.compile("\\bOR\\s+['\"]?\\w+['\"]?\\s*=\\s*['\"]?\\w+['\"]?", Pattern.CASE_INSENSITIVE),
        Pattern.compile("\\bOR\\s+1\\s*=\\s*1", Pattern.CASE_INSENSITIVE),
        Pattern.compile("\\bOR\\s+['\"]\\w*['\"]\\s*=\\s*['\"]\\w*['\"]", Pattern.CASE_INSENSITIVE),
        
        // 条件永假+UNION: AND 1=0 UNION
        Pattern.compile("\\bAND\\s+1\\s*=\\s*0\\s+UNION", Pattern.CASE_INSENSITIVE),
        
        // 时间盲注
        Pattern.compile("\\bSLEEP\\s*\\(\\s*\\d+\\s*\\)", Pattern.CASE_INSENSITIVE),
        Pattern.compile("\\bBENCHMARK\\s*\\(", Pattern.CASE_INSENSITIVE),
        Pattern.compile("\\bWAITFOR\\s+DELAY", Pattern.CASE_INSENSITIVE),
        
        // 系统函数调用
        Pattern.compile("\\bUSER\\s*\\(\\s*\\)", Pattern.CASE_INSENSITIVE),
        Pattern.compile("\\bDATABASE\\s*\\(\\s*\\)", Pattern.CASE_INSENSITIVE),
        Pattern.compile("\\bVERSION\\s*\\(\\s*\\)", Pattern.CASE_INSENSITIVE),
        Pattern.compile("\\b@@\\w+", Pattern.CASE_INSENSITIVE)  // MySQL系统变量
    );

    /**
     * 验证结果
     */
    public static class ValidationResult {
        private final boolean valid;
        private final String message;
        private final String riskLevel; // LOW, MEDIUM, HIGH, CRITICAL

        private ValidationResult(boolean valid, String message, String riskLevel) {
            this.valid = valid;
            this.message = message;
            this.riskLevel = riskLevel;
        }

        public static ValidationResult success() {
            return new ValidationResult(true, "验证通过", "LOW");
        }

        public static ValidationResult fail(String message, String riskLevel) {
            return new ValidationResult(false, message, riskLevel);
        }

        public boolean isValid() { return valid; }
        public String getMessage() { return message; }
        public String getRiskLevel() { return riskLevel; }
    }

    /**
     * 完整的 SQL 安全验证
     * 
     * @param sql 待验证的 SQL
     * @return 验证结果
     */
    public static ValidationResult validate(String sql) {
        return validate(sql, null);
    }

    /**
     * 带表名白名单的 SQL 安全验证
     * 
     * @param sql 待验证的 SQL
     * @param allowedTables 允许访问的表名列表（可为 null 表示不限制）
     * @return 验证结果
     */
    public static ValidationResult validate(String sql, Set<String> allowedTables) {
        if (sql == null || sql.trim().isEmpty()) {
            return ValidationResult.fail("SQL 为空", "LOW");
        }

        // 预处理：标准化
        String normalizedSql = normalizeSql(sql);
        String upperSql = normalizedSql.toUpperCase();

        log.debug("[SQL安全验证] 原始: {}", sql);
        log.debug("[SQL安全验证] 标准化: {}", normalizedSql);

        // 1. 长度检查
        if (sql.length() > MAX_SQL_LENGTH) {
            return ValidationResult.fail("SQL 长度超出限制 (" + MAX_SQL_LENGTH + ")", "MEDIUM");
        }

        // 2. 基础语法检查：只允许 SELECT
        if (!upperSql.trim().startsWith("SELECT")) {
            return ValidationResult.fail("只允许 SELECT 查询语句", "HIGH");
        }

        // 3. DDL 关键字检查
        for (String keyword : DDL_KEYWORDS) {
            if (containsKeyword(upperSql, keyword)) {
                log.warn("[SQL安全验证] 检测到 DDL 关键字: {}", keyword);
                return ValidationResult.fail("包含禁止的 DDL 操作: " + keyword, "CRITICAL");
            }
        }

        // 4. DML 关键字检查
        for (String keyword : DML_KEYWORDS) {
            if (containsKeyword(upperSql, keyword)) {
                log.warn("[SQL安全验证] 检测到 DML 关键字: {}", keyword);
                return ValidationResult.fail("包含禁止的 DML 操作: " + keyword, "CRITICAL");
            }
        }

        // 5. 系统命令检查
        for (String keyword : SYSTEM_KEYWORDS) {
            if (containsKeyword(upperSql, keyword)) {
                log.warn("[SQL安全验证] 检测到系统命令: {}", keyword);
                return ValidationResult.fail("包含禁止的系统命令: " + keyword, "CRITICAL");
            }
        }

        // 6. 数据导出检查
        for (String keyword : EXPORT_KEYWORDS) {
            if (upperSql.contains(keyword)) {
                log.warn("[SQL安全验证] 检测到数据导出操作: {}", keyword);
                return ValidationResult.fail("包含禁止的数据导出操作: " + keyword, "CRITICAL");
            }
        }

        // 7. UNION 检查（可能的注入攻击）
        if (containsKeyword(upperSql, "UNION") && !isLegitimateUnion(normalizedSql)) {
            log.warn("[SQL安全验证] 检测到可疑的 UNION 操作");
            return ValidationResult.fail("包含可疑的 UNION 操作", "HIGH");
        }

        // 8. SQL 注入特征检测
        for (Pattern pattern : INJECTION_PATTERNS) {
            Matcher matcher = pattern.matcher(sql);
            if (matcher.find()) {
                String matched = matcher.group();
                log.warn("[SQL安全验证] 检测到注入特征: {}", matched);
                return ValidationResult.fail("检测到可疑的 SQL 注入特征", "CRITICAL");
            }
        }

        // 9. 子查询层级检查
        int subqueryDepth = countSubqueryDepth(normalizedSql);
        if (subqueryDepth > MAX_SUBQUERY_DEPTH) {
            return ValidationResult.fail("子查询层级过深 (" + subqueryDepth + " > " + MAX_SUBQUERY_DEPTH + ")", "MEDIUM");
        }

        // 10. JOIN 数量检查
        int joinCount = countJoins(upperSql);
        if (joinCount > MAX_JOIN_COUNT) {
            return ValidationResult.fail("JOIN 数量过多 (" + joinCount + " > " + MAX_JOIN_COUNT + ")", "MEDIUM");
        }

        // 11. 表名白名单检查
        if (allowedTables != null && !allowedTables.isEmpty()) {
            Set<String> usedTables = extractTableNames(normalizedSql);
            for (String table : usedTables) {
                if (!allowedTables.contains(table.toLowerCase())) {
                    log.warn("[SQL安全验证] 访问了未授权的表: {}", table);
                    return ValidationResult.fail("无权访问表: " + table, "HIGH");
                }
            }
        }

        log.info("[SQL安全验证] 通过: {}", sql.length() > 100 ? sql.substring(0, 100) + "..." : sql);
        return ValidationResult.success();
    }

    /**
     * 快速验证（简化版）
     */
    public static boolean isValid(String sql) {
        return validate(sql).isValid();
    }

    /**
     * 标准化 SQL
     */
    private static String normalizeSql(String sql) {
        // 移除多余空白
        sql = sql.replaceAll("\\s+", " ").trim();
        // 移除末尾分号
        if (sql.endsWith(";")) {
            sql = sql.substring(0, sql.length() - 1);
        }
        return sql;
    }

    /**
     * 检查是否包含关键字（单词边界匹配）
     */
    private static boolean containsKeyword(String sql, String keyword) {
        Pattern pattern = Pattern.compile("\\b" + Pattern.quote(keyword) + "\\b", Pattern.CASE_INSENSITIVE);
        return pattern.matcher(sql).find();
    }

    /**
     * 判断是否是合法的 UNION（用于合并查询结果）
     */
    private static boolean isLegitimateUnion(String sql) {
        // 简单判断：如果 UNION 后面紧跟着 SELECT，可能是合法的
        // 但如果是在 WHERE 条件中，则很可能是注入
        String upperSql = sql.toUpperCase();
        int unionIndex = upperSql.indexOf("UNION");
        if (unionIndex < 0) return true;
        
        // 检查 UNION 前面是否有完整的 SELECT 语句
        String beforeUnion = upperSql.substring(0, unionIndex).trim();
        // 合法的 UNION 前面应该是完整的查询（以 ) 或字段/表名结尾）
        return beforeUnion.endsWith(")") || beforeUnion.matches(".*\\w$");
    }

    /**
     * 计算子查询层级
     */
    private static int countSubqueryDepth(String sql) {
        int maxDepth = 0;
        int currentDepth = 0;
        boolean inString = false;
        char stringChar = 0;

        for (int i = 0; i < sql.length(); i++) {
            char c = sql.charAt(i);
            
            // 处理字符串
            if (c == '\'' || c == '"') {
                if (!inString) {
                    inString = true;
                    stringChar = c;
                } else if (c == stringChar) {
                    inString = false;
                }
                continue;
            }
            
            if (!inString) {
                if (c == '(') {
                    currentDepth++;
                    maxDepth = Math.max(maxDepth, currentDepth);
                } else if (c == ')') {
                    currentDepth = Math.max(0, currentDepth - 1);
                }
            }
        }
        
        return maxDepth;
    }

    /**
     * 统计 JOIN 数量
     */
    private static int countJoins(String sql) {
        int count = 0;
        Pattern pattern = Pattern.compile("\\b(?:INNER|LEFT|RIGHT|OUTER|CROSS|NATURAL)?\\s*JOIN\\b", Pattern.CASE_INSENSITIVE);
        Matcher matcher = pattern.matcher(sql);
        while (matcher.find()) {
            count++;
        }
        return count;
    }

    /**
     * 提取 SQL 中使用的表名
     */
    private static Set<String> extractTableNames(String sql) {
        Set<String> tables = new HashSet<>();
        
        // 匹配 FROM 和 JOIN 后面的表名
        Pattern pattern = Pattern.compile(
            "(?:FROM|JOIN)\\s+([`\\[]?\\w+[`\\]]?(?:\\s*\\.\\s*[`\\[]?\\w+[`\\]]?)?)",
            Pattern.CASE_INSENSITIVE
        );
        
        Matcher matcher = pattern.matcher(sql);
        while (matcher.find()) {
            String table = matcher.group(1)
                .replaceAll("[`\\[\\]]", "")  // 移除引号
                .toLowerCase();
            // 如果有 schema.table 格式，只取表名
            if (table.contains(".")) {
                table = table.substring(table.lastIndexOf(".") + 1);
            }
            tables.add(table);
        }
        
        return tables;
    }

    /**
     * 清理 SQL 中的危险内容（用于记录日志）
     */
    public static String sanitizeForLog(String sql) {
        if (sql == null) return "[null]";
        
        // 移除敏感信息
        sql = sql.replaceAll("'[^']*'", "'***'");
        sql = sql.replaceAll("\"[^\"]*\"", "\"***\"");
        
        // 截断过长内容
        if (sql.length() > 500) {
            sql = sql.substring(0, 500) + "...[truncated]";
        }
        
        return sql;
    }
}
