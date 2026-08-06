package com.moyun.ext.ai.engine.workflow.node;

import com.fasterxml.jackson.core.type.TypeReference;
import com.moyun.ext.ai.util.JsonUtils;
import com.moyun.ext.ai.engine.tool.ToolContext;
import com.moyun.ext.ai.engine.tool.ToolRegistry;
import com.moyun.ext.ai.engine.tool.ToolResult;
import com.moyun.ext.ai.engine.workflow.WorkflowContext;
import com.moyun.ext.ai.engine.workflow.WorkflowNode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 工具节点执行器
 *
 * <p>调用已注册的工具</p>
 *
 * @author laomao
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class ToolNodeExecutor extends BaseNodeExecutor {

    private final ToolRegistry toolRegistry;

    @Override
    public String getType() {
        return "tool";
    }

    @Override
    public NodeResult execute(WorkflowNode node, WorkflowContext context) {
        Map<String, Object> config = node.getConfig();
        if (config == null) {
            return NodeResult.fail("工具节点配置为空");
        }

        try {
            // 获取工具名称
            String toolName = (String) config.get("toolName");
            if (toolName == null || toolName.isEmpty()) {
                return NodeResult.fail("工具名称为空");
            }

            // 获取工具参数 - 优先使用 paramsJson
            Map<String, Object> params = new HashMap<>();
            String paramsJson = (String) config.get("paramsJson");
            if (paramsJson != null && !paramsJson.trim().isEmpty()) {
                // 先替换变量再解析JSON
                paramsJson = replaceVariables(paramsJson, context);
                Map<String, Object> parsedParams = JsonUtils.fromJson(paramsJson, new TypeReference<Map<String, Object>>() {});
                if (parsedParams != null) {
                    params = parsedParams;
                }
            }
            // 如果paramsJson为空，使用params对象
            if (params.isEmpty()) {
                @SuppressWarnings("unchecked")
                Map<String, Object> configParams = (Map<String, Object>) config.getOrDefault("params", new HashMap<>());
                params = configParams;
            }

            // 替换参数中的变量
            Map<String, Object> resolvedParams = new HashMap<>();
            for (Map.Entry<String, Object> entry : params.entrySet()) {
                Object value = entry.getValue();
                if (value instanceof String) {
                    value = replaceVariables((String) value, context);
                }
                resolvedParams.put(entry.getKey(), value);
            }

            String outputVariable = (String) config.getOrDefault("outputVariable", "tool_output");

            log.info("🔧 工具节点执行: tool={}, params={}", toolName, resolvedParams);

            // 检查工具是否存在
            if (!toolRegistry.hasTool(toolName)) {
                return NodeResult.fail("工具不存在: " + toolName);
            }

            // 构建工具上下文
            ToolContext toolContext = new ToolContext();
            toolContext.setWorkflowId(context.getWorkflowId());

            // 执行工具
            ToolResult result = toolRegistry.executeTool(toolName, toolContext, resolvedParams);

            if (result.isSuccess()) {
                log.info("🔧 工具执行成功: {}", result.getContent());
                context.setVariable(outputVariable, result.getContent());
                return NodeResult.success(result.getContent());
            } else {
                log.warn("🔧 工具执行失败: {}", result.getErrorMessage());
                return NodeResult.fail(result.getErrorMessage());
            }

        } catch (Exception e) {
            log.error("工具节点执行失败", e);
            return NodeResult.fail("工具执行失败: " + e.getMessage());
        }
    }

    // replaceVariables方法已移至BaseNodeExecutor
}
