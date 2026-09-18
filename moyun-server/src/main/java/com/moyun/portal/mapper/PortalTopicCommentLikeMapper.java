package com.moyun.portal.mapper;

import java.util.List;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import com.moyun.portal.domain.entity.PortalTopicCommentLike;

/**
 * 话题评论点赞 数据层
 *
 * @author moyun
 */
@Mapper
public interface PortalTopicCommentLikeMapper extends BaseMapper<PortalTopicCommentLike> {

    /**
     * 查询用户是否已点赞某评论
     */

    PortalTopicCommentLike selectByCommentAndUser(@Param("commentId") Long commentId, @Param("userId") Long userId);

    /**
     * 删除点赞记录（取消点赞）
     */

    int deleteByCommentAndUser(@Param("commentId") Long commentId, @Param("userId") Long userId);

    /**
     * 批量查询当前用户已点赞的评论 ID 集合
     */

    List<Long> selectLikedByUser(@Param("userId") Long userId, @Param("commentIds") List<Long> commentIds);
}
