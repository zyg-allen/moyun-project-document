package com.moyun.ext.ai.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.moyun.ext.ai.entity.ChatHistory;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

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
    @Select("SELECT session_id, MIN(create_time) as start_time, MAX(create_time) as last_time, " +
            "COUNT(*) as message_count, SUM(tokens_used) as total_tokens " +
            "FROM chat_history WHERE agent_id = #{agentId} " +
            "GROUP BY session_id ORDER BY last_time DESC LIMIT #{limit}")
    List<Map<String, Object>> getSessionsByAgentId(@Param("agentId") Long agentId, @Param("limit") int limit);
    
    /**
     * 获取智能体统计信息
     *
     * @param agentId 智能体ID
     * @return 统计信息
     */
    @Select("SELECT COUNT(DISTINCT session_id) as session_count, " +
            "COUNT(*) as message_count, " +
            "SUM(tokens_used) as total_tokens, " +
            "AVG(response_time) as avg_response_time " +
            "FROM chat_history WHERE agent_id = #{agentId}")
    Map<String, Object> getStatsByAgentId(@Param("agentId") Long agentId);
}
