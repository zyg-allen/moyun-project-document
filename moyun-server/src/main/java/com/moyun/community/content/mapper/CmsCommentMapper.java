package com.moyun.community.content.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.moyun.community.content.entity.CmsComment;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface CmsCommentMapper extends BaseMapper<CmsComment> {

    List<CmsComment> selectCommentListByArticle(@Param("articleId") Long articleId);

    List<CmsComment> selectCommentListByUser(@Param("userId") Long userId);

    int incrementLikeCount(@Param("commentId") Long commentId);
}