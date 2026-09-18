package com.moyun.ext.ai.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.moyun.ext.ai.entity.WorkflowExecution;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

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

    List<Map<String, Object>> statByAgent(@Param("agentId") Long agentId,
                                          @Param("startTime") LocalDateTime startTime,
                                          @Param("endTime") LocalDateTime endTime);

    /**
     * 按日期统计token使用量
     */

    List<Map<String, Object>> statByDate(@Param("startTime") LocalDateTime startTime,
                                         @Param("endTime") LocalDateTime endTime);

    /**
     * 按模型统计token使用量
     */

    List<Map<String, Object>> statByModel(@Param("startTime") LocalDateTime startTime,
                                          @Param("endTime") LocalDateTime endTime);

    /**
     * 获取总体统计
     */

    Map<String, Object> getTotalStats(@Param("startTime") LocalDateTime startTime,
                                      @Param("endTime") LocalDateTime endTime);

    /**
     * 按请求类型统计token使用量
     */

    List<Map<String, Object>> statByRequestType(@Param("startTime") LocalDateTime startTime,
                                                @Param("endTime") LocalDateTime endTime);
}
