package com.moyun.ext.ai.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.moyun.ext.ai.entity.AiSceneConfigHistory;
import org.apache.ibatis.annotations.Mapper;

/**
 * AI 场景配置版本快照 Mapper
 *
 * @author moyun
 * @since 2026-09-24
 */
@Mapper
public interface AiSceneConfigHistoryMapper extends BaseMapper<AiSceneConfigHistory> {
}
