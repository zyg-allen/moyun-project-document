package com.moyun.portal.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.moyun.portal.domain.entity.PortalWritingContest;
import org.apache.ibatis.annotations.Mapper;

/**
 * 创作挑战/征文活动 Mapper
 *
 * 简单查询使用 LambdaQueryWrapper，无需自定义 XML 方法。
 *
 * @author moyun
 */
@Mapper
public interface PortalWritingContestMapper extends BaseMapper<PortalWritingContest> {
}
