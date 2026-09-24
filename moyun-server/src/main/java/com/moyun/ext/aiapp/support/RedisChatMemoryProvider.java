package com.moyun.ext.aiapp.support;

import com.moyun.ext.ai.store.RedisChatMemoryStore;
import dev.langchain4j.data.message.ChatMessage;
import dev.langchain4j.memory.chat.MessageWindowChatMemory;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 会话记忆实现：复用底座 {@link RedisChatMemoryStore}（key 规范 / 序列化 / 30 天过期）
 *
 * <p>滑窗操作内部构建 {@code MessageWindowChatMemory} 临时实例（读写直通 Redis），
 * 裁剪口径与面试既有实现 100% 一致；接口层不暴露 ChatMemoryStore 类型。</p>
 *
 * @author moyun
 * @since 2026-09-24
 */
@Component
@RequiredArgsConstructor
public class RedisChatMemoryProvider implements ChatMemoryProvider {

    /** 默认滑窗消息数（调用方未指定时，对齐面试侧 DEFAULT_MAX_MESSAGES） */
    static final int DEFAULT_MAX_MESSAGES = 40;

    private final RedisChatMemoryStore redisChatMemoryStore;

    @Override
    public List<ChatMessage> readAll(String sessionId) {
        return redisChatMemoryStore.getMessages(sessionId);
    }

    @Override
    public List<ChatMessage> readWindow(String sessionId, Integer maxMessages) {
        return window(sessionId, maxMessages).messages();
    }

    @Override
    public void append(String sessionId, Integer maxMessages, List<ChatMessage> messages) {
        if (messages == null || messages.isEmpty()) {
            return;
        }
        MessageWindowChatMemory memory = window(sessionId, maxMessages);
        for (ChatMessage message : messages) {
            memory.add(message);
        }
    }

    @Override
    public int size(String sessionId) {
        return redisChatMemoryStore.getMessages(sessionId).size();
    }

    @Override
    public void clear(String sessionId) {
        redisChatMemoryStore.deleteMessages(sessionId);
    }

    private MessageWindowChatMemory window(String sessionId, Integer maxMessages) {
        return MessageWindowChatMemory.builder()
                .id(sessionId)
                .maxMessages(resolveMax(maxMessages))
                .chatMemoryStore(redisChatMemoryStore)
                .build();
    }

    static int resolveMax(Integer maxMessages) {
        return maxMessages != null && maxMessages > 0 ? maxMessages : DEFAULT_MAX_MESSAGES;
    }
}
