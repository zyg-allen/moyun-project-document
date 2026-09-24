package com.moyun.ext.cms.service.interview;

import com.moyun.ext.aigateway.support.ChatMemoryProvider;
import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.data.message.ChatMessage;
import dev.langchain4j.data.message.SystemMessage;
import dev.langchain4j.data.message.UserMessage;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 面试滑窗记忆服务（V4：适配网关 ChatMemoryProvider）
 *
 * <p>语音面试对话主干复用网关会话记忆端口（底层与管理端共享 RedisChatMemoryStore
 * 的存储结构与 key 规范）。system（面试官人设）/简历/岗位/JD 只在首轮注入一次，
 * 后续轮次自动追加，断点续接时若 Redis 已过期则从 DB 问答记录重建。</p>
 *
 * <p>滑窗口径不变：maxHistoryTurns × 2 条消息（agent 未配置时默认 40）。</p>
 *
 * @author moyun
 */
@Slf4j
@Service
public class InterviewChatMemoryService {

    /** 默认滑窗消息数（agent 未配置 maxHistoryTurns 时） */
    private static final int DEFAULT_MAX_MESSAGES = 40;

    private final ChatMemoryProvider memoryProvider;

    public InterviewChatMemoryService(ChatMemoryProvider memoryProvider) {
        this.memoryProvider = memoryProvider;
    }

    /** 滑窗 memoryId：与聊天会话命名空间隔离 */
    public String memoryId(Long interviewId) {
        return "voice-interview:" + interviewId;
    }

    /** 滑窗消息数换算：轮数×2（agent 未配置时默认 40） */
    public int toMaxMessages(Integer maxHistoryTurns) {
        return maxHistoryTurns != null && maxHistoryTurns > 0
                ? maxHistoryTurns * 2 : DEFAULT_MAX_MESSAGES;
    }

    /** 滑窗读取 */
    public List<ChatMessage> readWindow(Long interviewId, Integer maxHistoryTurns) {
        return memoryProvider.readWindow(memoryId(interviewId), toMaxMessages(maxHistoryTurns));
    }

    /** 批量追加（按滑窗裁剪写回） */
    public void append(Long interviewId, Integer maxHistoryTurns, List<ChatMessage> messages) {
        memoryProvider.append(memoryId(interviewId), toMaxMessages(maxHistoryTurns), messages);
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
        String id = memoryId(interviewId);
        if (!memoryProvider.readWindow(id, toMaxMessages(maxHistoryTurns)).isEmpty()) {
            return;
        }
        memoryProvider.append(id, toMaxMessages(maxHistoryTurns), List.of(
                SystemMessage.from(systemPrompt),
                new UserMessage(contextUserMsg),
                new AiMessage(openingMsg)));
        log.info("[InterviewMemory] 首轮滑窗初始化完成 interviewId={}", interviewId);
    }

    /**
     * 断点续接重建：Redis 滑窗仍在则直接复用；已过期则按 DB 问答记录逐对重建
     *
     * @param qaList 会话全部 QA（含首题），按 id 升序
     */
    public void rebuildFromDb(Long interviewId, Integer maxHistoryTurns, String systemPrompt,
                              String contextUserMsg, List<ChatMessage> qaPairs) {
        String id = memoryId(interviewId);
        if (!memoryProvider.readWindow(id, toMaxMessages(maxHistoryTurns)).isEmpty()) {
            log.info("[InterviewMemory] 滑窗仍在，无需重建 interviewId={}", interviewId);
            return;
        }
        java.util.List<ChatMessage> initial = new java.util.ArrayList<>();
        initial.add(SystemMessage.from(systemPrompt));
        initial.add(new UserMessage(contextUserMsg));
        initial.addAll(qaPairs);
        memoryProvider.append(id, toMaxMessages(maxHistoryTurns), initial);
        log.info("[InterviewMemory] 滑窗从 DB 重建完成 interviewId={} 消息数={}",
                interviewId, initial.size());
    }

    /** 面试结束后释放滑窗 */
    public void clear(Long interviewId) {
        try {
            memoryProvider.clear(memoryId(interviewId));
        } catch (Exception e) {
            log.warn("[InterviewMemory] 清理滑窗失败 interviewId={}：{}", interviewId, e.getMessage());
        }
    }
}
