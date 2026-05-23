package com.moyun.community.content.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.moyun.community.content.entity.Tag;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface TagMapper extends BaseMapper<Tag> {

    @Select("SELECT t.* FROM cms_tag t INNER JOIN cms_article_tag at ON t.tag_id = at.tag_id WHERE at.article_id = #{articleId}")
    List<Tag> selectTagsByArticleId(@Param("articleId") Long articleId);

    @Select("INSERT INTO cms_article_tag (article_id, tag_id) VALUES (#{articleId}, #{tagId})")
    void insertArticleTag(@Param("articleId") Long articleId, @Param("tagId") Long tagId);

    default void insertArticleTags(Long articleId, List<Long> tagIds) {
        for (Long tagId : tagIds) {
            insertArticleTag(articleId, tagId);
        }
    }

    @Select("DELETE FROM cms_article_tag WHERE article_id = #{articleId}")
    void deleteArticleTags(@Param("articleId") Long articleId);

    @Select("SELECT * FROM cms_tag WHERE status = '0' ORDER BY article_count DESC")
    List<Tag> selectPopularTags();
}
