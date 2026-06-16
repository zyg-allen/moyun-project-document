package com.moyun.portal.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.moyun.portal.domain.entity.PortalInterviewCommentLike;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

/**
 * 面经评论点赞 Mapper
 *
 * @author moyun
 */
@Mapper
public interface PortalInterviewCommentLikeMapper extends BaseMapper<PortalInterviewCommentLike>
{
    /**
     * 查询用户是否已点赞某评论
     */
    public PortalInterviewCommentLike selectLike(@Param("commentId") Long commentId, @Param("userId") Long userId);
}
