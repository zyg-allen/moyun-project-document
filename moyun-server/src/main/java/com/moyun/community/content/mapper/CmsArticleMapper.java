package com.moyun.community.content.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.moyun.community.content.entity.CmsArticle;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface CmsArticleMapper extends BaseMapper<CmsArticle> {

    List<CmsArticle> selectArticleList(@Param("article") CmsArticle article);

    CmsArticle selectArticleById(Long articleId);

    List<CmsArticle> selectHomeRecommendList();

    List<CmsArticle> selectHotArticleList();

    List<CmsArticle> selectLatestArticleList();

    List<CmsArticle> selectArticleListByCategory(@Param("categoryId") Long categoryId);

    List<CmsArticle> selectMyArticleList(@Param("authorId") Long authorId);

    List<CmsArticle> selectPendingAuditList();

    int updateArticleStatus(@Param("articleId") Long articleId, @Param("status") String status);

    int incrementViewCount(@Param("articleId") Long articleId);

    int incrementLikeCount(@Param("articleId") Long articleId);

    int incrementCollectCount(@Param("articleId") Long articleId);

    int incrementCommentCount(@Param("articleId") Long articleId);
}
