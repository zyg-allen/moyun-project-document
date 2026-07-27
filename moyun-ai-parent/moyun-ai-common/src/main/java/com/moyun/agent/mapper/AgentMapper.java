package com.moyun.agent.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.moyun.agent.entity.Agent;
import org.apache.ibatis.annotations.Mapper;

/**
 * 智能体Mapper
 */
@Mapper
public interface AgentMapper extends BaseMapper<Agent> {
}
