package com.moyun.ext.cms.service.impl;

import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.Map;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.moyun.ext.cms.service.IMessageService;
import com.moyun.ext.cms.service.IUserDashboardService;
import com.moyun.portal.domain.entity.PortalBookshelf;
import com.moyun.portal.domain.entity.PortalBookmark;
import com.moyun.portal.domain.entity.PortalColumn;
import com.moyun.portal.domain.entity.PortalFollow;
import com.moyun.portal.domain.entity.PortalInterviewExperience;
import com.moyun.portal.domain.entity.PortalInterviewSubmission;
import com.moyun.portal.domain.entity.PortalArticle;
import com.moyun.portal.domain.entity.PortalUserStats;
import com.moyun.portal.domain.vo.UserGrowthVO;
import com.moyun.portal.mapper.PortalArticleMapper;
import com.moyun.portal.mapper.PortalBookshelfMapper;
import com.moyun.portal.mapper.PortalBookmarkMapper;
import com.moyun.portal.mapper.PortalColumnMapper;
import com.moyun.portal.mapper.PortalFollowMapper;
import com.moyun.portal.mapper.PortalInterviewExperienceMapper;
import com.moyun.portal.mapper.PortalInterviewSubmissionMapper;
import com.moyun.portal.mapper.PortalUserResumeMapper;
import com.moyun.portal.mapper.PortalUserStatsMapper;
import com.moyun.portal.service.IPortalGrowthService;
import com.moyun.system.service.ISysNotificationService;

/**
 * 个人中心聚合 Dashboard 服务实现
 *
 * <p>聚合各模块统计数字。每个模块均使用单条 COUNT SQL，避免 N+1。
 * 成长值/等级/头衔复用 {@link IPortalGrowthService#getUserGrowth(Long)}，
 * 其等级映射基于 PortalGrowthServiceImpl 的 LEVEL_THRESHOLDS。</p>
 *
 * @author moyun
 */
@Slf4j
@Service
public class UserDashboardServiceImpl implements IUserDashboardService {

    @Autowired
    private PortalArticleMapper articleMapper;

    @Autowired
    private PortalBookmarkMapper bookmarkMapper;

    @Autowired
    private PortalBookshelfMapper bookshelfMapper;

    @Autowired
    private PortalInterviewSubmissionMapper interviewSubmissionMapper;

    @Autowired
    private PortalInterviewExperienceMapper interviewExperienceMapper;

    @Autowired
    private PortalUserResumeMapper userResumeMapper;

    @Autowired
    private PortalFollowMapper followMapper;

    @Autowired
    private PortalColumnMapper columnMapper;

    @Autowired
    private PortalUserStatsMapper userStatsMapper;

    @Autowired
    private IPortalGrowthService portalGrowthService;

    @Autowired
    private IMessageService messageService;

    @Autowired
    private ISysNotificationService sysNotificationService;

    @Override
    public Map<String, Object> getUserDashboard(Long userId) {
        // TODO: 加缓存（@Cacheable 或 Redis），dashboard 为高频接口，可按 userId 缓存并随相关写操作失效

        Map<String, Object> data = new LinkedHashMap<>(16);

        // ===== 内容模块（单条 COUNT SQL，status='published'）=====
        long articleCount = articleMapper.selectCount(
                new QueryWrapper<PortalArticle>()
                        .eq("author_id", userId)
                        .eq("status", "published"));
        data.put("articleCount", (int) articleCount);

        // ===== 收藏（portal_bookmark，按 user_id）=====
        long bookmarkCount = bookmarkMapper.selectCount(
                new QueryWrapper<PortalBookmark>().eq("user_id", userId));
        data.put("bookmarkCount", (int) bookmarkCount);

        // ===== 书架（portal_bookshelf，按 user_id）=====
        long bookshelfCount = bookshelfMapper.selectCount(
                new QueryWrapper<PortalBookshelf>().eq("user_id", userId));
        data.put("bookshelfCount", (int) bookshelfCount);

        // ===== 面试答题数（portal_interview_submission，按 user_id）=====
        long questionAttemptCount = interviewSubmissionMapper.selectCount(
                new QueryWrapper<PortalInterviewSubmission>().eq("user_id", userId));
        data.put("questionAttemptCount", (int) questionAttemptCount);

        // ===== 面经数（portal_interview_experience，按 user_id）=====
        long experienceCount = interviewExperienceMapper.selectCount(
                new QueryWrapper<PortalInterviewExperience>().eq("user_id", userId));
        data.put("experienceCount", (int) experienceCount);

        // ===== 简历数（portal_user_resume，status <> 'archived'，复用现有 countByUserId）=====
        data.put("resumeCount", userResumeMapper.countByUserId(userId));

        // ===== 关注 / 粉丝（portal_follow）=====
        // followingCount：当前用户关注了多少人（follower_id = userId）
        long followingCount = followMapper.selectCount(
                new QueryWrapper<PortalFollow>().eq("follower_id", userId));
        data.put("followingCount", (int) followingCount);
        // followerCount：当前用户有多少粉丝（following_id = userId），复用现有 countFollowers
        data.put("followerCount", (int) followMapper.countFollowers(userId));

        // ===== 专栏数（portal_column，status='published'）=====
        long columnCount = columnMapper.selectCount(
                new QueryWrapper<PortalColumn>()
                        .eq("user_id", userId)
                        .eq("status", "published"));
        data.put("columnCount", (int) columnCount);

        // ===== 错题本（表未建，预留）=====
        data.put("wrongQuestionCount", 0);

        // ===== 未读通知（sys_notification_read，user_type='portal'，复用 ISysNotificationService）=====
        data.put("unreadNotificationCount", sysNotificationService.countUnread(userId, "portal"));

        // ===== 未读私信（portal_message_session，复用 IMessageService.getUnreadCount）=====
        Integer unreadMessage = messageService.getUnreadCount(userId);
        data.put("unreadMessageCount", unreadMessage == null ? 0 : unreadMessage);

        // ===== 成长体系（portal_user_growth，复用 IPortalGrowthService.getUserGrowth，含 LEVEL_THRESHOLDS 等级映射）=====
        UserGrowthVO growth = portalGrowthService.getUserGrowth(userId);
        data.put("growthValue", growth != null && growth.getGrowthValue() != null ? growth.getGrowthValue() : 0);
        data.put("level", growth != null && growth.getLevel() != null ? growth.getLevel() : 1);
        data.put("levelTitle", growth != null && growth.getTitle() != null ? growth.getTitle() : "初出茅庐");

        // ===== 签到（portal_user_stats：checkinStreak + lastCheckinDate → todayCheckin，单次查询）=====
        PortalUserStats stats = userStatsMapper.selectByUserId(userId);
        int checkinStreak = (stats != null && stats.getCheckinStreak() != null) ? stats.getCheckinStreak() : 0;
        boolean todayCheckin = stats != null
                && stats.getLastCheckinDate() != null
                && stats.getLastCheckinDate().equals(LocalDate.now());
        data.put("todayCheckin", todayCheckin);
        data.put("checkinStreak", checkinStreak);

        return data;
    }
}
