package com.moyun.community.content.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.moyun.community.content.entity.Article;
import com.moyun.community.content.model.dto.ArticleDetailDto;
import com.moyun.community.content.model.dto.ArticlePublishDto;
import com.moyun.community.content.model.vo.ArticleListVo;
import com.moyun.community.content.model.vo.HomeVo;

import java.util.List;

public interface ArticleService extends IService<Article> {

    HomeVo getHomeData();

    List<ArticleListVo> getLatestArticles(int limit);

    List<ArticleListVo> getHotArticles(int limit);

    List<ArticleListVo> getRecommendArticles(int limit);

    List<ArticleListVo> getArticlesByCategory(Long categoryId, int limit);

    List<ArticleListVo> getArticlesByType(String articleType, int limit);

    ArticleDetailDto getArticleDetail(Long articleId, Long userId);

    Article saveArticle(ArticlePublishDto dto, Long userId);

    Article updateArticle(ArticlePublishDto dto, Long userId);

    Article submitForReview(Long articleId, Long userId);

    List<ArticleListVo> getMyArticles(Long userId);

    List<ArticleListVo> getPendingArticles();

    boolean approveArticle(Long articleId, Long auditorId, String reason);

    boolean rejectArticle(Long articleId, Long auditorId, String reason);

    boolean deleteArticle(Long articleId, Long userId);

    void incrementViewCount(Long articleId);

    boolean toggleLike(Long articleId, Long userId);

    boolean toggleCollect(Long articleId, Long userId);
}
