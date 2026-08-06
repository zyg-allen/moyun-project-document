package com.moyun.ext.ai.engine.tool.builtin;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.moyun.ext.ai.engine.tool.ToolContext;
import com.moyun.ext.ai.engine.tool.ToolExecutor;
import com.moyun.ext.ai.engine.tool.ToolResult;
import com.moyun.ext.ai.engine.workflow.WorkflowEngine;
import com.moyun.ext.ai.entity.Workflow;
import com.moyun.ext.ai.mapper.WorkflowMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

/**
 * 工作流工具
 *
 * <p>允许智能体在对话中调用已发布的工作流</p>
 * <p>这是工作流与智能体集成的关键桥梁</p>
 *
 * @author laomao
 */
@Slf4j
@Component
public class WorkflowTool implements ToolExecutor {

    @Autowired
    private WorkflowMapper workflowMapper;

    @Autowired
    @Lazy
    private WorkflowEngine workflowEngine;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public String getName() {
        return "execute_workflow";
    }

    @Override
    public String getDescription() {
        return "执行一个预定义的AI工作流。工作流是一系列编排好的AI处理步骤，可以完成复杂的任务。" +
               "使用此工具可以调用已发布的工作流来处理用户请求。";
    }

    @Override
    public String getParametersSchema() {
        return """
            {
                "type": "object",
                "properties": {
                    "workflow_id": {
                        "type": "number",
                        "description": "要执行的工作流ID"
                    },
                    "input": {
                        "type": "string",
                        "description": "传递给工作流的输入参数，通常是用户的问题或请求内容"
                    }
                },
                "required": ["workflow_id", "input"]
            }
            """;
    }

    @Override
    public ToolResult execute(ToolContext context, Map<String, Object> params) {
        try {
            // 获取参数
            Object workflowIdObj = params.get("workflow_id");
            String input = (String) params.get("input");

            if (workflowIdObj == null) {
                return ToolResult.fail("未指定工作流ID");
            }

            Long workflowId;
            if (workflowIdObj instanceof Number) {
                workflowId = ((Number) workflowIdObj).longValue();
            } else {
                workflowId = Long.parseLong(workflowIdObj.toString());
            }

            log.info("🔧 智能体调用工作流: workflowId={}, input={}", workflowId,
                    input != null && input.length() > 50 ? input.substring(0, 50) + "..." : input);

            // 检查工作流是否存在且已启用
            Workflow workflow = workflowMapper.selectById(workflowId);
            if (workflow == null) {
                return ToolResult.fail("工作流不存在: " + workflowId);
            }

            if (!Boolean.TRUE.equals(workflow.getEnabled())) {
                return ToolResult.fail("工作流未启用，请先发布工作流");
            }

            // 构建输入参数
            Map<String, Object> inputMap = new HashMap<>();
            inputMap.put("input", input);

            // 执行工作流
            WorkflowEngine.WorkflowResult result = workflowEngine.execute(workflowId, inputMap);

            if (result.isSuccess()) {
                Object output = result.getOutput();
                String outputStr;
                if (output instanceof String) {
                    outputStr = (String) output;
                } else {
                    outputStr = objectMapper.writeValueAsString(output);
                }

                log.info("🔧 工作流执行成功: duration={}ms", result.getDurationMs());
                return ToolResult.success(outputStr);
            } else {
                log.warn("🔧 工作流执行失败: {}", result.getErrorMessage());
                return ToolResult.fail("工作流执行失败: " + result.getErrorMessage());
            }

        } catch (Exception e) {
            log.error("执行工作流失败", e);
            return ToolResult.fail("执行工作流失败: " + e.getMessage());
        }
    }
}
