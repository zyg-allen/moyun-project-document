package com.moyun.ext.cms.service.impl;

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
import com.moyun.portal.domain.vo.UserGrowthVO;
import com.moyun.portal.mapper.PortalArticleMapper;
import com.moyun.portal.mapper.PortalBookshelfMapper;
import com.moyun.portal.mapper.PortalBookmarkMapper;
import com.moyun.portal.mapper.PortalColumnMapper;
import com.moyun.portal.mapper.PortalFollowMapper;
import com.moyun.portal.mapper.PortalInterviewExperienceMapper;
import com.moyun.portal.mapper.PortalInterviewSubmissionMapper;
import com.moyun.portal.mapper.PortalUserResumeMapper;
import com.moyun.portal.service.IPortalGrowthService;

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
    private IPortalGrowthService portalGrowthService;

    @Autowired
    private IMessageService messageService;

    @Override
    public Map<String, Object> getUserDashboard(Long userId) {
        // TODO: 加缓存（@Cacheable 或 Redis），dashboard 为高频接口，可按 userId 缓存并随相关写操作失效

        Map<String, Object> data = new LinkedHashMap<>(16);

        // ===== 内容模块（单条 COUNT SQL，status='published'）=====
        long articleCount = articleMapper.selectCount(
                new QueryWrapper<PortalArticle>()
                        .eq("author_id", userId)
                        .eq("status", "published"));
        data.put("articles", (int) articleCount);

        // ===== 收藏（portal_bookmark，按 user_id）=====
        long bookmarkCount = bookmarkMapper.selectCount(
                new QueryWrapper<PortalBookmark>().eq("user_id", userId));
        data.put("bookmarks", (int) bookmarkCount);

        // ===== 书架（portal_bookshelf，按 user_id）=====
        long bookshelfCount = bookshelfMapper.selectCount(
                new QueryWrapper<PortalBookshelf>().eq("user_id", userId));
        data.put("bookshelf", (int) bookshelfCount);

        // ===== 面试答题数（portal_interview_submission，按 user_id）=====
        long questionAttemptCount = interviewSubmissionMapper.selectCount(
                new QueryWrapper<PortalInterviewSubmission>().eq("user_id", userId));
        data.put("questions", (int) questionAttemptCount);

        // ===== 面经数（portal_interview_experience，按 user_id）=====
        long experienceCount = interviewExperienceMapper.selectCount(
                new QueryWrapper<PortalInterviewExperience>().eq("user_id", userId));
        data.put("experiences", (int) experienceCount);

        // ===== 简历数（portal_user_resume，status <> 'archived'，复用现有 countByUserId）=====
        data.put("resumes", userResumeMapper.countByUserId(userId));

        // ===== 关注 / 粉丝（portal_follow）=====
        // following：当前用户关注了多少人（follower_id = userId）
        long followingCount = followMapper.selectCount(
                new QueryWrapper<PortalFollow>().eq("follower_id", userId));
        data.put("following", (int) followingCount);
        // followers：当前用户有多少粉丝（following_id = userId），复用现有 countFollowers
        data.put("followers", (int) followMapper.countFollowers(userId));

        // ===== 专栏数（portal_column，status='published'）=====
        long columnCount = columnMapper.selectCount(
                new QueryWrapper<PortalColumn>()
                        .eq("user_id", userId)
                        .eq("status", "published"));
        data.put("columns", (int) columnCount);

        // ===== 未读消息（portal_message_session，复用 IMessageService.getUnreadCount）=====
        // 前端 UserDashboard.unreadMessages 期望为未读私信数
        Integer unreadMessage = messageService.getUnreadCount(userId,"portal");
        data.put("unreadMessages", unreadMessage == null ? 0 : unreadMessage);

        // ===== 成长体系（portal_user_growth，复用 IPortalGrowthService.getUserGrowth，含 LEVEL_THRESHOLDS 等级映射）=====
        UserGrowthVO growth = portalGrowthService.getUserGrowth(userId);
        data.put("growthValue", growth != null && growth.getGrowthValue() != null ? growth.getGrowthValue() : 0);
        data.put("growthLevel", growth != null && growth.getLevel() != null ? growth.getLevel() : 1);
        data.put("growthTitle", growth != null && growth.getTitle() != null ? growth.getTitle() : "初出茅庐");

        return data;
    }
}
