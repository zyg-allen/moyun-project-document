package com.moyun.agent.engine.workflow.node;

import com.moyun.agent.engine.workflow.NodeExecutor;
import com.moyun.agent.engine.workflow.WorkflowContext;
import com.moyun.agent.engine.workflow.WorkflowNode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * 开始节点执行器
 *
 * @author laomao
 */
@Slf4j
@Component
public class StartNodeExecutor implements NodeExecutor {

    @Override
    public String getType() {
        return "start";
    }

    @Override
    public NodeResult execute(WorkflowNode node, WorkflowContext context) {
        log.info("🚀 工作流开始执行, workflowId={}", context.getWorkflowId());

        // 将输入参数设置到变量中
        if (context.getInput() != null) {
            context.getVariables().putAll(context.getInput());
        }

        // 开始节点直接返回输入
        return NodeResult.success(context.getInput());
    }
}
