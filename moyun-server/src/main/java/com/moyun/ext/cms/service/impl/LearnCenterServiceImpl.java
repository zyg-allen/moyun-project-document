package com.moyun.ext.cms.service.impl;

import com.moyun.ext.cms.domain.vo.LearnDashboardVO;
import com.moyun.ext.cms.domain.vo.StudyPlanVO;
import com.moyun.ext.cms.domain.vo.WrongQuestionVO;
import com.moyun.ext.cms.service.ILearnCenterService;
import com.moyun.ext.cms.service.IStudyPlanService;
import com.moyun.ext.cms.service.IWrongQuestionService;
import com.moyun.portal.domain.entity.PortalUser;
import com.moyun.portal.mapper.PortalLearnStatMapper;
import com.moyun.portal.mapper.PortalUserMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * 学习中心聚合 Service 实现（任务 3.1）
 *
 * @author moyun
 */
@Service
public class LearnCenterServiceImpl implements ILearnCenterService {

    /** 连续打卡最长回溯天数 */
    private static final int STREAK_LOOKBACK_DAYS = 365;

    /** 错题预览数量上限 */
    private static final int RECENT_WRONG_LIMIT = 5;

    @Autowired private PortalLearnStatMapper learnStatMapper;
    @Autowired private PortalUserMapper portalUserMapper;
    @Autowired private IStudyPlanService studyPlanService;
    @Autowired private IWrongQuestionService wrongQuestionService;

    @Override
    public LearnDashboardVO getDashboard(Long currentUserId) {
        LearnDashboardVO vo = new LearnDashboardVO();
        vo.setLoggedIn(currentUserId != null);
        vo.setUserId(currentUserId);

        if (currentUserId == null) {
            // 未登录：仅返回结构骨架，统计为 0 / 空
            initEmpty(vo);
            return vo;
        }

        // 昵称（用于问候语）
        PortalUser user = portalUserMapper.selectPortalUserById(currentUserId);
        vo.setNickname(user != null ? user.getNickname() : null);

        // === 答题统计 ===
        Long total = nullSafe(learnStatMapper.countSubmissionsByUser(currentUserId));
        Long success = nullSafe(learnStatMapper.countSuccessByUser(currentUserId));
        vo.setTotalQuestionCount(total);
        vo.setSuccessCount(success);
        vo.setPassRate(total > 0 ? (int) Math.round(success * 100.0 / total) : 0);
        vo.setTodayDoneCount(nullSafe(learnStatMapper.countTodaySubmissionsByUser(currentUserId)));

        // === 连续打卡天数（基于答题提交日期） ===
        vo.setStreakDays(computeStreak(currentUserId));

        // === 错题统计 ===
        vo.setWrongCount(wrongQuestionService.countWrong(currentUserId, null));
        vo.setTodayReviewCount(wrongQuestionService.countTodayReview(currentUserId));

        // === 学习计划（active） ===
        List<StudyPlanVO> activePlans = studyPlanService.listMyPlans(currentUserId, "active", 1, 5).getRecords();
        vo.setActivePlans(activePlans != null ? activePlans : new ArrayList<>());
        vo.setActivePlanCount((long) vo.getActivePlans().size());

        // === 最近错题预览 ===
        List<WrongQuestionVO> recent = wrongQuestionService.listRecentWrong(currentUserId, RECENT_WRONG_LIMIT);
        vo.setRecentWrongQuestions(recent != null ? recent : new ArrayList<>());

        return vo;
    }

    private void initEmpty(LearnDashboardVO vo) {
        vo.setTotalQuestionCount(0L);
        vo.setSuccessCount(0L);
        vo.setPassRate(0);
        vo.setStreakDays(0);
        vo.setWrongCount(0L);
        vo.setTodayReviewCount(0L);
        vo.setActivePlanCount(0L);
        vo.setTodayDoneCount(0L);
        vo.setActivePlans(new ArrayList<>());
        vo.setRecentWrongQuestions(new ArrayList<>());
    }

    private long nullSafe(Long v) {
        return v == null ? 0L : v;
    }

    /**
     * 计算连续打卡天数：从今日向前回溯，连续有答题记录则 +1
     */
    private int computeStreak(Long userId) {
        List<LocalDate> dates = learnStatMapper.selectRecentActiveDates(userId, STREAK_LOOKBACK_DAYS);
        if (dates == null || dates.isEmpty()) {
            return 0;
        }
        int streak = 0;
        LocalDate cursor = LocalDate.now();
        for (LocalDate d : dates) {
            if (d == null) {
                continue;
            }
            if (d.equals(cursor)) {
                streak++;
                cursor = cursor.minusDays(1);
            } else if (d.isBefore(cursor)) {
                break;
            }
        }
        return streak;
    }
}
