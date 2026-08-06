package com.moyun.ext.ai.engine.workflow.node;

import com.moyun.ext.ai.engine.workflow.NodeExecutor;
import com.moyun.ext.ai.engine.workflow.WorkflowContext;
import com.moyun.ext.ai.engine.workflow.WorkflowNode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.*;

/**
 * 并行执行节点
 *
 * <p>同时执行多个分支，等待全部完成后继续</p>
 *
 * @author laomao
 */
@Slf4j
@Component
public class ParallelNodeExecutor implements NodeExecutor {

    private final ExecutorService executorService = Executors.newFixedThreadPool(10);

    @Override
    public String getType() {
        return "parallel";
    }

    @Override
    public NodeResult execute(WorkflowNode node, WorkflowContext context) {
        Map<String, Object> config = node.getConfig();
        if (config == null) {
            return NodeResult.fail("并行节点配置为空");
        }

        try {
            // 获取配置
            @SuppressWarnings("unchecked")
            List<String> branches = (List<String>) config.getOrDefault("branches", new ArrayList<>());
            String outputVariable = (String) config.getOrDefault("outputVariable", "parallel_results");
            Integer timeoutSeconds = config.containsKey("timeout") ?
                ((Number) config.get("timeout")).intValue() : 60;
            String mode = (String) config.getOrDefault("mode", "all"); // all, any, race

            log.info("⚡ 并行节点开始执行: branches={}, mode={}", branches.size(), mode);

            // 标记并行分支
            context.setVariable("_parallel_branches", branches);
            context.setVariable("_parallel_mode", mode);

            // 并行节点本身只是标记，实际并行执行由WorkflowEngine处理
            // 这里返回成功，让引擎知道需要并行执行后续分支
            Map<String, Object> result = new HashMap<>();
            result.put("branches", branches);
            result.put("mode", mode);
            result.put("timeout", timeoutSeconds);

            context.setVariable(outputVariable, result);

            return NodeResult.builder()
                    .success(true)
                    .output(result)
                    .parallel(true) // 标记这是并行节点
                    .build();

        } catch (Exception e) {
            log.error("并行节点执行失败", e);
            return NodeResult.fail("并行执行失败: " + e.getMessage());
        }
    }
}
