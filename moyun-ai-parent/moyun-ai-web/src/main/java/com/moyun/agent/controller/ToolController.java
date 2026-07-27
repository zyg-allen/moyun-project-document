package com.moyun.agent.controller;

import com.moyun.agent.common.ApiResponse;
import com.moyun.agent.engine.tool.ToolContext;
import com.moyun.agent.engine.tool.ToolRegistry;
import com.moyun.agent.engine.tool.ToolResult;
import com.moyun.agent.entity.AgentTool;
import com.moyun.agent.service.ToolService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * 工具管理控制器
 *
 * <p>提供 Function Calling 工具的管理接口，包括：
 * <ul>
 *   <li>工具列表查询（全部/启用/智能体关联）</li>
 *   <li>智能体工具绑定</li>
 *   <li>工具的增删改查</li>
 *   <li>工具测试执行</li>
 * </ul>
 * </p>
 *
 * @author laomao
 * @time 2025/11/25
 */
@Slf4j
@Tag(name = "工具管理")
@RestController
@RequestMapping("/api/tool")
public class ToolController {

    @Autowired
    private ToolService toolService;

    @Autowired
    private ToolRegistry toolRegistry;

    /**
     * 获取所有工具列表
     *
     * @return 工具列表（按系统内置优先、分类排序）
     */
    @Operation(summary = "获取所有工具列表")
    @GetMapping("/list")
    public ApiResponse<List<AgentTool>> getAllTools() {
        return ApiResponse.success(toolService.getAllTools());
    }

    /**
     * 获取启用的工具列表
     *
     * @return 启用状态的工具列表
     */
    @Operation(summary = "获取启用的工具列表")
    @GetMapping("/enabled")
    public ApiResponse<List<AgentTool>> getEnabledTools() {
        return ApiResponse.success(toolService.getEnabledTools());
    }

    /**
     * 获取智能体关联的工具
     *
     * @param agentId 智能体ID
     * @return 该智能体关联的工具列表
     */
    @Operation(summary = "获取智能体关联的工具")
    @GetMapping("/agent/{agentId}")
    public ApiResponse<List<AgentTool>> getToolsByAgent(@PathVariable Long agentId) {
        return ApiResponse.success(toolService.getToolsByAgentId(agentId));
    }

    /**
     * 获取智能体关联的工具ID列表
     *
     * <p>用于前端编辑智能体时回显已选中的工具</p>
     *
     * @param agentId 智能体ID
     * @return 工具ID列表
     */
    @Operation(summary = "获取智能体关联的工具ID列表")
    @GetMapping("/agent/{agentId}/ids")
    public ApiResponse<List<Long>> getToolIdsByAgent(@PathVariable Long agentId) {
        return ApiResponse.success(toolService.getToolIdsByAgentId(agentId));
    }

    /**
     * 为智能体绑定工具
     *
     * <p>会先删除原有关联，再添加新关联</p>
     *
     * @param agentId 智能体ID
     * @param toolIds 工具ID列表（可为空，表示清空关联）
     * @return 操作结果
     */
    @Operation(summary = "为智能体绑定工具")
    @PostMapping("/agent/{agentId}/bind")
    public ApiResponse<Void> bindToolsToAgent(@PathVariable Long agentId, @RequestBody List<Long> toolIds) {
        toolService.bindToolsToAgent(agentId, toolIds);
        return ApiResponse.success();
    }

    /**
     * 创建自定义工具
     *
     * @param tool 工具信息
     * @return 创建后的工具（含ID）
     */
    @Operation(summary = "创建自定义工具")
    @PostMapping
    public ApiResponse<AgentTool> createTool(@RequestBody AgentTool tool) {
        return ApiResponse.success(toolService.createTool(tool));
    }

    /**
     * 更新工具
     *
     * @param tool 工具信息（需包含ID）
     * @return 操作结果
     */
    @Operation(summary = "更新工具")
    @PutMapping
    public ApiResponse<Void> updateTool(@RequestBody AgentTool tool) {
        toolService.updateTool(tool);
        return ApiResponse.success();
    }

    /**
     * 删除工具
     *
     * <p>注意：系统内置工具不允许删除</p>
     *
     * @param toolId 工具ID
     * @return 操作结果
     */
    @Operation(summary = "删除工具")
    @DeleteMapping("/{toolId}")
    public ApiResponse<Void> deleteTool(@PathVariable Long toolId) {
        boolean success = toolService.deleteTool(toolId);
        if (success) {
            return ApiResponse.success();
        } else {
            return ApiResponse.error("删除失败，可能是系统内置工具");
        }
    }

    /**
     * 切换工具启用状态
     *
     * @param toolId 工具ID
     * @param enabled 是否启用
     * @return 操作结果
     */
    @Operation(summary = "切换工具启用状态")
    @PutMapping("/{toolId}/toggle")
    public ApiResponse<Void> toggleToolEnabled(@PathVariable Long toolId, @RequestParam boolean enabled) {
        toolService.toggleToolEnabled(toolId, enabled);
        return ApiResponse.success();
    }

    /**
     * 测试工具执行
     *
     * <p>用于在工具管理页面测试工具是否正常工作</p>
     *
     * @param toolName 工具名称（唯一标识）
     * @param params 调用参数（可选）
     * @return 工具执行结果
     */
    @Operation(summary = "测试工具执行")
    @PostMapping("/test/{toolName}")
    public ApiResponse<ToolResult> testTool(@PathVariable String toolName,
                                       @RequestBody(required = false) Map<String, Object> params) {
        ToolContext context = ToolContext.builder()
                .agentId(0L)
                .conversationId(0L)
                .userQuery("测试调用")
                .build();

        ToolResult result = toolRegistry.executeTool(toolName, context, params != null ? params : Map.of());
        return ApiResponse.success(result);
    }
}
