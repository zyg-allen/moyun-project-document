package com.moyun.portal.mapper;

import java.util.List;
import java.util.Map;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

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
     * 根据文章ID列表批量查询已发布文章（用于"我的收藏"等场景，JOIN 作者与分类）
     * 仅返回 status='published' 的文章，避免展示草稿/已删除文章
     *
     * @param ids 文章ID列表
     * @return 文章列表
     */
    List<PortalArticle> selectArticlesByIds(@Param("ids") List<Long> ids);

    /**
     * 我购买的文章（付费阅读）分页查询
     * JOIN portal_tip_order 取 target_type='article_paid' AND status='paid' 的记录
     *
     * @param page   分页参数
     * @param userId 当前登录用户ID
     * @return 文章分页列表
     */
    Page<PortalArticle> selectPurchasedArticlesPage(Page<PortalArticle> page, @Param("userId") Long userId);

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

    int incrementViews(@Param("id") Long id, @Param("delta") long delta);

    /**
     * 原子增加点赞数
     *
     * @param id    文章ID
     * @param delta 增量（正数增加，负数减少）
     * @return 受影响行数
     */

    int incrementLikes(@Param("id") Long id, @Param("delta") long delta);

    /**
     * 原子增加评论数
     *
     * @param id    文章ID
     * @param delta 增量（正数增加，负数减少）
     * @return 受影响行数
     */

    int incrementComments(@Param("id") Long id, @Param("delta") long delta);

    /**
     * 原子增加收藏数
     *
     * @param id    文章ID
     * @param delta 增量（正数增加，负数减少）
     * @return 受影响行数
     */

    int incrementBookmarkCount(@Param("id") Long id, @Param("delta") long delta);

    /**
     * 按作者聚合文章统计（从文章表实时聚合，作为统计展示的真实数据源）
     * 仅统计已发布文章
     *
     * @param authorId 作者用户ID
     * @return Map 包含 articleCount / viewSum / likeSum / bookmarkSum / commentSum
     */

    Map<String, Object> selectAuthorArticleStats(@Param("authorId") Long authorId);

    /**
     * 批量按作者聚合文章统计（避免 N+1 查询，作者列表页等场景使用）
     * 仅统计已发布文章
     *
     * @param authorIds 作者用户ID集合
     * @return 每个作者一行，字段：authorId / articleCount / viewSum / likeSum / bookmarkSum / commentSum
     */

    List<Map<String, Object>> batchSelectAuthorArticleStats(@Param("authorIds") List<Long> authorIds);

    // ========== 运营首页聚合统计方法 ==========

    /**
     * 文章核心指标统计（全站总量）
     * @return Map 包含 totalArticles/publishedArticles/pendingArticles/draftArticles/totalViews/totalLikes/totalComments
     */

    Map<String, Object> selectArticleMetrics();

    /**
     * 按日期范围统计每日新增文章数（趋势图）
     * 使用 DATE_FORMAT 返回纯字符串，避免 java.sql.Date 序列化格式不一致导致日期 key 匹配失败
     */

    List<Map<String, Object>> selectDailyPublishTrend(@Param("startTime") java.time.LocalDateTime startTime);

    /**
     * 栏目排行榜：按文章数和浏览量聚合 Top N（仅统计文章类栏目 category_type='article'）
     */

    List<Map<String, Object>> selectCategoryRanking(@Param("limit") int limit);

    /**
     * 查询待审核文章列表（运营首页待办任务）
     */

    List<Map<String, Object>> selectPendingArticles(@Param("limit") int limit);

    /**
     * 统计待审核文章数量
     */

    long countPendingArticles();

    /**
     * 统计今日新增文章数（按 create_time >= startTime 过滤，含所有状态）
     * 用于首页"今日新增文章"卡片，口径与卡片名称一致
     */

    long countTodayNewArticles(@Param("startTime") java.time.LocalDateTime startTime);

    /**
     * 热门文章 Top N（按浏览量+点赞数加权排序，用于 Redis ZSet 初始化）
     * JOIN portal_user 获取作者名，避免前端 author 字段为空
     */

    List<Map<String, Object>> selectHotArticlesForRanking(@Param("limit") int limit);
}
