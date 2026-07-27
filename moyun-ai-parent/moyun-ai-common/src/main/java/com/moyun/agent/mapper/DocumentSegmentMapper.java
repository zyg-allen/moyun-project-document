package com.moyun.agent.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.moyun.agent.entity.DocumentSegment;
import org.apache.ibatis.annotations.Mapper;

/**
 * 文档分片Mapper
 */
@Mapper
public interface DocumentSegmentMapper extends BaseMapper<DocumentSegment> {
}
