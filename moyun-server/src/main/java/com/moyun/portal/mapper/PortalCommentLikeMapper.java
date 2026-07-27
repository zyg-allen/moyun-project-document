package com.moyun.portal.mapper;

import java.util.List;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.moyun.portal.domain.entity.PortalCommentLike;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

/**
 * 文章评论点赞 Mapper
 *
 * @author moyun
 */
@Mapper
public interface PortalCommentLikeMapper extends BaseMapper<PortalCommentLike>
{
    /**
     * 查询用户是否已点赞某评论
     */
    public PortalCommentLike selectLike(@Param("commentId") Long commentId, @Param("userId") Long userId);

    /**
     * 批量查询当前用户已点赞的评论 ID 集合
     * 用于评论列表加载时一次性填充 isLiked 状态，避免 N+1 查询
     */
    @Select("<script>SELECT comment_id FROM portal_comment_like WHERE user_id = #{userId} AND comment_id IN " +
            "<foreach item='id' collection='commentIds' open='(' separator=',' close=')'>#{id}</foreach></script>")
    List<Long> selectLikedByUser(@Param("userId") Long userId, @Param("commentIds") List<Long> commentIds);
}
