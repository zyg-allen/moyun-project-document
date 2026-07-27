package com.moyun.agent.service.chat;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * 流式对话会话管理器
 *
 * <p>管理活跃的流式对话会话，支持：
 * <ul>
 *   <li>会话注册和注销</li>
 *   <li>中断信号发送</li>
 *   <li>中断状态检查</li>
 * </ul>
 * </p>
 *
 * @author laomao
 * @since 2025-12-12
 */
@Slf4j
@Component
public class StreamingSessionManager {

    /**
     * 活跃会话映射表
     * Key: conversationId
     * Value: 中断标志
     */
    private final Map<Long, AtomicBoolean> activeSessions = new ConcurrentHashMap<>();

    /**
     * 会话请求ID映射（用于同一会话的多次请求）
     * Key: conversationId
     * Value: 当前请求的唯一ID
     */
    private final Map<Long, String> sessionRequestIds = new ConcurrentHashMap<>();

    /**
     * 注册新的流式会话
     *
     * @param conversationId 会话ID
     * @param requestId      请求唯一ID
     * @return 中断标志引用
     */
    public AtomicBoolean registerSession(Long conversationId, String requestId) {
        // 如果已有会话，先发送中断信号（取消之前的请求）
        if (activeSessions.containsKey(conversationId)) {
            log.info("🔄 会话 {} 已存在，中断旧请求", conversationId);
            interruptSession(conversationId);
        }

        AtomicBoolean abortFlag = new AtomicBoolean(false);
        activeSessions.put(conversationId, abortFlag);
        sessionRequestIds.put(conversationId, requestId);
        
        log.info("📝 注册流式会话: conversationId={}, requestId={}", conversationId, requestId);
        return abortFlag;
    }

    /**
     * 注销流式会话
     *
     * @param conversationId 会话ID
     */
    public void unregisterSession(Long conversationId) {
        activeSessions.remove(conversationId);
        sessionRequestIds.remove(conversationId);
        log.info("🗑️ 注销流式会话: conversationId={}", conversationId);
    }

    /**
     * 中断指定会话
     *
     * @param conversationId 会话ID
     * @return 是否成功发送中断信号
     */
    public boolean interruptSession(Long conversationId) {
        AtomicBoolean abortFlag = activeSessions.get(conversationId);
        if (abortFlag != null) {
            abortFlag.set(true);
            log.info("⏹️ 已发送中断信号: conversationId={}", conversationId);
            return true;
        }
        log.warn("⚠️ 会话不存在或已结束: conversationId={}", conversationId);
        return false;
    }

    /**
     * 检查会话是否应该中断
     *
     * @param conversationId 会话ID
     * @return 是否应该中断
     */
    public boolean shouldAbort(Long conversationId) {
        AtomicBoolean abortFlag = activeSessions.get(conversationId);
        return abortFlag != null && abortFlag.get();
    }

    /**
     * 检查会话是否活跃
     *
     * @param conversationId 会话ID
     * @return 是否活跃
     */
    public boolean isSessionActive(Long conversationId) {
        return activeSessions.containsKey(conversationId);
    }

    /**
     * 获取当前活跃会话数
     *
     * @return 活跃会话数
     */
    public int getActiveSessionCount() {
        return activeSessions.size();
    }

    /**
     * 清理所有会话（用于系统关闭时）
     */
    public void clearAllSessions() {
        activeSessions.clear();
        sessionRequestIds.clear();
        log.info("🧹 已清理所有流式会话");
    }
}
