package com.moyun.agent.engine.workflow.node;

import com.moyun.agent.util.JsonUtils;
import com.moyun.agent.engine.workflow.WorkflowContext;
import com.moyun.agent.engine.workflow.WorkflowNode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 变量设置节点执行器
 *
 * <p>设置或修改工作流变量</p>
 *
 * @author laomao
 */
@Slf4j
@Component
public class SetVariableNodeExecutor extends BaseNodeExecutor {

    @Override
    public String getType() {
        return "setvar";
    }

    @Override
    public NodeResult execute(WorkflowNode node, WorkflowContext context) {
        Map<String, Object> config = node.getConfig();
        if (config == null) {
            return NodeResult.fail("变量设置节点配置为空");
        }

        try {
            String variableName = (String) config.get("variableName");
            Object value = config.get("value");
            String valueType = (String) config.getOrDefault("valueType", "string");

            if (variableName == null || variableName.isEmpty()) {
                return NodeResult.fail("未指定变量名");
            }

            // 处理值
            Object finalValue = processValue(value, valueType, context);

            log.info("📝 设置变量: {}={}", variableName, finalValue);

            context.setVariable(variableName, finalValue);

            return NodeResult.success(finalValue);

        } catch (Exception e) {
            log.error("变量设置失败", e);
            return NodeResult.fail("变量设置失败: " + e.getMessage());
        }
    }

    /**
     * 处理变量值
     */
    private Object processValue(Object value, String valueType, WorkflowContext context) {
        if (value == null) {
            return null;
        }

        String strValue = value.toString();

        // 替换变量引用
        strValue = replaceVariables(strValue, context);

        // 根据类型转换
        switch (valueType.toLowerCase()) {
            case "number":
            case "int":
            case "integer":
                return Long.parseLong(strValue);
            case "float":
            case "double":
                return Double.parseDouble(strValue);
            case "boolean":
            case "bool":
                return Boolean.parseBoolean(strValue);
            case "json":
            case "object":
                Object jsonResult = JsonUtils.fromJson(strValue, Object.class);
                return jsonResult != null ? jsonResult : strValue;
            default:
                return strValue;
        }
    }

    // replaceVariables方法已移至BaseNodeExecutor
}
