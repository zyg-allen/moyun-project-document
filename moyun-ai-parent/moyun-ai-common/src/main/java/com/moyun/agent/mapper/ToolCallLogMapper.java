package com.moyun.agent.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.moyun.agent.entity.ToolCallLog;
import org.apache.ibatis.annotations.Mapper;

/**
 * 工具调用日志Mapper
 *
 * <p>提供工具调用日志表的数据访问，用于记录和查询工具执行历史</p>
 *
 * @author laomao
 * @time 2025/11/25
 */
@Mapper
public interface ToolCallLogMapper extends BaseMapper<ToolCallLog> {
}
