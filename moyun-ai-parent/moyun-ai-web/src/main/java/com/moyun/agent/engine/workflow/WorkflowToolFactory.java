package com.moyun.agent.engine.workflow;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.moyun.agent.engine.tool.ToolContext;
import com.moyun.agent.engine.tool.ToolExecutor;
import com.moyun.agent.engine.tool.ToolResult;
import com.moyun.agent.entity.Workflow;
import com.moyun.agent.mapper.WorkflowMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 工作流工具工厂
 *
 * <p>将工作流包装成工具，供智能体调用</p>
 * <p>每个已发布的工作流可以作为一个独立的工具被智能体使用</p>
 *
 * @author laomao
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class WorkflowToolFactory {

    private final WorkflowMapper workflowMapper;

    @Lazy
    private final WorkflowEngine workflowEngine;

    private final ObjectMapper objectMapper = new ObjectMapper();

    /** 缓存已创建的工作流工具 */
    private final Map<Long, WorkflowAsTool> toolCache = new ConcurrentHashMap<>();

    /**
     * 将工作流转换为工具
     *
     * @param workflowId 工作流ID
     * @return 工具实例，如果工作流不存在或未启用则返回null
     */
    public ToolExecutor createToolFromWorkflow(Long workflowId) {
        return toolCache.computeIfAbsent(workflowId, id -> {
            Workflow workflow = workflowMapper.selectById(id);
            if (workflow == null || !Boolean.TRUE.equals(workflow.getEnabled())) {
                return null;
            }
            return new WorkflowAsTool(workflow, workflowEngine, objectMapper);
        });
    }

    /**
     * 获取所有可用的工作流工具
     */
    public List<ToolExecutor> getAllWorkflowTools() {
        return workflowMapper.selectList(null).stream()
                .filter(w -> Boolean.TRUE.equals(w.getEnabled()))
                .map(w -> createToolFromWorkflow(w.getId()))
                .filter(t -> t != null)
                .toList();
    }

    /**
     * 刷新工具缓存
     */
    public void refreshCache(Long workflowId) {
        toolCache.remove(workflowId);
    }

    /**
     * 清空所有缓存
     */
    public void clearCache() {
        toolCache.clear();
    }

    /**
     * 工作流作为工具的实现
     */
    @Slf4j
    public static class WorkflowAsTool implements ToolExecutor {

        private final Workflow workflow;
        private final WorkflowEngine workflowEngine;
        private final ObjectMapper objectMapper;

        public WorkflowAsTool(Workflow workflow, WorkflowEngine workflowEngine, ObjectMapper objectMapper) {
            this.workflow = workflow;
            this.workflowEngine = workflowEngine;
            this.objectMapper = objectMapper;
        }

        @Override
        public String getName() {
            return "workflow_" + workflow.getId();
        }

        @Override
        public String getDescription() {
            String desc = workflow.getDescription();
            if (desc == null || desc.isEmpty()) {
                desc = "执行工作流: " + workflow.getName();
            }
            return desc + " (工作流ID: " + workflow.getId() + ")";
        }

        @Override
        public String getParametersSchema() {
            return """
                {
                    "type": "object",
                    "properties": {
                        "input": {
                            "type": "string",
                            "description": "传递给工作流的输入内容"
                        }
                    },
                    "required": ["input"]
                }
                """;
        }

        @Override
        public ToolResult execute(ToolContext context, Map<String, Object> params) {
            try {
                String input = (String) params.get("input");

                log.info("📦 执行工作流工具: name={}, input={}",
                        workflow.getName(),
                        input != null && input.length() > 50 ? input.substring(0, 50) + "..." : input);

                // 构建输入
                Map<String, Object> inputMap = new HashMap<>();
                inputMap.put("input", input);

                // 执行工作流
                WorkflowEngine.WorkflowResult result = workflowEngine.execute(workflow.getId(), inputMap);

                if (result.isSuccess()) {
                    Object output = result.getOutput();
                    String outputStr;
                    if (output instanceof String) {
                        outputStr = (String) output;
                    } else if (output != null) {
                        outputStr = objectMapper.writeValueAsString(output);
                    } else {
                        outputStr = "工作流执行完成";
                    }
                    return ToolResult.success(outputStr);
                } else {
                    return ToolResult.fail(result.getErrorMessage());
                }

            } catch (Exception e) {
                log.error("工作流工具执行失败", e);
                return ToolResult.fail("执行失败: " + e.getMessage());
            }
        }

        public Long getWorkflowId() {
            return workflow.getId();
        }

        public String getDisplayName() {
            return workflow.getName();
        }
    }
}
