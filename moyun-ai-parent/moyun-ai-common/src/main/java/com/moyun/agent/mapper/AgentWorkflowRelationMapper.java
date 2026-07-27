package com.moyun.agent.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.moyun.agent.entity.AgentWorkflowRelation;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * 智能体-工作流关联 Mapper
 *
 * @author laomao
 */
@Mapper
public interface AgentWorkflowRelationMapper extends BaseMapper<AgentWorkflowRelation> {
    
    /**
     * 获取智能体绑定的工作流ID列表
     */
    @Select("SELECT workflow_id FROM agent_workflow_relation WHERE agent_id = #{agentId} AND enabled = 1 ORDER BY sort_order")
    List<Long> selectWorkflowIdsByAgentId(@Param("agentId") Long agentId);
    
    /**
     * 获取绑定了该工作流的智能体ID列表
     */
    @Select("SELECT agent_id FROM agent_workflow_relation WHERE workflow_id = #{workflowId} AND enabled = 1")
    List<Long> selectAgentIdsByWorkflowId(@Param("workflowId") Long workflowId);
}
