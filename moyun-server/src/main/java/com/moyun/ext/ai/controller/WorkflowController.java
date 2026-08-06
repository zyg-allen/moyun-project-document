package com.moyun.ext.ai.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.moyun.core.base.AjaxResult;
import com.moyun.ext.ai.dto.WorkflowExecutionEvent;
import com.moyun.ext.ai.entity.Workflow;
import com.moyun.ext.ai.entity.WorkflowExecution;
import com.moyun.ext.ai.entity.WorkflowVersion;
import com.moyun.ext.ai.service.WorkflowService;
import com.moyun.ext.ai.engine.workflow.WorkflowEngine;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

@Slf4j
@Validated
@RestController
@RequestMapping("/cms/ai/workflow")
@RequiredArgsConstructor
@Tag(name = "工作流管理", description = "工作流的增删改查、执行和版本管理")
public class WorkflowController {

    private final WorkflowService workflowService;
    private final ObjectMapper objectMapper;

    @Operation(summary = "获取工作流列表", description = "返回所有工作流，按创建时间降序")
    @GetMapping("/list")
    @PreAuthorize("@ss.hasPermi('cms:ai:workflow:list')")
    public AjaxResult list() {
        try {
            List<Workflow> workflows = workflowService.listAll();
            log.debug("📊 获取工作流列表，数量: {}", workflows.size());
            return AjaxResult.success(workflows);
        } catch (Exception e) {
            log.error("❌ 获取工作流列表失败", e);
            return AjaxResult.error("获取失败: " + e.getMessage());
        }
    }

    @Operation(summary = "获取工作流详情", description = "根据ID获取工作流的完整信息")
    @GetMapping("/{id}")
    @PreAuthorize("@ss.hasPermi('cms:ai:workflow:query')")
    public AjaxResult get(@PathVariable @NotNull Long id) {
        try {
            Workflow workflow = workflowService.getById(id);
            if (workflow == null) {
                return AjaxResult.error("工作流不存在: " + id);
            }
            return AjaxResult.success(workflow);
        } catch (Exception e) {
            log.error("❌ 获取工作流失败: id={}", id, e);
            return AjaxResult.error("获取失败: " + e.getMessage());
        }
    }

    @Operation(summary = "创建工作流", description = "创建一个新的工作流，初始状态为草稿")
    @PostMapping("/create")
    @PreAuthorize("@ss.hasPermi('cms:ai:workflow:add')")
    public AjaxResult create(@Valid @RequestBody Workflow workflow) {
        try {
            Workflow created = workflowService.create(workflow);
            log.info("✅ 工作流创建成功: id={}, name={}", created.getId(), created.getName());
            return AjaxResult.success("创建成功", created);
        } catch (Exception e) {
            log.error("❌ 创建工作流失败", e);
            return AjaxResult.error("创建失败: " + e.getMessage());
        }
    }

    @Operation(summary = "更新工作流", description = "更新工作流的名称、描述等基本信息")
    @PutMapping("/update")
    @PreAuthorize("@ss.hasPermi('cms:ai:workflow:edit')")
    public AjaxResult update(@Valid @RequestBody Workflow workflow) {
        try {
            Workflow updated = workflowService.updateWorkflow(workflow);
            log.info("✅ 工作流更新成功: id={}, name={}", updated.getId(), updated.getName());
            return AjaxResult.success("更新成功", updated);
        } catch (Exception e) {
            log.error("❌ 更新工作流失败", e);
            return AjaxResult.error("更新失败: " + e.getMessage());
        }
    }

    @PostMapping("/{id}/graph")
    @PreAuthorize("@ss.hasPermi('cms:ai:workflow:edit')")
    public AjaxResult saveGraph(@PathVariable Long id, @RequestBody Map<String, String> body) {
        try {
            String graphData = body.get("graphData");
            workflowService.saveGraph(id, graphData);
            return AjaxResult.success();
        } catch (Exception e) {
            log.error("保存工作流图失败", e);
            return AjaxResult.error("保存失败: " + e.getMessage());
        }
    }

    @PostMapping("/{id}/publish")
    @PreAuthorize("@ss.hasPermi('cms:ai:workflow:publish')")
    public AjaxResult publish(@PathVariable Long id) {
        try {
            workflowService.publish(id);
            return AjaxResult.success();
        } catch (Exception e) {
            log.error("发布工作流失败", e);
            return AjaxResult.error("发布失败: " + e.getMessage());
        }
    }

    @PostMapping("/{id}/toggle")
    @PreAuthorize("@ss.hasPermi('cms:ai:workflow:edit')")
    public AjaxResult toggle(@PathVariable Long id, @RequestBody Map<String, Boolean> body) {
        try {
            Boolean enabled = body.get("enabled");
            if (Boolean.TRUE.equals(enabled)) {
                workflowService.enable(id);
            } else {
                workflowService.disable(id);
            }
            return AjaxResult.success();
        } catch (Exception e) {
            log.error("切换工作流状态失败", e);
            return AjaxResult.error("切换失败: " + e.getMessage());
        }
    }

    @PostMapping("/{id}/execute")
    @PreAuthorize("@ss.hasPermi('cms:ai:workflow:execute')")
    public AjaxResult execute(
            @PathVariable Long id, 
            @RequestParam(required = false, defaultValue = "false") Boolean testRun,
            @RequestBody(required = false) Map<String, Object> input) {
        try {
            WorkflowEngine.WorkflowResult execResult = Boolean.TRUE.equals(testRun) 
                    ? workflowService.executeForTest(id, input)
                    : workflowService.execute(id, input);
            Map<String, Object> data = new HashMap<>();
            data.put("output", execResult.getOutput());
            data.put("executionId", execResult.getExecutionId());
            data.put("durationMs", execResult.getDurationMs());
            data.put("nodeLogs", execResult.getNodeLogs());

            if (!execResult.isSuccess()) {
                return AjaxResult.error(execResult.getErrorMessage(), data);
            }
            return AjaxResult.success(data);
        } catch (Exception e) {
            log.error("执行工作流失败", e);
            return AjaxResult.error("执行失败: " + e.getMessage());
        }
    }

    @GetMapping("/{id}/executions")
    @PreAuthorize("@ss.hasPermi('cms:ai:workflow:query')")
    public AjaxResult getExecutions(@PathVariable Long id) {
        try {
            List<WorkflowExecution> executions = workflowService.getExecutionHistory(id);
            return AjaxResult.success(executions);
        } catch (Exception e) {
            log.error("获取执行历史失败", e);
            return AjaxResult.error("获取失败: " + e.getMessage());
        }
    }

    @GetMapping("/execution/{executionId}")
    @PreAuthorize("@ss.hasPermi('cms:ai:workflow:query')")
    public AjaxResult getExecution(@PathVariable Long executionId) {
        try {
            WorkflowExecution execution = workflowService.getExecution(executionId);
            return AjaxResult.success(execution);
        } catch (Exception e) {
            log.error("获取执行详情失败", e);
            return AjaxResult.error("获取失败: " + e.getMessage());
        }
    }

    @Operation(summary = "流式执行工作流（SSE）", description = "通过SSE实时推送执行进度和节点状态")
    @GetMapping(value = "/{id}/execute/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    @PreAuthorize("@ss.hasPermi('cms:ai:workflow:execute')")
    public SseEmitter executeStream(
            @PathVariable Long id,
            @RequestParam(required = false, defaultValue = "true") Boolean testRun,
            @RequestParam(required = false) String input) {
        
        log.info("📡 开始流式执行工作流: id={}, testRun={}, input={}", id, testRun, input);
        
        Map<String, Object> parsedInput = null;
        if (input != null && !input.isEmpty()) {
            try {
                parsedInput = objectMapper.readValue(input, Map.class);
            } catch (Exception e) {
                log.warn("解析input参数失败，使用纯文本: {}", input);
                parsedInput = new HashMap<>();
                parsedInput.put("input", input);
            }
        }
        final Map<String, Object> inputMap = parsedInput;
        
        SseEmitter emitter = new SseEmitter(5 * 60 * 1000L);
        
        new Thread(() -> {
            try {
                Consumer<WorkflowExecutionEvent> eventCallback = event -> {
                    try {
                        String eventJson = objectMapper.writeValueAsString(event);
                        emitter.send(SseEmitter.event()
                                .name(event.getType())
                                .data(eventJson));
                        log.debug("📤 发送事件: type={}, nodeId={}", event.getType(), event.getNodeId());
                    } catch (IOException e) {
                        log.error("❌ 发送SSE事件失败", e);
                        emitter.completeWithError(e);
                    }
                };
                
                WorkflowEngine.WorkflowResult result = workflowService.executeWithCallback(id, inputMap, testRun, eventCallback);
                
                if (result.isSuccess()) {
                    eventCallback.accept(WorkflowExecutionEvent.complete(
                            result.getOutput(),
                            result.getDurationMs()
                    ));
                } else {
                    eventCallback.accept(WorkflowExecutionEvent.error(
                            result.getErrorMessage()
                    ));
                }
                
                emitter.complete();
                log.info("✅ 工作流流式执行完成: id={}", id);
                
            } catch (Exception e) {
                log.error("❌ 工作流流式执行失败: id={}", id, e);
                try {
                    emitter.send(SseEmitter.event()
                            .name("error")
                            .data(objectMapper.writeValueAsString(
                                    WorkflowExecutionEvent.error(e.getMessage())
                            )));
                } catch (IOException ex) {
                    log.error("发送错误事件失败", ex);
                }
                emitter.completeWithError(e);
            }
        }).start();
        
        return emitter;
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("@ss.hasPermi('cms:ai:workflow:remove')")
    public AjaxResult delete(@PathVariable Long id) {
        try {
            workflowService.deleteWorkflow(id);
            return AjaxResult.success();
        } catch (Exception e) {
            log.error("删除工作流失败", e);
            return AjaxResult.error("删除失败: " + e.getMessage());
        }
    }

    @GetMapping("/published")
    @PreAuthorize("@ss.hasPermi('cms:ai:workflow:list')")
    public AjaxResult getPublished() {
        try {
            List<Workflow> workflows = workflowService.listEnabled();
            return AjaxResult.success(workflows);
        } catch (Exception e) {
            log.error("获取已发布工作流失败", e);
            return AjaxResult.error("获取失败: " + e.getMessage());
        }
    }

    @PostMapping("/{id}/bind/{agentId}")
    @PreAuthorize("@ss.hasPermi('cms:ai:workflow:edit')")
    public AjaxResult bindToAgent(@PathVariable Long id, @PathVariable Long agentId) {
        try {
            workflowService.bindToAgent(id, agentId);
            return AjaxResult.success();
        } catch (Exception e) {
            log.error("绑定工作流失败", e);
            return AjaxResult.error("绑定失败: " + e.getMessage());
        }
    }

    @DeleteMapping("/{id}/unbind/{agentId}")
    @PreAuthorize("@ss.hasPermi('cms:ai:workflow:edit')")
    public AjaxResult unbindFromAgent(@PathVariable Long id, @PathVariable Long agentId) {
        try {
            workflowService.unbindFromAgent(id, agentId);
            return AjaxResult.success();
        } catch (Exception e) {
            log.error("解绑工作流失败", e);
            return AjaxResult.error("解绑失败: " + e.getMessage());
        }
    }

    @GetMapping("/agent/{agentId}")
    @PreAuthorize("@ss.hasPermi('cms:ai:workflow:query')")
    public AjaxResult getAgentWorkflows(@PathVariable Long agentId) {
        try {
            List<Workflow> workflows = workflowService.getAgentWorkflows(agentId);
            return AjaxResult.success(workflows);
        } catch (Exception e) {
            log.error("获取智能体工作流失败", e);
            return AjaxResult.error("获取失败: " + e.getMessage());
        }
    }

    @PostMapping("/api/run/{workflowName}")
    @PreAuthorize("@ss.hasPermi('cms:ai:workflow:execute')")
    public AjaxResult runByName(@PathVariable String workflowName, @RequestBody(required = false) Map<String, Object> input) {
        try {
            WorkflowEngine.WorkflowResult execResult = workflowService.executeByName(workflowName, input);
            Map<String, Object> data = new HashMap<>();
            data.put("output", execResult.getOutput());
            data.put("executionId", execResult.getExecutionId());
            data.put("durationMs", execResult.getDurationMs());
            data.put("nodeLogs", execResult.getNodeLogs());

            if (!execResult.isSuccess()) {
                return AjaxResult.error(execResult.getErrorMessage(), data);
            }
            return AjaxResult.success(data);
        } catch (Exception e) {
            log.error("执行工作流失败", e);
            return AjaxResult.error("执行失败: " + e.getMessage());
        }
    }

    @GetMapping("/{id}/versions")
    @PreAuthorize("@ss.hasPermi('cms:ai:workflow:query')")
    public AjaxResult getVersions(@PathVariable Long id) {
        try {
            List<WorkflowVersion> versions = workflowService.getVersions(id);
            return AjaxResult.success(versions);
        } catch (Exception e) {
            log.error("获取版本列表失败", e);
            return AjaxResult.error("获取失败: " + e.getMessage());
        }
    }

    @PostMapping("/{id}/version")
    @PreAuthorize("@ss.hasPermi('cms:ai:workflow:add')")
    public AjaxResult createVersion(@PathVariable Long id, @RequestBody Map<String, String> body) {
        try {
            String graphData = body.get("graphData");
            String description = body.get("description");
            WorkflowVersion version = workflowService.createVersion(id, graphData, description);
            return AjaxResult.success(version);
        } catch (Exception e) {
            log.error("创建版本失败", e);
            return AjaxResult.error("创建失败: " + e.getMessage());
        }
    }

    @PostMapping("/{id}/rollback")
    @PreAuthorize("@ss.hasPermi('cms:ai:workflow:edit')")
    public AjaxResult rollbackVersion(@PathVariable Long id, @RequestBody Map<String, Integer> body) {
        try {
            Integer version = body.get("version");
            Workflow workflow = workflowService.rollbackVersion(id, version);
            return AjaxResult.success(workflow);
        } catch (Exception e) {
            log.error("回滚版本失败", e);
            return AjaxResult.error("回滚失败: " + e.getMessage());
        }
    }
}
