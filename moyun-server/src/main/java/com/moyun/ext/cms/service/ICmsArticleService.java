package com.moyun.ext.cms.service;

import java.util.List;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.moyun.ext.cms.domain.query.CmsArticleQuery;
import com.moyun.ext.cms.domain.vo.CmsArticleVO;
import com.moyun.portal.domain.entity.PortalArticle;

/**
 * CMS文章服务接口
 *
 * @author moyun
 */
public interface ICmsArticleService {

    /**
     * 查询文章分页列表（CMS专用）
     *
     * @param page  分页对象（泛型为 CmsArticleVO）
     * @param query 查询条件
     * @return 文章分页结果（VO）
     */
    Page<CmsArticleVO> selectArticlePage(Page<CmsArticleVO> page, CmsArticleQuery query);

    /**
     * 查询文章列表（不分页）
     *
     * @param query 查询条件
     * @return 文章列表（Entity）
     */
    List<PortalArticle> selectArticleList(CmsArticleQuery query);

    /**
     * 查询文章详情（CMS专用，含作者/分类信息）
     *
     * @param id 文章ID
     * @return 文章详情VO
     */
    CmsArticleVO selectArticleById(Long id);

    /**
     * 新增文章
     */
    int insertArticle(PortalArticle article);

    /**
     * 修改文章
     */
    int updateArticle(PortalArticle article);

    /**
     * 审核文章
     */
    int auditArticle(PortalArticle article);

    /**
     * 文章上下架
     */
    int publishArticle(PortalArticle article);

    /**
     * 设置推荐
     */
    int setFeatured(PortalArticle article);

    /**
     * 设置置顶
     */
    int setTop(PortalArticle article);

    /**
     * 设置轮播
     */
    int setCarousel(PortalArticle article);

    /**
     * 批量删除文章
     */
    int deleteArticleByIds(Long[] ids);
}