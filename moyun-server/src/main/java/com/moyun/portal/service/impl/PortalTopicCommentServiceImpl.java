package com.moyun.portal.service.impl;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.moyun.common.exception.system.ServiceException;
import com.moyun.portal.domain.entity.PortalTopic;
import com.moyun.portal.domain.entity.PortalTopicComment;
import com.moyun.portal.domain.entity.PortalTopicCommentLike;
import com.moyun.portal.domain.entity.PortalTopicPost;
import com.moyun.portal.domain.entity.PortalUser;
import com.moyun.portal.domain.vo.TopicCommentVO;
import com.moyun.portal.mapper.PortalTopicCommentLikeMapper;
import com.moyun.portal.mapper.PortalTopicCommentMapper;
import com.moyun.portal.mapper.PortalTopicMapper;
import com.moyun.portal.mapper.PortalTopicPostMapper;
import com.moyun.portal.mapper.PortalUserMapper;
import com.moyun.portal.service.IMentionService;
import com.moyun.portal.service.IPortalGrowthService;
import com.moyun.portal.service.IPortalTopicCommentService;
import com.moyun.portal.util.PortalSecurityUtils;
import com.moyun.system.service.ISensitiveWordService;

/**
 * 话题评论 服务实现
 *
 * <p>多态评论：target_type=topic 评论话题，target_type=post 评论观点。
 * 评论采用两级楼中楼：一级评论 parent_id=0/root_id=0，回复评论 parent_id=父评论ID/root_id=根评论ID。
 * 查询模式参考 PortalCommentServiceImpl：count → 一级分页 → 回复前 10 → 用户批量 → isLiked 批量。</p>
 *
 * @author moyun
 */
@Slf4j
@Service
public class PortalTopicCommentServiceImpl extends ServiceImpl<PortalTopicCommentMapper, PortalTopicComment> implements IPortalTopicCommentService {

    @Autowired
    private PortalUserMapper portalUserMapper;

    @Autowired
    private PortalTopicCommentLikeMapper portalTopicCommentLikeMapper;

    @Autowired
    private PortalTopicMapper portalTopicMapper;

    @Autowired
    private PortalTopicPostMapper portalTopicPostMapper;

    @Autowired
    private IPortalGrowthService portalGrowthService;

    @Autowired
    private IMentionService mentionService;

    @Autowired
    private ISensitiveWordService sensitiveWordService;

    @Override
    public Page<TopicCommentVO> getComments(String targetType, Long targetId, Integer pageNum, Integer pageSize, Long currentUserId) {
        if (pageNum == null || pageNum <= 0) pageNum = 1;
        if (pageSize == null || pageSize <= 0) pageSize = 10;

        Page<TopicCommentVO> voPage = new Page<>(pageNum, pageSize);

        // 1. count 一级评论总数
        long total = baseMapper.countRootComments(targetType, targetId);

        if (total == 0) {
            voPage.setTotal(0);
            voPage.setRecords(new ArrayList<>());
            return voPage;
        }
        voPage.setTotal(total);

        // 2. 分页查询一级评论（按时间倒序）
        int offset = (pageNum - 1) * pageSize;
        List<PortalTopicComment> roots = baseMapper.selectRoots(targetType, targetId, offset, pageSize);
        if (roots.isEmpty()) {
            voPage.setRecords(new ArrayList<>());
            return voPage;
        }

        // 3. 查询这些一级评论的回复（SQL 已用 ROW_NUMBER() 限制每组前 10 条）
        List<Long> rootIds = roots.stream().map(PortalTopicComment::getId).collect(Collectors.toList());
        List<PortalTopicComment> allReplies = baseMapper.selectRepliesByRootIds(rootIds);

        // 按 rootId 分组（SQL 已限制每组前 N 条，无需 Java 截取）
        Map<Long, List<PortalTopicComment>> repliesGroupByRoot = new HashMap<>();
        for (PortalTopicComment reply : allReplies) {
            repliesGroupByRoot.computeIfAbsent(reply.getRootId(), k -> new ArrayList<>()).add(reply);
        }

        // 4. 合并所有评论（一级 + 回复），批量查询用户信息
        List<PortalTopicComment> allComments = new ArrayList<>();
        allComments.addAll(roots);
        allComments.addAll(allReplies);

        Set<Long> userIds = new HashSet<>();
        for (PortalTopicComment c : allComments) {
            if (c.getAuthorId() != null) userIds.add(c.getAuthorId());
            if (c.getReplyTo() != null) userIds.add(c.getReplyTo());
        }
        Map<Long, PortalUser> userMap = new HashMap<>();
        if (!userIds.isEmpty()) {
            List<PortalUser> users = portalUserMapper.selectBatchIds(userIds);
            for (PortalUser u : users) {
                userMap.put(u.getId(), u);
            }
        }

        // 5. 批量查询当前用户的点赞状态
        Set<Long> likedCommentIds = new HashSet<>();
        if (currentUserId != null && !allComments.isEmpty()) {
            List<Long> commentIds = allComments.stream().map(PortalTopicComment::getId).collect(Collectors.toList());
            List<Long> likedList = portalTopicCommentLikeMapper.selectLikedByUser(currentUserId, commentIds);
            likedCommentIds.addAll(likedList);
        }

        // 6. 组装 VO（一级评论 + 回复前 N 条，N 在 SQL 层已限制）
        List<TopicCommentVO> voList = new ArrayList<>();
        for (PortalTopicComment root : roots) {
            TopicCommentVO rootVO = convertToVO(root, userMap, likedCommentIds, currentUserId);
            List<PortalTopicComment> replies = repliesGroupByRoot.get(root.getId());
            if (replies != null) {
                List<TopicCommentVO> replyVOs = new ArrayList<>();
                for (PortalTopicComment reply : replies) {
                    replyVOs.add(convertToVO(reply, userMap, likedCommentIds, currentUserId));
                }
                rootVO.setReplies(replyVOs);
            } else {
                rootVO.setReplies(new ArrayList<>());
            }
            voList.add(rootVO);
        }
        voPage.setRecords(voList);
        return voPage;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public PortalTopicComment createComment(PortalTopicComment comment, Long userId) {
        if (userId == null) {
            throw new ServiceException("请先登录");
        }
        if (comment.getContent() == null || comment.getContent().trim().isEmpty()) {
            throw new ServiceException("评论内容不能为空");
        }
        if (!"topic".equals(comment.getTargetType()) && !"post".equals(comment.getTargetType())) {
            throw new ServiceException("target_type 非法，仅支持 topic / post");
        }
        if (comment.getTargetId() == null) {
            throw new ServiceException("target_id 不能为空");
        }

        // 敏感词检测：评论无审核流，命中即拦截（block）并写入审计日志便于复核
        try {
            List<String> hits = sensitiveWordService.find(comment.getContent());
            if (hits != null && !hits.isEmpty()) {
                sensitiveWordService.detectAndLog(
                        "topic_comment", null, userId, comment.getContent(), "block");
                log.warn("话题评论命中敏感词已拦截：targetType={}, targetId={}, userId={}, hits={}",
                        comment.getTargetType(), comment.getTargetId(), userId, hits);
                throw new ServiceException("评论内容包含违规信息，请修改后重试");
            }
        } catch (ServiceException e) {
            throw e;
        } catch (Exception e) {
            log.warn("话题评论敏感词扫描异常：targetType={}, targetId={}, err={}",
                    comment.getTargetType(), comment.getTargetId(), e.getMessage());
        }

        // 校验目标存在
        Long targetAuthorId = null;
        if ("topic".equals(comment.getTargetType())) {
            PortalTopic topic = portalTopicMapper.selectById(comment.getTargetId());
            if (topic == null || "deleted".equals(topic.getStatus())) {
                throw new ServiceException("话题不存在");
            }
            targetAuthorId = topic.getCreatorId();
        } else {
            PortalTopicPost post = portalTopicPostMapper.selectById(comment.getTargetId());
            if (post == null || (post.getIsDeleted() != null && post.getIsDeleted() == 1)) {
                throw new ServiceException("观点不存在");
            }
            targetAuthorId = post.getUserId();
        }

        // 处理 parent_id / root_id 逻辑（参考 PortalCommentServiceImpl.insertPortalComment）
        if (comment.getParentId() == null || comment.getParentId() == 0) {
            // 一级评论
            comment.setParentId(0L);
            comment.setRootId(0L);
            comment.setReplyTo(null);
            comment.setReplyToContent("");
        } else {
            // 回复评论：查询父评论
            PortalTopicComment parent = baseMapper.selectById(comment.getParentId());
            if (parent == null) {
                throw new ServiceException("父评论不存在");
            }
            // root_id：父是一级评论则用父 ID；父是回复则继承父的 root_id
            if (parent.getParentId() != null && parent.getParentId() == 0) {
                comment.setRootId(parent.getId());
            } else {
                comment.setRootId(parent.getRootId());
            }
            // 设置被回复用户ID
            comment.setReplyTo(parent.getAuthorId());
            // 冗余被回复内容摘要（前 50 字）
            String replyContent = parent.getContent();
            if (replyContent != null && replyContent.length() > 50) {
                replyContent = replyContent.substring(0, 50) + "...";
            }
            comment.setReplyToContent(replyContent == null ? "" : replyContent);
        }

        comment.setAuthorId(userId);
        comment.setLikeCount(0);
        comment.setReplyCount(0);
        comment.setIsDeleted(0);
        comment.setCreatedTime(LocalDateTime.now());
        baseMapper.insert(comment);

        // 解析评论内容中的 @username 并向被提及用户发送通知（与 PortalCommentServiceImpl 保持一致）
        if (comment.getContent() != null) {
            try {
                mentionService.parseAndNotify(comment.getContent(), userId, "topic_comment", comment.getId());
            } catch (Exception e) {
                // @提及通知失败不影响评论主流程
                log.warn("话题评论 @提及通知失败: commentId={}, err={}", comment.getId(), e.getMessage());
            }
        }

        // 同步维护 target.comment_count（仅一级评论计入）和 root.reply_count
        if (comment.getParentId() != null && comment.getParentId() == 0) {
            // 一级评论：目标评论数 +1
            if ("topic".equals(comment.getTargetType())) {
                portalTopicMapper.incrementCommentCount(comment.getTargetId(), 1);
            } else {
                portalTopicPostMapper.incrementCommentCount(comment.getTargetId(), 1);
            }
            // 为目标作者记录"被评论"成长事件
            if (targetAuthorId != null && !targetAuthorId.equals(userId)) {
                String action = "topic".equals(comment.getTargetType()) ? "receive_topic_comment" : "receive_post_comment";
                try {
                    portalGrowthService.recordEventWithTarget("topic", action,
                            targetAuthorId, userId, comment.getTargetType(), comment.getTargetId());
                } catch (Exception e) {
                    log.warn("评论被评论成长事件触发失败: targetType={}, targetId={}, err={}",
                            comment.getTargetType(), comment.getTargetId(), e.getMessage());
                }
            }
        } else {
            // 回复评论：根评论的 reply_count +1
            if (comment.getRootId() != null && comment.getRootId() > 0) {
                baseMapper.incrementReplyCount(comment.getRootId(), 1);
            }
        }

        return comment;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteComment(Long commentId, Long userId) {
        if (userId == null) {
            throw new ServiceException("请先登录");
        }
        PortalTopicComment comment = baseMapper.selectById(commentId);
        if (comment == null || (comment.getIsDeleted() != null && comment.getIsDeleted() == 1)) {
            throw new ServiceException("评论不存在");
        }
        boolean isAdmin = PortalSecurityUtils.isAdmin();
        boolean isAuthor = userId.equals(comment.getAuthorId());
        // 话题发起人：评论挂在 topic 上时，校验当前用户是否为该话题的 creator
        boolean isTopicCreator = false;
        // 观点作者：评论挂在 post 上时，校验当前用户是否为该观点的作者
        boolean isPostAuthor = false;
        if ("topic".equals(comment.getTargetType())) {
            PortalTopic topic = portalTopicMapper.selectById(comment.getTargetId());
            if (topic != null && userId.equals(topic.getCreatorId())) {
                isTopicCreator = true;
            }
        } else if ("post".equals(comment.getTargetType())) {
            PortalTopicPost post = portalTopicPostMapper.selectById(comment.getTargetId());
            if (post != null && userId.equals(post.getUserId())) {
                isPostAuthor = true;
            }
        }
        if (!isAuthor && !isTopicCreator && !isPostAuthor && !isAdmin) {
            throw new ServiceException("无权删除该评论");
        }

        // 软删单条
        baseMapper.softDelete(commentId);

        if (comment.getParentId() != null && comment.getParentId() == 0) {
            // 删除的是一级评论：级联软删所有回复
            baseMapper.softDeleteByRoot(commentId);
            // 目标 comment_count -1
            if ("topic".equals(comment.getTargetType())) {
                portalTopicMapper.incrementCommentCount(comment.getTargetId(), -1);
            } else {
                portalTopicPostMapper.incrementCommentCount(comment.getTargetId(), -1);
            }
        } else {
            // 删除的是回复：根评论的 reply_count -1
            if (comment.getRootId() != null && comment.getRootId() > 0) {
                baseMapper.decrementReplyCount(comment.getRootId(), 1);
            }
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Map<String, Object> toggleCommentLike(Long commentId, Long userId) {
        if (userId == null) {
            throw new ServiceException("请先登录");
        }
        PortalTopicComment comment = baseMapper.selectById(commentId);
        if (comment == null || (comment.getIsDeleted() != null && comment.getIsDeleted() == 1)) {
            throw new ServiceException("评论不存在");
        }

        PortalTopicCommentLike exist = portalTopicCommentLikeMapper.selectByCommentAndUser(commentId, userId);
        Map<String, Object> result = new HashMap<>();
        boolean isLiked;
        int currentLikeCount = comment.getLikeCount() == null ? 0 : comment.getLikeCount();

        if (exist != null) {
            portalTopicCommentLikeMapper.deleteByCommentAndUser(commentId, userId);
            baseMapper.incrementLikeCount(commentId, -1);
            currentLikeCount = Math.max(0, currentLikeCount - 1);
            isLiked = false;
        } else {
            PortalTopicCommentLike like = new PortalTopicCommentLike();
            like.setCommentId(commentId);
            like.setUserId(userId);
            like.setCreatedTime(LocalDateTime.now());
            try {
                portalTopicCommentLikeMapper.insert(like);
            } catch (DuplicateKeyException e) {
                // 并发冲突，按"已赞→取消"语义处理，保证幂等
                portalTopicCommentLikeMapper.deleteByCommentAndUser(commentId, userId);
                baseMapper.incrementLikeCount(commentId, -1);
                currentLikeCount = Math.max(0, currentLikeCount - 1);
                result.put("isLiked", false);
                result.put("likeCount", currentLikeCount);
                return result;
            }
            baseMapper.incrementLikeCount(commentId, 1);
            currentLikeCount = currentLikeCount + 1;
            isLiked = true;

            // 为评论作者记录"评论被点赞"成长事件
            if (comment.getAuthorId() != null && !comment.getAuthorId().equals(userId)) {
                try {
                    portalGrowthService.recordEventWithTarget("topic", "receive_comment_like",
                            comment.getAuthorId(), userId, "topic_comment", commentId);
                } catch (Exception e) {
                    log.warn("评论点赞成长事件触发失败: commentId={}, userId={}, err={}", commentId, userId, e.getMessage());
                }
            }
        }
        result.put("isLiked", isLiked);
        result.put("likeCount", currentLikeCount);
        return result;
    }

    @Override
    public Page<TopicCommentVO> getCmsCommentList(Integer pageNum, Integer pageSize, String targetType, Long targetId) {
        if (pageNum == null || pageNum <= 0) pageNum = 1;
        if (pageSize == null || pageSize <= 0) pageSize = 10;
        Page<PortalTopicComment> page = new Page<>(pageNum, pageSize);

        LambdaQueryWrapper<PortalTopicComment> wrapper = new LambdaQueryWrapper<>();
        if (targetType != null && !targetType.isEmpty()) {
            wrapper.eq(PortalTopicComment::getTargetType, targetType);
        }
        if (targetId != null) {
            wrapper.eq(PortalTopicComment::getTargetId, targetId);
        }
        wrapper.orderByDesc(PortalTopicComment::getCreatedTime);

        Page<PortalTopicComment> resultPage = baseMapper.selectPage(page, wrapper);

        // CMS 视角扁平展示所有评论（不嵌套 replies），传 null userId 不查 isLiked
        Page<TopicCommentVO> voPage = new Page<>(resultPage.getCurrent(), resultPage.getSize(), resultPage.getTotal());
        List<PortalTopicComment> records = resultPage.getRecords();
        if (records == null || records.isEmpty()) {
            voPage.setRecords(new ArrayList<>());
            return voPage;
        }
        // 批量查询作者
        Set<Long> userIds = records.stream()
                .map(PortalTopicComment::getAuthorId)
                .filter(java.util.Objects::nonNull)
                .collect(Collectors.toSet());
        records.stream()
                .map(PortalTopicComment::getReplyTo)
                .filter(java.util.Objects::nonNull)
                .forEach(userIds::add);
        Map<Long, PortalUser> userMap = new HashMap<>();
        if (!userIds.isEmpty()) {
            List<PortalUser> users = portalUserMapper.selectBatchIds(userIds);
            for (PortalUser u : users) {
                userMap.put(u.getId(), u);
            }
        }

        List<TopicCommentVO> voList = new ArrayList<>();
        for (PortalTopicComment c : records) {
            TopicCommentVO vo = convertToVO(c, userMap, null, null);
            voList.add(vo);
        }
        voPage.setRecords(voList);
        return voPage;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void cmsDeleteComment(Long commentId) {
        PortalTopicComment comment = baseMapper.selectById(commentId);
        if (comment == null) {
            throw new ServiceException("评论不存在");
        }
        if (comment.getIsDeleted() != null && comment.getIsDeleted() == 1) {
            return;
        }
        baseMapper.softDelete(commentId);
        if (comment.getParentId() != null && comment.getParentId() == 0) {
            baseMapper.softDeleteByRoot(commentId);
            if ("topic".equals(comment.getTargetType())) {
                portalTopicMapper.incrementCommentCount(comment.getTargetId(), -1);
            } else {
                portalTopicPostMapper.incrementCommentCount(comment.getTargetId(), -1);
            }
        } else {
            if (comment.getRootId() != null && comment.getRootId() > 0) {
                baseMapper.decrementReplyCount(comment.getRootId(), 1);
            }
        }
    }

    // ==================== 私有工具方法 ====================

    private TopicCommentVO convertToVO(PortalTopicComment comment, Map<Long, PortalUser> userMap,
                                       Set<Long> likedCommentIds, Long currentUserId) {
        TopicCommentVO vo = new TopicCommentVO();
        BeanUtils.copyProperties(comment, vo);

        PortalUser author = userMap.get(comment.getAuthorId());
        if (author != null) {
            vo.setAuthorUsername(author.getUsername());
            vo.setAuthorNickname(author.getNickname());
            vo.setAuthorAvatar(author.getAvatar());
        }
        if (comment.getReplyTo() != null && comment.getReplyTo() > 0) {
            PortalUser replyToUser = userMap.get(comment.getReplyTo());
            if (replyToUser != null) {
                vo.setReplyToUsername(replyToUser.getUsername());
                vo.setReplyToNickname(replyToUser.getNickname());
            }
        }
        if (likedCommentIds != null && currentUserId != null) {
            vo.setIsLiked(likedCommentIds.contains(comment.getId()));
            vo.setIsOwner(currentUserId.equals(comment.getAuthorId()));
        } else {
            vo.setIsLiked(false);
            vo.setIsOwner(false);
        }
        return vo;
    }
}
