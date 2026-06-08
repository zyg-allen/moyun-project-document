package com.moyun.portal.service;

/**
 * 文章浏览数据同步服务
 * 用于维护 portal_article.views 和 portal_article_view 表的数据一致性
 */
public interface IPortalArticleViewService {

    /**
     * 修复单篇文章的阅读量（从 portal_article_view 重新统计）
     * @param articleId 文章ID
     * @return 修复后的阅读量
     */
    long repairArticleViews(Long articleId);

    /**
     * 修复所有文章的阅读量（批量操作）
     * @return 修复的文章数量
     */
    long repairAllArticleViews();

    /**
     * 校验文章阅读量是否一致
     * @param articleId 文章ID
     * @return true=一致, false=不一致
     */
    boolean checkArticleViewsConsistency(Long articleId);

    /**
     * 获取文章的真实阅读量（从 portal_article_view 表统计）
     * @param articleId 文章ID
     * @return 真实阅读量
     */
    int getRealArticleViews(Long articleId);
}
