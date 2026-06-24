package com.moyun.portal.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.moyun.portal.domain.entity.PortalHelpArticle;
import com.moyun.portal.domain.entity.PortalHelpCategory;

import java.util.List;

/**
 * 帮助中心（前台）Service 接口
 *
 * @author moyun
 */
public interface IPortalHelpService {

    /**
     * 查询所有启用的分类（前台展示）
     */
    List<PortalHelpCategory> selectActiveCategoryList();

    /**
     * 根据分类ID查询文章列表
     */
    List<PortalHelpArticle> selectArticlesByCategory(Long categoryId);

    /**
     * 查询精选文章
     */
    List<PortalHelpArticle> selectFeaturedArticles(Integer limit);

    /**
     * 搜索帮助文章
     */
    List<PortalHelpArticle> searchArticles(String keyword);

    /**
     * 查询文章详情（同时增加查看次数）
     */
    PortalHelpArticle selectArticleDetail(Long id);
}
