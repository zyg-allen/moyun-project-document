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
     * 用 DATE_FORMAT 显式返回 'yyyy-MM-dd' 字符串，避免 java.sql.Date 序列化差异
     */
    @Select("SELECT DATE_FORMAT(v.view_time, '%Y-%m-%d') AS date, COUNT(*) AS value " +
            "FROM portal_article_view v " +
            "INNER JOIN portal_article a ON a.id = v.article_id " +
            "WHERE a.author_id = #{userId} AND v.view_time >= #{startTime} " +
            "GROUP BY DATE_FORMAT(v.view_time, '%Y-%m-%d') ORDER BY date")
    List<Map<String, Object>> dailyViewTrend(@Param("userId") Long userId,
                                             @Param("startTime") LocalDateTime startTime);

    /**
     * 当前用户文章近 N 天每日新增点赞数（portal_like JOIN portal_article）
     * 用 DATE_FORMAT 显式返回 'yyyy-MM-dd' 字符串，避免 java.sql.Date 序列化差异
     */
    @Select("SELECT DATE_FORMAT(l.create_time, '%Y-%m-%d') AS date, COUNT(*) AS value " +
            "FROM portal_like l " +
            "INNER JOIN portal_article a ON a.id = l.article_id " +
            "WHERE a.author_id = #{userId} AND l.create_time >= #{startTime} " +
            "GROUP BY DATE_FORMAT(l.create_time, '%Y-%m-%d') ORDER BY date")
    List<Map<String, Object>> dailyLikeTrend(@Param("userId") Long userId,
                                             @Param("startTime") LocalDateTime startTime);

    /**
     * 当前用户文章近 N 天每日新增收藏数（portal_bookmark JOIN portal_article）
     * 用 DATE_FORMAT 显式返回 'yyyy-MM-dd' 字符串，避免 java.sql.Date 序列化差异
     */
    @Select("SELECT DATE_FORMAT(b.create_time, '%Y-%m-%d') AS date, COUNT(*) AS value " +
            "FROM portal_bookmark b " +
            "INNER JOIN portal_article a ON a.id = b.article_id " +
            "WHERE a.author_id = #{userId} AND b.create_time >= #{startTime} " +
            "GROUP BY DATE_FORMAT(b.create_time, '%Y-%m-%d') ORDER BY date")
    List<Map<String, Object>> dailyBookmarkTrend(@Param("userId") Long userId,
                                                 @Param("startTime") LocalDateTime startTime);

    /**
     * 当前用户近 N 天每日新增粉丝数（portal_follow.following_id = 当前用户）
     * 用 DATE_FORMAT 显式返回 'yyyy-MM-dd' 字符串，避免 java.sql.Date 序列化差异
     */
    @Select("SELECT DATE_FORMAT(f.create_time, '%Y-%m-%d') AS date, COUNT(*) AS value " +
            "FROM portal_follow f " +
            "WHERE f.following_id = #{userId} AND f.create_time >= #{startTime} " +
            "GROUP BY DATE_FORMAT(f.create_time, '%Y-%m-%d') ORDER BY date")
    List<Map<String, Object>> dailyFollowerTrend(@Param("userId") Long userId,
                                                 @Param("startTime") LocalDateTime startTime);

    // ==================== 创作日历热力图（近 1 年） ====================

    /**
     * 当前用户近 1 年文章创建/更新按日统计（UNION create_time 与 update_time）
     * 用 DATE_FORMAT 显式返回 'yyyy-MM-dd' 字符串
     */
    @Select("SELECT dt AS date, SUM(cnt) AS count FROM ( " +
            "  SELECT DATE_FORMAT(create_time, '%Y-%m-%d') AS dt, COUNT(*) AS cnt FROM portal_article " +
            "  WHERE author_id = #{userId} AND create_time >= #{startTime} " +
            "  GROUP BY DATE_FORMAT(create_time, '%Y-%m-%d') " +
            "  UNION ALL " +
            "  SELECT DATE_FORMAT(update_time, '%Y-%m-%d') AS dt, COUNT(*) AS cnt FROM portal_article " +
            "  WHERE author_id = #{userId} AND update_time >= #{startTime} " +
            "  GROUP BY DATE_FORMAT(update_time, '%Y-%m-%d') " +
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
