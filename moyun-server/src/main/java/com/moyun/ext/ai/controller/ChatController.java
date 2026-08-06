package com.moyun.ext.ai.controller;

import com.moyun.core.base.AjaxResult;
import com.moyun.ext.ai.dto.ChatRequest;
import com.moyun.ext.ai.common.ListResponse;
import com.moyun.ext.ai.entity.Agent;
import com.moyun.ext.ai.service.AgentService;
import com.moyun.ext.ai.service.DynamicChatService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import com.moyun.ext.ai.service.chat.StreamingSessionManager;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;

import java.util.List;

@Slf4j
@Tag(name = "智能对话")
@RestController
@RequestMapping("/cms/ai/chat")
public class ChatController {
    @Autowired
    private AgentService agentService;

    @Autowired
    private DynamicChatService dynamicChatService;

    @Autowired
    private StreamingSessionManager streamingSessionManager;

    @Operation(summary = "流式对话", description = "使用SSE实现实时对话，支持多模态图片输入")
    @PostMapping(value = "/stream", produces = "text/stream;charset=utf-8")
    @PreAuthorize("@ss.hasPermi('cms:ai:chat:list')")
    public Flux<String> chat(@RequestBody ChatRequest chatRequest) {
        int imageCount = chatRequest.getImages() != null ? chatRequest.getImages().size() : 0;
        log.info("收到对话请求 - AgentID: {}, ConversationID: {}, Message: {}, 图片数: {}",
            chatRequest.getAgentId(), chatRequest.getConversationId(), chatRequest.getMessage(), imageCount);

        if (chatRequest.getAgentId() == null) {
            log.warn("❌ 对话请求缺少agentId");
            return Flux.just("❌ 请先选择一个智能体");
        }

        log.debug("✅ 使用 DynamicChatService 处理对话");

        return dynamicChatService.chat(
            chatRequest.getConversationId() != null ? chatRequest.getConversationId() : chatRequest.getMemoryId(),
            chatRequest.getMessage(),
            chatRequest.getAgentId(),
            chatRequest.getIsGreeting() != null ? chatRequest.getIsGreeting() : false,
            chatRequest.getImages()
        );
    }

    @Operation(summary = "中断对话", description = "停止正在生成的AI响应")
    @PostMapping("/abort/{conversationId}")
    @PreAuthorize("@ss.hasPermi('cms:ai:chat:list')")
    public AjaxResult abortChat(@PathVariable Long conversationId) {
        log.info("收到中断请求 - conversationId: {}", conversationId);

        boolean success = streamingSessionManager.interruptSession(conversationId);
        if (success) {
            log.info("✅ 成功中断对话: conversationId={}", conversationId);
            return AjaxResult.success(true);
        } else {
            log.warn("⚠️ 中断失败（会话不存在或已结束）: conversationId={}", conversationId);
            return AjaxResult.success(false);
        }
    }

    @Operation(summary = "重新生成", description = "重新生成AI的最后一条回复")
    @PostMapping(value = "/regenerate", produces = "text/stream;charset=utf-8")
    @PreAuthorize("@ss.hasPermi('cms:ai:chat:list')")
    public Flux<String> regenerate(@RequestBody ChatRequest chatRequest) {
        log.info("收到重新生成请求 - AgentID: {}, ConversationID: {}, Message: {}",
            chatRequest.getAgentId(), chatRequest.getConversationId(), chatRequest.getMessage());

        if (chatRequest.getAgentId() == null || chatRequest.getConversationId() == null) {
            log.warn("❌ 重新生成请求缺少必要参数");
            return Flux.just("❌ 缺少必要参数");
        }

        return dynamicChatService.chat(
            chatRequest.getConversationId(),
            chatRequest.getMessage(),
            chatRequest.getAgentId(),
            false
        );
    }

    @Operation(summary = "获取启用的智能体列表", description = "查询所有已启用的智能体")
    @GetMapping("/agents")
    @PreAuthorize("@ss.hasPermi('cms:ai:chat:list')")
    public AjaxResult getAgents() {
        try {
            List<Agent> agents = agentService.listEnabled();
            log.debug("获取启用智能体列表成功 - 数量: {}", agents.size());
            return AjaxResult.success(new ListResponse<>(agents));
        } catch (Exception e) {
            log.error("获取启用智能体列表失败", e);
            return AjaxResult.error("获取失败: " + e.getMessage());
        }
    }
}
