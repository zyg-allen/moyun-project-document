package com.moyun.ext.ai.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.moyun.ext.ai.engine.tool.ToolContext;
import com.moyun.ext.ai.engine.tool.ToolRegistry;
import com.moyun.ext.ai.engine.tool.ToolResult;
import com.moyun.ext.ai.entity.AgentTool;
import com.moyun.ext.ai.entity.Workflow;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 工具调用服务
 *
 * <p>处理对话中的 Function Calling 工具调用逻辑，包括：
 * <ul>
 *   <li>检查智能体是否配置了工具</li>
 *   <li>构建工具说明提示词（添加到系统提示词中）</li>
 *   <li>检测AI回复中的工具调用标记</li>
 *   <li>执行工具调用并返回结果</li>
 * </ul>
 * </p>
 *
 * <p>工具调用格式：[TOOL_CALL]{"tool":"工具名","params":{参数}}[/TOOL_CALL]</p>
 *
 * @author laomao
 * @time 2025/11/25
 */
@Slf4j
@Service
public class ToolCallingService {

    @Autowired
    private ToolService toolService;

    @Autowired
    private ToolRegistry toolRegistry;

    @Autowired
    private WorkflowService workflowService;

    /** JSON 解析器 */
    private final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * 工具调用标记的正则表达式
     *
     * <p>匹配格式：[TOOL_CALL]{...JSON...}[/TOOL_CALL]</p>
     * <p>使用 DOTALL 模式支持跨行匹配</p>
     */
    private static final Pattern TOOL_CALL_PATTERN = Pattern.compile(
            "\\[TOOL_CALL\\]\\s*(\\{.*?\\})\\s*\\[/TOOL_CALL\\]",
            Pattern.DOTALL
    );

    /**
     * 检查智能体是否配置了工具
     *
     * @param agentId 智能体ID
     * @return 是否配置了工具
     */
    public boolean hasTools(Long agentId) {
        List<AgentTool> tools = toolService.getToolsByAgentId(agentId);
        return tools != null && !tools.isEmpty();
    }

    /**
     * 构建带工具说明的系统提示词
     *
     * <p>生成的提示词包含：
     * <ul>
     *   <li>工具调用格式说明</li>
     *   <li>可用工具列表及参数定义</li>
     *   <li>绑定的工作流列表</li>
     *   <li>使用注意事项</li>
     * </ul>
     * </p>
     *
     * @param agentId 智能体ID
     * @return 工具说明提示词（如果没有工具则返回空字符串）
     */
    public String buildToolPrompt(Long agentId) {
        List<AgentTool> tools = toolService.getToolsByAgentId(agentId);
        List<Workflow> workflows = workflowService.getAgentWorkflows(agentId);

        boolean hasTools = tools != null && !tools.isEmpty();
        boolean hasWorkflows = workflows != null && !workflows.isEmpty();

        if (!hasTools && !hasWorkflows) {
            return "";
        }

        StringBuilder sb = new StringBuilder();
        sb.append("\n\n【可用工具 - 最高优先级】\n");
        sb.append("你拥有以下工具能力，当用户询问需要实时信息或需要执行特定任务时，必须优先使用工具获取信息。\n\n");
        sb.append("工具调用格式（严格遵守）：\n");
        sb.append("[TOOL_CALL]{\"tool\":\"工具英文名\",\"params\":{参数对象}}[/TOOL_CALL]\n\n");

        // 添加普通工具
        if (hasTools) {
            sb.append("可用工具列表：\n");
            for (AgentTool tool : tools) {
                sb.append(String.format("- %s (英文名: %s): %s\n",
                        tool.getDisplayName(), tool.getName(), tool.getDescription()));
                sb.append(String.format("  参数: %s\n", tool.getParameters()));
            }
        }

        // 添加工作流工具
        if (hasWorkflows) {
            sb.append("\n可用工作流（作为工具调用）：\n");
            for (Workflow wf : workflows) {
                sb.append(String.format("- %s (英文名: workflow_%d): %s\n",
                        wf.getName(), wf.getId(),
                        wf.getDescription() != null ? wf.getDescription() : "执行预定义的AI工作流"));
                sb.append("  参数: {\"input\": \"用户输入内容\"}\n");
            }
        }

        sb.append("\n⚠️ 工具使用规则（必须遵守）：\n");
        sb.append("1. 当用户询问天气、时间等实时信息时，必须调用对应工具\n");
        sb.append("2. tool字段必须使用上面列出的【英文名】\n");
        sb.append("3. 工具调用优先级高于知识库规则\n");
        sb.append("4. 工作流工具用于执行复杂的预定义任务\n");
        sb.append("5. 示例：调用工作流 [TOOL_CALL]{\"tool\":\"workflow_1\",\"params\":{\"input\":\"用户的问题\"}}[/TOOL_CALL]\n");

        return sb.toString();
    }

    /**
     * 检查智能体是否配置了工具或工作流
     */
    public boolean hasToolsOrWorkflows(Long agentId) {
        return hasTools(agentId) || hasWorkflows(agentId);
    }

    /**
     * 检查智能体是否配置了工作流
     */
    public boolean hasWorkflows(Long agentId) {
        List<Workflow> workflows = workflowService.getAgentWorkflows(agentId);
        return workflows != null && !workflows.isEmpty();
    }

    /**
     * 检测并执行工具调用
     *
     * @param aiResponse AI的回复内容
     * @param context 工具执行上下文
     * @return 处理后的内容（如果有工具调用，返回工具结果；否则返回null）
     */
    public ToolCallResult detectAndExecute(String aiResponse, ToolContext context) {
        Matcher matcher = TOOL_CALL_PATTERN.matcher(aiResponse);

        if (!matcher.find()) {
            return null; // 没有工具调用
        }

        String toolCallJson = matcher.group(1);  // 已经是完整的JSON对象
        log.info("🔧 检测到工具调用: {}", toolCallJson);

        try {
            Map<String, Object> callData = objectMapper.readValue(toolCallJson,
                    new TypeReference<Map<String, Object>>() {});

            String toolName = (String) callData.get("tool");
            @SuppressWarnings("unchecked")
            Map<String, Object> params = (Map<String, Object>) callData.get("params");

            if (toolName == null) {
                return ToolCallResult.error("工具名称为空");
            }

            if (!toolRegistry.hasTool(toolName)) {
                return ToolCallResult.error("工具不存在: " + toolName);
            }

            // 执行工具
            ToolResult result = toolRegistry.executeTool(toolName, context, params != null ? params : Map.of());

            // 构建返回结果
            String beforeCall = aiResponse.substring(0, matcher.start()).trim();
            String afterCall = aiResponse.substring(matcher.end()).trim();

            return ToolCallResult.builder()
                    .hasToolCall(true)
                    .toolName(toolName)
                    .toolResult(result)
                    .beforeCallText(beforeCall)
                    .afterCallText(afterCall)
                    .build();

        } catch (Exception e) {
            log.error("解析工具调用失败: {}", toolCallJson, e);
            return ToolCallResult.error("解析工具调用失败: " + e.getMessage());
        }
    }

    /**
     * 工具调用结果
     *
     * <p>封装工具调用的检测和执行结果，包含：
     * <ul>
     *   <li>是否检测到工具调用</li>
     *   <li>工具名称和执行结果</li>
     *   <li>工具调用前后的文本内容</li>
     *   <li>错误信息（如果执行失败）</li>
     * </ul>
     * </p>
     */
    @lombok.Data
    @lombok.Builder
    @lombok.AllArgsConstructor
    @lombok.NoArgsConstructor
    public static class ToolCallResult {
        /** 是否检测到工具调用 */
        private boolean hasToolCall;
        /** 工具名称 */
        private String toolName;
        /** 工具执行结果 */
        private ToolResult toolResult;
        /** 工具调用标记之前的文本 */
        private String beforeCallText;
        /** 工具调用标记之后的文本 */
        private String afterCallText;
        /** 错误信息 */
        private String errorMessage;

        /**
         * 创建错误结果
         *
         * @param message 错误信息
         * @return 错误结果对象
         */
        public static ToolCallResult error(String message) {
            return ToolCallResult.builder()
                    .hasToolCall(true)
                    .errorMessage(message)
                    .build();
        }

        /**
         * 判断工具是否执行成功
         *
         * @return 是否成功
         */
        public boolean isSuccess() {
            return toolResult != null && toolResult.isSuccess();
        }
    }
}
