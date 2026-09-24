package com.moyun.ext.aigateway.support;

import dev.langchain4j.data.message.ChatMessage;

import java.util.List;

/**
 * 网关会话记忆端口
 *
 * <p>网关侧窄端口：不直接依赖 LangChain4j ChatMemoryStore 类型，统一面试滑窗
 * 与通用会话两种读取口径。实现必须复用底座 {@code RedisChatMemoryStore} 的
 * 存储结构与 Redis key 规范（chat:memory:{sessionId}），与管理端 DynamicChatService
 * 共享同一份记忆，不另起炉灶。</p>
 *
 * <p>滑窗裁剪口径与 {@code MessageWindowChatMemory} 一致：窗口满时优先淘汰
 * 最旧的非 SystemMessage（人设永驻窗口）。</p>
 *
 * @author moyun
 * @since 2026-09-24
 */
public interface ChatMemoryProvider {

    /** 全量读取（摘要生成等需要完整历史的场景） */
    List<ChatMessage> readAll(String sessionId);

    /** 滑窗读取（尾部 maxMessages 条，SystemMessage 保留） */
    List<ChatMessage> readWindow(String sessionId, Integer maxMessages);

    /** 批量追加并按滑窗裁剪写回 */
    void append(String sessionId, Integer maxMessages, List<ChatMessage> messages);

    /** 当前消息总数 */
    int size(String sessionId);

    /** 清空会话记忆 */
    void clear(String sessionId);
}
