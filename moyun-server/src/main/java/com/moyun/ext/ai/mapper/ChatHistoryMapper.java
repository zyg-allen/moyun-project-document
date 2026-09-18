package com.moyun.ext.ai.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.moyun.ext.ai.entity.ChatHistory;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;
import java.util.Map;

/**
 * 对话历史Mapper
 *
 * @author laomao
 */
@Mapper
public interface ChatHistoryMapper extends BaseMapper<ChatHistory> {
    
    /**
     * 获取智能体的会话列表
     *
     * @param agentId 智能体ID
     * @param limit 限制数量
     * @return 会话列表
     */

    List<Map<String, Object>> getSessionsByAgentId(@Param("agentId") Long agentId, @Param("limit") int limit);
    
    /**
     * 获取智能体统计信息
     *
     * @param agentId 智能体ID
     * @return 统计信息
     */

    Map<String, Object> getStatsByAgentId(@Param("agentId") Long agentId);
}
