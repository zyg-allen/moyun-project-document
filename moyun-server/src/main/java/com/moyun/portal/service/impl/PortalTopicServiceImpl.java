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
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.moyun.ext.cms.service.IFeedService;
import com.moyun.common.exception.system.ServiceException;
import com.moyun.portal.domain.entity.PortalTopic;
import com.moyun.portal.domain.entity.PortalTopicLike;
import com.moyun.portal.domain.entity.PortalTopicPost;
import com.moyun.portal.domain.entity.PortalUser;
import com.moyun.portal.domain.vo.TopicListVO;
import com.moyun.portal.domain.vo.TopicVO;
import com.moyun.portal.mapper.PortalTopicCommentMapper;
import com.moyun.portal.mapper.PortalTopicLikeMapper;
import com.moyun.portal.mapper.PortalTopicMapper;
import com.moyun.portal.mapper.PortalTopicPostMapper;
import com.moyun.portal.mapper.PortalUserMapper;
import com.moyun.portal.service.IPortalGrowthService;
import com.moyun.portal.service.IPortalTopicService;
import com.moyun.portal.util.PortalSecurityUtils;

/**
 * 话题 服务实现
 *
 * @author moyun
 */
@Slf4j
@Service
public class PortalTopicServiceImpl extends ServiceImpl<PortalTopicMapper, PortalTopic> implements IPortalTopicService {

    @Autowired
    private PortalUserMapper portalUserMapper;

    @Autowired
    private PortalTopicLikeMapper portalTopicLikeMapper;

    @Autowired
    private IPortalGrowthService portalGrowthService;

    @Autowired
    private IFeedService feedService;

    @Autowired
    private PortalTopicPostMapper portalTopicPostMapper;

    @Autowired
    private PortalTopicCommentMapper portalTopicCommentMapper;

    @Override
    public Page<TopicListVO> getTopicList(Integer pageNum, Integer pageSize, String sort, String keyword) {
        if (pageNum == null || pageNum <= 0) pageNum = 1;
        if (pageSize == null || pageSize <= 0) pageSize = 10;
        Page<PortalTopic> page = new Page<>(pageNum, pageSize);

        LambdaQueryWrapper<PortalTopic> wrapper = new LambdaQueryWrapper<>();
        // 前台列表仅展示未删除话题
        wrapper.eq(PortalTopic::getStatus, "active");
        if (keyword != null && !keyword.isEmpty()) {
            wrapper.like(PortalTopic::getTitle, keyword);
        }
        // 排序：latest=创建时间倒序 / hot=点赞+观点数倒序 / active=最后观点时间倒序
        if ("hot".equals(sort)) {
            wrapper.orderByDesc(PortalTopic::getLikeCount).orderByDesc(PortalTopic::getPostCount);
        } else if ("active".equals(sort)) {
            wrapper.orderByDesc(PortalTopic::getLastPostTime);
        } else {
            wrapper.orderByDesc(PortalTopic::getCreatedTime);
        }
        // 置顶永远排第一
        wrapper.orderByDesc(PortalTopic::getPinned);

        Page<PortalTopic> resultPage = baseMapper.selectPage(page, wrapper);
        return convertToListVOPage(resultPage);
    }

    @Override
    public TopicVO getTopicDetail(Long id, Long currentUserId) {
        PortalTopic topic = baseMapper.selectById(id);
        if (topic == null || "deleted".equals(topic.getStatus())) {
            return null;
        }
        TopicVO vo = new TopicVO();
        BeanUtils.copyProperties(topic, vo);

        // 填充创建者信息
        PortalUser creator = portalUserMapper.selectById(topic.getCreatorId());
        if (creator != null) {
            vo.setCreatorUsername(creator.getUsername());
            vo.setCreatorNickname(creator.getNickname());
            vo.setCreatorAvatar(creator.getAvatar());
        }

        // 当前用户的互动状态
        if (currentUserId != null) {
            vo.setIsOwner(currentUserId.equals(topic.getCreatorId()));
            PortalTopicLike like = portalTopicLikeMapper.selectByTopicAndUser(id, currentUserId);
            vo.setIsLiked(like != null);
        } else {
            vo.setIsOwner(false);
            vo.setIsLiked(false);
        }
        return vo;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public PortalTopic createTopic(PortalTopic topic, Long userId) {
        if (userId == null) {
            throw new ServiceException("请先登录");
        }
        if (topic.getTitle() == null || topic.getTitle().trim().isEmpty()) {
            throw new ServiceException("话题标题不能为空");
        }
        // 校验认证创作者
        PortalUser user = portalUserMapper.selectById(userId);
        if (user == null) {
            throw new ServiceException("用户不存在");
        }
        Integer isCertified = user.getIsCertifiedCreator();
        if (isCertified == null || isCertified != 1) {
            throw new ServiceException("仅认证创作者可发起话题");
        }

        topic.setCreatorId(userId);
        topic.setStatus("active");
        topic.setPinned(0);
        topic.setViewCount(0);
        topic.setPostCount(0);
        topic.setLikeCount(0);
        topic.setCommentCount(0);
        topic.setCreatedTime(LocalDateTime.now());
        baseMapper.insert(topic);

        // 触发成长事件 create_topic
        try {
            portalGrowthService.recordEvent("topic", "create_topic", userId, "topic", topic.getId());
        } catch (Exception e) {
            log.warn("话题创建成长事件触发失败: userId={}, topicId={}, err={}", userId, topic.getId(), e.getMessage());
        }

        // 发布 Feed 事件 create_topic
        try {
            feedService.publishEvent(userId, "create_topic", "topic", topic.getId(),
                    topic.getTitle(), topic.getDescription(), topic.getCover());
        } catch (Exception e) {
            log.warn("话题创建 Feed 事件发布失败: topicId={}, err={}", topic.getId(), e.getMessage());
        }

        return topic;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public PortalTopic updateTopic(Long id, PortalTopic topic, Long userId) {
        if (userId == null) {
            throw new ServiceException("请先登录");
        }
        PortalTopic exist = baseMapper.selectById(id);
        if (exist == null) {
            throw new ServiceException("话题不存在");
        }
        if (!userId.equals(exist.getCreatorId())) {
            throw new ServiceException("仅话题发起人可编辑");
        }
        // 仅允许更新部分字段
        LambdaUpdateWrapper<PortalTopic> updateWrapper = new LambdaUpdateWrapper<>();
        updateWrapper.eq(PortalTopic::getId, id);
        if (topic.getTitle() != null) updateWrapper.set(PortalTopic::getTitle, topic.getTitle());
        if (topic.getDescription() != null) updateWrapper.set(PortalTopic::getDescription, topic.getDescription());
        if (topic.getCover() != null) updateWrapper.set(PortalTopic::getCover, topic.getCover());
        baseMapper.update(null, updateWrapper);
        return baseMapper.selectById(id);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteTopic(Long id, Long userId) {
        PortalTopic exist = baseMapper.selectById(id);
        if (exist == null) {
            throw new ServiceException("话题不存在");
        }
        // 权限校验：creator / 门户 admin / CMS admin（CMS 后台已通过 @PreAuthorize 校验）
        boolean isCreator = userId != null && userId.equals(exist.getCreatorId());
        boolean isPortalAdmin = PortalSecurityUtils.isAdmin();
        boolean isCmsAdmin = isCmsAdminContext();
        if (!isCreator && !isPortalAdmin && !isCmsAdmin) {
            throw new ServiceException("无权删除该话题");
        }
        // 软删：status=deleted
        LambdaUpdateWrapper<PortalTopic> updateWrapper = new LambdaUpdateWrapper<>();
        updateWrapper.eq(PortalTopic::getId, id).set(PortalTopic::getStatus, "deleted");
        baseMapper.update(null, updateWrapper);

        // 级联软删该话题下的所有观点
        portalTopicPostMapper.softDeleteByTopic(id);

        // 级联软删该话题相关的所有评论
        // 1) 评论挂在话题上的（target_type='topic'）
        portalTopicCommentMapper.softDeleteByTopicId(id);
        // 2) 评论挂在该话题下观点上的（target_type='post' AND target_id IN 该话题下的观点 ID）
        LambdaQueryWrapper<PortalTopicPost> postQw = new LambdaQueryWrapper<>();
        postQw.select(PortalTopicPost::getId)
                .eq(PortalTopicPost::getTopicId, id);
        List<Long> postIds = portalTopicPostMapper.selectList(postQw)
                .stream()
                .map(PortalTopicPost::getId)
                .collect(Collectors.toList());
        if (!postIds.isEmpty()) {
            portalTopicCommentMapper.softDeleteByPostIds(postIds);
        }

        // 清理 Feed 事件（同步删除 portal_feed_event 与 portal_feed_inbox 中相关记录）
        try {
            feedService.deleteEvent("topic", id);
        } catch (Exception e) {
            log.warn("话题删除清理 Feed 事件失败: topicId={}, err={}", id, e.getMessage());
        }
    }

    /**
     * 判断当前是否为 CMS 后台管理员上下文（已通过 @PreAuthorize 校验权限）
     *
     * @return true 表示当前为 CMS 后台管理员
     */
    private boolean isCmsAdminContext() {
        try {
            org.springframework.security.core.Authentication auth = PortalSecurityUtils.getAuthentication();
            return auth != null && auth.getPrincipal() instanceof com.moyun.core.base.model.LoginUser;
        } catch (Exception e) {
            return false;
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Map<String, Object> toggleTopicLike(Long id, Long userId) {
        if (userId == null) {
            throw new ServiceException("请先登录");
        }
        PortalTopic topic = baseMapper.selectById(id);
        if (topic == null || "deleted".equals(topic.getStatus())) {
            throw new ServiceException("话题不存在");
        }

        PortalTopicLike exist = portalTopicLikeMapper.selectByTopicAndUser(id, userId);
        Map<String, Object> result = new HashMap<>();
        boolean isLiked;
        int currentLikeCount = topic.getLikeCount() == null ? 0 : topic.getLikeCount();

        if (exist != null) {
            // 已赞 → 取消
            portalTopicLikeMapper.deleteByTopicAndUser(id, userId);
            baseMapper.incrementLikeCount(id, -1);
            currentLikeCount = Math.max(0, currentLikeCount - 1);
            isLiked = false;
        } else {
            // 未赞 → 点赞
            PortalTopicLike like = new PortalTopicLike();
            like.setTopicId(id);
            like.setUserId(userId);
            like.setCreatedTime(LocalDateTime.now());
            try {
                portalTopicLikeMapper.insert(like);
            } catch (DuplicateKeyException e) {
                // 并发场景：另一事务已先插入（uk_topic_user 唯一索引触发）
                // 按"已赞→取消"语义处理，保证幂等
                portalTopicLikeMapper.deleteByTopicAndUser(id, userId);
                baseMapper.incrementLikeCount(id, -1);
                currentLikeCount = Math.max(0, currentLikeCount - 1);
                result.put("isLiked", false);
                result.put("likeCount", currentLikeCount);
                return result;
            }
            baseMapper.incrementLikeCount(id, 1);
            currentLikeCount = currentLikeCount + 1;
            isLiked = true;

            // 为话题发起人记录"话题被点赞"成长事件
            if (topic.getCreatorId() != null && !topic.getCreatorId().equals(userId)) {
                try {
                    portalGrowthService.recordEventWithTarget("topic", "receive_topic_like",
                            topic.getCreatorId(), userId, "topic", id);
                } catch (Exception e) {
                    log.warn("话题点赞成长事件触发失败: topicId={}, userId={}, err={}", id, userId, e.getMessage());
                }
            }
        }
        result.put("isLiked", isLiked);
        result.put("likeCount", currentLikeCount);
        return result;
    }

    @Override
    public void incrementViewCount(Long id) {
        baseMapper.incrementViewCount(id, 1);
    }

    @Override
    public Page<TopicListVO> getCmsTopicList(Integer pageNum, Integer pageSize, String keyword, String status) {
        if (pageNum == null || pageNum <= 0) pageNum = 1;
        if (pageSize == null || pageSize <= 0) pageSize = 10;
        Page<PortalTopic> page = new Page<>(pageNum, pageSize);

        LambdaQueryWrapper<PortalTopic> wrapper = new LambdaQueryWrapper<>();
        if (keyword != null && !keyword.isEmpty()) {
            wrapper.like(PortalTopic::getTitle, keyword);
        }
        if (status != null && !status.isEmpty()) {
            wrapper.eq(PortalTopic::getStatus, status);
        }
        wrapper.orderByDesc(PortalTopic::getPinned).orderByDesc(PortalTopic::getCreatedTime);

        Page<PortalTopic> resultPage = baseMapper.selectPage(page, wrapper);
        return convertToListVOPage(resultPage);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateTopicStatus(Long id, String status) {
        if (!"active".equals(status) && !"archived".equals(status) && !"deleted".equals(status)) {
            throw new ServiceException("状态非法，仅支持 active / archived / deleted");
        }
        LambdaUpdateWrapper<PortalTopic> updateWrapper = new LambdaUpdateWrapper<>();
        updateWrapper.eq(PortalTopic::getId, id).set(PortalTopic::getStatus, status);
        baseMapper.update(null, updateWrapper);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateTopicPinned(Long id, Integer pinned) {
        if (pinned == null || (pinned != 0 && pinned != 1)) {
            throw new ServiceException("pinned 仅支持 0 / 1");
        }
        LambdaUpdateWrapper<PortalTopic> updateWrapper = new LambdaUpdateWrapper<>();
        updateWrapper.eq(PortalTopic::getId, id).set(PortalTopic::getPinned, pinned);
        baseMapper.update(null, updateWrapper);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void featureTopic(Long id) {
        PortalTopic topic = baseMapper.selectById(id);
        if (topic == null) {
            throw new ServiceException("话题不存在");
        }
        if (Integer.valueOf(1).equals(topic.getIsFeatured())) {
            throw new ServiceException("话题已精选，不可重复加精");
        }
        baseMapper.markFeatured(id);
        // 触发 topic_featured 成长事件
        if (topic.getCreatorId() != null) {
            try {
                portalGrowthService.recordEventWithTarget("topic", "topic_featured", topic.getCreatorId(), null, "topic", id);
            } catch (Exception e) {
                log.warn("话题精选成长事件触发失败: topicId={}, err={}", id, e.getMessage());
            }
        }
    }

    @Override
    public Page<TopicListVO> getMyTopics(Integer pageNum, Integer pageSize, Long userId) {
        if (pageNum == null || pageNum <= 0) pageNum = 1;
        if (pageSize == null || pageSize <= 0) pageSize = 10;
        Page<PortalTopic> page = new Page<>(pageNum, pageSize);

        LambdaQueryWrapper<PortalTopic> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(PortalTopic::getCreatorId, userId);
        wrapper.ne(PortalTopic::getStatus, "deleted");
        wrapper.orderByDesc(PortalTopic::getCreatedTime);

        Page<PortalTopic> resultPage = baseMapper.selectPage(page, wrapper);
        return convertToListVOPage(resultPage);
    }

    // ==================== 私有工具方法 ====================

    /**
     * 将实体分页结果转换为 ListVO 分页结果，并批量填充创建者信息
     */
    private Page<TopicListVO> convertToListVOPage(Page<PortalTopic> resultPage) {
        Page<TopicListVO> voPage = new Page<>(resultPage.getCurrent(), resultPage.getSize(), resultPage.getTotal());
        List<PortalTopic> records = resultPage.getRecords();
        if (records == null || records.isEmpty()) {
            voPage.setRecords(new ArrayList<>());
            return voPage;
        }

        // 批量查询创建者
        Set<Long> creatorIds = records.stream()
                .map(PortalTopic::getCreatorId)
                .filter(java.util.Objects::nonNull)
                .collect(Collectors.toSet());
        Map<Long, PortalUser> userMap = new HashMap<>();
        if (!creatorIds.isEmpty()) {
            List<PortalUser> users = portalUserMapper.selectBatchIds(creatorIds);
            for (PortalUser u : users) {
                userMap.put(u.getId(), u);
            }
        }

        List<TopicListVO> voList = new ArrayList<>();
        for (PortalTopic topic : records) {
            TopicListVO vo = new TopicListVO();
            BeanUtils.copyProperties(topic, vo);
            PortalUser creator = userMap.get(topic.getCreatorId());
            if (creator != null) {
                vo.setCreatorNickname(creator.getNickname());
                vo.setCreatorAvatar(creator.getAvatar());
            }
            voList.add(vo);
        }
        voPage.setRecords(voList);
        return voPage;
    }
}
