package com.moyun.ext.ai.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.moyun.ext.ai.entity.ChatHistory;

import java.util.List;
import java.util.Map;

/**
 * 对话历史服务接口
 *
 * @author laomao
 */
public interface ChatHistoryService extends IService<ChatHistory> {
    
    /**
     * 保存对话记录
     */
    void saveChat(Long agentId, String sessionId, String userMessage, String assistantMessage,
                  Integer tokensUsed, String retrievalResults, Integer retrievalCount, Integer responseTime);
    
    /**
     * 获取会话历史
     */
    List<ChatHistory> getSessionHistory(String sessionId, int limit);
    
    /**
     * 获取智能体的会话列表
     */
    List<Map<String, Object>> getAgentSessions(Long agentId, int limit);
    
    /**
     * 获取智能体统计信息
     */
    Map<String, Object> getAgentStats(Long agentId);
    
    /**
     * 删除会话
     */
    boolean deleteSession(String sessionId);
    
    /**
     * 清空智能体所有对话
     */
    boolean clearAgentHistory(Long agentId);
}
