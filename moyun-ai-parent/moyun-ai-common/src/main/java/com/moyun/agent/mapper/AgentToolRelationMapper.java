package com.moyun.agent.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.moyun.agent.entity.AgentToolRelation;
import org.apache.ibatis.annotations.Mapper;

/**
 * 智能体-工具关联Mapper
 *
 * <p>提供智能体与工具关联表的数据访问</p>
 *
 * @author laomao
 * @time 2025/11/25
 */
@Mapper
public interface AgentToolRelationMapper extends BaseMapper<AgentToolRelation> {
}
