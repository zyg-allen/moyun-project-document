package com.moyun.ext.cms.service.impl;

import java.time.LocalDateTime;
import java.util.List;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.moyun.core.base.page.PageDomain;
import com.moyun.ext.cms.domain.vo.FeedEventVO;
import com.moyun.ext.cms.service.IFeedService;
import com.moyun.portal.domain.entity.PortalArticle;
import com.moyun.portal.domain.entity.PortalFeedEvent;
import com.moyun.portal.domain.entity.PortalUser;
import com.moyun.portal.mapper.PortalArticleMapper;
import com.moyun.portal.mapper.PortalFeedEventMapper;
import com.moyun.portal.mapper.PortalFeedInboxMapper;
import com.moyun.portal.mapper.PortalFollowMapper;
import com.moyun.portal.mapper.PortalUserMapper;
import com.moyun.util.bean.PageUtils;

/**
 * 动态/Feed 流 Service 实现（推拉结合，先实现"读时拉"模式）
 *
 * <p>设计说明：</p>
 * <ul>
 *   <li>getFollowingFeed：读时拉模式，JOIN portal_follow（follower_id = 当前用户）。</li>
 *   <li>publishEvent：写入事件后，粉丝数 &lt; 1000 时同步批量写入关注者收件箱（推模式），
 *       作为后续切换收件箱读模式的优化储备。</li>
 *   <li>deleteEvent：删除事件时同步清理收件箱。</li>
 * </ul>
 *
 * @author moyun
 */
@Slf4j
@Service
public class FeedServiceImpl implements IFeedService {

    /** 粉丝数阈值：小于此值时同步推送至收件箱 */
    private static final long SYNC_PUSH_THRESHOLD = 1000L;

    @Autowired
    private PortalFeedEventMapper feedEventMapper;

    @Autowired
    private PortalFeedInboxMapper feedInboxMapper;

    @Autowired
    private PortalFollowMapper followMapper;

    @Autowired
    private PortalUserMapper portalUserMapper;

    @Autowired
    private PortalArticleMapper portalArticleMapper;

    /**
     * 关注的人的动态（分页，读时拉模式）
     */
    @Override
    public Page<FeedEventVO> getFollowingFeed(Long userId, PageDomain query) {
        Page<FeedEventVO> page = PageUtils.buildPage(query);
        return feedEventMapper.selectFollowingEvents(page, userId);
    }

    /**
     * 全站热门动态（最近 7 天，分页）
     */
    @Override
    public Page<FeedEventVO> getHotFeed(PageDomain query) {
        Page<FeedEventVO> page = PageUtils.buildPage(query);
        return feedEventMapper.selectHotEvents(page);
    }

    /**
     * 发布动态事件：写入事件 + 同步推送至粉丝收件箱
     */
    @Override
    public Long publishEvent(Long userId, String eventType, String targetType, Long targetId,
                             String title, String summary, String cover) {
        if (userId == null || eventType == null || targetType == null || targetId == null) {
            log.warn("[Feed] publishEvent 参数缺失：userId={}, eventType={}, targetType={}, targetId={}",
                    userId, eventType, targetType, targetId);
            return null;
        }

        // 1. 写入动态事件
        PortalFeedEvent event = new PortalFeedEvent();
        event.setUserId(userId);
        event.setEventType(eventType);
        event.setTargetType(targetType);
        event.setTargetId(targetId);
        event.setTitle(title);
        event.setSummary(summary);
        event.setCover(cover);
        event.setCreatedTime(LocalDateTime.now());
        int rows = feedEventMapper.insertFeedEvent(event);
        if (rows <= 0 || event.getId() == null) {
            log.warn("[Feed] 动态事件写入失败：userId={}, targetType={}, targetId={}", userId, targetType, targetId);
            return null;
        }

        // 2. 同步推送给粉丝收件箱（粉丝数 < 阈值时同步写）
        try {
            long followerCount = followMapper.countFollowers(userId);
            if (followerCount < SYNC_PUSH_THRESHOLD) {
                List<Long> followerIds = followMapper.selectFollowers(userId);
                if (followerIds != null && !followerIds.isEmpty()) {
                    feedInboxMapper.batchInsert(event.getId(), followerIds, event.getCreatedTime());
                }
            } else {
                // 粉丝量较大时跳过同步推送，后续可切换为异步任务 / 仅读时拉模式
                log.info("[Feed] 用户 {} 粉丝数 {} 超过同步阈值 {}，跳过收件箱同步推送", userId, followerCount, SYNC_PUSH_THRESHOLD);
            }
        } catch (Exception e) {
            // 收件箱推送失败不影响事件本身（读时拉模式仍可正常读取）
            log.error("[Feed] 推送粉丝收件箱失败：eventId={}, userId={}", event.getId(), userId, e);
        }

        return event.getId();
    }

    /**
     * 删除事件（如文章被删除）：同步清理收件箱
     */
    @Override
    public int deleteEvent(String targetType, Long targetId) {
        if (targetType == null || targetId == null) {
            return 0;
        }
        // 先查询关联事件，清理对应收件箱
        List<PortalFeedEvent> events = feedEventMapper.selectByTarget(targetType, targetId);
        if (events != null) {
            for (PortalFeedEvent event : events) {
                try {
                    feedInboxMapper.deleteByEventId(event.getId());
                } catch (Exception e) {
                    log.error("[Feed] 清理收件箱失败：eventId={}", event.getId(), e);
                }
            }
        }
        return feedEventMapper.deleteByTarget(targetType, targetId);
    }
}
