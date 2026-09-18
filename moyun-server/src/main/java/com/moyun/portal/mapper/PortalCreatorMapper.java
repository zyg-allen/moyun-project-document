package com.moyun.portal.mapper;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

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

    List<Map<String, Object>> dailyViewTrend(@Param("userId") Long userId,
                                             @Param("startTime") LocalDateTime startTime);

    /**
     * 当前用户文章近 N 天每日新增点赞数（portal_like JOIN portal_article）
     * 用 DATE_FORMAT 显式返回 'yyyy-MM-dd' 字符串，避免 java.sql.Date 序列化差异
     */

    List<Map<String, Object>> dailyLikeTrend(@Param("userId") Long userId,
                                             @Param("startTime") LocalDateTime startTime);

    /**
     * 当前用户文章近 N 天每日新增收藏数（portal_bookmark JOIN portal_article）
     * 用 DATE_FORMAT 显式返回 'yyyy-MM-dd' 字符串，避免 java.sql.Date 序列化差异
     */

    List<Map<String, Object>> dailyBookmarkTrend(@Param("userId") Long userId,
                                                 @Param("startTime") LocalDateTime startTime);

    /**
     * 当前用户近 N 天每日新增粉丝数（portal_follow.following_id = 当前用户）
     * 用 DATE_FORMAT 显式返回 'yyyy-MM-dd' 字符串，避免 java.sql.Date 序列化差异
     */

    List<Map<String, Object>> dailyFollowerTrend(@Param("userId") Long userId,
                                                 @Param("startTime") LocalDateTime startTime);

    // ==================== 创作日历热力图（近 1 年） ====================

    /**
     * 当前用户近 1 年文章创建/更新按日统计（UNION create_time 与 update_time）
     * 用 DATE_FORMAT 显式返回 'yyyy-MM-dd' 字符串
     */

    List<Map<String, Object>> calendarHeatmap(@Param("userId") Long userId,
                                              @Param("startTime") LocalDateTime startTime);

    // ==================== 读者画像（近 30 天） ====================

    /**
     * 当前用户文章近 30 天读者地域分布 Top10
     * <p>
     * 现有表无城市字段，按 portal_article_view.ip 聚合作为地域近似，
     * 无访问记录时返回空集合（前端展示"暂无地域分布数据"）。
     */

    List<Map<String, Object>> readerRegionTop10(@Param("userId") Long userId,
                                               @Param("startTime") LocalDateTime startTime);

    /**
     * 当前用户文章近 30 天读者时段分布（0-23 时）
     */

    List<Map<String, Object>> readerHourDistribution(@Param("userId") Long userId,
                                                    @Param("startTime") LocalDateTime startTime);

    /**
     * 读者画像：近 30 天读者性别分布
     * <p>数据局限：仅统计登录读者（user_id 非空），游客无法统计；
     * 读者未填写 gender 时归入 "unknown" 桶。</p>
     * <p>用 COUNT(DISTINCT v.user_id) 而非 COUNT(*)，避免同一读者多次阅读被重复计数。</p>
     */

    List<Map<String, Object>> readerGenderDistribution(@Param("userId") Long userId,
                                                       @Param("startTime") LocalDateTime startTime);

    /**
     * 读者画像：近 30 天读者年龄段分布
     * <p>年龄段划分：under_18 / 18_24 / 25_30 / 31_35 / 36_45 / over_45 / unknown</p>
     * <p>数据局限（用户已指出）：
     * <ul>
     *   <li>仅统计登录读者，游客无法统计</li>
     *   <li>birthday 为用户自填字符串，可能不真实（除非实名制）</li>
     *   <li>birthday 为空或格式异常（STR_TO_DATE 返回 NULL）时归入 "unknown" 桶</li>
     * </ul>
     * </p>
     * <p>用 STR_TO_DATE 兼容字符串存储，TIMESTAMPDIFF 计算年龄。
     * 修复：原 SQL 对 birthday 为非日期格式时 STR_TO_DATE 返回 NULL，
     * TIMESTAMPDIFF 返回 NULL，所有 WHEN 条件均不匹配，错误落入 ELSE 'over_45'。
     * 现在显式判断 STR_TO_DATE IS NULL 归入 'unknown'，避免污染 over_45 桶。</p>
     */

    List<Map<String, Object>> readerAgeRangeDistribution(@Param("userId") Long userId,
                                                          @Param("startTime") LocalDateTime startTime);
}
