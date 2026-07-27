package com.moyun.agent.engine.workflow.node;

import com.moyun.agent.engine.workflow.WorkflowContext;
import com.moyun.agent.engine.workflow.WorkflowNode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * While循环节点
 *
 * <p>根据条件循环执行，直到条件不满足</p>
 *
 * @author laomao
 */
@Slf4j
@Component
public class WhileNodeExecutor extends BaseNodeExecutor {
    private static final int MAX_ITERATIONS = 1000; // 防止无限循环

    @Override
    public String getType() {
        return "while";
    }

    @Override
    public NodeResult execute(WorkflowNode node, WorkflowContext context) {
        Map<String, Object> config = node.getConfig();
        if (config == null) {
            return NodeResult.fail("循环节点配置为空");
        }

        try {
            String condition = (String) config.get("condition");
            String counterVariable = (String) config.getOrDefault("counterVariable", "loop_count");
            Integer maxIterations = config.containsKey("maxIterations") ?
                ((Number) config.get("maxIterations")).intValue() : MAX_ITERATIONS;

            if (condition == null || condition.isEmpty()) {
                return NodeResult.fail("循环条件为空");
            }

            // 获取或初始化循环计数器
            Integer counter = (Integer) context.getVariable(counterVariable);
            if (counter == null) {
                counter = 0;
            }

            // 检查是否超过最大迭代次数
            if (counter >= maxIterations) {
                log.warn("🔄 循环达到最大次数限制: {}", maxIterations);
                context.removeVariable(counterVariable);
                return NodeResult.success("loop_end", "exit");
            }

            // 评估条件
            boolean shouldContinue = evaluateCondition(condition, context);

            log.info("🔄 循环节点: counter={}, condition={}, continue={}", counter, condition, shouldContinue);

            if (shouldContinue) {
                // 增加计数器
                context.setVariable(counterVariable, counter + 1);
                context.setVariable("_loop_index", counter);

                // 返回继续循环的句柄
                return NodeResult.success(counter, "loop");
            } else {
                // 循环结束，清理计数器
                context.removeVariable(counterVariable);
                context.removeVariable("_loop_index");

                return NodeResult.success("loop_end", "exit");
            }

        } catch (Exception e) {
            log.error("循环节点执行失败", e);
            return NodeResult.fail("循环执行失败: " + e.getMessage());
        }
    }

    /**
     * 评估条件表达式
     * 
     * @param condition 条件表达式
     * @param context 工作流上下文
     * @return 条件是否满足
     */
    private boolean evaluateCondition(String condition, WorkflowContext context) {
        // 替换变量
        String resolved = replaceVariables(condition, context);

        // 简单条件评估
        try {
            // 处理比较操作
            if (resolved.contains("<=")) {
                String[] parts = resolved.split("<=");
                return Double.parseDouble(parts[0].trim()) <= Double.parseDouble(parts[1].trim());
            } else if (resolved.contains(">=")) {
                String[] parts = resolved.split(">=");
                return Double.parseDouble(parts[0].trim()) >= Double.parseDouble(parts[1].trim());
            } else if (resolved.contains("<")) {
                String[] parts = resolved.split("<");
                return Double.parseDouble(parts[0].trim()) < Double.parseDouble(parts[1].trim());
            } else if (resolved.contains(">")) {
                String[] parts = resolved.split(">");
                return Double.parseDouble(parts[0].trim()) > Double.parseDouble(parts[1].trim());
            } else if (resolved.contains("==")) {
                String[] parts = resolved.split("==");
                return parts[0].trim().equals(parts[1].trim());
            } else if (resolved.contains("!=")) {
                String[] parts = resolved.split("!=");
                return !parts[0].trim().equals(parts[1].trim());
            }

            // 布尔值
            return Boolean.parseBoolean(resolved.trim());
        } catch (Exception e) {
            log.warn("条件评估失败: {}", resolved);
            return false;
        }
    }
}
