package com.moyun.agent.engine.workflow.node;

import com.moyun.agent.engine.workflow.NodeExecutor;
import com.moyun.agent.engine.workflow.WorkflowContext;
import com.moyun.agent.engine.workflow.WorkflowNode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * 结束节点执行器
 *
 * @author laomao
 */
@Slf4j
@Component
public class EndNodeExecutor implements NodeExecutor {

    @Override
    public String getType() {
        return "end";
    }

    @Override
    public NodeResult execute(WorkflowNode node, WorkflowContext context) {
        log.info("✅ 工作流执行结束, workflowId={}", context.getWorkflowId());

        // 获取配置的输出变量
        Map<String, Object> config = node.getConfig();
        Object output;

        if (config != null && config.containsKey("outputVariable")) {
            String outputVar = (String) config.get("outputVariable");
            output = context.getVariable(outputVar);
        } else {
            // 默认返回所有变量
            output = context.getVariables();
        }

        context.setFinalOutput(output);
        return NodeResult.terminate(output);
    }
}
