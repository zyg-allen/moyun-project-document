package com.moyun.portal.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.moyun.portal.domain.entity.PortalReadingProgress;
import com.moyun.portal.domain.query.ReadingProgressQuery;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 阅读进度表 数据层
 *
 * @author moyun
 */
@Mapper
public interface PortalReadingProgressMapper extends BaseMapper<PortalReadingProgress>
{
    Page<PortalReadingProgress> selectPortalReadingProgressPage(Page<PortalReadingProgress> page, @Param("query") ReadingProgressQuery query);

    List<PortalReadingProgress> selectPortalReadingProgressList(@Param("query") ReadingProgressQuery query);

    PortalReadingProgress selectPortalReadingProgressById(@Param("id") Long id);

    PortalReadingProgress selectByUserAndBook(@Param("userId") Long userId, @Param("bookId") Long bookId);

    int insertPortalReadingProgress(PortalReadingProgress portalReadingProgress);

    int updatePortalReadingProgress(PortalReadingProgress portalReadingProgress);

    int deletePortalReadingProgressById(@Param("id") Long id);

    int deletePortalReadingProgressByIds(@Param("ids") Long[] ids);
}
