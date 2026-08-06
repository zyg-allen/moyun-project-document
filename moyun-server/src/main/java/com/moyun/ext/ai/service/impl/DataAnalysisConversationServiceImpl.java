package com.moyun.ext.ai.service.impl;

import com.moyun.ext.ai.dto.DataQueryRequest;
import com.moyun.ext.ai.service.DataAnalysisConversationService;
import com.moyun.ext.ai.service.DataQueryService;
import com.moyun.ext.ai.vo.DataQueryResponse;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 数据分析多轮对话服务实现
 * 支持上下文感知的连续查询
 * 
 * @author laomao
 */
@Slf4j
@Service
public class DataAnalysisConversationServiceImpl implements DataAnalysisConversationService {
    
    @Autowired
    private DataQueryService dataQueryService;
    
    // 会话上下文存储（内存）
    private final Map<String, ConversationContext> sessions = new ConcurrentHashMap<>();
    
    // 追问关键词
    private static final Set<String> FOLLOW_UP_KEYWORDS = new HashSet<>(Arrays.asList(
        "其中", "里面", "当中", "之中",
        "他们", "它们", "这些", "那些",
        "再", "继续", "然后", "接着",
        "也", "还", "另外"
    ));
    
    @Override
    public DataQueryResponse queryWithContext(DataQueryRequest request) {
        String sessionId = request.getSessionId();
        if (sessionId == null || sessionId.isEmpty()) {
            // 无会话ID，直接查询
            return dataQueryService.intelligentQuery(request);
        }
        
        log.info("带上下文查询，sessionId: {}, query: {}", sessionId, request.getQuery());
        
        // 获取或创建会话上下文
        ConversationContext context = sessions.computeIfAbsent(sessionId, k -> new ConversationContext());
        
        // 判断是否为追问
        boolean isFollowUp = isFollowUpQuery(request.getQuery(), sessionId);
        
        DataQueryResponse response;
        if (isFollowUp && context.getLastResult() != null) {
            // 追问：基于上次结果生成查询
            log.info("检测到追问查询，使用上下文信息");
            response = handleFollowUpQuery(request, context);
        } else {
            // 新查询
            log.info("新查询，清除旧上下文");
            response = dataQueryService.intelligentQuery(request);
        }
        
        // 保存查询结果到上下文
        if (response.getSuccess()) {
            context.addQuery(request.getQuery(), response);
            context.setLastResult(response);
            log.info("查询结果已保存到上下文");
        }
        
        return response;
    }
    
    @Override
    public boolean isFollowUpQuery(String query, String sessionId) {
        if (query == null || query.isEmpty()) {
            return false;
        }
        
        // 检查是否有会话上下文
        ConversationContext context = sessions.get(sessionId);
        if (context == null || context.getLastResult() == null) {
            return false;
        }
        
        // 检查是否包含追问关键词
        String lowerQuery = query.toLowerCase();
        for (String keyword : FOLLOW_UP_KEYWORDS) {
            if (lowerQuery.contains(keyword)) {
                log.info("检测到追问关键词: {}", keyword);
                return true;
            }
        }
        
        // 检查是否为简短查询（可能是追问）
        if (query.length() < 15 && context.getHistoryCount() > 0) {
            log.info("检测到简短查询，可能是追问");
            return true;
        }
        
        return false;
    }
    
    @Override
    public void clearContext(String sessionId) {
        if (sessionId != null) {
            sessions.remove(sessionId);
            log.info("已清除会话上下文: {}", sessionId);
        }
    }
    
    /**
     * 处理追问查询
     */
    private DataQueryResponse handleFollowUpQuery(DataQueryRequest request, ConversationContext context) {
        DataQueryResponse lastResult = context.getLastResult();
        String query = request.getQuery();
        
        // 构建增强的查询提示
        String enhancedQuery = buildEnhancedQuery(query, lastResult);
        
        // 创建新的请求
        DataQueryRequest enhancedRequest = new DataQueryRequest();
        enhancedRequest.setDatasourceId(request.getDatasourceId());
        enhancedRequest.setQuery(enhancedQuery);
        enhancedRequest.setNeedAnalysis(request.getNeedAnalysis());
        enhancedRequest.setSessionId(request.getSessionId());
        
        // 执行查询
        return dataQueryService.intelligentQuery(enhancedRequest);
    }
    
    /**
     * 构建增强的查询（包含上下文信息）
     */
    private String buildEnhancedQuery(String query, DataQueryResponse lastResult) {
        StringBuilder enhanced = new StringBuilder();
        
        // 添加上下文信息
        enhanced.append("[上下文信息]\n");
        enhanced.append("上次查询返回了 ").append(lastResult.getTotalCount()).append(" 条数据");
        
        if (lastResult.getGeneratedSql() != null) {
            enhanced.append("，SQL: ").append(lastResult.getGeneratedSql());
        }
        
        if (lastResult.getColumns() != null && !lastResult.getColumns().isEmpty()) {
            enhanced.append("\n包含字段: ");
            enhanced.append(lastResult.getColumns().stream()
                .map(col -> col.getColumnName())
                .reduce((a, b) -> a + ", " + b)
                .orElse(""));
        }
        
        enhanced.append("\n\n[当前问题]\n");
        enhanced.append(query);
        enhanced.append("\n\n请基于上次查询结果回答当前问题。");
        
        return enhanced.toString();
    }
    
    /**
     * 会话上下文
     */
    @Data
    private static class ConversationContext {
        private List<QueryRecord> history = new ArrayList<>();
        private DataQueryResponse lastResult;
        private long lastAccessTime = System.currentTimeMillis();
        
        public void addQuery(String query, DataQueryResponse response) {
            QueryRecord record = new QueryRecord();
            record.setQuery(query);
            record.setResponse(response);
            record.setTimestamp(System.currentTimeMillis());
            
            history.add(record);
            lastAccessTime = System.currentTimeMillis();
            
            // 最多保存10条历史
            if (history.size() > 10) {
                history.remove(0);
            }
        }
        
        public int getHistoryCount() {
            return history.size();
        }
    }
    
    /**
     * 查询记录
     */
    @Data
    private static class QueryRecord {
        private String query;
        private DataQueryResponse response;
        private long timestamp;
    }
    
    /**
     * 定时清理过期会话
     * 每30分钟执行一次，清理超过1小时未访问的会话
     */
    @Scheduled(fixedRate = 1800000) // 30分钟
    public void cleanExpiredSessions() {
        long now = System.currentTimeMillis();
        long expirationTime = 3600000; // 1小时
        
        int removedCount = 0;
        Iterator<Map.Entry<String, ConversationContext>> iterator = sessions.entrySet().iterator();
        
        while (iterator.hasNext()) {
            Map.Entry<String, ConversationContext> entry = iterator.next();
            ConversationContext context = entry.getValue();
            
            if (now - context.getLastAccessTime() > expirationTime) {
                iterator.remove();
                removedCount++;
            }
        }
        
        if (removedCount > 0) {
            log.info("清理过期会话: {}个, 剩余会话: {}个", removedCount, sessions.size());
        }
    }
}
