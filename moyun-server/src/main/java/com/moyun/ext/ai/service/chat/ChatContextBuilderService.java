package com.moyun.ext.ai.service.chat;

import com.moyun.ext.ai.entity.Agent;
import dev.langchain4j.rag.content.Content;

import java.util.List;

/**
 * 对话上下文构建服务接口
 *
 * <p>负责构建对话过程中的各种上下文，包括：
 * <ul>
 *   <li>系统提示词构建</li>
 *   <li>RAG检索上下文构建</li>
 *   <li>用户消息处理</li>
 * </ul>
 * </p>
 *
 * @author laomao
 * @since 2025-12-11
 */
public interface ChatContextBuilderService {

    /**
     * 构建系统提示词
     *
     * @param agent      智能体配置
     * @param hasTools   是否配置了工具
     * @param toolPrompt 工具提示词（如果有）
     * @return 完整的系统提示词
     */
    String buildSystemPrompt(Agent agent, boolean hasTools, String toolPrompt);

    /**
     * 构建RAG检索上下文
     *
     * <p>将检索到的内容列表转换为发送给AI的上下文格式</p>
     *
     * @param contents 检索到的内容列表
     * @return RAG上下文构建结果
     */
    RagContextResult buildRagContext(List<Content> contents);

    /**
     * 构建处理后的用户消息
     *
     * @param ragContext      RAG上下文（可为null）
     * @param userMessage     原始用户消息
     * @param workflowContext 工作流上下文（可为null）
     * @param hasRagContent   是否有RAG内容
     * @return 处理后的用户消息
     */
    String buildProcessedUserMessage(String ragContext, String userMessage, 
                                      String workflowContext, boolean hasRagContent);

    /**
     * 构建无RAG内容时的提示消息
     *
     * @param userMessage 用户消息
     * @return 提示消息
     */
    String buildNoRagContentMessage(String userMessage);

    /**
     * RAG上下文构建结果
     */
    class RagContextResult {
        private final String context;
        private final int imageCount;

        public RagContextResult(String context, int imageCount) {
            this.context = context;
            this.imageCount = imageCount;
        }

        public String getContext() {
            return context;
        }

        public int getImageCount() {
            return imageCount;
        }
    }
}
