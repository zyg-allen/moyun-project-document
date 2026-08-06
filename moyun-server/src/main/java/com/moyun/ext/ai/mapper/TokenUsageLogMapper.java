package com.moyun.ext.ai.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.moyun.ext.ai.entity.WorkflowExecution;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * Token使用记录Mapper
 */
@Mapper
public interface TokenUsageLogMapper extends BaseMapper<WorkflowExecution.TokenUsageLog> {

    /**
     * 按智能体统计token使用量
     */
    @Select("SELECT agent_id, model_name, " +
            "SUM(input_tokens) as total_input, " +
            "SUM(output_tokens) as total_output, " +
            "SUM(total_tokens) as total_tokens, " +
            "SUM(cost) as total_cost, " +
            "COUNT(*) as request_count " +
            "FROM token_usage_log " +
            "WHERE agent_id = #{agentId} " +
            "AND create_time BETWEEN #{startTime} AND #{endTime} " +
            "GROUP BY agent_id, model_name")
    List<Map<String, Object>> statByAgent(@Param("agentId") Long agentId,
                                          @Param("startTime") LocalDateTime startTime,
                                          @Param("endTime") LocalDateTime endTime);

    /**
     * 按日期统计token使用量
     */
    @Select("SELECT DATE(create_time) as stat_date, model_name, " +
            "SUM(input_tokens) as total_input, " +
            "SUM(output_tokens) as total_output, " +
            "SUM(total_tokens) as total_tokens, " +
            "SUM(cost) as total_cost, " +
            "COUNT(*) as request_count " +
            "FROM token_usage_log " +
            "WHERE create_time BETWEEN #{startTime} AND #{endTime} " +
            "GROUP BY DATE(create_time), model_name " +
            "ORDER BY stat_date DESC")
    List<Map<String, Object>> statByDate(@Param("startTime") LocalDateTime startTime,
                                         @Param("endTime") LocalDateTime endTime);

    /**
     * 按模型统计token使用量
     */
    @Select("SELECT model_name, model_provider, " +
            "SUM(input_tokens) as total_input, " +
            "SUM(output_tokens) as total_output, " +
            "SUM(total_tokens) as total_tokens, " +
            "SUM(cost) as total_cost, " +
            "COUNT(*) as request_count " +
            "FROM token_usage_log " +
            "WHERE create_time BETWEEN #{startTime} AND #{endTime} " +
            "GROUP BY model_name, model_provider")
    List<Map<String, Object>> statByModel(@Param("startTime") LocalDateTime startTime,
                                          @Param("endTime") LocalDateTime endTime);

    /**
     * 获取总体统计
     */
    @Select("SELECT " +
            "COALESCE(SUM(input_tokens), 0) as total_input, " +
            "COALESCE(SUM(output_tokens), 0) as total_output, " +
            "COALESCE(SUM(total_tokens), 0) as total_tokens, " +
            "COALESCE(SUM(cost), 0) as total_cost, " +
            "COUNT(*) as request_count " +
            "FROM token_usage_log " +
            "WHERE create_time BETWEEN #{startTime} AND #{endTime}")
    Map<String, Object> getTotalStats(@Param("startTime") LocalDateTime startTime,
                                      @Param("endTime") LocalDateTime endTime);

    /**
     * 按请求类型统计token使用量
     */
    @Select("SELECT request_type, " +
            "COALESCE(SUM(input_tokens), 0) as total_input, " +
            "COALESCE(SUM(output_tokens), 0) as total_output, " +
            "COALESCE(SUM(total_tokens), 0) as total_tokens, " +
            "COALESCE(SUM(cost), 0) as total_cost, " +
            "COUNT(*) as request_count " +
            "FROM token_usage_log " +
            "WHERE create_time BETWEEN #{startTime} AND #{endTime} " +
            "GROUP BY request_type")
    List<Map<String, Object>> statByRequestType(@Param("startTime") LocalDateTime startTime,
                                                @Param("endTime") LocalDateTime endTime);
}
