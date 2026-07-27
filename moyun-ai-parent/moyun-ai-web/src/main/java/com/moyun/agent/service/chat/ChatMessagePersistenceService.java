package com.moyun.agent.service.chat;

import dev.langchain4j.rag.content.Content;

import java.util.List;

/**
 * 对话消息持久化服务接口
 *
 * <p>负责保存对话消息到数据库，包括：
 * <ul>
 *   <li>用户消息保存</li>
 *   <li>AI响应保存</li>
 *   <li>对话历史统计</li>
 * </ul>
 * </p>
 *
 * @author laomao
 * @since 2025-12-11
 */
public interface ChatMessagePersistenceService {

    /**
     * 保存用户消息
     *
     * @param conversationId 会话ID
     * @param userMessage    用户消息
     */
    void saveUserMessage(Long conversationId, String userMessage);

    /**
     * 保存AI响应消息
     *
     * @param conversationId       会话ID
     * @param aiResponse           AI响应内容
     * @param validReferenceSources 有效的参考来源列表
     * @param toolResultHtml       工具执行结果HTML（可为null）
     * @return 清理后的响应内容
     */
    String saveAssistantMessage(Long conversationId, String aiResponse,
                                 List<Content> validReferenceSources, String toolResultHtml);

    /**
     * 保存对话历史（用于统计）
     *
     * @param agentId          智能体ID
     * @param conversationId   会话ID
     * @param userMessage      用户消息
     * @param cleanedResponse  清理后的响应
     * @param tokensUsed       使用的Token数
     * @param referenceSources 参考来源JSON
     * @param retrievalCount   检索结果数量
     */
    void saveChatHistory(Long agentId, Long conversationId, String userMessage,
                         String cleanedResponse, int tokensUsed, String referenceSources,
                         int retrievalCount);

    /**
     * 清理AI响应中的工具调用标记和图片占位符
     *
     * @param aiResponse AI原始响应
     * @return 清理后的响应
     */
    String cleanAiResponse(String aiResponse);
}
