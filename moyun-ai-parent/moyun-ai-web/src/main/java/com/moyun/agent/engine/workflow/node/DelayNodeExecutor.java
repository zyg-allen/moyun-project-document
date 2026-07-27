package com.moyun.agent.engine.workflow.node;

import com.moyun.agent.engine.workflow.NodeExecutor;
import com.moyun.agent.engine.workflow.WorkflowContext;
import com.moyun.agent.engine.workflow.WorkflowNode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * 延迟节点执行器
 *
 * <p>在工作流中添加延迟等待</p>
 *
 * @author laomao
 */
@Slf4j
@Component
public class DelayNodeExecutor implements NodeExecutor {

    @Override
    public String getType() {
        return "delay";
    }

    @Override
    public NodeResult execute(WorkflowNode node, WorkflowContext context) {
        Map<String, Object> config = node.getConfig();
        if (config == null) {
            config = Map.of();
        }

        try {
            int delayMs = config.containsKey("delayMs") ? ((Number) config.get("delayMs")).intValue() : 1000;
            int maxDelayMs = 60000; // 最大60秒

            delayMs = Math.min(delayMs, maxDelayMs);

            log.info("⏱️ 延迟等待: {}ms", delayMs);

            Thread.sleep(delayMs);

            log.info("⏱️ 延迟完成");

            return NodeResult.success(delayMs);

        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return NodeResult.fail("延迟被中断");
        } catch (Exception e) {
            log.error("延迟节点执行失败", e);
            return NodeResult.fail("延迟执行失败: " + e.getMessage());
        }
    }
}
