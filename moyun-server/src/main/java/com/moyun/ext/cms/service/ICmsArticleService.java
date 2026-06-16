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
public interface ICmsArticleService
{
    /**
     * 查询文章分页列表
     *
     * @param page 分页参数
     * @param query 查询条件
     * @return 文章分页列表
     */
    Page<CmsArticleVO> selectArticlePage(Page<CmsArticleVO> page, CmsArticleQuery query);

    /**
     * 查询文章列表
     *
     * @param query 查询条件
     * @return 文章列表
     */
    List<PortalArticle> selectArticleList(CmsArticleQuery query);

    /**
     * 查询文章详情
     *
     * @param id 文章ID
     * @return 文章信息（CMS视图对象，含作者信息/分类信息）
     */
    CmsArticleVO selectArticleById(Long id);

    /**
     * 新增文章
     *
     * @param article 文章信息
     * @return 结果
     */
    int insertArticle(PortalArticle article);

    /**
     * 修改文章
     *
     * @param article 文章信息
     * @return 结果
     */
    int updateArticle(PortalArticle article);

    /**
     * 审核文章
     *
     * @param article 文章信息
     * @return 结果
     */
    int auditArticle(PortalArticle article);

    /**
     * 文章上下架
     *
     * @param article 文章信息
     * @return 结果
     */
    int publishArticle(PortalArticle article);

    /**
     * 设置推荐
     *
     * @param article 文章信息
     * @return 结果
     */
    int setFeatured(PortalArticle article);

    /**
     * 设置置顶
     *
     * @param article 文章信息
     * @return 结果
     */
    int setTop(PortalArticle article);

    /**
     * 设置轮播
     *
     * @param article 文章信息
     * @return 结果
     */
    int setCarousel(PortalArticle article);

    /**
     * 批量删除文章
     *
     * @param ids 需要删除的文章ID
     * @return 结果
     */
    int deleteArticleByIds(Long[] ids);
}
