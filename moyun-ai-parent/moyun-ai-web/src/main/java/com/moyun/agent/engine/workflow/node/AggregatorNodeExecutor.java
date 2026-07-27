package com.moyun.agent.engine.workflow.node;

import com.moyun.agent.util.JsonUtils;
import com.moyun.agent.engine.workflow.NodeExecutor;
import com.moyun.agent.engine.workflow.WorkflowContext;
import com.moyun.agent.engine.workflow.WorkflowNode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.stream.Collectors;

/**
 * 聚合节点执行器
 *
 * <p>将多个变量聚合为一个结果</p>
 *
 * @author laomao
 */
@Slf4j
@Component
public class AggregatorNodeExecutor implements NodeExecutor {

    @Override
    public String getType() {
        return "aggregator";
    }

    @Override
    public NodeResult execute(WorkflowNode node, WorkflowContext context) {
        Map<String, Object> config = node.getConfig();
        if (config == null) {
            return NodeResult.fail("聚合节点配置为空");
        }

        try {
            String mode = (String) config.getOrDefault("mode", "object"); // object, array, concat, sum, avg
            @SuppressWarnings("unchecked")
            List<String> variables = (List<String>) config.getOrDefault("variables", new ArrayList<>());
            String outputVariable = (String) config.getOrDefault("outputVariable", "aggregated");
            String separator = (String) config.getOrDefault("separator", "\n");

            log.info("📊 聚合节点: mode={}, variables={}", mode, variables.size());

            Object result;

            switch (mode.toLowerCase()) {
                case "array":
                case "list":
                    result = aggregateToArray(variables, context);
                    break;
                case "concat":
                case "join":
                    result = aggregateToString(variables, context, separator);
                    break;
                case "sum":
                    result = aggregateSum(variables, context);
                    break;
                case "avg":
                case "average":
                    result = aggregateAverage(variables, context);
                    break;
                case "min":
                    result = aggregateMin(variables, context);
                    break;
                case "max":
                    result = aggregateMax(variables, context);
                    break;
                case "count":
                    result = aggregateCount(variables, context);
                    break;
                case "first":
                    result = aggregateFirst(variables, context);
                    break;
                case "last":
                    result = aggregateLast(variables, context);
                    break;
                case "object":
                case "map":
                default:
                    result = aggregateToObject(variables, context);
                    break;
            }

            log.info("📊 聚合完成: result type={}", result != null ? result.getClass().getSimpleName() : "null");

            context.setVariable(outputVariable, result);

            return NodeResult.success(result);

        } catch (Exception e) {
            log.error("聚合节点执行失败", e);
            return NodeResult.fail("聚合执行失败: " + e.getMessage());
        }
    }

    private Map<String, Object> aggregateToObject(List<String> variables, WorkflowContext context) {
        Map<String, Object> result = new LinkedHashMap<>();
        for (String varName : variables) {
            Object value = context.getVariable(varName);
            if (value != null) {
                result.put(varName, value);
            }
        }
        return result;
    }

    private List<Object> aggregateToArray(List<String> variables, WorkflowContext context) {
        List<Object> result = new ArrayList<>();
        for (String varName : variables) {
            Object value = context.getVariable(varName);
            if (value != null) {
                if (value instanceof Collection) {
                    result.addAll((Collection<?>) value);
                } else {
                    result.add(value);
                }
            }
        }
        return result;
    }

    private String aggregateToString(List<String> variables, WorkflowContext context, String separator) {
        return variables.stream()
                .map(context::getVariable)
                .filter(Objects::nonNull)
                .map(Object::toString)
                .collect(Collectors.joining(separator));
    }

    private Double aggregateSum(List<String> variables, WorkflowContext context) {
        return variables.stream()
                .map(context::getVariable)
                .filter(Objects::nonNull)
                .mapToDouble(this::toDouble)
                .sum();
    }

    private Double aggregateAverage(List<String> variables, WorkflowContext context) {
        return variables.stream()
                .map(context::getVariable)
                .filter(Objects::nonNull)
                .mapToDouble(this::toDouble)
                .average()
                .orElse(0.0);
    }

    private Double aggregateMin(List<String> variables, WorkflowContext context) {
        return variables.stream()
                .map(context::getVariable)
                .filter(Objects::nonNull)
                .mapToDouble(this::toDouble)
                .min()
                .orElse(0.0);
    }

    private Double aggregateMax(List<String> variables, WorkflowContext context) {
        return variables.stream()
                .map(context::getVariable)
                .filter(Objects::nonNull)
                .mapToDouble(this::toDouble)
                .max()
                .orElse(0.0);
    }

    private Integer aggregateCount(List<String> variables, WorkflowContext context) {
        int count = 0;
        for (String varName : variables) {
            Object value = context.getVariable(varName);
            if (value != null) {
                if (value instanceof Collection) {
                    count += ((Collection<?>) value).size();
                } else {
                    count++;
                }
            }
        }
        return count;
    }

    private Object aggregateFirst(List<String> variables, WorkflowContext context) {
        for (String varName : variables) {
            Object value = context.getVariable(varName);
            if (value != null) {
                return value;
            }
        }
        return null;
    }

    private Object aggregateLast(List<String> variables, WorkflowContext context) {
        Object last = null;
        for (String varName : variables) {
            Object value = context.getVariable(varName);
            if (value != null) {
                last = value;
            }
        }
        return last;
    }

    private double toDouble(Object value) {
        if (value instanceof Number) {
            return ((Number) value).doubleValue();
        }
        try {
            return Double.parseDouble(value.toString());
        } catch (NumberFormatException e) {
            return 0.0;
        }
    }
}
