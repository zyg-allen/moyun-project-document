package com.moyun.community.content.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.moyun.community.content.entity.Article;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.util.List;

@Mapper
public interface ArticleMapper extends BaseMapper<Article> {

    @Select("SELECT * FROM cms_article WHERE status = 'published' AND is_deleted = 0 ORDER BY is_top DESC, publish_time DESC LIMIT #{limit}")
    List<Article> selectLatestPublished(@Param("limit") int limit);

    @Select("SELECT * FROM cms_article WHERE status = 'published' AND is_deleted = 0 ORDER BY view_count DESC, like_count DESC LIMIT #{limit}")
    List<Article> selectHotPublished(@Param("limit") int limit);

    @Select("SELECT * FROM cms_article WHERE status = 'published' AND is_deleted = 0 AND is_recommend = 1 ORDER BY publish_time DESC LIMIT #{limit}")
    List<Article> selectRecommendPublished(@Param("limit") int limit);

    @Select("SELECT * FROM cms_article WHERE author_id = #{authorId} AND is_deleted = 0 ORDER BY create_time DESC")
    List<Article> selectByAuthorId(@Param("authorId") Long authorId);

    @Select("SELECT * FROM cms_article WHERE category_id = #{categoryId} AND status = 'published' AND is_deleted = 0 ORDER BY publish_time DESC LIMIT #{limit}")
    List<Article> selectByCategoryId(@Param("categoryId") Long categoryId, @Param("limit") int limit);

    @Select("SELECT * FROM cms_article WHERE article_type = #{articleType} AND status = 'published' AND is_deleted = 0 ORDER BY publish_time DESC LIMIT #{limit}")
    List<Article> selectByArticleType(@Param("articleType") String articleType, @Param("limit") int limit);

    @Select("SELECT * FROM cms_article WHERE status = #{status} AND is_deleted = 0 ORDER BY create_time DESC")
    List<Article> selectByStatus(@Param("status") String status);

    @Update("UPDATE cms_article SET view_count = view_count + 1 WHERE article_id = #{articleId}")
    void incrementViewCount(@Param("articleId") Long articleId);

    @Update("UPDATE cms_article SET like_count = like_count + 1 WHERE article_id = #{articleId}")
    void incrementLikeCount(@Param("articleId") Long articleId);

    @Update("UPDATE cms_article SET like_count = CASE WHEN like_count > 0 THEN like_count - 1 ELSE 0 END WHERE article_id = #{articleId}")
    void decrementLikeCount(@Param("articleId") Long articleId);

    @Update("UPDATE cms_article SET collect_count = collect_count + 1 WHERE article_id = #{articleId}")
    void incrementCollectCount(@Param("articleId") Long articleId);

    @Update("UPDATE cms_article SET collect_count = CASE WHEN collect_count > 0 THEN collect_count - 1 ELSE 0 END WHERE article_id = #{articleId}")
    void decrementCollectCount(@Param("articleId") Long articleId);
}
