package com.moyun.portal.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.moyun.portal.domain.entity.PortalArticleView;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 文章浏览记录Mapper
 */
@Mapper
public interface PortalArticleViewMapper extends BaseMapper<PortalArticleView>
{
    /**
     * 检查是否已经浏览过（同一用户或IP在有效时间内）
     */
    @Select("SELECT COUNT(*) FROM portal_article_view " +
            "WHERE article_id = #{articleId} " +
            "AND view_time > #{startTime} " +
            "AND ((user_id IS NOT NULL AND user_id = #{userId}) " +
            "OR (user_id IS NULL AND ip = #{ip}))")
    int countRecentViews(@Param("articleId") Long articleId,
                         @Param("userId") Long userId,
                         @Param("ip") String ip,
                         @Param("startTime") LocalDateTime startTime);

    /**
     * 检查IP是否已经浏览过（有效时间内）
     */
    @Select("SELECT COUNT(*) FROM portal_article_view " +
            "WHERE article_id = #{articleId} " +
            "AND view_time > #{startTime} " +
            "AND ip = #{ip}")
    int countRecentViewsByIp(@Param("articleId") Long articleId,
                             @Param("ip") String ip,
                             @Param("startTime") LocalDateTime startTime);

    /**
     * 获取有效阅读数（防止重复计数）
     */
    @Select("SELECT COUNT(DISTINCT " +
            "CASE WHEN user_id IS NOT NULL THEN CONCAT('u:', user_id) " +
            "ELSE CONCAT('i:', ip) END) " +
            "FROM portal_article_view " +
            "WHERE article_id = #{articleId}")
    int countUniqueViews(@Param("articleId") Long articleId);

    /**
     * 查询所有有浏览记录的文章ID
     */
    @Select("SELECT DISTINCT article_id FROM portal_article_view WHERE article_id IS NOT NULL")
    List<Long> selectAllViewedArticleIds();
}
