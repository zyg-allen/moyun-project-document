package com.moyun.community.content.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.moyun.community.content.entity.CmsArticle;

import java.util.List;

public interface ICmsArticleService extends IService<CmsArticle> {

    List<CmsArticle> selectArticleList(CmsArticle article);

    CmsArticle selectArticleById(Long articleId);

    List<CmsArticle> selectHomeRecommendList();

    List<CmsArticle> selectHotArticleList();

    List<CmsArticle> selectLatestArticleList();

    List<CmsArticle> selectArticleListByCategory(Long categoryId);

    List<CmsArticle> selectMyArticleList(Long authorId);

    List<CmsArticle> selectPendingAuditList();

    boolean incrementViewCount(Long articleId);

    boolean incrementLikeCount(Long articleId);

    boolean incrementCollectCount(Long articleId);

    boolean incrementCommentCount(Long articleId);

    boolean updateArticleStatus(Long articleId, String status);
}