package com.moyun.ext.ai.engine.workflow.node;

import com.moyun.ext.ai.engine.workflow.NodeExecutor;
import com.moyun.ext.ai.engine.workflow.WorkflowContext;
import com.moyun.ext.ai.engine.workflow.WorkflowNode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

/**
 * 合并节点
 *
 * <p>等待所有并行分支完成后合并结果</p>
 *
 * @author laomao
 */
@Slf4j
@Component
public class MergeNodeExecutor implements NodeExecutor {

    @Override
    public String getType() {
        return "merge";
    }

    @Override
    public NodeResult execute(WorkflowNode node, WorkflowContext context) {
        Map<String, Object> config = node.getConfig();

        try {
            String outputVariable = config != null ?
                (String) config.getOrDefault("outputVariable", "merged_result") : "merged_result";
            String mergeMode = config != null ?
                (String) config.getOrDefault("mode", "object") : "object"; // object, array, first, last, concat

            log.info("🔀 合并节点执行: mode={}", mergeMode);

            // 收集所有分支结果（新的并行执行方式会使用 branch_X_ 前缀）
            Map<String, Object> branchResults = collectBranchResults(context);

            log.info("🔀 收集到 {} 个分支结果", branchResults.size());

            Object mergedResult;
            switch (mergeMode) {
                case "array":
                    mergedResult = new java.util.ArrayList<>(branchResults.values());
                    break;
                case "first":
                    mergedResult = branchResults.values().stream().findFirst().orElse(null);
                    break;
                case "last":
                    Object[] values = branchResults.values().toArray();
                    mergedResult = values.length > 0 ? values[values.length - 1] : null;
                    break;
                case "concat":
                    StringBuilder sb = new StringBuilder();
                    branchResults.values().forEach(v -> {
                        if (v != null) {
                            sb.append(v.toString()).append("\n");
                        }
                    });
                    mergedResult = sb.toString().trim();
                    break;
                case "sum":
                    double sum = 0;
                    for (Object v : branchResults.values()) {
                        if (v instanceof Number) {
                            sum += ((Number) v).doubleValue();
                        }
                    }
                    mergedResult = sum;
                    break;
                case "object":
                default:
                    mergedResult = branchResults;
                    break;
            }

            context.setVariable(outputVariable, mergedResult);

            // 清理并行状态变量
            cleanupParallelState(context);

            log.info("🔀 合并完成: {}", mergedResult);
            return NodeResult.success(mergedResult);

        } catch (Exception e) {
            log.error("合并节点执行失败", e);
            return NodeResult.fail("合并失败: " + e.getMessage());
        }
    }

    /**
     * 收集所有分支结果
     * 
     * <p>支持两种格式：</p>
     * <ul>
     *   <li>旧格式：_branch_results Map</li>
     *   <li>新格式：branch_X_variableName 变量</li>
     * </ul>
     */
    private Map<String, Object> collectBranchResults(WorkflowContext context) {
        Map<String, Object> results = new HashMap<>();
        
        // 1. 检查旧格式的 _branch_results
        @SuppressWarnings("unchecked")
        Map<String, Object> legacyResults = (Map<String, Object>) context.getVariable("_branch_results");
        if (legacyResults != null && !legacyResults.isEmpty()) {
            results.putAll(legacyResults);
        }
        
        // 2. 收集新格式的分支变量 (branch_0_xxx, branch_1_xxx, ...)
        Map<String, Object> variables = context.getVariables();
        Map<String, Map<String, Object>> branchVars = new HashMap<>();
        
        for (Map.Entry<String, Object> entry : variables.entrySet()) {
            String key = entry.getKey();
            if (key.startsWith("branch_") && key.contains("_")) {
                // 解析 branch_X_variableName
                String[] parts = key.split("_", 3);
                if (parts.length >= 3) {
                    String branchId = "branch_" + parts[1];
                    String varName = parts[2];
                    
                    branchVars.computeIfAbsent(branchId, k -> new HashMap<>())
                              .put(varName, entry.getValue());
                }
            }
        }
        
        // 将分支变量合并到结果中
        for (Map.Entry<String, Map<String, Object>> entry : branchVars.entrySet()) {
            String branchId = entry.getKey();
            Map<String, Object> vars = entry.getValue();
            
            // 如果分支只有一个变量，直接使用其值
            if (vars.size() == 1) {
                results.put(branchId, vars.values().iterator().next());
            } else {
                results.put(branchId, vars);
            }
        }
        
        return results;
    }

    /**
     * 清理并行状态变量
     */
    private void cleanupParallelState(WorkflowContext context) {
        // 清理内部变量
        context.removeVariable("_branch_results");
        context.removeVariable("_parallel_branches");
        context.removeVariable("_parallel_mode");
        context.removeVariable("_parallel_timeout");
        
        // 清理分支变量（可选，保留可供后续使用）
        // 如果需要清理，取消下面的注释
        /*
        Map<String, Object> variables = new HashMap<>(context.getVariables());
        for (String key : variables.keySet()) {
            if (key.startsWith("branch_") && key.contains("_")) {
                context.removeVariable(key);
            }
        }
        */
    }
}
