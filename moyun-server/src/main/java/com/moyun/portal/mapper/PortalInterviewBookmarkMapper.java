package com.moyun.portal.mapper;

import java.util.List;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import com.moyun.portal.domain.entity.PortalInterviewBookmark;

/**
 * 题目收藏 Mapper
 *
 * @author moyun
 */
@Mapper
public interface PortalInterviewBookmarkMapper extends BaseMapper<PortalInterviewBookmark>
{
    /**
     * 查询用户是否已收藏某题目
     */
    public PortalInterviewBookmark selectBookmark(@Param("questionId") Long questionId, @Param("userId") Long userId);

    /**
     * 查询用户收藏列表
     */
    public List<PortalInterviewBookmark> selectBookmarkListByUserId(@Param("userId") Long userId);
}
