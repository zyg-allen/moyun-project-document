package com.moyun.ext.ai.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.moyun.ext.ai.entity.Agent;
import org.apache.ibatis.annotations.Mapper;

/**
 * 智能体Mapper
 */
@Mapper
public interface AgentMapper extends BaseMapper<Agent> {
}
