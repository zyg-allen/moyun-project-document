package com.moyun.agent.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.moyun.agent.entity.WorkflowExecution;
import org.apache.ibatis.annotations.Mapper;

/**
 * Token使用统计汇总Mapper
 */
@Mapper
public interface TokenUsageSummaryMapper extends BaseMapper<WorkflowExecution.TokenUsageSummary> {
}
