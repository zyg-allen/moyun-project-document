package com.moyun.ext.ai.service.chat;

import dev.langchain4j.data.message.ChatMessage;

import java.util.List;

/**
 * 对话摘要服务接口
 *
 * <p>将长对话历史压缩为摘要，优化Token使用：
 * <ul>
 *   <li>自动检测何时需要摘要</li>
 *   <li>保留关键信息和上下文</li>
 *   <li>支持增量摘要更新</li>
 * </ul>
 * </p>
 *
 * @author laomao
 * @since 2025-12-12
 */
public interface ConversationSummaryService {

    /**
     * 判断是否需要生成摘要
     *
     * @param messages     当前消息列表
     * @param maxMessages  最大消息数阈值
     * @return true如果需要生成摘要
     */
    boolean needsSummary(List<ChatMessage> messages, int maxMessages);

    /**
     * 生成对话摘要
     *
     * @param messages 需要摘要的消息列表
     * @return 摘要文本
     */
    String generateSummary(List<ChatMessage> messages);

    /**
     * 压缩对话历史
     *
     * <p>将旧消息替换为摘要，保留最近的消息</p>
     *
     * @param messages      原始消息列表
     * @param keepRecent    保留的最近消息数
     * @return 压缩后的消息列表（摘要 + 最近消息）
     */
    List<ChatMessage> compressHistory(List<ChatMessage> messages, int keepRecent);

    /**
     * 更新增量摘要
     *
     * <p>在已有摘要基础上添加新对话内容</p>
     *
     * @param existingSummary 已有摘要
     * @param newMessages     新增的消息
     * @return 更新后的摘要
     */
    String updateSummary(String existingSummary, List<ChatMessage> newMessages);

    /**
     * 估算消息列表的Token数
     *
     * @param messages 消息列表
     * @return 估算的Token数
     */
    int estimateTokenCount(List<ChatMessage> messages);
}
