package com.moyun.portal.mapper;

import java.util.List;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import com.moyun.portal.domain.entity.PortalInterviewComment;

/**
 * 面经评论 Mapper
 *
 * @author moyun
 */
@Mapper
public interface PortalInterviewCommentMapper extends BaseMapper<PortalInterviewComment>
{
    /**
     * 根据面经ID查询一级评论列表
     */
    public List<PortalInterviewComment> selectCommentsByExperienceId(@Param("experienceId") Long experienceId);

    /**
     * 根据父评论ID查询子评论
     */
    public List<PortalInterviewComment> selectCommentsByParentId(@Param("parentId") Long parentId);

    /**
     * 根据面经ID统计评论数
     */
    public long countByExperienceId(@Param("experienceId") Long experienceId);
}
