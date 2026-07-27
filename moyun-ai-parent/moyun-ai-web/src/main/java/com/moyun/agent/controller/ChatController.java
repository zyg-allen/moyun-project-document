package com.moyun.agent.controller;

import com.moyun.agent.dto.ChatRequest;
import com.moyun.agent.common.ApiResponse;
import com.moyun.agent.common.ListResponse;
import com.moyun.agent.entity.Agent;
import com.moyun.agent.service.AgentService;
import com.moyun.agent.service.DynamicChatService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import com.moyun.agent.service.chat.StreamingSessionManager;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;

import java.util.List;

/**
 * 智能对话控制器
 *
 * <p>提供流式对话接口和智能体列表查询</p>
 *
 * @author laomao
 * @time 2025/11/19 17:52
 */
@Slf4j
@Tag(name = "智能对话")
@RestController
@RequestMapping("/api/chat")
public class ChatController {
    @Autowired
    private AgentService agentService;

    @Autowired
    private DynamicChatService dynamicChatService;

    @Autowired
    private StreamingSessionManager streamingSessionManager;

    /**
     * 流式对话接口
     *
     * <p>使用Server-Sent Events (SSE)实现流式响应，逐token返回AI生成内容</p>
     *
     * @param chatRequest 对话请求
     * @return 流式响应
     */
    @Operation(summary = "流式对话", description = "使用SSE实现实时对话，支持多模态图片输入")
    @PostMapping(value = "/stream", produces = "text/stream;charset=utf-8")
    public Flux<String> chat(@RequestBody ChatRequest chatRequest) {
        int imageCount = chatRequest.getImages() != null ? chatRequest.getImages().size() : 0;
        log.info("收到对话请求 - AgentID: {}, ConversationID: {}, Message: {}, 图片数: {}",
            chatRequest.getAgentId(), chatRequest.getConversationId(), chatRequest.getMessage(), imageCount);

        // 必须选择智能体
        if (chatRequest.getAgentId() == null) {
            log.warn("❌ 对话请求缺少agentId");
            return Flux.just("❌ 请先选择一个智能体");
        }

        log.debug("✅ 使用 DynamicChatService 处理对话");

        // 使用动态对话服务（使用智能体的提示词和知识库）
        return dynamicChatService.chat(
            chatRequest.getConversationId() != null ? chatRequest.getConversationId() : chatRequest.getMemoryId(),
            chatRequest.getMessage(),
            chatRequest.getAgentId(),
            chatRequest.getIsGreeting() != null ? chatRequest.getIsGreeting() : false,
            chatRequest.getImages()  // 多模态图片
        );
    }

    /**
     * 中断流式对话
     *
     * <p>发送中断信号停止正在生成的AI响应</p>
     *
     * @param conversationId 会话ID
     * @return 中断结果
     */
    @Operation(summary = "中断对话", description = "停止正在生成的AI响应")
    @PostMapping("/abort/{conversationId}")
    public ResponseEntity<ApiResponse<Boolean>> abortChat(@PathVariable Long conversationId) {
        log.info("收到中断请求 - conversationId: {}", conversationId);
        
        boolean success = streamingSessionManager.interruptSession(conversationId);
        if (success) {
            log.info("✅ 成功中断对话: conversationId={}", conversationId);
            return ResponseEntity.ok(ApiResponse.success(true));
        } else {
            log.warn("⚠️ 中断失败（会话不存在或已结束）: conversationId={}", conversationId);
            return ResponseEntity.ok(ApiResponse.success(false));
        }
    }

    /**
     * 重新生成AI回复
     *
     * <p>删除最后一条AI回复，重新生成答案</p>
     *
     * @param chatRequest 对话请求（包含conversationId、agentId和原始用户消息）
     * @return 流式响应
     */
    @Operation(summary = "重新生成", description = "重新生成AI的最后一条回复")
    @PostMapping(value = "/regenerate", produces = "text/stream;charset=utf-8")
    public Flux<String> regenerate(@RequestBody ChatRequest chatRequest) {
        log.info("收到重新生成请求 - AgentID: {}, ConversationID: {}, Message: {}",
            chatRequest.getAgentId(), chatRequest.getConversationId(), chatRequest.getMessage());

        if (chatRequest.getAgentId() == null || chatRequest.getConversationId() == null) {
            log.warn("❌ 重新生成请求缺少必要参数");
            return Flux.just("❌ 缺少必要参数");
        }

        // 重新调用对话服务（使用相同的消息重新生成）
        return dynamicChatService.chat(
            chatRequest.getConversationId(),
            chatRequest.getMessage(),
            chatRequest.getAgentId(),
            false
        );
    }

    /**
     * 获取启用的智能体列表
     *
     * <p>返回所有已启用的智能体，供用户选择</p>
     *
     * @return 启用的智能体列表
     */
    @Operation(summary = "获取启用的智能体列表", description = "查询所有已启用的智能体")
    @GetMapping("/agents")
    public ResponseEntity<ApiResponse<ListResponse<Agent>>> getAgents() {
        try {
            List<Agent> agents = agentService.listEnabled();
            log.debug("获取启用智能体列表成功 - 数量: {}", agents.size());
            return ResponseEntity.ok(ApiResponse.success(new ListResponse<>(agents)));
        } catch (Exception e) {
            log.error("获取启用智能体列表失败", e);
            return ResponseEntity.ok(ApiResponse.error("获取失败: " + e.getMessage()));
        }
    }
}
