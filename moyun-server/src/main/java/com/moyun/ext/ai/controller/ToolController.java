package com.moyun.ext.ai.controller;

import com.moyun.core.base.AjaxResult;
import com.moyun.ext.ai.engine.tool.ToolContext;
import com.moyun.ext.ai.engine.tool.ToolRegistry;
import com.moyun.ext.ai.engine.tool.ToolResult;
import com.moyun.ext.ai.entity.AgentTool;
import com.moyun.ext.ai.service.ToolService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@Slf4j
@Tag(name = "工具管理")
@RestController
@RequestMapping("/cms/ai/tool")
public class ToolController {

    @Autowired
    private ToolService toolService;

    @Autowired
    private ToolRegistry toolRegistry;

    @Operation(summary = "获取所有工具列表")
    @GetMapping("/list")
    @PreAuthorize("@ss.hasPermi('cms:ai:tool:list')")
    public AjaxResult getAllTools() {
        return AjaxResult.success(toolService.getAllTools());
    }

    @Operation(summary = "获取启用的工具列表")
    @GetMapping("/enabled")
    @PreAuthorize("@ss.hasPermi('cms:ai:tool:list')")
    public AjaxResult getEnabledTools() {
        return AjaxResult.success(toolService.getEnabledTools());
    }

    @Operation(summary = "获取智能体关联的工具")
    @GetMapping("/agent/{agentId}")
    @PreAuthorize("@ss.hasPermi('cms:ai:tool:query')")
    public AjaxResult getToolsByAgent(@PathVariable Long agentId) {
        return AjaxResult.success(toolService.getToolsByAgentId(agentId));
    }

    @Operation(summary = "获取智能体关联的工具ID列表")
    @GetMapping("/agent/{agentId}/ids")
    @PreAuthorize("@ss.hasPermi('cms:ai:tool:query')")
    public AjaxResult getToolIdsByAgent(@PathVariable Long agentId) {
        return AjaxResult.success(toolService.getToolIdsByAgentId(agentId));
    }

    @Operation(summary = "为智能体绑定工具")
    @PostMapping("/agent/{agentId}/bind")
    @PreAuthorize("@ss.hasPermi('cms:ai:tool:edit')")
    public AjaxResult bindToolsToAgent(@PathVariable Long agentId, @RequestBody List<Long> toolIds) {
        toolService.bindToolsToAgent(agentId, toolIds);
        return AjaxResult.success();
    }

    @Operation(summary = "创建自定义工具")
    @PostMapping
    @PreAuthorize("@ss.hasPermi('cms:ai:tool:add')")
    public AjaxResult createTool(@RequestBody AgentTool tool) {
        return AjaxResult.success(toolService.createTool(tool));
    }

    @Operation(summary = "更新工具")
    @PutMapping
    @PreAuthorize("@ss.hasPermi('cms:ai:tool:edit')")
    public AjaxResult updateTool(@RequestBody AgentTool tool) {
        toolService.updateTool(tool);
        return AjaxResult.success();
    }

    @Operation(summary = "删除工具")
    @DeleteMapping("/{toolId}")
    @PreAuthorize("@ss.hasPermi('cms:ai:tool:remove')")
    public AjaxResult deleteTool(@PathVariable Long toolId) {
        boolean success = toolService.deleteTool(toolId);
        if (success) {
            return AjaxResult.success();
        } else {
            return AjaxResult.error("删除失败，可能是系统内置工具");
        }
    }

    @Operation(summary = "切换工具启用状态")
    @PutMapping("/{toolId}/toggle")
    @PreAuthorize("@ss.hasPermi('cms:ai:tool:edit')")
    public AjaxResult toggleToolEnabled(@PathVariable Long toolId, @RequestParam boolean enabled) {
        toolService.toggleToolEnabled(toolId, enabled);
        return AjaxResult.success();
    }

    @Operation(summary = "测试工具执行")
    @PostMapping("/test/{toolName}")
    @PreAuthorize("@ss.hasPermi('cms:ai:tool:query')")
    public AjaxResult testTool(@PathVariable String toolName,
                                       @RequestBody(required = false) Map<String, Object> params) {
        ToolContext context = ToolContext.builder()
                .agentId(0L)
                .conversationId(0L)
                .userQuery("测试调用")
                .build();

        ToolResult result = toolRegistry.executeTool(toolName, context, params != null ? params : Map.of());
        return AjaxResult.success(result);
    }
}
