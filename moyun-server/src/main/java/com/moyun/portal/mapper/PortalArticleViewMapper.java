package com.moyun.portal.mapper;

import java.time.LocalDateTime;
import java.util.List;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import com.moyun.portal.domain.entity.PortalArticleView;

/**
 * 文章浏览记录Mapper
 */
@Mapper
public interface PortalArticleViewMapper extends BaseMapper<PortalArticleView>
{
    /**
     * 检查是否已经浏览过（同一用户或IP在有效时间内）
     */

    int countRecentViews(@Param("articleId") Long articleId,
                         @Param("userId") Long userId,
                         @Param("ip") String ip,
                         @Param("startTime") LocalDateTime startTime);

    /**
     * 检查IP是否已经浏览过（有效时间内）
     */

    int countRecentViewsByIp(@Param("articleId") Long articleId,
                             @Param("ip") String ip,
                             @Param("startTime") LocalDateTime startTime);

    /**
     * 获取有效阅读数（防止重复计数）
     */

    int countUniqueViews(@Param("articleId") Long articleId);

    /**
     * 查询所有有浏览记录的文章ID
     */

    List<Long> selectAllViewedArticleIds();

    // ========== 运营首页访客统计 ==========

    /**
     * 今日访客数（UV，去重用户ID+IP）
     */

    long countTodayVisitors(@Param("startTime") LocalDateTime startTime);

    /**
     * 今日页面浏览量（PV）
     */

    long countTodayPageViews(@Param("startTime") LocalDateTime startTime);

    /**
     * 近N天每日UV趋势（折线图）
     */

    List<java.util.Map<String, Object>> selectDailyVisitorTrend(@Param("startTime") LocalDateTime startTime);
}
