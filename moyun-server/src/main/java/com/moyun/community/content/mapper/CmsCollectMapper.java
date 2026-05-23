package com.moyun.community.content.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.moyun.community.content.entity.CmsCollect;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface CmsCollectMapper extends BaseMapper<CmsCollect> {

    List<CmsCollect> selectCollectListByUser(@Param("userId") Long userId);

    CmsCollect selectByUserAndArticle(@Param("userId") Long userId, @Param("articleId") Long articleId);

    int updateStatus(@Param("id") Long id, @Param("status") Integer status);
}