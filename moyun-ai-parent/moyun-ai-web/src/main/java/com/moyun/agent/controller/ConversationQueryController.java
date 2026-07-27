package com.moyun.agent.controller;

import com.moyun.agent.common.Result;
import com.moyun.agent.dto.DataQueryRequest;
import com.moyun.agent.service.DataAnalysisConversationService;
import com.moyun.agent.vo.DataQueryResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

/**
 * 多轮对话查询控制器
 * 
 * @author laomao
 */
@Slf4j
@RestController
@RequestMapping("/api/conversation-query")
@Tag(name = "多轮对话查询", description = "支持上下文感知的连续查询")
public class ConversationQueryController {
    
    @Autowired(required = false)
    private DataAnalysisConversationService conversationService;
    
    /**
     * 带上下文的智能查询
     */
    @Operation(summary = "多轮对话查询", description = "支持追问和上下文理解")
    @PostMapping("/query")
    public Result<DataQueryResponse> conversationQuery(@RequestBody DataQueryRequest request) {
        try {
            if (conversationService == null) {
                return Result.error("多轮对话功能未启用");
            }
            
            DataQueryResponse response = conversationService.queryWithContext(request);
            return Result.success(response);
        } catch (Exception e) {
            log.error("多轮对话查询失败", e);
            return Result.error("查询失败: " + e.getMessage());
        }
    }
    
    /**
     * 清除会话上下文
     */
    @Operation(summary = "清除会话上下文")
    @DeleteMapping("/session/{sessionId}")
    public Result<Void> clearSession(@PathVariable String sessionId) {
        try {
            if (conversationService == null) {
                return Result.error("多轮对话功能未启用");
            }
            
            conversationService.clearContext(sessionId);
            return Result.success(null);
        } catch (Exception e) {
            log.error("清除会话失败", e);
            return Result.error("清除失败: " + e.getMessage());
        }
    }
    
    /**
     * 检查是否为追问
     */
    @Operation(summary = "检查是否为追问查询")
    @GetMapping("/check-followup")
    public Result<Boolean> checkFollowUp(
            @RequestParam String query,
            @RequestParam String sessionId) {
        try {
            if (conversationService == null) {
                return Result.error("多轮对话功能未启用");
            }
            
            boolean isFollowUp = conversationService.isFollowUpQuery(query, sessionId);
            return Result.success(isFollowUp);
        } catch (Exception e) {
            log.error("检查追问失败", e);
            return Result.error("检查失败: " + e.getMessage());
        }
    }
}
