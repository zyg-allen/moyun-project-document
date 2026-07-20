package com.moyun.portal.service;

import java.util.Map;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;

import com.moyun.portal.domain.entity.PortalTopicPost;
import com.moyun.portal.domain.vo.TopicPostVO;

/**
 * 话题观点 服务层
 *
 * @author moyun
 */
public interface IPortalTopicPostService extends IService<PortalTopicPost> {

    /**
     * 观点分页（按楼层正序）
     *
     * @param topicId        话题ID
     * @param pageNum        页码
     * @param pageSize       每页数量
     * @param currentUserId  当前登录用户ID（可为 null）
     * @return 分页结果
     */
    Page<TopicPostVO> getPostsByTopic(Long topicId, Integer pageNum, Integer pageSize, Long currentUserId);

    /**
     * 发表观点（SELECT FOR UPDATE 锁话题行获取楼层号，触发成长事件 post_opinion）
     *
     * @param topicId  话题ID
     * @param post     观点对象（content/images 由前端传入）
     * @param userId   当前登录用户ID
     * @return 创建后的观点对象（含 id、floor）
     */
    PortalTopicPost createPost(Long topicId, PortalTopicPost post, Long userId);

    /**
     * 删除观点（作者/话题发起人/admin，软删，同步减少 topic.post_count）
     *
     * @param postId  观点ID
     * @param userId  当前登录用户ID
     */
    void deletePost(Long postId, Long userId);

    /**
     * 观点点赞（幂等，触发成长事件 receive_post_like）
     *
     * @param postId  观点ID
     * @param userId  当前登录用户ID
     * @return 含 isLiked 和 likeCount
     */
    Map<String, Object> togglePostLike(Long postId, Long userId);

    /**
     * 当前用户发表的观点分页
     *
     * @param pageNum  页码
     * @param pageSize 每页数量
     * @param userId   当前登录用户ID
     * @return 分页结果
     */
    Page<TopicPostVO> getMyPosts(Integer pageNum, Integer pageSize, Long userId);

    /**
     * CMS 分页查询观点（所有话题的所有观点）
     *
     * @param pageNum  页码
     * @param pageSize 每页数量
     * @param topicId  话题ID筛选（可选）
     * @return 分页结果
     */
    Page<TopicPostVO> getCmsPostList(Integer pageNum, Integer pageSize, Long topicId);

    /**
     * CMS 删除观点（软删）
     *
     * @param postId 观点ID
     */
    void cmsDeletePost(Long postId);
}
