package com.moyun.agent.engine.workflow.node;

import com.fasterxml.jackson.core.type.TypeReference;
import com.moyun.agent.util.JsonUtils;
import com.moyun.agent.engine.workflow.WorkflowContext;
import com.moyun.agent.engine.workflow.WorkflowNode;
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

            // 替换SQL中的变量
            sql = replaceVariables(sql, context);

            // 获取参数
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

        // 替换变量
        paramsJson = replaceVariables(paramsJson, context);

        // 解析JSON数组
        List<Object> paramList = JsonUtils.fromJson(paramsJson, new TypeReference<List<Object>>() {});
        if (paramList == null || paramList.isEmpty()) {
            return null;
        }

        return paramList.toArray();
    }
}
