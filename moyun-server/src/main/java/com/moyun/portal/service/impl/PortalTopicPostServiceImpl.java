package com.moyun.portal.service.impl;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.moyun.common.exception.system.ServiceException;
import com.moyun.portal.domain.entity.PortalTopic;
import com.moyun.portal.domain.entity.PortalTopicPost;
import com.moyun.portal.domain.entity.PortalTopicPostLike;
import com.moyun.portal.domain.entity.PortalUser;
import com.moyun.portal.domain.vo.TopicPostVO;
import com.moyun.portal.mapper.PortalTopicCommentMapper;
import com.moyun.portal.mapper.PortalTopicMapper;
import com.moyun.portal.mapper.PortalTopicPostLikeMapper;
import com.moyun.portal.mapper.PortalTopicPostMapper;
import com.moyun.portal.mapper.PortalUserMapper;
import com.moyun.portal.service.IPortalGrowthService;
import com.moyun.portal.service.IPortalTopicPostService;
import com.moyun.portal.util.PortalSecurityUtils;
import com.moyun.system.service.ISensitiveWordService;

/**
 * 话题观点 服务实现
 *
 * @author moyun
 */
@Slf4j
@Service
public class PortalTopicPostServiceImpl extends ServiceImpl<PortalTopicPostMapper, PortalTopicPost> implements IPortalTopicPostService {

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    @Autowired
    private PortalTopicMapper portalTopicMapper;

    @Autowired
    private PortalTopicPostLikeMapper portalTopicPostLikeMapper;

    @Autowired
    private PortalUserMapper portalUserMapper;

    @Autowired
    private IPortalGrowthService portalGrowthService;

    @Autowired
    private PortalTopicCommentMapper portalTopicCommentMapper;

    @Autowired
    private ISensitiveWordService sensitiveWordService;

    @Override
    public Page<TopicPostVO> getPostsByTopic(Long topicId, Integer pageNum, Integer pageSize, Long currentUserId) {
        if (pageNum == null || pageNum <= 0) pageNum = 1;
        if (pageSize == null || pageSize <= 0) pageSize = 10;
        Page<PortalTopicPost> page = new Page<>(pageNum, pageSize);

        LambdaQueryWrapper<PortalTopicPost> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(PortalTopicPost::getTopicId, topicId);
        wrapper.eq(PortalTopicPost::getIsDeleted, 0);
        // 仅查一级观点（parent_post_id 为 NULL），楼中楼通过评论接口单独加载
        wrapper.isNull(PortalTopicPost::getParentPostId);
        wrapper.orderByAsc(PortalTopicPost::getFloor);

        Page<PortalTopicPost> resultPage = baseMapper.selectPage(page, wrapper);
        return convertToPostVOPage(resultPage, currentUserId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public PortalTopicPost createPost(Long topicId, PortalTopicPost post, Long userId) {
        if (userId == null) {
            throw new ServiceException("请先登录");
        }
        if (post.getContent() == null || post.getContent().trim().isEmpty()) {
            throw new ServiceException("观点内容不能为空");
        }
        // SELECT FOR UPDATE 锁话题行，保证楼层号并发安全
        PortalTopic topic = portalTopicMapper.selectForUpdate(topicId);
        if (topic == null || "deleted".equals(topic.getStatus())) {
            throw new ServiceException("话题不存在");
        }
        if ("archived".equals(topic.getStatus())) {
            throw new ServiceException("话题已归档，无法发表观点");
        }

        // 敏感词检测：观点无审核流，命中即拦截（block）并写入审计日志便于复核。
        // 拦截场景下观点不入库，日志 biz_id 留空，由 content 片段定位。
        try {
            List<String> hits = sensitiveWordService.find(post.getContent());
            if (hits != null && !hits.isEmpty()) {
                sensitiveWordService.detectAndLog(
                        "topic_post", null, userId, post.getContent(), "block");
                log.warn("观点命中敏感词已拦截：topicId={}, userId={}, hits={}", topicId, userId, hits);
                throw new ServiceException("观点内容包含违规信息，请修改后重试");
            }
        } catch (ServiceException e) {
            throw e;
        } catch (Exception e) {
            // 敏感词扫描异常不阻断主流程，仅记录日志
            log.warn("观点敏感词扫描异常：topicId={}, err={}", topicId, e.getMessage());
        }

        // 获取下一楼层号（基于 MAX(floor)+1，配合行锁保证唯一）
        Integer nextFloor = baseMapper.selectNextFloor(topicId);
        if (nextFloor == null) nextFloor = 1;

        post.setTopicId(topicId);
        post.setUserId(userId);
        post.setFloor(nextFloor);
        post.setLikeCount(0);
        post.setCommentCount(0);
        post.setIsDeleted(0);
        post.setCreatedTime(LocalDateTime.now());
        // images 字段：前端可能传 List<String>，但实体上是 String（JSON）。这里由 Controller 已序列化好。
        baseMapper.insert(post);

        // 同步话题观点数 + 最后观点时间/用户
        portalTopicMapper.incrementPostCount(topicId, 1, post.getCreatedTime(), userId);

        // 触发成长事件 post_opinion
        try {
            portalGrowthService.recordEvent("topic", "post_opinion", userId, "post", post.getId());
        } catch (Exception e) {
            log.warn("观点发表成长事件触发失败: userId={}, postId={}, err={}", userId, post.getId(), e.getMessage());
        }

        return post;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deletePost(Long postId, Long userId) {
        if (userId == null) {
            throw new ServiceException("请先登录");
        }
        PortalTopicPost post = baseMapper.selectById(postId);
        if (post == null || (post.getIsDeleted() != null && post.getIsDeleted() == 1)) {
            throw new ServiceException("观点不存在");
        }
        // 权限：作者 / 话题发起人 / admin
        boolean isAdmin = PortalSecurityUtils.isAdmin();
        boolean isAuthor = userId.equals(post.getUserId());
        boolean isTopicCreator = false;
        PortalTopic topic = portalTopicMapper.selectById(post.getTopicId());
        if (topic != null && userId.equals(topic.getCreatorId())) {
            isTopicCreator = true;
        }
        if (!isAuthor && !isTopicCreator && !isAdmin) {
            throw new ServiceException("无权删除该观点");
        }

        // 软删
        baseMapper.softDelete(postId);
        // 级联软删该观点下的所有评论（target_type='post' AND target_id=postId）
        portalTopicCommentMapper.softDeleteByPostId(postId);
        // 同步减少话题观点数（仅减计数，不清空 last_post_time / last_poster_id）
        portalTopicMapper.decrementPostCount(post.getTopicId());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Map<String, Object> togglePostLike(Long postId, Long userId) {
        if (userId == null) {
            throw new ServiceException("请先登录");
        }
        PortalTopicPost post = baseMapper.selectById(postId);
        if (post == null || (post.getIsDeleted() != null && post.getIsDeleted() == 1)) {
            throw new ServiceException("观点不存在");
        }

        PortalTopicPostLike exist = portalTopicPostLikeMapper.selectByPostAndUser(postId, userId);
        Map<String, Object> result = new HashMap<>();
        boolean isLiked;
        int currentLikeCount = post.getLikeCount() == null ? 0 : post.getLikeCount();

        if (exist != null) {
            // 已赞 → 取消
            portalTopicPostLikeMapper.deleteByPostAndUser(postId, userId);
            baseMapper.incrementLikeCount(postId, -1);
            currentLikeCount = Math.max(0, currentLikeCount - 1);
            isLiked = false;
        } else {
            // 未赞 → 点赞
            PortalTopicPostLike like = new PortalTopicPostLike();
            like.setPostId(postId);
            like.setUserId(userId);
            like.setCreatedTime(LocalDateTime.now());
            try {
                portalTopicPostLikeMapper.insert(like);
            } catch (DuplicateKeyException e) {
                // 并发冲突，按"已赞→取消"语义处理，保证幂等
                portalTopicPostLikeMapper.deleteByPostAndUser(postId, userId);
                baseMapper.incrementLikeCount(postId, -1);
                currentLikeCount = Math.max(0, currentLikeCount - 1);
                result.put("isLiked", false);
                result.put("likeCount", currentLikeCount);
                return result;
            }
            baseMapper.incrementLikeCount(postId, 1);
            currentLikeCount = currentLikeCount + 1;
            isLiked = true;

            // 为观点作者记录"观点被点赞"成长事件
            if (post.getUserId() != null && !post.getUserId().equals(userId)) {
                try {
                    portalGrowthService.recordEventWithTarget("topic", "receive_post_like",
                            post.getUserId(), userId, "post", postId);
                } catch (Exception e) {
                    log.warn("观点点赞成长事件触发失败: postId={}, userId={}, err={}", postId, userId, e.getMessage());
                }
            }
        }
        result.put("isLiked", isLiked);
        result.put("likeCount", currentLikeCount);
        return result;
    }

    @Override
    public Page<TopicPostVO> getMyPosts(Integer pageNum, Integer pageSize, Long userId) {
        if (pageNum == null || pageNum <= 0) pageNum = 1;
        if (pageSize == null || pageSize <= 0) pageSize = 10;
        Page<PortalTopicPost> page = new Page<>(pageNum, pageSize);

        LambdaQueryWrapper<PortalTopicPost> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(PortalTopicPost::getUserId, userId);
        wrapper.eq(PortalTopicPost::getIsDeleted, 0);
        wrapper.orderByDesc(PortalTopicPost::getCreatedTime);

        Page<PortalTopicPost> resultPage = baseMapper.selectPage(page, wrapper);
        return convertToPostVOPage(resultPage, userId);
    }

    @Override
    public Page<TopicPostVO> getCmsPostList(Integer pageNum, Integer pageSize, Long topicId) {
        if (pageNum == null || pageNum <= 0) pageNum = 1;
        if (pageSize == null || pageSize <= 0) pageSize = 10;
        Page<PortalTopicPost> page = new Page<>(pageNum, pageSize);

        LambdaQueryWrapper<PortalTopicPost> wrapper = new LambdaQueryWrapper<>();
        if (topicId != null) {
            wrapper.eq(PortalTopicPost::getTopicId, topicId);
        }
        wrapper.orderByDesc(PortalTopicPost::getCreatedTime);

        Page<PortalTopicPost> resultPage = baseMapper.selectPage(page, wrapper);
        // CMS 视角不需要 isLiked，传 null
        return convertToPostVOPage(resultPage, null);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void cmsDeletePost(Long postId) {
        PortalTopicPost post = baseMapper.selectById(postId);
        if (post == null) {
            throw new ServiceException("观点不存在");
        }
        if (post.getIsDeleted() != null && post.getIsDeleted() == 1) {
            return;
        }
        baseMapper.softDelete(postId);
        // 级联软删该观点下的所有评论
        portalTopicCommentMapper.softDeleteByPostId(postId);
        // 同步减少话题观点数（仅减计数，不清空 last_post_time / last_poster_id）
        portalTopicMapper.decrementPostCount(post.getTopicId());
    }

    // ==================== 私有工具方法 ====================

    private Page<TopicPostVO> convertToPostVOPage(Page<PortalTopicPost> resultPage, Long currentUserId) {
        Page<TopicPostVO> voPage = new Page<>(resultPage.getCurrent(), resultPage.getSize(), resultPage.getTotal());
        List<PortalTopicPost> records = resultPage.getRecords();
        if (records == null || records.isEmpty()) {
            voPage.setRecords(new ArrayList<>());
            return voPage;
        }

        // 批量查询发布者
        Set<Long> userIds = records.stream()
                .map(PortalTopicPost::getUserId)
                .filter(java.util.Objects::nonNull)
                .collect(Collectors.toSet());
        // 也包含被回复用户
        records.stream()
                .map(PortalTopicPost::getReplyToUserId)
                .filter(java.util.Objects::nonNull)
                .forEach(userIds::add);

        Map<Long, PortalUser> userMap = new HashMap<>();
        if (!userIds.isEmpty()) {
            List<PortalUser> users = portalUserMapper.selectBatchIds(userIds);
            for (PortalUser u : users) {
                userMap.put(u.getId(), u);
            }
        }

        // 批量查询当前用户的点赞状态
        Set<Long> likedPostIds = new java.util.HashSet<>();
        if (currentUserId != null && !records.isEmpty()) {
            List<Long> postIds = records.stream().map(PortalTopicPost::getId).collect(Collectors.toList());
            List<Long> likedList = portalTopicPostLikeMapper.selectLikedByUser(currentUserId, postIds);
            likedPostIds.addAll(likedList);
        }

        List<TopicPostVO> voList = new ArrayList<>();
        for (PortalTopicPost post : records) {
            TopicPostVO vo = new TopicPostVO();
            BeanUtils.copyProperties(post, vo);
            // images JSON → List<String>
            vo.setImages(parseImages(post.getImages()));

            PortalUser author = userMap.get(post.getUserId());
            if (author != null) {
                vo.setUsername(author.getUsername());
                vo.setNickname(author.getNickname());
                vo.setAvatar(author.getAvatar());
            }
            if (post.getReplyToUserId() != null) {
                PortalUser replyToUser = userMap.get(post.getReplyToUserId());
                if (replyToUser != null) {
                    vo.setReplyToNickname(replyToUser.getNickname());
                }
            }
            if (currentUserId != null) {
                vo.setIsLiked(likedPostIds.contains(post.getId()));
                vo.setIsOwner(currentUserId.equals(post.getUserId()));
            } else {
                vo.setIsLiked(false);
                vo.setIsOwner(false);
            }
            voList.add(vo);
        }
        voPage.setRecords(voList);
        return voPage;
    }

    /**
     * 解析 images JSON 字符串为 List<String>。容错：失败返回 null。
     */
    private List<String> parseImages(String images) {
        if (images == null || images.isEmpty()) {
            return null;
        }
        try {
            return OBJECT_MAPPER.readValue(images, new TypeReference<List<String>>() {});
        } catch (Exception e) {
            log.warn("解析 images JSON 失败: {}", images, e);
            return null;
        }
    }
}
