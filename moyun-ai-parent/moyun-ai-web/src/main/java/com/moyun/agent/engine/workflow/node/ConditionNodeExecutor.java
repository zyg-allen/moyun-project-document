package com.moyun.agent.engine.workflow.node;

import com.moyun.agent.engine.workflow.WorkflowContext;
import com.moyun.agent.engine.workflow.WorkflowNode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 条件分支节点执行器
 *
 * <p>根据条件表达式决定走哪个分支</p>
 *
 * @author laomao
 */
@Slf4j
@Component
public class ConditionNodeExecutor extends BaseNodeExecutor {

    @Override
    public String getType() {
        return "condition";
    }

    @Override
    public NodeResult execute(WorkflowNode node, WorkflowContext context) {
        Map<String, Object> config = node.getConfig();
        if (config == null) {
            return NodeResult.fail("条件节点配置为空");
        }

        try {
            // 获取条件表达式
            String expression = (String) config.get("expression");
            if (expression == null || expression.isEmpty()) {
                return NodeResult.fail("条件表达式为空");
            }

            // 替换变量
            expression = replaceVariables(expression, context);

            log.info("🔀 条件节点执行: expression={}", expression);

            // 评估条件
            boolean result = evaluateCondition(expression, context);

            log.info("🔀 条件结果: {}", result);

            // 返回对应的输出句柄
            String nextHandle = result ? "true" : "false";
            return NodeResult.success(result, nextHandle);

        } catch (Exception e) {
            log.error("条件节点执行失败", e);
            return NodeResult.fail("条件评估失败: " + e.getMessage());
        }
    }

    /**
     * 评估条件表达式
     */
    private boolean evaluateCondition(String expression, WorkflowContext context) {
        try {
            expression = expression.trim();

            // 大于等于 >=
            if (expression.contains(">=")) {
                String[] parts = expression.split(">=", 2);
                if (parts.length == 2) {
                    double left = parseNumber(parts[0].trim());
                    double right = parseNumber(parts[1].trim());
                    return left >= right;
                }
            }

            // 小于等于 <=
            if (expression.contains("<=")) {
                String[] parts = expression.split("<=", 2);
                if (parts.length == 2) {
                    double left = parseNumber(parts[0].trim());
                    double right = parseNumber(parts[1].trim());
                    return left <= right;
                }
            }

            // 大于 >
            if (expression.contains(">") && !expression.contains(">=")) {
                String[] parts = expression.split(">", 2);
                if (parts.length == 2) {
                    double left = parseNumber(parts[0].trim());
                    double right = parseNumber(parts[1].trim());
                    return left > right;
                }
            }

            // 小于 <
            if (expression.contains("<") && !expression.contains("<=")) {
                String[] parts = expression.split("<", 2);
                if (parts.length == 2) {
                    double left = parseNumber(parts[0].trim());
                    double right = parseNumber(parts[1].trim());
                    return left < right;
                }
            }

            // 等于 ==
            if (expression.contains("==")) {
                String[] parts = expression.split("==", 2);
                if (parts.length == 2) {
                    String left = parts[0].trim();
                    String right = parts[1].trim().replace("'", "").replace("\"", "");
                    return left.equals(right);
                }
            }

            // 不等于 !=
            if (expression.contains("!=")) {
                String[] parts = expression.split("!=", 2);
                if (parts.length == 2) {
                    String left = parts[0].trim();
                    String right = parts[1].trim().replace("'", "").replace("\"", "");
                    return !left.equals(right);
                }
            }

            // 包含 contains
            if (expression.contains(" contains ")) {
                Pattern p = Pattern.compile("(.+)\\s+contains\\s+['\"](.+)['\"]");
                Matcher m = p.matcher(expression);
                if (m.find()) {
                    String value = m.group(1).trim();
                    String keyword = m.group(2);
                    return value.toLowerCase().contains(keyword.toLowerCase());
                }
            }

            // 以...开头 startsWith
            if (expression.contains(" startsWith ")) {
                Pattern p = Pattern.compile("(.+)\\s+startsWith\\s+['\"](.+)['\"]");
                Matcher m = p.matcher(expression);
                if (m.find()) {
                    return m.group(1).trim().startsWith(m.group(2));
                }
            }

            // 以...结尾 endsWith
            if (expression.contains(" endsWith ")) {
                Pattern p = Pattern.compile("(.+)\\s+endsWith\\s+['\"](.+)['\"]");
                Matcher m = p.matcher(expression);
                if (m.find()) {
                    return m.group(1).trim().endsWith(m.group(2));
                }
            }

            // 为空判断 isEmpty
            if (expression.endsWith(" isEmpty") || expression.endsWith(".isEmpty")) {
                String value = expression.replace(" isEmpty", "").replace(".isEmpty", "").trim();
                return value.isEmpty();
            }

            // 非空判断 isNotEmpty
            if (expression.endsWith(" isNotEmpty") || expression.endsWith(".isNotEmpty")) {
                String value = expression.replace(" isNotEmpty", "").replace(".isNotEmpty", "").trim();
                return !value.isEmpty();
            }

            // 布尔值判断
            if ("true".equalsIgnoreCase(expression)) {
                return true;
            }
            if ("false".equalsIgnoreCase(expression)) {
                return false;
            }

            // 非空非零则为 true
            return !expression.isEmpty() && !"0".equals(expression) && !"null".equalsIgnoreCase(expression);

        } catch (Exception e) {
            log.warn("条件表达式评估异常: {}", expression, e);
            return false;
        }
    }

    /**
     * 解析数字
     */
    private double parseNumber(String str) {
        try {
            return Double.parseDouble(str.replace("'", "").replace("\"", ""));
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    // replaceVariables方法已移至BaseNodeExecutor
}
