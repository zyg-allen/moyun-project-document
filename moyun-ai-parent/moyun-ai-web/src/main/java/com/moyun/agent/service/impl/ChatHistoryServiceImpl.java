package com.moyun.agent.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.moyun.agent.entity.ChatHistory;
import com.moyun.agent.mapper.ChatHistoryMapper;
import com.moyun.agent.service.ChatHistoryService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * 对话历史服务实现
 *
 * @author laomao
 */
@Slf4j
@Service
public class ChatHistoryServiceImpl extends ServiceImpl<ChatHistoryMapper, ChatHistory> implements ChatHistoryService {
    
    @Override
    public void saveChat(Long agentId, String sessionId, String userMessage, String assistantMessage,
                         Integer tokensUsed, String retrievalResults, Integer retrievalCount, Integer responseTime) {
        ChatHistory history = new ChatHistory();
        history.setAgentId(agentId);
        history.setSessionId(sessionId);
        history.setUserMessage(userMessage);
        history.setAssistantMessage(assistantMessage);
        history.setTokensUsed(tokensUsed != null ? tokensUsed : 0);
        history.setRetrievalResults(retrievalResults);
        history.setRetrievalCount(retrievalCount != null ? retrievalCount : 0);
        history.setResponseTime(responseTime != null ? responseTime : 0);
        history.setCreateTime(LocalDateTime.now());
        
        save(history);
        log.debug("保存对话记录 - agentId: {}, sessionId: {}", agentId, sessionId);
    }
    
    @Override
    public List<ChatHistory> getSessionHistory(String sessionId, int limit) {
        return list(new LambdaQueryWrapper<ChatHistory>()
                .eq(ChatHistory::getSessionId, sessionId)
                .orderByAsc(ChatHistory::getCreateTime)
                .last("LIMIT " + limit));
    }
    
    @Override
    public List<Map<String, Object>> getAgentSessions(Long agentId, int limit) {
        return baseMapper.getSessionsByAgentId(agentId, limit);
    }
    
    @Override
    public Map<String, Object> getAgentStats(Long agentId) {
        return baseMapper.getStatsByAgentId(agentId);
    }
    
    @Override
    public boolean deleteSession(String sessionId) {
        return remove(new LambdaQueryWrapper<ChatHistory>()
                .eq(ChatHistory::getSessionId, sessionId));
    }
    
    @Override
    public boolean clearAgentHistory(Long agentId) {
        return remove(new LambdaQueryWrapper<ChatHistory>()
                .eq(ChatHistory::getAgentId, agentId));
    }
}
