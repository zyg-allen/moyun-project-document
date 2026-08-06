package com.moyun.ext.ai.engine.workflow.node;

import com.moyun.ext.ai.engine.workflow.NodeExecutor;
import com.moyun.ext.ai.engine.workflow.WorkflowContext;
import com.moyun.ext.ai.engine.workflow.WorkflowEngine;
import com.moyun.ext.ai.engine.workflow.WorkflowNode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 子工作流节点执行器
 *
 * <p>调用其他工作流作为子流程</p>
 *
 * @author laomao
 */
@Slf4j
@Component
public class SubWorkflowNodeExecutor implements NodeExecutor {

    private final WorkflowEngine workflowEngine;
    private static final Pattern VARIABLE_PATTERN = Pattern.compile("\\{\\{\\s*(\\w+)\\s*\\}\\}");

    public SubWorkflowNodeExecutor(@Lazy WorkflowEngine workflowEngine) {
        this.workflowEngine = workflowEngine;
    }

    @Override
    public String getType() {
        return "subflow";
    }

    @Override
    public NodeResult execute(WorkflowNode node, WorkflowContext context) {
        Map<String, Object> config = node.getConfig();
        if (config == null) {
            return NodeResult.fail("子工作流节点配置为空");
        }

        try {
            Object workflowIdObj = config.get("workflowId");
            @SuppressWarnings("unchecked")
            Map<String, String> inputMapping = (Map<String, String>) config.getOrDefault("inputMapping", new HashMap<>());
            @SuppressWarnings("unchecked")
            Map<String, String> outputMapping = (Map<String, String>) config.getOrDefault("outputMapping", new HashMap<>());
            String outputVariable = (String) config.getOrDefault("outputVariable", "subflow_output");
            boolean inheritVariables = Boolean.TRUE.equals(config.getOrDefault("inheritVariables", false));

            if (workflowIdObj == null) {
                return NodeResult.fail("未指定子工作流ID");
            }

            Long workflowId;
            if (workflowIdObj instanceof Number) {
                workflowId = ((Number) workflowIdObj).longValue();
            } else {
                workflowId = Long.parseLong(workflowIdObj.toString());
            }

            log.info("📦 子工作流调用: workflowId={}", workflowId);

            // 构建子工作流输入
            Map<String, Object> subInput = new HashMap<>();

            // 继承父工作流变量
            if (inheritVariables) {
                subInput.putAll(context.getVariables());
            }

            // 应用输入映射
            for (Map.Entry<String, String> entry : inputMapping.entrySet()) {
                String subVarName = entry.getKey();
                String parentExpression = entry.getValue();
                Object value = resolveExpression(parentExpression, context);
                subInput.put(subVarName, value);
            }

            // 执行子工作流
            WorkflowEngine.WorkflowResult result = workflowEngine.execute(workflowId, subInput);

            if (!result.isSuccess()) {
                log.warn("📦 子工作流执行失败: {}", result.getErrorMessage());
                return NodeResult.fail("子工作流执行失败: " + result.getErrorMessage());
            }

            log.info("📦 子工作流执行成功: duration={}ms", result.getDurationMs());

            // 应用输出映射
            Object subOutput = result.getOutput();
            if (subOutput instanceof Map) {
                @SuppressWarnings("unchecked")
                Map<String, Object> outputMap = (Map<String, Object>) subOutput;

                for (Map.Entry<String, String> entry : outputMapping.entrySet()) {
                    String subVarName = entry.getKey();
                    String parentVarName = entry.getValue();
                    if (outputMap.containsKey(subVarName)) {
                        context.setVariable(parentVarName, outputMap.get(subVarName));
                    }
                }
            }

            context.setVariable(outputVariable, subOutput);

            return NodeResult.success(subOutput);

        } catch (Exception e) {
            log.error("子工作流执行失败", e);
            return NodeResult.fail("子工作流执行失败: " + e.getMessage());
        }
    }

    /**
     * 解析表达式
     */
    private Object resolveExpression(String expression, WorkflowContext context) {
        if (expression == null) return null;

        // 简单变量引用
        Matcher matcher = VARIABLE_PATTERN.matcher(expression);
        if (matcher.matches()) {
            return context.getVariable(matcher.group(1));
        }

        // 字符串模板
        StringBuffer result = new StringBuffer();
        matcher = VARIABLE_PATTERN.matcher(expression);
        boolean found = false;
        while (matcher.find()) {
            found = true;
            String varName = matcher.group(1);
            Object value = context.getVariable(varName);
            String replacement = value != null ? value.toString() : "";
            matcher.appendReplacement(result, Matcher.quoteReplacement(replacement));
        }

        if (found) {
            matcher.appendTail(result);
            return result.toString();
        }

        return expression;
    }
}
