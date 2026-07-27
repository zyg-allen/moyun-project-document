package com.moyun.agent.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.moyun.agent.entity.WorkflowExecution;
import org.apache.ibatis.annotations.Mapper;

/**
 * 工作流执行记录Mapper
 *
 * @author laomao
 */
@Mapper
public interface WorkflowExecutionMapper extends BaseMapper<WorkflowExecution> {
}
