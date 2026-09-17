package com.moyun.ext.cms.service.interview;

import com.moyun.ext.ai.store.RedisChatMemoryStore;
import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.data.message.ChatMessage;
import dev.langchain4j.data.message.SystemMessage;
import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.memory.chat.MessageWindowChatMemory;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 面试滑窗记忆服务（V3 重构）
 *
 * <p>语音面试对话主干复用统一 AI 会话的上下文机制：MessageWindowChatMemory + Redis 滑窗。
 * system（面试官人设）/简历/岗位/JD 只在首轮注入一次，后续轮次自动追加，
 * 断点续接时若 Redis 已过期则从 DB 问答记录重建。</p>
 *
 * @author moyun
 */
@Slf4j
@Service
public class InterviewChatMemoryService {

    /** 默认滑窗消息数（agent 未配置 maxHistoryTurns 时） */
    private static final int DEFAULT_MAX_MESSAGES = 40;

    @Autowired
    private RedisChatMemoryStore redisChatMemoryStore;

    /** 滑窗 memoryId：与聊天会话命名空间隔离 */
    public String memoryId(Long interviewId) {
        return "voice-interview:" + interviewId;
    }

    /** 获取滑窗（每次构建新实例，读写直通 Redis） */
    public MessageWindowChatMemory getMemory(Long interviewId, Integer maxHistoryTurns) {
        int maxMessages = maxHistoryTurns != null && maxHistoryTurns > 0
                ? maxHistoryTurns * 2 : DEFAULT_MAX_MESSAGES;
        return MessageWindowChatMemory.builder()
                .id(memoryId(interviewId))
                .maxMessages(maxMessages)
                .chatMemoryStore(redisChatMemoryStore)
                .build();
    }

    /**
     * 首轮初始化（幂等）：滑窗为空时写入 system + 上下文 user + 开场 assistant，非空跳过
     *
     * @param systemPrompt  面试官系统提示词（agent 人设 + 岗位/难度/JD 渲染）
     * @param contextUserMsg 结构化上下文（简历摘要/岗位 JD，wrapData 包裹后的文本）
     * @param openingMsg    开场白 + 首题
     */
    public void initFirstTurn(Long interviewId, Integer maxHistoryTurns,
                              String systemPrompt, String contextUserMsg, String openingMsg) {
        MessageWindowChatMemory memory = getMemory(interviewId, maxHistoryTurns);
        if (!memory.messages().isEmpty()) {
            return;
        }
        memory.add(SystemMessage.from(systemPrompt));
        memory.add(new UserMessage(contextUserMsg));
        memory.add(new AiMessage(openingMsg));
        log.info("[InterviewMemory] 首轮滑窗初始化完成 interviewId={}", interviewId);
    }

    /**
     * 断点续接重建：Redis 滑窗仍在则直接复用；已过期则按 DB 问答记录逐对重建
     *
     * @param qaList 会话全部 QA（含首题），按 id 升序
     */
    public void rebuildFromDb(Long interviewId, Integer maxHistoryTurns, String systemPrompt,
                              String contextUserMsg, List<ChatMessage> qaPairs) {
        MessageWindowChatMemory memory = getMemory(interviewId, maxHistoryTurns);
        if (!memory.messages().isEmpty()) {
            log.info("[InterviewMemory] 滑窗仍在，无需重建 interviewId={}", interviewId);
            return;
        }
        memory.add(SystemMessage.from(systemPrompt));
        memory.add(new UserMessage(contextUserMsg));
        for (ChatMessage msg : qaPairs) {
            memory.add(msg);
        }
        log.info("[InterviewMemory] 滑窗从 DB 重建完成 interviewId={} 消息数={}",
                interviewId, memory.messages().size());
    }

    /** 面试结束后释放滑窗 */
    public void clear(Long interviewId) {
        try {
            redisChatMemoryStore.deleteMessages(memoryId(interviewId));
        } catch (Exception e) {
            log.warn("[InterviewMemory] 清理滑窗失败 interviewId={}：{}", interviewId, e.getMessage());
        }
    }
}
