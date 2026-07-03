package com.moyun.ext.cms.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.moyun.common.exception.system.ServiceException;
import com.moyun.core.base.page.PageDomain;
import com.moyun.ext.cms.domain.query.TopicQuery;
import com.moyun.ext.cms.domain.vo.TopicListItemVO;
import com.moyun.ext.cms.domain.vo.TopicPostVO;
import com.moyun.ext.cms.domain.vo.TopicVO;
import com.moyun.ext.cms.service.ITopicService;
import com.moyun.portal.domain.entity.PortalTopic;
import com.moyun.portal.domain.entity.PortalTopicFollow;
import com.moyun.portal.mapper.PortalTopicFollowMapper;
import com.moyun.portal.mapper.PortalTopicMapper;
import com.moyun.util.bean.PageUtils;
import com.moyun.util.string.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.regex.Pattern;

/**
 * 话题/超话 Service 实现（社交深化与商业化 4.2）
 *
 * @author moyun
 */
@Service
public class TopicServiceImpl implements ITopicService {

    /** slug 合法校验：小写字母、数字、短横线 */
    private static final Pattern SLUG_PATTERN = Pattern.compile("^[a-z0-9]+(?:-[a-z0-9]+)*$");

    @Autowired private PortalTopicMapper topicMapper;
    @Autowired private PortalTopicFollowMapper topicFollowMapper;

    // ========================================================================
    // 列表 / 详情
    // ========================================================================
    @Override
    public Page<TopicListItemVO> listTopics(TopicQuery query) {
        Page<TopicListItemVO> page = PageUtils.buildPage(query);
        return topicMapper.selectListPage(page, query);
    }

    @Override
    public Page<TopicListItemVO> hotTopics(PageDomain query) {
        Page<TopicListItemVO> page = PageUtils.buildPage(query);
        return topicMapper.selectHotPage(page);
    }

    @Override
    public TopicVO getTopicDetail(String slug, Long currentUserId) {
        TopicVO vo = topicMapper.selectDetailBySlug(slug);
        if (vo == null) {
            return null;
        }
        if (currentUserId != null) {
            PortalTopicFollow follow = topicFollowMapper.selectByTopicAndUser(vo.getId(), currentUserId);
            vo.setIsFollowed(follow != null);
        } else {
            vo.setIsFollowed(false);
        }
        return vo;
    }

    @Override
    public Page<TopicPostVO> listTopicPosts(String slug, PageDomain query) {
        TopicVO vo = topicMapper.selectDetailBySlug(slug);
        if (vo == null) {
            throw new ServiceException("话题不存在");
        }
        Page<TopicPostVO> page = PageUtils.buildPage(query);
        return topicMapper.selectTopicPosts(page, vo.getName());
    }

    // ========================================================================
    // 关注 / 取消关注
    // ========================================================================
    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean toggleFollow(Long topicId, Long userId) {
        if (userId == null) {
            throw new ServiceException("请登录后操作");
        }
        PortalTopic topic = topicMapper.selectById(topicId);
        if (topic == null) {
            throw new ServiceException("话题不存在");
        }
        PortalTopicFollow existing = topicFollowMapper.selectByTopicAndUser(topicId, userId);
        if (existing != null) {
            topicFollowMapper.deleteById(existing.getId());
            topicMapper.updateFollowCount(topicId, -1);
            return false;
        }
        PortalTopicFollow follow = new PortalTopicFollow();
        follow.setTopicId(topicId);
        follow.setUserId(userId);
        follow.setCreatedTime(LocalDateTime.now());
        try {
            topicFollowMapper.insert(follow);
        } catch (DuplicateKeyException e) {
            // 并发兜底：已关注，按已关注处理
            topicMapper.updateFollowCount(topicId, 1);
            return true;
        }
        topicMapper.updateFollowCount(topicId, 1);
        return true;
    }

    // ========================================================================
    // 后台管理
    // ========================================================================
    @Override
    public Page<TopicListItemVO> cmsListTopics(TopicQuery query) {
        Page<TopicListItemVO> page = PageUtils.buildPage(query);
        return topicMapper.selectCmsListPage(page, query);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long cmsAddTopic(PortalTopic topic) {
        if (topic == null || StringUtils.isEmpty(topic.getName())) {
            throw new ServiceException("话题名称不能为空");
        }
        normalizeSlug(topic);
        topic.setPostCount(0);
        topic.setFollowCount(0);
        topic.setStatus(StringUtils.isNotEmpty(topic.getStatus()) ? topic.getStatus() : "active");
        topic.setCreatedTime(LocalDateTime.now());
        topicMapper.insert(topic);
        return topic.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public int cmsUpdateTopic(PortalTopic topic) {
        if (topic == null || topic.getId() == null) {
            throw new ServiceException("话题ID不能为空");
        }
        PortalTopic existing = topicMapper.selectById(topic.getId());
        if (existing == null) {
            throw new ServiceException("话题不存在");
        }
        if (StringUtils.isNotEmpty(topic.getName())) {
            existing.setName(topic.getName());
        }
        if (StringUtils.isNotEmpty(topic.getSlug())) {
            normalizeSlug(topic);
            existing.setSlug(topic.getSlug());
        }
        existing.setDescription(topic.getDescription());
        existing.setCover(topic.getCover());
        if (topic.getStatus() != null) {
            existing.setStatus(topic.getStatus());
        }
        return topicMapper.updateById(existing);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public int cmsDeleteTopic(Long id) {
        LambdaQueryWrapper<PortalTopicFollow> followQw = Wrappers.<PortalTopicFollow>lambdaQuery()
                .eq(PortalTopicFollow::getTopicId, id);
        topicFollowMapper.delete(followQw);
        return topicMapper.deleteById(id);
    }

    // ========================================================================
    // 内部工具
    // ========================================================================

    /**
     * 生成并校验 slug：若未提供则按名称生成。
     * 纯 ASCII slug 需匹配 [a-z0-9-]；含中文等多字节字符的 slug 不做严格校验（URL 编码后可访问）。
     */
    private void normalizeSlug(PortalTopic topic) {
        String slug = topic.getSlug();
        if (StringUtils.isEmpty(slug)) {
            slug = generateSlug(topic.getName());
            topic.setSlug(slug);
        }
        slug = slug.trim().toLowerCase().replaceAll("\\s+", "-").replaceAll("-+", "-");
        // 去除首尾短横线
        while (slug.startsWith("-")) {
            slug = slug.substring(1);
        }
        while (slug.endsWith("-")) {
            slug = slug.substring(0, slug.length() - 1);
        }
        if (StringUtils.isEmpty(slug)) {
            throw new ServiceException("slug 不能为空");
        }
        topic.setSlug(slug);
        // 仅对纯 ASCII slug 做严格格式校验
        if (slug.matches("^[\\x00-\\x7F]+$") && !SLUG_PATTERN.matcher(slug).matches()) {
            throw new ServiceException("slug 只能包含小写字母、数字和短横线，且不能以短横线开头/结尾");
        }
    }

    /**
     * 根据名称生成 slug（中文保留，空格/特殊字符转短横线）。
     */
    private String generateSlug(String name) {
        if (StringUtils.isEmpty(name)) {
            return "topic";
        }
        return name.trim().toLowerCase().replaceAll("[\\s/\\?#]+", "-")
                .replaceAll("-+", "-");
    }
}
