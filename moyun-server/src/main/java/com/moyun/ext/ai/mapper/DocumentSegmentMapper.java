package com.moyun.ext.ai.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.moyun.ext.ai.entity.DocumentSegment;
import org.apache.ibatis.annotations.Mapper;

/**
 * 文档分片Mapper
 */
@Mapper
public interface DocumentSegmentMapper extends BaseMapper<DocumentSegment> {
}
