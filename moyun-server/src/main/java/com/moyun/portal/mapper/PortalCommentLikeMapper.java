package com.moyun.portal.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.moyun.portal.domain.PortalCommentLike;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

/**
 * 门户评论点赞表 数据层
 *
 * @author moyun
 */
@Mapper
public interface PortalCommentLikeMapper extends BaseMapper<PortalCommentLike> {

    /**
     * 根据条件分页查询评论点赞列表
     *
     * @param portalCommentLike 评论点赞信息
     * @return 评论点赞信息集合信息
     */
    public List<PortalCommentLike> selectPortalCommentLikeList(PortalCommentLike portalCommentLike);

    /**
     * 通过用户ID和评论ID查询评论点赞
     *
     * @param userId    用户ID
     * @param commentId 评论ID
     * @return 评论点赞对象信息
     */
    public PortalCommentLike selectPortalCommentLikeById(Long userId, Long commentId);

    /**
     * 新增评论点赞信息
     *
     * @param portalCommentLike 评论点赞信息
     * @return 结果
     */
    public int insertPortalCommentLike(PortalCommentLike portalCommentLike);

    /**
     * 通过用户ID和评论ID删除评论点赞
     *
     * @param userId    用户ID
     * @param commentId 评论ID
     * @return 结果
     */
    public int deletePortalCommentLikeById(Long userId, Long commentId);

    /**
     * 批量删除评论点赞信息
     *
     * @param ids 需要删除的评论点赞ID
     * @return 结果
     */
    public int deletePortalCommentLikeByIds(Long[] ids);
}
