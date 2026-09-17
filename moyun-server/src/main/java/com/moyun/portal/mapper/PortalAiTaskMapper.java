package com.moyun.portal.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.moyun.portal.domain.entity.PortalAiTask;
import org.apache.ibatis.annotations.Mapper;

/**
 * 通用 AI 异步任务 Mapper
 *
 * @author moyun
 */
@Mapper
public interface PortalAiTaskMapper extends BaseMapper<PortalAiTask> {
}
