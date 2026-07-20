package com.moyun.portal.service;

import java.util.Map;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;

import com.moyun.portal.domain.entity.PortalTopicComment;
import com.moyun.portal.domain.vo.TopicCommentVO;

/**
 * 话题评论 服务层
 *
 * @author moyun
 */
public interface IPortalTopicCommentService extends IService<PortalTopicComment> {

    /**
     * 评论分页（两级楼中楼）
     *
     * @param targetType     目标类型：topic / post
     * @param targetId       目标ID
     * @param pageNum        页码
     * @param pageSize       每页数量
     * @param currentUserId  当前登录用户ID（可为 null）
     * @return 分页结果（含一级评论 + 回复前 10 条 + isLiked 批量）
     */
    Page<TopicCommentVO> getComments(String targetType, Long targetId, Integer pageNum, Integer pageSize, Long currentUserId);

    /**
     * 创建评论（参考 PortalCommentServiceImpl 的 parent_id/root_id 逻辑）
     * - 一级评论：parent_id=0, root_id=0
     * - 回复评论：parent_id=父评论ID, root_id=根评论ID
     * 同步维护 target.comment_count 和 root.reply_count
     * 触发成长事件 receive_topic_comment 或 receive_post_comment
     *
     * @param comment 评论对象（targetType/targetId/content/parentId 由前端传入）
     * @param userId  当前登录用户ID
     * @return 创建后的评论对象（含 id）
     */
    PortalTopicComment createComment(PortalTopicComment comment, Long userId);

    /**
     * 软删评论（级联软删回复，同步 reply_count 和 target.comment_count）
     *
     * @param commentId 评论ID
     * @param userId    当前登录用户ID
     */
    void deleteComment(Long commentId, Long userId);

    /**
     * 评论点赞（触发成长事件 receive_comment_like）
     *
     * @param commentId 评论ID
     * @param userId    当前登录用户ID
     * @return 含 isLiked 和 likeCount
     */
    Map<String, Object> toggleCommentLike(Long commentId, Long userId);

    /**
     * CMS 分页查询评论（所有目标的所有评论）
     *
     * @param pageNum     页码
     * @param pageSize    每页数量
     * @param targetType  目标类型筛选（可选）
     * @param targetId    目标ID筛选（可选）
     * @return 分页结果
     */
    Page<TopicCommentVO> getCmsCommentList(Integer pageNum, Integer pageSize, String targetType, Long targetId);

    /**
     * CMS 删除评论（软删）
     *
     * @param commentId 评论ID
     */
    void cmsDeleteComment(Long commentId);
}
