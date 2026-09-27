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

/**
 * 并行执行节点
 *
 * <p>同时执行多个分支，等待全部完成后继续</p>
 *
 * <p><b>v13.5</b>：删除历史遗留的实例字段
 * {@code private final ExecutorService executorService = Executors.newFixedThreadPool(10)}——
 * 该字段自始至终<strong>无任何使用点</strong>（真正的并行执行在 {@code WorkflowEngine}，
 * 用的是受 Spring 管理的 {@code workflowParallelExecutor}）。它的存在只是白占 10 个线程
 * 的池对象，且每个实例一份。</p>
 *
 * @author laomao
 */
@Slf4j
@Component
public class ParallelNodeExecutor implements NodeExecutor {

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
