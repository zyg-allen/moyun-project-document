package com.moyun.ext.ai.controller;

import com.moyun.core.base.AjaxResult;
import com.moyun.ext.ai.service.KnowledgeProcessProgressService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@Slf4j
@Tag(name = "知识库进度管理")
@RestController
@RequestMapping("/cms/ai/knowledge/progress")
public class KnowledgeProgressController {

    @Autowired
    private KnowledgeProcessProgressService progressService;

    @Operation(summary = "获取处理进度")
    @GetMapping("/{knowledgeId}")
    @PreAuthorize("@ss.hasPermi('cms:ai:knowledge-base:query')")
    public AjaxResult getProgress(
            @PathVariable Long knowledgeId) {
        try {
            KnowledgeProcessProgressService.ProcessProgress progress = progressService.getProgress(knowledgeId);

            if (progress == null) {
                return AjaxResult.success("无进度信息", null);
            }

            return AjaxResult.success(progress);

        } catch (Exception e) {
            log.error("获取进度失败 - knowledgeId: {}", knowledgeId, e);
            return AjaxResult.error("获取进度失败: " + e.getMessage());
        }
    }

    @Operation(summary = "检查是否正在处理")
    @GetMapping("/{knowledgeId}/is-processing")
    @PreAuthorize("@ss.hasPermi('cms:ai:knowledge-base:query')")
    public AjaxResult isProcessing(@PathVariable Long knowledgeId) {
        try {
            boolean processing = progressService.isProcessing(knowledgeId);
            return AjaxResult.success(processing);
        } catch (Exception e) {
            log.error("检查处理状态失败 - knowledgeId: {}", knowledgeId, e);
            return AjaxResult.error("检查失败: " + e.getMessage());
        }
    }
}
