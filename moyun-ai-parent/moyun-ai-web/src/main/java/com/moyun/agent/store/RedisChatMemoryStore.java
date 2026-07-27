package com.moyun.agent.store;

import com.moyun.agent.constant.RedisKeys;
import com.moyun.agent.exception.BusinessException;
import com.moyun.agent.exception.ErrorCode;
import dev.langchain4j.data.message.ChatMessage;
import dev.langchain4j.data.message.ChatMessageDeserializer;
import dev.langchain4j.data.message.ChatMessageSerializer;
import dev.langchain4j.store.memory.chat.ChatMemoryStore;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * Redis实现的聊天记忆存储
 *
 * <p>基于Redis存储对话历史，支持LangChain4j的ChatMemoryStore接口，
 * 提供对话上下文的持久化存储，默认30天过期</p>
 *
 * @author laomao
 * @time 2025/11/23
 */
@Slf4j
@Component
public class RedisChatMemoryStore implements ChatMemoryStore {

    @Autowired
    private RedisTemplate<String, String> redisTemplate;

    @Override
    public List<ChatMessage> getMessages(Object memoryId) {
        try {
            String key = RedisKeys.chatMemory(memoryId);
            String content = redisTemplate.opsForValue().get(key);

            if (content == null || content.isEmpty()) {
                log.debug("Redis中未找到memoryId={} 的聊天记录", memoryId);
                return new LinkedList<>();
            }

            List<ChatMessage> messages = ChatMessageDeserializer.messagesFromJson(content);
            log.debug("从Redis获取到 {} 条聊天记录, memoryId={}", messages.size(), memoryId);
            return messages;

        } catch (Exception e) {
            log.error("从Redis获取聊天记录失败, memoryId={}, error={}", memoryId, e.getMessage(), e);
            return new LinkedList<>();
        }
    }

    @Override
    public void updateMessages(Object memoryId, List<ChatMessage> messages) {
        try {
            String key = RedisKeys.chatMemory(memoryId);
            String content = ChatMessageSerializer.messagesToJson(messages);

            // 保存到Redis并设置过期时间
            redisTemplate.opsForValue().set(key, content, RedisKeys.CHAT_MEMORY_EXPIRE_DAYS, TimeUnit.DAYS);

            log.debug("更新聊天记录到Redis成功, memoryId={}, 消息数={}", memoryId, messages.size());

        } catch (Exception e) {
            log.error("更新聊天记录到Redis失败, memoryId={}, error={}", memoryId, e.getMessage(), e);
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "保存聊天记录到Redis失败: " + e.getMessage(), e);
        }
    }

    @Override
    public void deleteMessages(Object memoryId) {
        try {
            String key = RedisKeys.chatMemory(memoryId);
            Boolean deleted = redisTemplate.delete(key);

            if (Boolean.TRUE.equals(deleted)) {
                log.debug("从Redis删除聊天记录成功, memoryId={}", memoryId);
            } else {
                log.warn("Redis中未找到要删除的聊天记录, memoryId={}", memoryId);
            }

        } catch (Exception e) {
            log.error("从Redis删除聊天记录失败, memoryId={}, error={}", memoryId, e.getMessage(), e);
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "删除聊天记录失败: " + e.getMessage(), e);
        }
    }
}
