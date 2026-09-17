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

    @lombok.Data
    public static class GenerateRequest {
        @NotBlank(message = "工作流描述不能为空")
        @Size(min = 10, max = 2000, message = "工作流描述长度应在10-2000字符之间")
        private String description;

        @Size(max = 100, message = "工作流名称不能超过100字符")
        private String workflowName;
    }
}
