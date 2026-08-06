package com.moyun.ext.ai.service;

import com.moyun.ext.ai.dto.DataQueryRequest;
import com.moyun.ext.ai.vo.DataQueryResponse;

/**
 * 数据分析多轮对话服务
 * 支持上下文感知的连续查询
 * 
 * @author laomao
 */
public interface DataAnalysisConversationService {
    
    /**
     * 带上下文的智能查询
     * 
     * @param request 查询请求
     * @return 查询响应
     */
    DataQueryResponse queryWithContext(DataQueryRequest request);
    
    /**
     * 判断是否为追问查询
     * 
     * @param query 查询文本
     * @param sessionId 会话ID
     * @return 是否为追问
     */
    boolean isFollowUpQuery(String query, String sessionId);
    
    /**
     * 清除会话上下文
     * 
     * @param sessionId 会话ID
     */
    void clearContext(String sessionId);
}
