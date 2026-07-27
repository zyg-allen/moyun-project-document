package com.moyun.agent.service;

import reactor.core.publisher.Flux;
import java.util.List;

/**
 * 动态对话服务接口
 *
 * <p>提供基于RAG的智能对话功能，支持流式响应、知识库检索、工具调用和工作流触发。</p>
 *
 * @author laomao
 * @since 1.0
 */
public interface DynamicChatService {
    /**
     * 发起对话（基于会话ID）
     *
     * @param conversationId 会话ID
     * @param userMessage    用户消息
     * @return 流式响应内容
     */
    Flux<String> chat(Long conversationId, String userMessage, Long agentId);

    /**
     * 发起对话（基于智能体ID）
     *
     * @param agentId        智能体ID
     * @param conversationId 会话ID
     * @param userMessage    用户消息
     * @return 流式响应内容
     */
    Flux<String> chat(Long conversationId, String userMessage, Long agentId, boolean isGreeting);

    /**
     * 发起多模态对话（支持图片输入）
     *
     * @param conversationId 会话ID
     * @param userMessage    用户消息
     * @param agentId        智能体ID
     * @param isGreeting     是否为系统问候
     * @param images         图片列表（Base64编码）
     * @return 流式响应内容
     */
    Flux<String> chat(Long conversationId, String userMessage, Long agentId, boolean isGreeting, List<String> images);
}
