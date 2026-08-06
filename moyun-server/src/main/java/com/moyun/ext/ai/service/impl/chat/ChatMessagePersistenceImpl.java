package com.moyun.ext.ai.service.impl.chat;

import com.moyun.ext.ai.service.ChatHistoryService;
import com.moyun.ext.ai.service.ConversationService;
import com.moyun.ext.ai.service.chat.ChatMessagePersistenceService;
import com.moyun.ext.ai.service.chat.ChatResponseBuilderService;
import dev.langchain4j.rag.content.Content;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 对话消息持久化服务实现类
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
@Slf4j
@Service
public class ChatMessagePersistenceImpl implements ChatMessagePersistenceService {

    /** 工具调用标记正则 */
    private static final Pattern TOOL_CALL_PATTERN = Pattern.compile(
            "\\[TOOL_CALL\\].*?\\[/TOOL_CALL\\]", Pattern.DOTALL);

    /** 图片占位符正则 */
    private static final Pattern IMAGE_PLACEHOLDER_PATTERN = Pattern.compile("\\[\\[IMAGE_\\d+\\]\\]");

    @Autowired
    private ConversationService conversationService;

    @Autowired
    private ChatHistoryService chatHistoryService;

    @Autowired
    private ChatResponseBuilderService chatResponseBuilderService;

    /**
     * 保存用户消息
     *
     * @param conversationId 会话ID
     * @param userMessage    用户消息
     */
    @Override
    public void saveUserMessage(Long conversationId, String userMessage) {
        if (conversationId == null) {
            return;
        }

        try {
            conversationService.addMessage(conversationId, "user", userMessage, null);
            log.info("✅ 保存用户消息到数据库");
        } catch (Exception e) {
            log.error("❌ 保存用户消息失败", e);
        }
    }

    /**
     * 保存AI响应消息
     *
     * @param conversationId       会话ID
     * @param aiResponse           AI响应内容
     * @param validReferenceSources 有效的参考来源列表
     * @param toolResultHtml       工具执行结果HTML（可为null）
     * @return 清理后的响应内容
     */
    @Override
    public String saveAssistantMessage(Long conversationId, String aiResponse,
                                        List<Content> validReferenceSources, String toolResultHtml) {
        if (conversationId == null) {
            return aiResponse;
        }

        try {
            // 清理响应内容
            String cleanedResponse = cleanAiResponse(aiResponse);

            // 如果有工具调用结果，追加到响应内容
            if (toolResultHtml != null && !toolResultHtml.isEmpty()) {
                if (cleanedResponse.isEmpty()) {
                    cleanedResponse = toolResultHtml;
                } else {
                    cleanedResponse = cleanedResponse + "\n\n" + toolResultHtml;
                }
            }

            // 构建参考来源JSON
            String referenceSources = null;
            if (validReferenceSources != null && !validReferenceSources.isEmpty()) {
                referenceSources = chatResponseBuilderService.buildReferenceSourcesJson(validReferenceSources);
                log.info("✅ 保存 {} 个参考来源到数据库", validReferenceSources.size());
            }

            // 保存到数据库
            conversationService.addMessage(conversationId, "assistant", cleanedResponse, referenceSources);
            log.info("✅ 对话消息已保存到数据库: conversationId={}", conversationId);

            return cleanedResponse;
        } catch (Exception e) {
            log.error("❌ 保存对话消息失败", e);
            return aiResponse;
        }
    }

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
    @Override
    public void saveChatHistory(Long agentId, Long conversationId, String userMessage,
                                 String cleanedResponse, int tokensUsed, String referenceSources,
                                 int retrievalCount) {
        try {
            chatHistoryService.saveChat(
                    agentId,
                    String.valueOf(conversationId),
                    userMessage,
                    cleanedResponse,
                    tokensUsed,
                    referenceSources,
                    retrievalCount,
                    0 // responseTime暂不计算
            );
            log.debug("✅ 对话历史已保存: agentId={}", agentId);
        } catch (Exception e) {
            log.warn("保存对话历史失败: {}", e.getMessage());
        }
    }

    /**
     * 清理AI响应中的工具调用标记和图片占位符
     *
     * @param aiResponse AI原始响应
     * @return 清理后的响应
     */
    @Override
    public String cleanAiResponse(String aiResponse) {
        if (aiResponse == null) {
            return "";
        }

        // 1. 清理工具调用标记
        String cleaned = TOOL_CALL_PATTERN.matcher(aiResponse).replaceAll("").trim();

        // 2. 清理图片占位符
        Matcher matcher = IMAGE_PLACEHOLDER_PATTERN.matcher(cleaned);
        long imageCount = matcher.results().count();
        if (imageCount > 0) {
            cleaned = IMAGE_PLACEHOLDER_PATTERN.matcher(cleaned).replaceAll("");
            log.info("🧹 清理了 {} 个图片占位符（历史消息将通过referenceSources重新构建图片）", imageCount);
        }

        return cleaned;
    }
}
