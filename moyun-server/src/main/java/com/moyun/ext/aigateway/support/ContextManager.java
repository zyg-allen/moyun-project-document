package com.moyun.ext.aigateway.support;

import com.moyun.ext.ai.constant.RedisKeys;
import com.moyun.ext.ai.service.LLMService;
import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.data.message.ChatMessage;
import dev.langchain4j.data.message.SystemMessage;
import dev.langchain4j.data.message.UserMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

/**
 * 网关会话上下文管理器
 *
 * <p>会话流式通道的上下文装配：滑窗读取 + 摘要注入 + 本轮瞬态指令拼接。
 * 滑窗外早期消息的摘要<b>异步预生成</b>（下一轮生效），不阻塞当前轮。</p>
 *
 * <p>消息装配顺序：[SystemMessage(人设)] → [早期摘要] → 滑窗历史 → 本轮瞬态指令。
 * 本轮用户输入在装配前先入滑窗（与面试既有行为一致：LLM 失败时用户消息不丢）。</p>
 *
 * @author moyun
 * @since 2026-09-24
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ContextManager {

    private final ChatMemoryProvider memoryProvider;
    private final RedisTemplate<String, String> redisTemplate;

    /** 摘要异步预生成线程（单线程足够：非关键路径，失败静默降级为无摘要） */
    private static final ExecutorService SUMMARY_EXECUTOR = Executors.newSingleThreadExecutor(r -> {
        Thread t = new Thread(r, "ai-context-summary");
        t.setDaemon(true);
        return t;
    });

    @Autowired(required = false)
    private LLMService llmService;

    /**
     * 构建本轮完整消息列表：当前输入入滑窗 → 滑窗 + 摘要 + 瞬态指令
     *
     * @param sessionId  会话记忆ID（业务侧定义，如 voice-interview:{id}）
     * @param maxMessages 滑窗大小（null=默认40）
     * @param userInput  本轮用户输入（空/blank 时跳过入窗，仅历史+指令）
     * @param directives 本轮瞬态指令（不入滑窗，仅本轮发送）
     */
    public List<ChatMessage> buildTurnMessages(String sessionId, Integer maxMessages,
                                               String userInput, List<String> directives) {
        if (userInput != null && !userInput.isBlank()) {
            memoryProvider.append(sessionId, maxMessages, List.of(new UserMessage(userInput)));
        }
        List<ChatMessage> messages = new ArrayList<>(memoryProvider.readWindow(sessionId, maxMessages));
        // 早期摘要注入（位置：首条 SystemMessage 之后，窗口历史之前）
        String summary = readSummary(sessionId);
        if (summary != null && !summary.isBlank()) {
            int insertAt = 0;
            if (!messages.isEmpty() && messages.get(0) instanceof SystemMessage) {
                insertAt = 1;
            }
            messages.add(insertAt, SystemMessage.from("【早期对话摘要（滑窗外上下文补充）】\n" + summary));
        }
        if (directives != null) {
            for (String directive : directives) {
                if (directive != null && !directive.isBlank()) {
                    messages.add(new UserMessage(directive));
                }
            }
        }
        return messages;
    }

    /**
     * 本轮成功后追加 AI 回复入滑窗，并在超窗时异步预生成摘要（不阻塞当前轮）
     */
    public void recordAiReply(String sessionId, Integer maxMessages, String aiText) {
        if (aiText == null || aiText.isBlank()) {
            return;
        }
        memoryProvider.append(sessionId, maxMessages, List.of(AiMessage.from(aiText)));
        int total = memoryProvider.size(sessionId);
        if (total > RedisChatMemoryProvider.resolveMax(maxMessages)) {
            SUMMARY_EXECUTOR.execute(() -> refreshSummary(sessionId));
        }
    }

    /** 读取已生成的早期摘要（无摘要/异常返回 null） */
    public String readSummary(String sessionId) {
        try {
            return redisTemplate.opsForValue().get(RedisKeys.chatMemorySummary(sessionId));
        } catch (Exception e) {
            log.warn("[aigateway:Context] 读取会话摘要失败 sessionId={}：{}", sessionId, e.getMessage());
            return null;
        }
    }

    /** 异步刷新摘要：滑窗外早期消息 + 旧摘要 → LLM 压缩为一段（失败静默，保留旧摘要） */
    private void refreshSummary(String sessionId) {
        try {
            if (llmService == null) {
                return;
            }
            List<ChatMessage> all = memoryProvider.readAll(sessionId);
            if (all.isEmpty()) {
                return;
            }
            // SystemMessage 为人设不参与摘要；早期消息取全量前段（尾部窗口仍在滑窗内不重复）
            int windowSize = Math.min(all.size(), RedisChatMemoryProvider.resolveMax(null));
            List<ChatMessage> early = all.subList(0, Math.max(0, all.size() - windowSize + 4));
            StringBuilder sb = new StringBuilder();
            for (ChatMessage msg : early) {
                if (msg instanceof SystemMessage) {
                    continue;
                }
                if (msg instanceof UserMessage um && um.singleText() != null) {
                    sb.append("用户：").append(truncate(um.singleText(), 500)).append('\n');
                } else if (msg instanceof AiMessage am && am.text() != null) {
                    sb.append("面试官：").append(truncate(am.text(), 500)).append('\n');
                }
            }
            if (sb.isEmpty()) {
                return;
            }
            String old = readSummary(sessionId);
            String prompt = "请将以下对话记录压缩为一段不超过300字的摘要（保留关键事实、结论与待办线索，客观陈述），"
                    + "直接输出摘要正文：\n" + (old == null ? "" : "【已有摘要，请融合更新】\n" + old + "\n\n") + sb;
            String summary = llmService.generate(prompt);
            if (summary != null && !summary.isBlank()) {
                redisTemplate.opsForValue().set(RedisKeys.chatMemorySummary(sessionId),
                        summary.trim(), RedisKeys.CHAT_MEMORY_EXPIRE_DAYS, TimeUnit.DAYS);
            }
        } catch (Exception e) {
            log.warn("[aigateway:Context] 会话摘要异步刷新失败 sessionId={}：{}", sessionId, e.getMessage());
        }
    }

    private String truncate(String text, int max) {
        return text.length() <= max ? text : text.substring(0, max) + "…";
    }
}
