package com.moyun.ext.ai.controller;

import com.moyun.core.base.AjaxResult;
import com.moyun.ext.ai.entity.Agent;
import com.moyun.ext.ai.service.AgentService;
import com.moyun.ext.ai.service.DynamicChatService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;

import java.util.UUID;

@Slf4j
@Tag(name = "智能体API")
@RestController
@RequestMapping("/cms/ai/agent-api")
public class AgentApiController {
    
    @Autowired
    private AgentService agentService;
    
    @Autowired
    private DynamicChatService dynamicChatService;
    
    @Data
    public static class ApiChatRequest {
        private String message;
        private String sessionId;
    }
    
    @Operation(summary = "API对话（流式）", description = "通过API Key调用智能体进行对话")
    @PostMapping(value = "/{id}/chat", produces = "text/event-stream;charset=utf-8")
    public Flux<String> apiChat(
            @PathVariable Long id,
            @RequestHeader(value = "Authorization", required = false) String authorization,
            @RequestBody ApiChatRequest request) {
        
        log.info("收到API对话请求 - AgentID: {}", id);
        
        Agent agent = agentService.getById(id);
        if (agent == null) {
            return Flux.just("data: {\"error\": \"智能体不存在\"}\n\n");
        }
        
        if (!Boolean.TRUE.equals(agent.getApiEnabled())) {
            return Flux.just("data: {\"error\": \"该智能体未启用API访问\"}\n\n");
        }
        
        String apiKey = extractApiKey(authorization);
        if (apiKey == null || !apiKey.equals(agent.getApiKey())) {
            return Flux.just("data: {\"error\": \"API Key无效\"}\n\n");
        }
        
        if (request.getMessage() == null || request.getMessage().trim().isEmpty()) {
            return Flux.just("data: {\"error\": \"消息不能为空\"}\n\n");
        }
        
        String sessionId = request.getSessionId();
        if (sessionId == null || sessionId.trim().isEmpty()) {
            sessionId = UUID.randomUUID().toString();
        }
        
        log.info("API对话 - AgentID: {}, SessionID: {}, Message: {}", id, sessionId, request.getMessage());
        
        final String finalSessionId = sessionId;
        return dynamicChatService.chat(
            Long.parseLong(finalSessionId.hashCode() + ""),
            request.getMessage(),
            id,
            false
        ).map(token -> "data: " + token.replace("\n", "\\n") + "\n\n");
    }
    
    @Operation(summary = "API对话（非流式）", description = "通过API Key调用智能体进行对话，返回完整响应")
    @PostMapping("/{id}/chat/sync")
    @PreAuthorize("@ss.hasPermi('cms:ai:agent-api:query')")
    public AjaxResult apiChatSync(
            @PathVariable Long id,
            @RequestHeader(value = "Authorization", required = false) String authorization,
            @RequestBody ApiChatRequest request) {
        
        log.info("收到API同步对话请求 - AgentID: {}", id);
        
        Agent agent = agentService.getById(id);
        if (agent == null) {
            return AjaxResult.error("智能体不存在");
        }
        
        if (!Boolean.TRUE.equals(agent.getApiEnabled())) {
            return AjaxResult.error("该智能体未启用API访问");
        }
        
        String apiKey = extractApiKey(authorization);
        if (apiKey == null || !apiKey.equals(agent.getApiKey())) {
            return AjaxResult.error("API Key无效");
        }
        
        if (request.getMessage() == null || request.getMessage().trim().isEmpty()) {
            return AjaxResult.error("消息不能为空");
        }
        
        String sessionId = request.getSessionId();
        if (sessionId == null || sessionId.trim().isEmpty()) {
            sessionId = UUID.randomUUID().toString();
        }
        
        try {
            StringBuilder responseBuilder = new StringBuilder();
            final String finalSessionId = sessionId;
            
            dynamicChatService.chat(
                Long.parseLong(String.valueOf(Math.abs(finalSessionId.hashCode()))),
                request.getMessage(),
                id,
                false
            ).doOnNext(responseBuilder::append).blockLast();
            
            ChatResponse response = new ChatResponse();
            response.setSessionId(finalSessionId);
            response.setMessage(responseBuilder.toString());
            
            return AjaxResult.success(response);
        } catch (Exception e) {
            log.error("API对话失败", e);
            return AjaxResult.error("对话失败: " + e.getMessage());
        }
    }
    
    @Data
    public static class ChatResponse {
        private String sessionId;
        private String message;
    }
    
    private String extractApiKey(String authorization) {
        if (authorization == null) return null;
        
        if (authorization.startsWith("Bearer ")) {
            return authorization.substring(7);
        }
        
        if (authorization.startsWith("sk-")) {
            return authorization;
        }
        
        return null;
    }
    
    @Operation(summary = "发布应用", description = "将智能体发布为可访问的应用")
    @PostMapping("/{id}/publish")
    @PreAuthorize("@ss.hasPermi('cms:ai:agent-api:publish')")
    public AjaxResult publishAgent(@PathVariable Long id) {
        try {
            Agent agent = agentService.getById(id);
            if (agent == null) {
                return AjaxResult.error("智能体不存在");
            }
            
            String publishToken = "pub-" + UUID.randomUUID().toString().replace("-", "").substring(0, 24);
            agent.setPublishEnabled(true);
            agent.setPublishToken(publishToken);
            agentService.updateById(agent);
            
            PublishResult result = new PublishResult();
            result.setPublishToken(publishToken);
            result.setEmbedUrl("/app/" + publishToken);
            result.setIframeCode("<iframe src=\"" + "/app/" + publishToken + "\" width=\"400\" height=\"600\" frameborder=\"0\"></iframe>");
            
            return AjaxResult.success(result);
        } catch (Exception e) {
            log.error("发布应用失败", e);
            return AjaxResult.error("发布失败: " + e.getMessage());
        }
    }
    
    @Operation(summary = "取消发布")
    @DeleteMapping("/{id}/publish")
    @PreAuthorize("@ss.hasPermi('cms:ai:agent-api:publish')")
    public AjaxResult unpublishAgent(@PathVariable Long id) {
        try {
            Agent agent = agentService.getById(id);
            if (agent == null) {
                return AjaxResult.error("智能体不存在");
            }
            
            agent.setPublishEnabled(false);
            agent.setPublishToken(null);
            agentService.updateById(agent);
            
            return AjaxResult.success("已取消发布", null);
        } catch (Exception e) {
            log.error("取消发布失败", e);
            return AjaxResult.error("取消失败: " + e.getMessage());
        }
    }
    
    @Data
    public static class PublishResult {
        private String publishToken;
        private String embedUrl;
        private String iframeCode;
    }
    
    @Operation(summary = "访问已发布应用（流式）")
    @PostMapping(value = "/app/{token}/chat", produces = "text/event-stream;charset=utf-8")
    public Flux<String> appChat(
            @PathVariable String token,
            @RequestBody ApiChatRequest request) {
        
        log.info("收到应用访问请求 - Token: {}", token);
        
        Agent agent = agentService.getByPublishToken(token);
        if (agent == null) {
            return Flux.just("data: {\"error\": \"应用不存在或未发布\"}\n\n");
        }
        
        if (!Boolean.TRUE.equals(agent.getPublishEnabled())) {
            return Flux.just("data: {\"error\": \"应用已下线\"}\n\n");
        }
        
        if (request.getMessage() == null || request.getMessage().trim().isEmpty()) {
            return Flux.just("data: {\"error\": \"消息不能为空\"}\n\n");
        }
        
        String sessionId = request.getSessionId();
        if (sessionId == null || sessionId.trim().isEmpty()) {
            sessionId = UUID.randomUUID().toString();
        }
        
        final String finalSessionId = sessionId;
        return dynamicChatService.chat(
            Long.parseLong(String.valueOf(Math.abs(finalSessionId.hashCode()))),
            request.getMessage(),
            agent.getId(),
            false
        ).map(t -> "data: " + t.replace("\n", "\\n") + "\n\n");
    }
    
    @Operation(summary = "获取已发布应用信息")
    @GetMapping("/app/{token}/info")
    public AjaxResult getAppInfo(@PathVariable String token) {
        Agent agent = agentService.getByPublishToken(token);
        if (agent == null || !Boolean.TRUE.equals(agent.getPublishEnabled())) {
            return AjaxResult.error("应用不存在或未发布");
        }
        
        AppInfo info = new AppInfo();
        info.setName(agent.getName());
        info.setDescription(agent.getDescription());
        info.setWelcomeMessage(agent.getWelcomeMessage());
        info.setSuggestedQuestions(agent.getSuggestedQuestions());
        
        return AjaxResult.success(info);
    }
    
    @Data
    public static class AppInfo {
        private String name;
        private String description;
        private String welcomeMessage;
        private String suggestedQuestions;
    }
}
