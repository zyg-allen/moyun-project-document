package com.moyun.agent.controller;

import com.moyun.agent.common.ApiResponse;
import com.moyun.agent.entity.Agent;
import com.moyun.agent.service.AgentService;
import com.moyun.agent.service.DynamicChatService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;

import java.util.UUID;

/**
 * 智能体API访问控制器
 * 
 * <p>提供外部系统调用智能体的API接口</p>
 *
 * @author laomao
 */
@Slf4j
@Tag(name = "智能体API")
@RestController
@RequestMapping("/api/agent")
public class AgentApiController {
    
    @Autowired
    private AgentService agentService;
    
    @Autowired
    private DynamicChatService dynamicChatService;
    
    /**
     * API对话请求
     */
    @Data
    public static class ApiChatRequest {
        private String message;
        private String sessionId;  // 可选，不传则自动生成
    }
    
    /**
     * 通过API调用智能体对话（流式）
     */
    @Operation(summary = "API对话（流式）", description = "通过API Key调用智能体进行对话")
    @PostMapping(value = "/{id}/chat", produces = "text/event-stream;charset=utf-8")
    public Flux<String> apiChat(
            @PathVariable Long id,
            @RequestHeader(value = "Authorization", required = false) String authorization,
            @RequestBody ApiChatRequest request) {
        
        log.info("收到API对话请求 - AgentID: {}", id);
        
        // 验证智能体
        Agent agent = agentService.getById(id);
        if (agent == null) {
            return Flux.just("data: {\"error\": \"智能体不存在\"}\n\n");
        }
        
        // 检查API是否启用
        if (!Boolean.TRUE.equals(agent.getApiEnabled())) {
            return Flux.just("data: {\"error\": \"该智能体未启用API访问\"}\n\n");
        }
        
        // 验证API Key
        String apiKey = extractApiKey(authorization);
        if (apiKey == null || !apiKey.equals(agent.getApiKey())) {
            return Flux.just("data: {\"error\": \"API Key无效\"}\n\n");
        }
        
        // 验证消息
        if (request.getMessage() == null || request.getMessage().trim().isEmpty()) {
            return Flux.just("data: {\"error\": \"消息不能为空\"}\n\n");
        }
        
        // 生成或使用会话ID
        String sessionId = request.getSessionId();
        if (sessionId == null || sessionId.trim().isEmpty()) {
            sessionId = UUID.randomUUID().toString();
        }
        
        log.info("API对话 - AgentID: {}, SessionID: {}, Message: {}", id, sessionId, request.getMessage());
        
        // 调用对话服务
        final String finalSessionId = sessionId;
        return dynamicChatService.chat(
            Long.parseLong(finalSessionId.hashCode() + ""),
            request.getMessage(),
            id,
            false
        ).map(token -> "data: " + token.replace("\n", "\\n") + "\n\n");
    }
    
    /**
     * 通过API调用智能体对话（非流式）
     */
    @Operation(summary = "API对话（非流式）", description = "通过API Key调用智能体进行对话，返回完整响应")
    @PostMapping("/{id}/chat/sync")
    public ResponseEntity<ApiResponse<ChatResponse>> apiChatSync(
            @PathVariable Long id,
            @RequestHeader(value = "Authorization", required = false) String authorization,
            @RequestBody ApiChatRequest request) {
        
        log.info("收到API同步对话请求 - AgentID: {}", id);
        
        // 验证智能体
        Agent agent = agentService.getById(id);
        if (agent == null) {
            return ResponseEntity.ok(ApiResponse.error("智能体不存在"));
        }
        
        // 检查API是否启用
        if (!Boolean.TRUE.equals(agent.getApiEnabled())) {
            return ResponseEntity.ok(ApiResponse.error("该智能体未启用API访问"));
        }
        
        // 验证API Key
        String apiKey = extractApiKey(authorization);
        if (apiKey == null || !apiKey.equals(agent.getApiKey())) {
            return ResponseEntity.status(401).body(ApiResponse.error("API Key无效"));
        }
        
        // 验证消息
        if (request.getMessage() == null || request.getMessage().trim().isEmpty()) {
            return ResponseEntity.ok(ApiResponse.error("消息不能为空"));
        }
        
        // 生成或使用会话ID
        String sessionId = request.getSessionId();
        if (sessionId == null || sessionId.trim().isEmpty()) {
            sessionId = UUID.randomUUID().toString();
        }
        
        try {
            // 收集流式响应
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
            
            return ResponseEntity.ok(ApiResponse.success(response));
        } catch (Exception e) {
            log.error("API对话失败", e);
            return ResponseEntity.ok(ApiResponse.error("对话失败: " + e.getMessage()));
        }
    }
    
    /**
     * 对话响应
     */
    @Data
    public static class ChatResponse {
        private String sessionId;
        private String message;
    }
    
    /**
     * 从Authorization头提取API Key
     */
    private String extractApiKey(String authorization) {
        if (authorization == null) return null;
        
        // 支持 "Bearer sk-xxx" 格式
        if (authorization.startsWith("Bearer ")) {
            return authorization.substring(7);
        }
        
        // 支持直接传 "sk-xxx"
        if (authorization.startsWith("sk-")) {
            return authorization;
        }
        
        return null;
    }
    
    // ========== 应用发布相关 ==========
    
    /**
     * 发布应用 - 生成访问Token
     */
    @Operation(summary = "发布应用", description = "将智能体发布为可访问的应用")
    @PostMapping("/{id}/publish")
    public ResponseEntity<ApiResponse<PublishResult>> publishAgent(@PathVariable Long id) {
        try {
            Agent agent = agentService.getById(id);
            if (agent == null) {
                return ResponseEntity.ok(ApiResponse.error("智能体不存在"));
            }
            
            // 生成发布Token
            String publishToken = "pub-" + UUID.randomUUID().toString().replace("-", "").substring(0, 24);
            agent.setPublishEnabled(true);
            agent.setPublishToken(publishToken);
            agentService.updateById(agent);
            
            PublishResult result = new PublishResult();
            result.setPublishToken(publishToken);
            result.setEmbedUrl("/app/" + publishToken);
            result.setIframeCode("<iframe src=\"" + "/app/" + publishToken + "\" width=\"400\" height=\"600\" frameborder=\"0\"></iframe>");
            
            return ResponseEntity.ok(ApiResponse.success(result));
        } catch (Exception e) {
            log.error("发布应用失败", e);
            return ResponseEntity.ok(ApiResponse.error("发布失败: " + e.getMessage()));
        }
    }
    
    /**
     * 取消发布
     */
    @Operation(summary = "取消发布")
    @DeleteMapping("/{id}/publish")
    public ResponseEntity<ApiResponse<Void>> unpublishAgent(@PathVariable Long id) {
        try {
            Agent agent = agentService.getById(id);
            if (agent == null) {
                return ResponseEntity.ok(ApiResponse.error("智能体不存在"));
            }
            
            agent.setPublishEnabled(false);
            agent.setPublishToken(null);
            agentService.updateById(agent);
            
            return ResponseEntity.ok(ApiResponse.success("已取消发布", null));
        } catch (Exception e) {
            log.error("取消发布失败", e);
            return ResponseEntity.ok(ApiResponse.error("取消失败: " + e.getMessage()));
        }
    }
    
    @Data
    public static class PublishResult {
        private String publishToken;
        private String embedUrl;
        private String iframeCode;
    }
    
    // ========== 发布应用访问接口 ==========
    
    /**
     * 通过发布Token访问应用（流式）
     */
    @Operation(summary = "访问已发布应用（流式）")
    @PostMapping(value = "/app/{token}/chat", produces = "text/event-stream;charset=utf-8")
    public Flux<String> appChat(
            @PathVariable String token,
            @RequestBody ApiChatRequest request) {
        
        log.info("收到应用访问请求 - Token: {}", token);
        
        // 通过Token查找智能体
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
    
    /**
     * 获取已发布应用信息
     */
    @Operation(summary = "获取已发布应用信息")
    @GetMapping("/app/{token}/info")
    public ResponseEntity<ApiResponse<AppInfo>> getAppInfo(@PathVariable String token) {
        Agent agent = agentService.getByPublishToken(token);
        if (agent == null || !Boolean.TRUE.equals(agent.getPublishEnabled())) {
            return ResponseEntity.ok(ApiResponse.error("应用不存在或未发布"));
        }
        
        AppInfo info = new AppInfo();
        info.setName(agent.getName());
        info.setDescription(agent.getDescription());
        info.setWelcomeMessage(agent.getWelcomeMessage());
        info.setSuggestedQuestions(agent.getSuggestedQuestions());
        
        return ResponseEntity.ok(ApiResponse.success(info));
    }
    
    @Data
    public static class AppInfo {
        private String name;
        private String description;
        private String welcomeMessage;
        private String suggestedQuestions;
    }
}
