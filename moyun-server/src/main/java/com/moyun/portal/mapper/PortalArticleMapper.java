package com.moyun.portal.mapper;

import java.util.List;
import java.util.Map;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import com.moyun.ext.cms.domain.query.CmsArticleQuery;
import com.moyun.ext.cms.domain.vo.CmsArticleVO;
import com.moyun.portal.domain.entity.PortalArticle;
import com.moyun.portal.domain.query.ArticleQuery;

/**
 * 门户文章表 数据层
 *
 * @author moyun
 */
@Mapper
public interface PortalArticleMapper extends BaseMapper<PortalArticle> {

    /**
     * 根据条件分页查询文章列表
     *
     * @param page 分页参数
     * @param query 查询条件
     * @return 分页结果
     */
    Page<PortalArticle> selectPortalArticlePage(Page<PortalArticle> page, @Param("params") ArticleQuery query);

    /**
     * 查询"我的文章"分页列表（按 authorId 过滤，不强制 status=published）
     * 用于作者查看自己所有状态的文章（草稿/待审核/已发布/已拒绝）
     *
     * @param page 分页参数
     * @param query 查询条件（authorId 必填，status 可选）
     * @return 分页结果
     */
    Page<PortalArticle> selectMyArticlesPage(Page<PortalArticle> page, @Param("params") ArticleQuery query);

    /**
     * 根据条件查询文章列表（不分页，用于导出等场景）
     *
     * @param query 查询条件
     * @return 文章列表
     */
    List<PortalArticle> selectPortalArticleList(@Param("params") ArticleQuery query);

    /**
     * 查询热门文章（按浏览量排序）
     *
     * @param page 分页参数
     * @return 文章列表
     */
    List<PortalArticle> selectHotArticles(Page<PortalArticle> page);

    /**
     * 查询精选文章
     *
     * @param page 分页参数
     * @return 文章列表
     */
    List<PortalArticle> selectFeaturedArticles(Page<PortalArticle> page);

    /**
     * 查询轮播文章
     *
     * @return 文章列表
     */
    List<PortalArticle> selectCarouselArticles();

    /**
     * 查询相关文章
     *
     * @param page 分页参数
     * @param currentId 当前文章ID
     * @return 文章列表
     */
    List<PortalArticle> selectRelatedArticles(Page<PortalArticle> page, @Param("currentId") Long currentId);

    /**
     * 查询最新文章
     *
     * @param page 分页参数
     * @return 文章列表
     */
    List<PortalArticle> selectLatestArticles(Page<PortalArticle> page);

    /**
     * 通过文章ID查询文章
     *
     * @param id 文章ID
     * @return 文章对象
     */
    PortalArticle selectPortalArticleById(Long id);

    /**
     * 通过文章别名查询文章
     *
     * @param slug 文章别名
     * @return 文章对象
     */
    PortalArticle selectPortalArticleBySlug(@Param("slug") String slug);

    /**
     * 新增文章信息
     *
     * @param portalArticle 文章信息
     * @return 结果
     */
    int insertPortalArticle(PortalArticle portalArticle);

    /**
     * 修改文章信息
     *
     * @param portalArticle 文章信息
     * @return 结果
     */
    int updatePortalArticle(PortalArticle portalArticle);

    /**
     * 通过文章ID删除文章
     *
     * @param id 文章ID
     * @return 结果
     */
    int deletePortalArticleById(Long id);

    /**
     * 批量删除文章信息
     *
     * @param ids 需要删除的文章ID
     * @return 结果
     */
    int deletePortalArticleByIds(Long[] ids);

    // ========== CMS专用方法 ==========

    /**
     * CMS分页查询文章（所有状态），返回CMS视图对象
     *
     * @param page 分页参数
     * @param query 查询条件（CMS专用）
     * @return 分页结果
     */
    Page<CmsArticleVO> selectCmsArticlePage(Page<CmsArticleVO> page, @Param("params") CmsArticleQuery query);

    /**
     * CMS不分页查询文章（所有状态），返回CMS视图对象
     *
     * @param query 查询条件（CMS专用）
     * @return 文章列表
     */
    List<CmsArticleVO> selectCmsArticleList(@Param("params") CmsArticleQuery query);

    /**
     * CMS根据ID查询文章，返回CMS视图对象
     *
     * @param id 文章ID
     * @return 文章对象
     */
    CmsArticleVO selectCmsArticleById(Long id);

    // ========== 原子计数更新方法 ==========

    /**
     * 原子增加浏览量
     *
     * @param id    文章ID
     * @param delta 增量（正数增加，负数减少）
     * @return 受影响行数
     */
    @Update("UPDATE portal_article SET views = views + #{delta} WHERE id = #{id}")
    int incrementViews(@Param("id") Long id, @Param("delta") long delta);

    /**
     * 原子增加点赞数
     *
     * @param id    文章ID
     * @param delta 增量（正数增加，负数减少）
     * @return 受影响行数
     */
    @Update("UPDATE portal_article SET likes = likes + #{delta} WHERE id = #{id}")
    int incrementLikes(@Param("id") Long id, @Param("delta") long delta);

    /**
     * 原子增加评论数
     *
     * @param id    文章ID
     * @param delta 增量（正数增加，负数减少）
     * @return 受影响行数
     */
    @Update("UPDATE portal_article SET comments = comments + #{delta} WHERE id = #{id}")
    int incrementComments(@Param("id") Long id, @Param("delta") long delta);

    /**
     * 原子增加收藏数
     *
     * @param id    文章ID
     * @param delta 增量（正数增加，负数减少）
     * @return 受影响行数
     */
    @Update("UPDATE portal_article SET bookmark_count = bookmark_count + #{delta} WHERE id = #{id}")
    int incrementBookmarkCount(@Param("id") Long id, @Param("delta") long delta);

    /**
     * 按作者聚合文章统计（从文章表实时聚合，作为统计展示的真实数据源）
     * 仅统计已发布文章
     *
     * @param authorId 作者用户ID
     * @return Map 包含 articleCount / viewSum / likeSum / bookmarkSum / commentSum
     */
    @Select("SELECT count(*) AS articleCount, " +
            "coalesce(sum(views), 0) AS viewSum, " +
            "coalesce(sum(likes), 0) AS likeSum, " +
            "coalesce(sum(bookmark_count), 0) AS bookmarkSum, " +
            "coalesce(sum(comments), 0) AS commentSum " +
            "FROM portal_article " +
            "WHERE author_id = #{authorId} AND status = 'published'")
    Map<String, Object> selectAuthorArticleStats(@Param("authorId") Long authorId);

    // ========== 运营首页聚合统计方法 ==========

    /**
     * 文章核心指标统计（全站总量）
     * @return Map 包含 totalArticles/publishedArticles/pendingArticles/draftArticles/totalViews/totalLikes/totalComments
     */
    @Select("SELECT " +
            "count(*) AS totalArticles, " +
            "sum(CASE WHEN status = 'published' THEN 1 ELSE 0 END) AS publishedArticles, " +
            "sum(CASE WHEN status = 'pending' THEN 1 ELSE 0 END) AS pendingArticles, " +
            "sum(CASE WHEN status = 'draft' THEN 1 ELSE 0 END) AS draftArticles, " +
            "coalesce(sum(views), 0) AS totalViews, " +
            "coalesce(sum(likes), 0) AS totalLikes, " +
            "coalesce(sum(comments), 0) AS totalComments " +
            "FROM portal_article")
    Map<String, Object> selectArticleMetrics();

    /**
     * 按日期范围统计每日新增文章数（趋势图）
     */
    @Select("SELECT DATE(create_time) AS date, count(*) AS value " +
            "FROM portal_article " +
            "WHERE create_time >= #{startTime} " +
            "GROUP BY DATE(create_time) ORDER BY date")
    List<Map<String, Object>> selectDailyPublishTrend(@Param("startTime") java.time.LocalDateTime startTime);

    /**
     * 栏目排行榜：按文章数和浏览量聚合 Top N
     */
    @Select("SELECT c.id AS categoryId, c.name AS categoryName, " +
            "count(a.id) AS articleCount, " +
            "coalesce(sum(a.views), 0) AS totalViews, " +
            "coalesce(sum(a.likes), 0) AS totalLikes " +
            "FROM portal_category c " +
            "LEFT JOIN portal_article a ON a.category_id = c.id AND a.status = 'published' " +
            "GROUP BY c.id, c.name " +
            "ORDER BY articleCount DESC, totalViews DESC " +
            "LIMIT #{limit}")
    List<Map<String, Object>> selectCategoryRanking(@Param("limit") int limit);

    /**
     * 查询待审核文章列表（运营首页待办任务）
     */
    @Select("SELECT a.id, a.title, a.author_id AS authorId, a.create_time, " +
            "u.nickname AS authorNickname, u.username AS authorUsername " +
            "FROM portal_article a " +
            "LEFT JOIN portal_user u ON u.id = a.author_id " +
            "WHERE a.status = 'pending' " +
            "ORDER BY a.create_time ASC " +
            "LIMIT #{limit}")
    List<Map<String, Object>> selectPendingArticles(@Param("limit") int limit);

    /**
     * 统计待审核文章数量
     */
    @Select("SELECT count(*) FROM portal_article WHERE status = 'pending'")
    long countPendingArticles();

    /**
     * 热门文章 Top N（按浏览量+点赞数加权排序，用于 Redis ZSet 初始化）
     */
    @Select("SELECT id, title, views, likes, " +
            "(coalesce(views,0) + coalesce(likes,0) * 5) AS score, " +
            "author_id AS authorId " +
            "FROM portal_article " +
            "WHERE status = 'published' " +
            "ORDER BY score DESC " +
            "LIMIT #{limit}")
    List<Map<String, Object>> selectHotArticlesForRanking(@Param("limit") int limit);
}
