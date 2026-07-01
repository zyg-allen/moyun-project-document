package com.moyun.portal.mapper;

import java.util.List;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import com.moyun.portal.domain.entity.PortalReadingProgress;
import com.moyun.portal.domain.query.ReadingProgressQuery;

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

    /**
     * v1.0 第二阶段：章节级进度 upsert
     * 存在则更新章节相关字段，不存在则插入新记录
     */
    int upsertChapterProgress(PortalReadingProgress portalReadingProgress);

    /**
     * v1.0 第二阶段：查询用户最近阅读记录（按 last_read_time 倒序）
     */
    List<PortalReadingProgress> selectRecentReading(@Param("userId") Long userId, @Param("limit") int limit);

    int insertPortalReadingProgress(PortalReadingProgress portalReadingProgress);

    int updatePortalReadingProgress(PortalReadingProgress portalReadingProgress);

    int deletePortalReadingProgressById(@Param("id") Long id);

    int deletePortalReadingProgressByIds(@Param("ids") Long[] ids);
}
