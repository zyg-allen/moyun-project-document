package com.moyun.ext.ai.controller;

import com.moyun.core.base.AjaxResult;
import com.moyun.ext.ai.dto.ChatRequest;
import com.moyun.ext.ai.common.ListResponse;
import com.moyun.ext.ai.entity.Agent;
import com.moyun.ext.ai.entity.AiSceneConfig;
import com.moyun.ext.ai.service.AgentService;
import com.moyun.ext.ai.service.DynamicChatService;
import com.moyun.ext.aiapp.registry.AiSceneRegistry;
import com.moyun.ext.aiapp.support.AiExecuteLogService;
import com.moyun.ext.aiapp.support.SceneRateLimiter;
import com.moyun.util.security.SecurityUtils;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import com.moyun.ext.ai.service.chat.StreamingSessionManager;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;

import java.util.List;
import java.util.UUID;

@Slf4j
@Tag(name = "智能对话")
@RestController
@RequestMapping("/cms/ai/chat")
public class ChatController {

    /** 对话链路治理场景（ai_scene_config.default_chat 行承载限流参数） */
    private static final String SCENE_DEFAULT_CHAT = "default_chat";

    @Autowired
    private AgentService agentService;

    @Autowired
    private DynamicChatService dynamicChatService;

    @Autowired
    private StreamingSessionManager streamingSessionManager;

    @Autowired
    private AiSceneRegistry aiSceneRegistry;

    @Autowired
    private SceneRateLimiter sceneRateLimiter;

    @Autowired
    private AiExecuteLogService aiExecuteLogService;

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

        // default_chat 治理前置——「场景×用户」限流（问候语系统触发，不占用户额度）
        boolean isGreeting = Boolean.TRUE.equals(chatRequest.getIsGreeting());
        Long userId = currentUserId();
        if (!isGreeting && !tryAcquireChatRateLimit(userId, chatRequest.getMessage())) {
            return Flux.just("❌ 请求过于频繁，请稍后再试");
        }

        String requestId = UUID.randomUUID().toString().replace("-", "");
        long startTime = System.currentTimeMillis();
        StringBuilder output = new StringBuilder();
        return dynamicChatService.chat(
            chatRequest.getConversationId() != null ? chatRequest.getConversationId() : chatRequest.getMemoryId(),
            chatRequest.getMessage(),
            chatRequest.getAgentId(),
            isGreeting,
            chatRequest.getImages()
        )
            .doOnNext(output::append)
            .doOnComplete(() -> aiExecuteLogService.record(requestId, userId, SCENE_DEFAULT_CHAT,
                "dynamicChatService", "dynamic_agent", null,
                chatRequest.getMessage(), output.toString(), "success", null,
                System.currentTimeMillis() - startTime))
            .doOnError(e -> aiExecuteLogService.record(requestId, userId, SCENE_DEFAULT_CHAT,
                "dynamicChatService", "dynamic_agent", null,
                chatRequest.getMessage(), output.toString(), "fail", e.getMessage(),
                System.currentTimeMillis() - startTime));
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

        // 重新生成同样走 default_chat 限流
        Long userId = currentUserId();
        if (!tryAcquireChatRateLimit(userId, chatRequest.getMessage())) {
            return Flux.just("❌ 请求过于频繁，请稍后再试");
        }

        String requestId = UUID.randomUUID().toString().replace("-", "");
        long startTime = System.currentTimeMillis();
        StringBuilder output = new StringBuilder();
        return dynamicChatService.chat(
            chatRequest.getConversationId(),
            chatRequest.getMessage(),
            chatRequest.getAgentId(),
            false
        )
            .doOnNext(output::append)
            .doOnComplete(() -> aiExecuteLogService.record(requestId, userId, SCENE_DEFAULT_CHAT,
                "dynamicChatService", "dynamic_agent", null,
                chatRequest.getMessage(), output.toString(), "success", null,
                System.currentTimeMillis() - startTime))
            .doOnError(e -> aiExecuteLogService.record(requestId, userId, SCENE_DEFAULT_CHAT,
                "dynamicChatService", "dynamic_agent", null,
                chatRequest.getMessage(), output.toString(), "fail", e.getMessage(),
                System.currentTimeMillis() - startTime));
    }

    /**
     * default_chat「场景×用户」限流。配置行未部署（getConfig 为 null）时不限流，
     * 与历史行为一致；限流参数读 ai_scene_config.rate_limit_count / rate_limit_time，管理端改完即生效。
     *
     * @return true=放行；false=已限流（失败日志已记录）
     */
    private boolean tryAcquireChatRateLimit(Long userId, String userMessage) {
        AiSceneConfig config = aiSceneRegistry.getConfig(SCENE_DEFAULT_CHAT);
        if (config == null) {
            return true;
        }
        int limit = config.getRateLimitCount() != null ? config.getRateLimitCount() : 100;
        int window = config.getRateLimitTime() != null ? config.getRateLimitTime() : 60;
        String identity = userId != null ? String.valueOf(userId) : "anonymous";
        SceneRateLimiter.RateResult rate = sceneRateLimiter.tryAcquire(SCENE_DEFAULT_CHAT, identity, limit, window);
        if (!rate.allowed()) {
            log.info("[chat:治理] 限流触发: identity={}, {}/{}", identity, rate.current(), rate.limit());
            aiExecuteLogService.record(UUID.randomUUID().toString().replace("-", ""), userId, SCENE_DEFAULT_CHAT,
                "dynamicChatService", "dynamic_agent", null, userMessage, null,
                "fail", "rate_limited", 0);
            return false;
        }
        return true;
    }

    /** 当前登录用户（未登录上下文返回 null，限流按 anonymous 处理） */
    private Long currentUserId() {
        try {
            return SecurityUtils.getUserId();
        } catch (Exception e) {
            return null;
        }
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
