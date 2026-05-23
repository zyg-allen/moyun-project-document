package com.moyun.community.content.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.moyun.community.content.entity.Collect;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface CollectMapper extends BaseMapper<Collect> {

    @Select("SELECT COUNT(*) > 0 FROM cms_collect WHERE user_id = #{userId} AND article_id = #{articleId} AND status = 1")
    boolean existsCollect(@Param("userId") Long userId, @Param("articleId") Long articleId);

    @Select("UPDATE cms_collect SET status = 0 WHERE user_id = #{userId} AND article_id = #{articleId}")
    void cancelCollect(@Param("userId") Long userId, @Param("articleId") Long articleId);

    @Select("INSERT INTO cms_collect (user_id, article_id, status) VALUES (#{userId}, #{articleId}, 1)")
    void addCollect(@Param("userId") Long userId, @Param("articleId") Long articleId);
}
