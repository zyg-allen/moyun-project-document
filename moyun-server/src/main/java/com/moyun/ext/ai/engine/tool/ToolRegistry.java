package com.moyun.ext.ai.engine.tool;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.moyun.ext.ai.engine.workflow.WorkflowToolFactory;
import com.moyun.ext.ai.entity.AgentTool;
import com.moyun.ext.ai.entity.ToolCallLog;
import com.moyun.ext.ai.mapper.AgentToolMapper;
import com.moyun.ext.ai.mapper.ToolCallLogMapper;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 工具注册中心
 *
 * <p>Function Calling 工具的核心管理组件，负责：
 * <ul>
 *   <li>自动发现并注册所有实现了 ToolExecutor 接口的内置工具</li>
 *   <li>提供工具的查询和执行能力</li>
 *   <li>异步记录工具调用日志</li>
 * </ul>
 * </p>
 *
 * <p>工具注册流程：
 * <ol>
 *   <li>Spring 容器启动时，自动注入所有 ToolExecutor 实现</li>
 *   <li>@PostConstruct 方法中遍历并注册到 builtinTools 映射表</li>
 *   <li>执行时根据工具名称查找对应的执行器</li>
 * </ol>
 * </p>
 *
 * @author laomao
 * @time 2025/11/25
 */
@Slf4j
@Component
public class ToolRegistry {

    /**
     * 内置工具注册表
     *
     * <p>Key: 工具名称（唯一标识），Value: 工具执行器</p>
     */
    private final Map<String, ToolExecutor> builtinTools = new ConcurrentHashMap<>();

    @Autowired
    private AgentToolMapper agentToolMapper;

    @Autowired
    private ToolCallLogMapper toolCallLogMapper;

    /** 自动注入所有实现了 ToolExecutor 接口的 Bean */
    @Autowired(required = false)
    private List<ToolExecutor> toolExecutors;

    @Autowired
    @Lazy
    private WorkflowToolFactory workflowToolFactory;

    /** JSON 解析器 */
    private final ObjectMapper objectMapper = new ObjectMapper();

    /** 工作流工具名称模式: workflow_123 */
    private static final Pattern WORKFLOW_TOOL_PATTERN = Pattern.compile("workflow_(\\d+)");

    /**
     * 初始化工具注册中心
     *
     * <p>在 Spring 容器启动后自动执行，遍历所有 ToolExecutor 实现并注册</p>
     */
    @PostConstruct
    public void init() {
        if (toolExecutors != null) {
            for (ToolExecutor executor : toolExecutors) {
                registerBuiltin(executor);
            }
        }
        log.info("✅ 工具注册中心初始化完成，已注册 {} 个内置工具", builtinTools.size());
    }

    /**
     * 注册内置工具
     *
     * @param executor 工具执行器
     */
    public void registerBuiltin(ToolExecutor executor) {
        builtinTools.put(executor.getName(), executor);
        log.info("📦 注册内置工具: {}", executor.getName());
    }

    /**
     * 获取智能体可用的所有工具定义
     *
     * @param agentId 智能体ID
     * @return 工具定义列表
     */
    public List<ToolDefinition> getToolsForAgent(Long agentId) {
        List<ToolDefinition> definitions = new ArrayList<>();

        // 查询智能体关联的工具
        List<AgentTool> tools = agentToolMapper.selectToolsByAgentId(agentId);

        for (AgentTool tool : tools) {
            definitions.add(ToolDefinition.builder()
                    .name(tool.getName())
                    .displayName(tool.getDisplayName())
                    .description(tool.getDescription())
                    .parameters(tool.getParameters())
                    .build());
        }

        return definitions;
    }

    /**
     * 获取所有可用工具
     *
     * <p>用于工具管理页面展示</p>
     *
     * @return 所有工具列表
     */
    public List<AgentTool> getAllTools() {
        return agentToolMapper.selectList(null);
    }

    /**
     * 执行工具调用
     *
     * <p>执行流程：
     * <ol>
     *   <li>根据工具名称查找执行器</li>
     *   <li>调用执行器的 execute 方法</li>
     *   <li>记录执行耗时</li>
     *   <li>异步记录调用日志</li>
     * </ol>
     * </p>
     *
     * @param toolName 工具名称（唯一标识）
     * @param context 执行上下文（包含智能体ID、会话ID等）
     * @param params 调用参数
     * @return 执行结果
     */
    public ToolResult executeTool(String toolName, ToolContext context, Map<String, Object> params) {
        long startTime = System.currentTimeMillis();

        // 获取工具执行器（支持英文名和中文显示名）
        ToolExecutor executor = getToolExecutor(toolName);
        if (executor == null) {
            log.warn("❌ 工具不存在: {}", toolName);
            return ToolResult.fail("工具不存在: " + toolName);
        }

        log.info("🔧 开始执行工具: {} ({}), 参数: {}", toolName, executor.getName(), params);

        ToolResult result;
        String status = "success";
        String errorMessage = null;

        try {
            result = executor.execute(context, params);
            result.setDurationMs(System.currentTimeMillis() - startTime);

            if (!result.isSuccess()) {
                status = "failed";
                errorMessage = result.getErrorMessage();
            }

            log.info("✅ 工具执行完成: {}, 耗时: {}ms, 成功: {}",
                    toolName, result.getDurationMs(), result.isSuccess());

        } catch (Exception e) {
            log.error("❌ 工具执行异常: {}", toolName, e);
            result = ToolResult.fail(e.getMessage());
            result.setDurationMs(System.currentTimeMillis() - startTime);
            status = "failed";
            errorMessage = e.getMessage();
        }

        // 异步记录日志
        logToolCallAsync(context, toolName, params, result, status, errorMessage);

        return result;
    }

    /**
     * 解析工具调用请求
     *
     * @param toolCallJson LLM返回的工具调用JSON
     * @return 工具名称和参数
     */
    public ToolCallRequest parseToolCall(String toolCallJson) {
        try {
            Map<String, Object> callData = objectMapper.readValue(toolCallJson,
                    new TypeReference<Map<String, Object>>() {});

            String toolName = (String) callData.get("tool");
            @SuppressWarnings("unchecked")
            Map<String, Object> params = (Map<String, Object>) callData.get("params");

            return new ToolCallRequest(toolName, params);
        } catch (Exception e) {
            log.error("解析工具调用失败: {}", toolCallJson, e);
            return null;
        }
    }

    /**
     * 异步记录工具调用日志
     *
     * <p>使用 @Async 注解异步执行，不阻塞主流程</p>
     *
     * @param context 执行上下文
     * @param toolName 工具名称
     * @param params 调用参数
     * @param result 执行结果
     * @param status 状态（success/failed）
     * @param errorMessage 错误信息（如果失败）
     */
    @Async
    public void logToolCallAsync(ToolContext context, String toolName,
                                 Map<String, Object> params, ToolResult result,
                                 String status, String errorMessage) {
        try {
            ToolCallLog callLog = ToolCallLog.builder()
                    .conversationId(context.getConversationId())
                    .messageId(context.getMessageId())
                    .agentId(context.getAgentId())
                    .toolName(toolName)
                    .inputParams(objectMapper.writeValueAsString(params))
                    .outputResult(result.getContent())
                    .status(status)
                    .errorMessage(errorMessage)
                    .durationMs((int) result.getDurationMs())
                    .createTime(LocalDateTime.now())
                    .build();

            toolCallLogMapper.insert(callLog);
        } catch (Exception e) {
            // 日志记录失败不影响主流程
            log.debug("工具调用日志记录失败: {}", e.getMessage());
        }
    }

    /**
     * 检查工具是否存在
     *
     * @param toolName 工具名称（支持英文名、中文显示名或工作流工具名）
     * @return 是否存在
     */
    public boolean hasTool(String toolName) {
        // 检查内置工具
        if (builtinTools.containsKey(toolName)) {
            return true;
        }
        // 检查工作流工具
        if (isWorkflowTool(toolName)) {
            Long workflowId = extractWorkflowId(toolName);
            return workflowId != null && workflowToolFactory.createToolFromWorkflow(workflowId) != null;
        }
        // 检查显示名称
        return findToolByDisplayName(toolName) != null;
    }

    /**
     * 判断是否是工作流工具
     */
    private boolean isWorkflowTool(String toolName) {
        return toolName != null && WORKFLOW_TOOL_PATTERN.matcher(toolName).matches();
    }

    /**
     * 从工具名称中提取工作流ID
     */
    private Long extractWorkflowId(String toolName) {
        Matcher matcher = WORKFLOW_TOOL_PATTERN.matcher(toolName);
        if (matcher.matches()) {
            try {
                return Long.parseLong(matcher.group(1));
            } catch (NumberFormatException e) {
                return null;
            }
        }
        return null;
    }

    /**
     * 根据显示名称查找工具
     *
     * @param displayName 工具显示名称（中文名）
     * @return 工具执行器，未找到返回null
     */
    public ToolExecutor findToolByDisplayName(String displayName) {
        for (ToolExecutor executor : builtinTools.values()) {
            // 检查数据库中的工具显示名称
            List<AgentTool> allTools = agentToolMapper.selectList(null);
            for (AgentTool tool : allTools) {
                if (tool.getDisplayName().equals(displayName) && builtinTools.containsKey(tool.getName())) {
                    return builtinTools.get(tool.getName());
                }
            }
        }
        return null;
    }

    /**
     * 获取工具执行器（支持英文名、中文显示名或工作流工具名）
     *
     * @param toolName 工具名称
     * @return 工具执行器
     */
    public ToolExecutor getToolExecutor(String toolName) {
        // 优先使用英文名查找内置工具
        ToolExecutor executor = builtinTools.get(toolName);
        if (executor != null) {
            return executor;
        }

        // 检查是否是工作流工具
        if (isWorkflowTool(toolName)) {
            Long workflowId = extractWorkflowId(toolName);
            if (workflowId != null) {
                ToolExecutor workflowTool = workflowToolFactory.createToolFromWorkflow(workflowId);
                if (workflowTool != null) {
                    return workflowTool;
                }
            }
        }

        // 尝试通过显示名称查找
        return findToolByDisplayName(toolName);
    }

    /**
     * 工具调用请求记录
     *
     * @param toolName 工具名称
     * @param params 调用参数
     */
    public record ToolCallRequest(String toolName, Map<String, Object> params) {}
}
