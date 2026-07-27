package com.moyun.agent.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.moyun.agent.common.Result;
import com.moyun.agent.dto.WorkflowExecutionEvent;
import com.moyun.agent.entity.Workflow;
import com.moyun.agent.entity.WorkflowExecution;
import com.moyun.agent.entity.WorkflowVersion;
import com.moyun.agent.service.WorkflowService;
import com.moyun.agent.engine.workflow.WorkflowEngine;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

/**
 * 工作流控制器
 * 
 * <p>提供工作流的全生命周期管理API，包括：</p>
 * <ul>
 *     <li>列表查询和详情查看</li>
 *     <li>创建、更新、删除</li>
 *     <li>发布、启用、禁用</li>
 *     <li>保存图数据</li>
 *     <li>执行工作流</li>
 *     <li>版本管理</li>
 *     <li>统计数据</li>
 * </ul>
 *
 * @author laomao
 * @since 2025-11-30
 */
@Slf4j
@Validated
@RestController
@RequestMapping("/api/workflow")
@RequiredArgsConstructor
@Tag(name = "工作流管理", description = "工作流的增删改查、执行和版本管理")
public class WorkflowController {

    private final WorkflowService workflowService;
    private final ObjectMapper objectMapper;

    /**
     * 获取工作流列表
     * 
     * <p>按创建时间降序返回所有工作流</p>
     * 
     * @return 工作流列表
     */
    @Operation(summary = "获取工作流列表", description = "返回所有工作流，按创建时间降序")
    @GetMapping("/list")
    public Result<List<Workflow>> list() {
        try {
            List<Workflow> workflows = workflowService.listAll();
            log.debug("📊 获取工作流列表，数量: {}", workflows.size());
            return Result.success(workflows);
        } catch (Exception e) {
            log.error("❌ 获取工作流列表失败", e);
            return Result.error("获取失败: " + e.getMessage());
        }
    }

    /**
     * 获取单个工作流详情
     * 
     * @param id 工作流ID
     * @return 工作流详情
     */
    @Operation(summary = "获取工作流详情", description = "根据ID获取工作流的完整信息")
    @GetMapping("/{id}")
    public Result<Workflow> get(@PathVariable @NotNull Long id) {
        try {
            Workflow workflow = workflowService.getById(id);
            if (workflow == null) {
                return Result.error("工作流不存在: " + id);
            }
            return Result.success(workflow);
        } catch (Exception e) {
            log.error("❌ 获取工作流失败: id={}", id, e);
            return Result.error("获取失败: " + e.getMessage());
        }
    }

    /**
     * 公开分享接口 - 获取工作流用于预览
     * 
     * <p>此接口不需要登录，用于分享链接访问</p>
     * 
     * @param id 工作流ID
     * @return 工作流详情（仅包含必要信息）
     */
    @Operation(summary = "分享预览", description = "公开接口，获取工作流用于分享预览")
    @GetMapping("/share/{id}")
    public Result<Workflow> getForShare(@PathVariable @NotNull Long id) {
        try {
            Workflow workflow = workflowService.getById(id);
            if (workflow == null) {
                return Result.error("工作流不存在或已删除");
            }
            log.info("📤 分享访问工作流: id={}, name={}", id, workflow.getName());
            return Result.success(workflow);
        } catch (Exception e) {
            log.error("❌ 获取分享工作流失败: id={}", id, e);
            return Result.error("获取失败: " + e.getMessage());
        }
    }

    /**
     * 创建工作流
     * 
     * <p>创建新的工作流，初始状态为草稿（draft）</p>
     * 
     * @param workflow 工作流信息
     * @return 创建后的工作流
     */
    @Operation(summary = "创建工作流", description = "创建一个新的工作流，初始状态为草稿")
    @PostMapping("/create")
    public Result<Workflow> create(@Valid @RequestBody Workflow workflow) {
        try {
            Workflow created = workflowService.create(workflow);
            log.info("✅ 工作流创建成功: id={}, name={}", created.getId(), created.getName());
            return Result.success("创建成功", created);
        } catch (Exception e) {
            log.error("❌ 创建工作流失败", e);
            return Result.error("创建失败: " + e.getMessage());
        }
    }

    /**
     * 更新工作流
     * 
     * <p>更新工作流的基本信息（名称、描述等）</p>
     * 
     * @param workflow 工作流信息
     * @return 更新后的工作流
     */
    @Operation(summary = "更新工作流", description = "更新工作流的名称、描述等基本信息")
    @PutMapping("/update")
    public Result<Workflow> update(@Valid @RequestBody Workflow workflow) {
        try {
            Workflow updated = workflowService.updateWorkflow(workflow);
            log.info("✅ 工作流更新成功: id={}, name={}", updated.getId(), updated.getName());
            return Result.success("更新成功", updated);
        } catch (Exception e) {
            log.error("❌ 更新工作流失败", e);
            return Result.error("更新失败: " + e.getMessage());
        }
    }

    /**
     * 保存工作流图
     */
    @PostMapping("/{id}/graph")
    public Map<String, Object> saveGraph(@PathVariable Long id, @RequestBody Map<String, String> body) {
        Map<String, Object> result = new HashMap<>();
        try {
            String graphData = body.get("graphData");
            workflowService.saveGraph(id, graphData);
            result.put("success", true);
        } catch (Exception e) {
            log.error("保存工作流图失败", e);
            result.put("success", false);
            result.put("message", e.getMessage());
        }
        return result;
    }

    /**
     * 发布工作流
     */
    @PostMapping("/{id}/publish")
    public Map<String, Object> publish(@PathVariable Long id) {
        Map<String, Object> result = new HashMap<>();
        try {
            workflowService.publish(id);
            result.put("success", true);
        } catch (Exception e) {
            log.error("发布工作流失败", e);
            result.put("success", false);
            result.put("message", e.getMessage());
        }
        return result;
    }

    /**
     * 启用/禁用工作流
     */
    @PostMapping("/{id}/toggle")
    public Map<String, Object> toggle(@PathVariable Long id, @RequestBody Map<String, Boolean> body) {
        Map<String, Object> result = new HashMap<>();
        try {
            Boolean enabled = body.get("enabled");
            if (Boolean.TRUE.equals(enabled)) {
                workflowService.enable(id);
            } else {
                workflowService.disable(id);
            }
            result.put("success", true);
        } catch (Exception e) {
            log.error("切换工作流状态失败", e);
            result.put("success", false);
            result.put("message", e.getMessage());
        }
        return result;
    }

    /**
     * 执行工作流
     * @param id 工作流ID
     * @param testRun 是否为测试运行（测试运行跳过enabled检查，允许执行未发布的工作流）
     * @param input 输入参数
     */
    @PostMapping("/{id}/execute")
    public Map<String, Object> execute(
            @PathVariable Long id, 
            @RequestParam(required = false, defaultValue = "false") Boolean testRun,
            @RequestBody(required = false) Map<String, Object> input) {
        Map<String, Object> result = new HashMap<>();
        try {
            // 测试运行使用executeForTest，正式运行使用execute
            WorkflowEngine.WorkflowResult execResult = Boolean.TRUE.equals(testRun) 
                    ? workflowService.executeForTest(id, input)
                    : workflowService.execute(id, input);
            result.put("success", execResult.isSuccess());

            Map<String, Object> data = new HashMap<>();
            data.put("output", execResult.getOutput());
            data.put("executionId", execResult.getExecutionId());
            data.put("durationMs", execResult.getDurationMs());
            data.put("nodeLogs", execResult.getNodeLogs());
            result.put("data", data);

            if (!execResult.isSuccess()) {
                result.put("message", execResult.getErrorMessage());
            }
        } catch (Exception e) {
            log.error("执行工作流失败", e);
            result.put("success", false);
            result.put("message", e.getMessage());
        }
        return result;
    }

    /**
     * 获取执行历史
     */
    @GetMapping("/{id}/executions")
    public Map<String, Object> getExecutions(@PathVariable Long id) {
        Map<String, Object> result = new HashMap<>();
        try {
            List<WorkflowExecution> executions = workflowService.getExecutionHistory(id);
            result.put("success", true);
            result.put("data", executions);
        } catch (Exception e) {
            log.error("获取执行历史失败", e);
            result.put("success", false);
            result.put("message", e.getMessage());
        }
        return result;
    }

    /**
     * 获取执行详情
     */
    @GetMapping("/execution/{executionId}")
    public Map<String, Object> getExecution(@PathVariable Long executionId) {
        Map<String, Object> result = new HashMap<>();
        try {
            WorkflowExecution execution = workflowService.getExecution(executionId);
            result.put("success", true);
            result.put("data", execution);
        } catch (Exception e) {
            log.error("获取执行详情失败", e);
            result.put("success", false);
            result.put("message", e.getMessage());
        }
        return result;
    }

    /**
     * 流式执行工作流（SSE）
     * 
     * <p>通过Server-Sent Events实时推送执行进度，包括：</p>
     * <ul>
     *     <li>工作流开始事件</li>
     *     <li>每个节点的开始/完成/错误事件</li>
     *     <li>节点输出数据</li>
     *     <li>工作流完成/错误事件</li>
     * </ul>
     * 
     * @param id 工作流ID
     * @param testRun 是否为测试运行（可选，默认true）
     * @param input 输入参数（可选）
     * @return SSE流
     */
    @Operation(summary = "流式执行工作流（SSE）", description = "通过SSE实时推送执行进度和节点状态")
    @GetMapping(value = "/{id}/execute/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter executeStream(
            @PathVariable Long id,
            @RequestParam(required = false, defaultValue = "true") Boolean testRun,
            @RequestParam(required = false) String input) {
        
        log.info("📡 开始流式执行工作流: id={}, testRun={}, input={}", id, testRun, input);
        
        // 解析input参数（如果是JSON字符串）
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
        
        SseEmitter emitter = new SseEmitter(5 * 60 * 1000L); // 5分钟超时
        
        // 在新线程中异步执行，避免阻塞
        new Thread(() -> {
            try {
                // 创建事件回调
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
                
                // 执行工作流（带事件回调）
                WorkflowEngine.WorkflowResult result = workflowService.executeWithCallback(id, inputMap, testRun, eventCallback);
                
                // 发送完成事件
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

    /**
     * 删除工作流
     */
    @DeleteMapping("/{id}")
    public Map<String, Object> delete(@PathVariable Long id) {
        Map<String, Object> result = new HashMap<>();
        try {
            workflowService.deleteWorkflow(id);
            result.put("success", true);
        } catch (Exception e) {
            log.error("删除工作流失败", e);
            result.put("success", false);
            result.put("message", e.getMessage());
        }
        return result;
    }

    /**
     * 获取已发布的工作流列表（可被智能体绑定）
     */
    @GetMapping("/published")
    public Map<String, Object> getPublished() {
        Map<String, Object> result = new HashMap<>();
        try {
            List<Workflow> workflows = workflowService.listEnabled();
            result.put("success", true);
            result.put("data", workflows);
        } catch (Exception e) {
            log.error("获取已发布工作流失败", e);
            result.put("success", false);
            result.put("message", e.getMessage());
        }
        return result;
    }

    /**
     * 绑定工作流到智能体
     */
    @PostMapping("/{id}/bind/{agentId}")
    public Map<String, Object> bindToAgent(@PathVariable Long id, @PathVariable Long agentId) {
        Map<String, Object> result = new HashMap<>();
        try {
            workflowService.bindToAgent(id, agentId);
            result.put("success", true);
        } catch (Exception e) {
            log.error("绑定工作流失败", e);
            result.put("success", false);
            result.put("message", e.getMessage());
        }
        return result;
    }

    /**
     * 解绑工作流
     */
    @DeleteMapping("/{id}/unbind/{agentId}")
    public Map<String, Object> unbindFromAgent(@PathVariable Long id, @PathVariable Long agentId) {
        Map<String, Object> result = new HashMap<>();
        try {
            workflowService.unbindFromAgent(id, agentId);
            result.put("success", true);
        } catch (Exception e) {
            log.error("解绑工作流失败", e);
            result.put("success", false);
            result.put("message", e.getMessage());
        }
        return result;
    }

    /**
     * 获取智能体绑定的工作流
     */
    @GetMapping("/agent/{agentId}")
    public Map<String, Object> getAgentWorkflows(@PathVariable Long agentId) {
        Map<String, Object> result = new HashMap<>();
        try {
            List<Workflow> workflows = workflowService.getAgentWorkflows(agentId);
            result.put("success", true);
            result.put("data", workflows);
        } catch (Exception e) {
            log.error("获取智能体工作流失败", e);
            result.put("success", false);
            result.put("message", e.getMessage());
        }
        return result;
    }

    /**
     * 公开API - 通过工作流名称执行
     * 这个API可以被外部系统调用
     */
    @PostMapping("/api/run/{workflowName}")
    public Map<String, Object> runByName(@PathVariable String workflowName, @RequestBody(required = false) Map<String, Object> input) {
        Map<String, Object> result = new HashMap<>();
        try {
            WorkflowEngine.WorkflowResult execResult = workflowService.executeByName(workflowName, input);
            result.put("success", execResult.isSuccess());

            Map<String, Object> data = new HashMap<>();
            data.put("output", execResult.getOutput());
            data.put("executionId", execResult.getExecutionId());
            data.put("durationMs", execResult.getDurationMs());
            data.put("nodeLogs", execResult.getNodeLogs());
            result.put("data", data);

            if (!execResult.isSuccess()) {
                result.put("message", execResult.getErrorMessage());
            }
        } catch (Exception e) {
            log.error("执行工作流失败", e);
            result.put("success", false);
            result.put("message", e.getMessage());
        }
        return result;
    }

    /**
     * 获取工作流版本列表
     */
    @GetMapping("/{id}/versions")
    public Map<String, Object> getVersions(@PathVariable Long id) {
        Map<String, Object> result = new HashMap<>();
        try {
            List<WorkflowVersion> versions = workflowService.getVersions(id);
            result.put("success", true);
            result.put("data", versions);
        } catch (Exception e) {
            log.error("获取版本列表失败", e);
            result.put("success", false);
            result.put("message", e.getMessage());
        }
        return result;
    }

    /**
     * 创建新版本
     */
    @PostMapping("/{id}/version")
    public Map<String, Object> createVersion(@PathVariable Long id, @RequestBody Map<String, String> body) {
        Map<String, Object> result = new HashMap<>();
        try {
            String graphData = body.get("graphData");
            String description = body.get("description");
            WorkflowVersion version = workflowService.createVersion(id, graphData, description);
            result.put("success", true);
            result.put("data", version);
        } catch (Exception e) {
            log.error("创建版本失败", e);
            result.put("success", false);
            result.put("message", e.getMessage());
        }
        return result;
    }

    /**
     * 回滚到指定版本
     */
    @PostMapping("/{id}/rollback")
    public Map<String, Object> rollbackVersion(@PathVariable Long id, @RequestBody Map<String, Integer> body) {
        Map<String, Object> result = new HashMap<>();
        try {
            Integer version = body.get("version");
            Workflow workflow = workflowService.rollbackVersion(id, version);
            result.put("success", true);
            result.put("data", workflow);
        } catch (Exception e) {
            log.error("回滚版本失败", e);
            result.put("success", false);
            result.put("message", e.getMessage());
        }
        return result;
    }
}
