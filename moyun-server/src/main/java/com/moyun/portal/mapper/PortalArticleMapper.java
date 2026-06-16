package com.moyun.portal.mapper;

import java.util.List;

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
}
