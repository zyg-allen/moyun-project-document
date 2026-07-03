package com.moyun.portal.mapper;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

/**
 * 创作者中心 聚合查询 Mapper
 * <p>
 * 直接聚合现有表（portal_article / portal_article_view / portal_like /
 * portal_bookmark / portal_follow），不新建任何表。
 *
 * @author moyun
 */
@Mapper
public interface PortalCreatorMapper {

    // ==================== 数据看板（近 30 天） ====================

    /**
     * 当前用户文章近 N 天每日阅读总数（portal_article_view JOIN portal_article）
     */
    @Select("SELECT DATE(v.view_time) AS date, COUNT(*) AS value " +
            "FROM portal_article_view v " +
            "INNER JOIN portal_article a ON a.id = v.article_id " +
            "WHERE a.author_id = #{userId} AND v.view_time >= #{startTime} " +
            "GROUP BY DATE(v.view_time) ORDER BY date")
    List<Map<String, Object>> dailyViewTrend(@Param("userId") Long userId,
                                             @Param("startTime") LocalDateTime startTime);

    /**
     * 当前用户文章近 N 天每日新增点赞数（portal_like JOIN portal_article）
     */
    @Select("SELECT DATE(l.create_time) AS date, COUNT(*) AS value " +
            "FROM portal_like l " +
            "INNER JOIN portal_article a ON a.id = l.article_id " +
            "WHERE a.author_id = #{userId} AND l.create_time >= #{startTime} " +
            "GROUP BY DATE(l.create_time) ORDER BY date")
    List<Map<String, Object>> dailyLikeTrend(@Param("userId") Long userId,
                                             @Param("startTime") LocalDateTime startTime);

    /**
     * 当前用户文章近 N 天每日新增收藏数（portal_bookmark JOIN portal_article）
     */
    @Select("SELECT DATE(b.create_time) AS date, COUNT(*) AS value " +
            "FROM portal_bookmark b " +
            "INNER JOIN portal_article a ON a.id = b.article_id " +
            "WHERE a.author_id = #{userId} AND b.create_time >= #{startTime} " +
            "GROUP BY DATE(b.create_time) ORDER BY date")
    List<Map<String, Object>> dailyBookmarkTrend(@Param("userId") Long userId,
                                                 @Param("startTime") LocalDateTime startTime);

    /**
     * 当前用户近 N 天每日新增粉丝数（portal_follow.following_id = 当前用户）
     */
    @Select("SELECT DATE(f.create_time) AS date, COUNT(*) AS value " +
            "FROM portal_follow f " +
            "WHERE f.following_id = #{userId} AND f.create_time >= #{startTime} " +
            "GROUP BY DATE(f.create_time) ORDER BY date")
    List<Map<String, Object>> dailyFollowerTrend(@Param("userId") Long userId,
                                                 @Param("startTime") LocalDateTime startTime);

    // ==================== 创作日历热力图（近 1 年） ====================

    /**
     * 当前用户近 1 年文章创建/更新按日统计（UNION create_time 与 update_time）
     */
    @Select("SELECT dt AS date, SUM(cnt) AS count FROM ( " +
            "  SELECT DATE(create_time) AS dt, COUNT(*) AS cnt FROM portal_article " +
            "  WHERE author_id = #{userId} AND create_time >= #{startTime} " +
            "  GROUP BY DATE(create_time) " +
            "  UNION ALL " +
            "  SELECT DATE(update_time) AS dt, COUNT(*) AS cnt FROM portal_article " +
            "  WHERE author_id = #{userId} AND update_time >= #{startTime} " +
            "  GROUP BY DATE(update_time) " +
            ") t GROUP BY dt ORDER BY date")
    List<Map<String, Object>> calendarHeatmap(@Param("userId") Long userId,
                                              @Param("startTime") LocalDateTime startTime);

    // ==================== 读者画像（近 30 天） ====================

    /**
     * 当前用户文章近 30 天读者地域分布 Top10
     * <p>
     * 现有表无城市字段，按 portal_article_view.ip 聚合作为地域近似，
     * 无访问记录时返回空集合（前端展示"暂无地域分布数据"）。
     */
    @Select("SELECT COALESCE(v.ip, '未知') AS region, COUNT(*) AS value " +
            "FROM portal_article_view v " +
            "INNER JOIN portal_article a ON a.id = v.article_id " +
            "WHERE a.author_id = #{userId} AND v.view_time >= #{startTime} " +
            "GROUP BY v.ip " +
            "ORDER BY value DESC " +
            "LIMIT 10")
    List<Map<String, Object>> readerRegionTop10(@Param("userId") Long userId,
                                               @Param("startTime") LocalDateTime startTime);

    /**
     * 当前用户文章近 30 天读者时段分布（0-23 时）
     */
    @Select("SELECT HOUR(v.view_time) AS hour, COUNT(*) AS value " +
            "FROM portal_article_view v " +
            "INNER JOIN portal_article a ON a.id = v.article_id " +
            "WHERE a.author_id = #{userId} AND v.view_time >= #{startTime} " +
            "GROUP BY HOUR(v.view_time) ORDER BY hour")
    List<Map<String, Object>> readerHourDistribution(@Param("userId") Long userId,
                                                    @Param("startTime") LocalDateTime startTime);
}
