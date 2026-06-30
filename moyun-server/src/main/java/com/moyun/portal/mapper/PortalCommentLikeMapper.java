package com.moyun.portal.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.moyun.portal.domain.entity.PortalCommentLike;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

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
}
