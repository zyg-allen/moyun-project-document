package com.moyun.ext.ai.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.moyun.ext.ai.entity.AgentTool;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * 智能体工具Mapper
 *
 * <p>提供工具表的数据访问，继承 MyBatis-Plus 的 BaseMapper，
 * 并扩展自定义查询方法</p>
 *
 * @author laomao
 * @time 2025/11/25
 */
@Mapper
public interface AgentToolMapper extends BaseMapper<AgentTool> {
    
    /**
     * 查询智能体关联的所有启用工具
     *
     * @param agentId 智能体ID
     * @return 工具列表
     */
    @Select("SELECT t.* FROM agent_tool t " +
            "INNER JOIN agent_tool_relation r ON t.id = r.tool_id " +
            "WHERE r.agent_id = #{agentId} AND r.enabled = 1 AND t.enabled = 1")
    List<AgentTool> selectToolsByAgentId(@Param("agentId") Long agentId);
    
    /**
     * 查询所有启用的系统内置工具
     *
     * @return 工具列表
     */
    @Select("SELECT * FROM agent_tool WHERE is_system = 1 AND enabled = 1")
    List<AgentTool> selectSystemTools();
}
