package com.moyun.ext.ai.engine.workflow.node;

import com.fasterxml.jackson.core.type.TypeReference;
import com.moyun.ext.ai.util.JsonUtils;
import com.moyun.ext.ai.engine.workflow.WorkflowContext;
import com.moyun.ext.ai.engine.workflow.WorkflowNode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 数据库节点执行器
 *
 * <p>支持执行SQL查询和更新操作</p>
 * <p>支持的操作类型：</p>
 * <ul>
 *     <li>query - 查询操作，返回结果集</li>
 *     <li>update - 更新操作，返回影响行数</li>
 *     <li>insert - 插入操作，返回影响行数</li>
 *     <li>delete - 删除操作，返回影响行数</li>
 * </ul>
 *
 * <p><b>安全机制：</b>SQL 语句中禁止直接拼接变量值，所有动态值必须通过 paramsJson 以
 * 参数化方式（? 占位符）传入，防止 SQL 注入。变量替换仅允许用于 SQL 的静态片段
 * （如动态表名前缀），但不允许拼接用户可控的值。</p>
 *
 * @author laomao
 * @since 2025-12-12
 */
@Slf4j
@Component
public class DatabaseNodeExecutor extends BaseNodeExecutor {

    @Autowired(required = false)
    private JdbcTemplate jdbcTemplate;

    @Override
    public String getType() {
        return "database";
    }

    @Override
    public NodeResult execute(WorkflowNode node, WorkflowContext context) {
        Map<String, Object> config = node.getConfig();
        if (config == null) {
            return NodeResult.fail("数据库节点配置为空");
        }

        // 检查数据库连接
        if (jdbcTemplate == null) {
            return NodeResult.fail("数据库连接未配置");
        }

        try {
            // 获取配置
            String operation = (String) config.getOrDefault("operation", "query");
            String sql = (String) config.get("sql");
            String outputVariable = (String) config.getOrDefault("outputVariable", "db_result");

            if (sql == null || sql.trim().isEmpty()) {
                return NodeResult.fail("SQL语句为空");
            }

            // 安全检查：禁止 SQL 中直接拼接变量值（{{var}}），必须使用 ? 占位符 + paramsJson
            if (containsVariableReference(sql)) {
                return NodeResult.fail("SQL 语句中禁止直接使用 {{var}} 拼接变量值（防止 SQL 注入），"
                        + "请使用 ? 占位符并在 paramsJson 中以参数形式传入");
            }

            // 获取参数（paramsJson 中的变量引用会被解析替换）
            Object[] params = getParams(config, context);

            log.info("🗃️ 数据库节点执行: operation={}, sql={}", operation,
                    sql.length() > 100 ? sql.substring(0, 100) + "..." : sql);

            Object result;
            switch (operation.toLowerCase()) {
                case "query":
                    result = executeQuery(sql, params);
                    break;
                case "update":
                case "insert":
                case "delete":
                    result = executeUpdate(sql, params);
                    break;
                default:
                    return NodeResult.fail("不支持的操作类型: " + operation);
            }

            log.info("🗃️ 数据库执行完成: result={}",
                    result instanceof List ? ((List<?>) result).size() + " 条记录" : result);

            // 设置输出变量
            context.setVariable(outputVariable, result);

            return NodeResult.success(result);

        } catch (Exception e) {
            log.error("数据库节点执行失败", e);
            return NodeResult.fail("数据库执行失败: " + e.getMessage());
        }
    }

    /**
     * 检测字符串中是否包含变量引用 {{var}}
     */
    private boolean containsVariableReference(String text) {
        if (text == null) return false;
        return text.indexOf("{{") != -1 && text.indexOf("}}") != -1
                && java.util.regex.Pattern.compile("\\{\\{\\s*\\w+\\s*\\}\\}").matcher(text).find();
    }

    /**
     * 执行查询操作
     */
    private List<Map<String, Object>> executeQuery(String sql, Object[] params) {
        if (params != null && params.length > 0) {
            return jdbcTemplate.queryForList(sql, params);
        }
        return jdbcTemplate.queryForList(sql);
    }

    /**
     * 执行更新操作
     */
    private int executeUpdate(String sql, Object[] params) {
        if (params != null && params.length > 0) {
            return jdbcTemplate.update(sql, params);
        }
        return jdbcTemplate.update(sql);
    }

    /**
     * 获取SQL参数
     */
    private Object[] getParams(Map<String, Object> config, WorkflowContext context) {
        String paramsJson = (String) config.get("paramsJson");
        if (paramsJson == null || paramsJson.trim().isEmpty()) {
            return null;
        }

        // 替换变量（参数值是受控的，会作为预编译参数传入，安全）
        paramsJson = replaceVariables(paramsJson, context);

        // 解析JSON数组
        List<Object> paramList = JsonUtils.fromJson(paramsJson, new TypeReference<List<Object>>() {});
        if (paramList == null || paramList.isEmpty()) {
            return null;
        }

        return paramList.toArray();
    }
}
