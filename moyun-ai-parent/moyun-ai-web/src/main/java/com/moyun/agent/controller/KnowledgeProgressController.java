package com.moyun.agent.controller;

import com.moyun.agent.common.ApiResponse;
import com.moyun.agent.service.KnowledgeProcessProgressService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * 知识库处理进度控制器
 *
 * <p>提供知识库文档处理进度的实时查询功能</p>
 *
 * @author laomao
 */
@Slf4j
@Tag(name = "知识库进度管理")
@RestController
@RequestMapping("/api/knowledge/progress")
public class KnowledgeProgressController {

    @Autowired
    private KnowledgeProcessProgressService progressService;

    /**
     * 获取知识库处理进度
     *
     * @param knowledgeId 知识库ID
     * @return 进度信息
     */
    @Operation(summary = "获取处理进度")
    @GetMapping("/{knowledgeId}")
    public ResponseEntity<ApiResponse<KnowledgeProcessProgressService.ProcessProgress>> getProgress(
            @PathVariable Long knowledgeId) {
        try {
            KnowledgeProcessProgressService.ProcessProgress progress = progressService.getProgress(knowledgeId);

            if (progress == null) {
                // 没有进度信息，可能是还未开始或已完成很久
                return ResponseEntity.ok(ApiResponse.success("无进度信息", null));
            }

            return ResponseEntity.ok(ApiResponse.success(progress));

        } catch (Exception e) {
            log.error("获取进度失败 - knowledgeId: {}", knowledgeId, e);
            return ResponseEntity.ok(ApiResponse.error("获取进度失败: " + e.getMessage()));
        }
    }

    /**
     * 检查是否正在处理
     *
     * @param knowledgeId 知识库ID
     * @return 是否正在处理
     */
    @Operation(summary = "检查是否正在处理")
    @GetMapping("/{knowledgeId}/is-processing")
    public ResponseEntity<ApiResponse<Boolean>> isProcessing(@PathVariable Long knowledgeId) {
        try {
            boolean processing = progressService.isProcessing(knowledgeId);
            return ResponseEntity.ok(ApiResponse.success(processing));
        } catch (Exception e) {
            log.error("检查处理状态失败 - knowledgeId: {}", knowledgeId, e);
            return ResponseEntity.ok(ApiResponse.error("检查失败: " + e.getMessage()));
        }
    }
}
