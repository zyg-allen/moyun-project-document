package com.moyun.ext.ai.controller;

import com.moyun.core.base.AjaxResult;
import com.moyun.ext.ai.service.WorkflowGeneratorService;
import com.moyun.ext.ai.service.WorkflowGeneratorService.GenerateResult;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@Slf4j
@Validated
@RestController
@RequestMapping("/cms/ai/workflow-generator")
@Tag(name = "工作流智能生成", description = "通过自然语言自动生成工作流")
@RequiredArgsConstructor
public class WorkflowGeneratorController {
    
    private final WorkflowGeneratorService generatorService;
    
    @Operation(summary = "生成工作流", description = "根据自然语言描述生成工作流JSON，不自动保存")
    @PostMapping("/generate")
    @PreAuthorize("@ss.hasPermi('cms:ai:workflow-generator:generate')")
    public AjaxResult generate(@Valid @RequestBody GenerateRequest request) {
        log.info("🪄 收到工作流生成请求，描述长度: {} 字符", request.getDescription().length());
        
        try {
            GenerateResult result = generatorService.generate(request.getDescription());
            
            if (result.isSuccess()) {
                log.info("✅ 工作流生成成功: {} 个节点", result.getNodeCount());
                return AjaxResult.success("生成成功", result);
            } else {
                return AjaxResult.error(result.getErrorMessage());
            }
        } catch (Exception e) {
            log.error("工作流生成失败", e);
            return AjaxResult.error("生成失败: " + e.getMessage());
        }
    }
    
    @Operation(summary = "生成并保存工作流", description = "根据自然语言描述生成工作流并自动保存")
    @PostMapping("/generate-and-save")
    @PreAuthorize("@ss.hasPermi('cms:ai:workflow-generator:add')")
    public AjaxResult generateAndSave(@RequestBody GenerateRequest request) {
        log.info("🪄 收到工作流生成并保存请求: {}", request.getDescription());
        
        if (request.getDescription() == null || request.getDescription().trim().isEmpty()) {
            return AjaxResult.error("请输入工作流描述");
        }
        
        try {
            Long workflowId = generatorService.generateAndSave(
                    request.getDescription(), 
                    request.getWorkflowName()
            );
            
            log.info("✅ 工作流生成并保存成功: id={}", workflowId);
            
            return AjaxResult.success("工作流已创建", Map.of(
                    "workflowId", workflowId,
                    "message", "工作流已生成并保存，可在工作流管理中查看和编辑"
            ));
        } catch (Exception e) {
            log.error("工作流生成保存失败", e);
            return AjaxResult.error("生成失败: " + e.getMessage());
        }
    }
    
    @Operation(summary = "优化工作流", description = "根据指令修改现有工作流")
    @PostMapping("/optimize/{workflowId}")
    @PreAuthorize("@ss.hasPermi('cms:ai:workflow-generator:edit')")
    public AjaxResult optimize(
            @PathVariable Long workflowId,
            @RequestBody OptimizeRequest request) {
        
        log.info("🔧 收到工作流优化请求: workflowId={}, instruction={}", workflowId, request.getInstruction());
        
        if (request.getInstruction() == null || request.getInstruction().trim().isEmpty()) {
            return AjaxResult.error("请输入修改指令");
        }
        
        try {
            GenerateResult result = generatorService.optimize(workflowId, request.getInstruction());
            
            if (result.isSuccess()) {
                log.info("✅ 工作流优化成功");
                return AjaxResult.success("优化成功", result);
            } else {
                return AjaxResult.error(result.getErrorMessage());
            }
        } catch (Exception e) {
            log.error("工作流优化失败", e);
            return AjaxResult.error("优化失败: " + e.getMessage());
        }
    }
    
    @Operation(summary = "获取生成提示", description = "获取工作流生成的示例和提示")
    @GetMapping("/tips")
    @PreAuthorize("@ss.hasPermi('cms:ai:workflow-generator:query')")
    public AjaxResult getTips() {
        return AjaxResult.success(Map.of(
                "examples", new String[]{
                        "创建一个简单的翻译流程：接收用户输入，翻译成英文后返回",
                        "创建客服流程：先判断用户问题类型，技术问题从知识库查询后回答，投诉问题直接转人工",
                        "创建内容审核流程：先用AI检测敏感词，有敏感词则拒绝，没有则调用API发布",
                        "创建数据处理流程：调用API获取数据，然后用代码处理，最后用LLM生成报告",
                        "创建智能问答流程：先从FAQ知识库检索，然后让客服助手结合检索结果回答"
                },
                "tips", new String[]{
                        "💡 可以直接说「用XX智能体处理」，系统会自动匹配",
                        "💡 可以说「从XX知识库查询」，系统会关联知识库",
                        "💡 支持条件分支：「如果...则...否则...」",
                        "💡 支持循环：「对每个...执行...」",
                        "💡 支持并行：「同时执行A和B，然后合并结果」"
                },
                "nodeTypes", new String[]{
                        "LLM调用 - AI生成、分析、翻译、总结",
                        "条件分支 - 根据条件走不同流程",
                        "知识库检索 - 从知识库中查找相关内容",
                        "智能体调用 - 调用已配置的智能体",
                        "HTTP请求 - 调用外部API",
                        "代码执行 - 执行JavaScript代码",
                        "工具调用 - 使用系统工具",
                        "分类器 - 对输入进行分类",
                        "信息提取 - 从文本提取结构化信息",
                        "循环 - 遍历列表执行操作",
                        "并行/合并 - 并行执行多个分支"
                }
        ));
    }
    
    @lombok.Data
    public static class GenerateRequest {
        @NotBlank(message = "工作流描述不能为空")
        @Size(min = 10, max = 2000, message = "工作流描述长度应在10-2000字符之间")
        private String description;
        
        @Size(max = 100, message = "工作流名称不能超过100字符")
        private String workflowName;
    }
    
    @lombok.Data
    public static class OptimizeRequest {
        @NotBlank(message = "修改指令不能为空")
        @Size(min = 5, max = 1000, message = "修改指令长度应在5-1000字符之间")
        private String instruction;
    }
}
