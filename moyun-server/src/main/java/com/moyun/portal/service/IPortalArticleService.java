package com.moyun.portal.service;

import java.util.List;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;

import com.moyun.portal.domain.entity.PortalArticle;
import com.moyun.portal.domain.query.ArticleQuery;

/**
 * 门户文章 业务层
 *
 * @author moyun
 */
public interface IPortalArticleService extends IService<PortalArticle> {

    /**
     * 根据条件分页查询文章列表
     *
     * @param page 分页参数
     * @param portalArticle 文章信息
     * @return 分页结果
     */
    Page<PortalArticle> selectPortalArticlePage(Page<PortalArticle> page, ArticleQuery portalArticle);

    /**
     * 查询"我的文章"分页列表（按 authorId 过滤，不强制 status=published）
     * 用于作者查看自己所有状态的文章（草稿/待审核/已发布/已拒绝）
     *
     * @param page 分页参数
     * @param query 查询条件（authorId 必填，status 可选）
     * @return 分页结果
     */
    Page<PortalArticle> selectMyArticlesPage(Page<PortalArticle> page, ArticleQuery query);

    /**
     * 根据条件查询文章列表（不分页，用于导出等场景）
     *
     * @param portalArticle 文章信息
     * @return 文章信息集合
     */
    List<PortalArticle> selectPortalArticleList(ArticleQuery portalArticle);

    /**
     * 通过文章ID查询文章
     *
     * @param id 文章ID
     * @return 文章对象信息
     */
    PortalArticle selectPortalArticleById(Long id);

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

    /**
     * 前台发布文章（带自动发布逻辑）
     * 发布后状态为 pending（待审核），审核通过后变更为 published
     *
     * @param portalArticle 文章信息
     * @return 结果
     */
    int publishArticle(PortalArticle portalArticle);

    /**
     * 前台保存草稿（真实入库，返回包含 id 的实体）
     * 新建草稿：status = draft
     * 更新草稿：保持 status = draft
     *
     * @param portalArticle 文章信息（id 为空时新建，非空时更新）
     * @return 入库后的文章实体（含 id、createTime、updateTime）
     */
    PortalArticle saveDraft(PortalArticle portalArticle);
}
