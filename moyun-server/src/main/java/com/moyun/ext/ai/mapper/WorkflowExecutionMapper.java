package com.moyun.ext.ai.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.moyun.ext.ai.entity.WorkflowExecution;
import org.apache.ibatis.annotations.Mapper;

/**
 * 工作流执行记录Mapper
 *
 * @author laomao
 */
@Mapper
public interface WorkflowExecutionMapper extends BaseMapper<WorkflowExecution> {
}
