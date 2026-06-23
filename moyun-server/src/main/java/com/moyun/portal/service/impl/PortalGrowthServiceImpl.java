package com.moyun.portal.service.impl;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.moyun.portal.domain.entity.PortalAchievement;
import com.moyun.portal.domain.entity.PortalGrowthLog;
import com.moyun.portal.domain.entity.PortalGrowthRule;
import com.moyun.portal.domain.entity.PortalUserBadge;
import com.moyun.portal.domain.entity.PortalUserGrowth;
import com.moyun.portal.domain.entity.PortalUserStats;
import com.moyun.portal.domain.vo.AchievementVO;
import com.moyun.portal.domain.vo.UserBadgeVO;
import com.moyun.portal.domain.vo.UserGrowthVO;
import com.moyun.portal.domain.vo.UserStatsVO;
import com.moyun.portal.mapper.PortalAchievementMapper;
import com.moyun.portal.mapper.PortalGrowthLogMapper;
import com.moyun.portal.mapper.PortalGrowthRuleMapper;
import com.moyun.portal.mapper.PortalUserBadgeMapper;
import com.moyun.portal.mapper.PortalUserGrowthMapper;
import com.moyun.portal.mapper.PortalUserStatsMapper;
import com.moyun.portal.service.IPortalGrowthService;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 门户用户成长体系 服务实现
 *
 * 核心设计：
 * 1. 同步调用（非 Spring Event），在调用方事务内执行，保证一致性
 * 2. 原子 SQL 更新统计和成长值，避免并发丢失
 * 3. 每日上限校验通过 portal_growth_log 查询当日次数
 * 4. 成就检测基于成长事件累计次数
 *
 * @author moyun
 */
@Slf4j
@Service
public class PortalGrowthServiceImpl implements IPortalGrowthService {

    @Autowired
    private PortalUserGrowthMapper growthMapper;

    @Autowired
    private PortalUserStatsMapper statsMapper;

    @Autowired
    private PortalGrowthLogMapper logMapper;

    @Autowired
    private PortalGrowthRuleMapper ruleMapper;

    @Autowired
    private PortalAchievementMapper achievementMapper;

    @Autowired
    private PortalUserBadgeMapper badgeMapper;

    private static final ObjectMapper objectMapper = new ObjectMapper();

    /** 等级阈值表：index=等级-1，value=该等级所需的最低成长值 */
    private static final int[] LEVEL_THRESHOLDS = {0, 100, 300, 700, 1500, 3000, 6000, 10000, 20000};
    private static final String[] LEVEL_TITLES = {
            "初出茅庐", "小试牛刀", "锋芒初露", "登堂入室", "渐入佳境",
            "炉火纯青", "游刃有余", "登峰造极", "一代宗师"
    };

    @Override
    @Transactional(rollbackFor = Exception.class)
    public int recordEvent(String module, String action, Long userId, String entityType, Long entityId) {
        return recordEventWithTarget(module, action, userId, null, entityType, entityId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public int recordEventWithTarget(String module, String action, Long userId, Long targetUserId,
                                     String entityType, Long entityId) {
        if (userId == null) {
            return 0;
        }

        // 1. 查询成长规则
        PortalGrowthRule rule = ruleMapper.selectByModuleAndAction(module, action);
        if (rule == null) {
            log.debug("成长规则未配置: module={}, action={}", module, action);
            return 0;
        }

        // 2. 每日上限校验
        if (rule.getDailyLimit() != null && rule.getDailyLimit() > 0) {
            int todayCount = logMapper.countTodayAction(userId, module, action, LocalDate.now());
            if (todayCount >= rule.getDailyLimit()) {
                log.debug("今日成长值已达上限: userId={}, action={}, limit={}", userId, action, rule.getDailyLimit());
                return 0;
            }
        }

        // 3. VIP 1.5 倍加成
        int delta = rule.getGrowthDelta();
        boolean isVip = isUserVip(userId);
        if (isVip) {
            delta = (int) (delta * 1.5);
        }

        // 4. 确保用户成长记录和统计记录存在
        growthMapper.insertIfNotExists(userId);
        statsMapper.insertIfNotExists(userId);

        // 5. 记录成长事件流水
        PortalGrowthLog growthLog = new PortalGrowthLog();
        growthLog.setUserId(userId);
        growthLog.setTargetUserId(targetUserId);
        growthLog.setModule(module);
        growthLog.setAction(action);
        growthLog.setEntityType(entityType);
        growthLog.setEntityId(entityId);
        growthLog.setGrowthDelta(delta);
        growthLog.setDescription(rule.getDescription() + (isVip ? "(VIP×1.5)" : ""));
        growthLog.setCreateTime(LocalDateTime.now());
        logMapper.insert(growthLog);

        // 6. 原子增加成长值
        growthMapper.addGrowth(userId, delta);

        // 7. 更新统计聚合表
        updateStats(module, action, userId, delta);

        // 8. 更新等级
        updateLevel(userId);

        // 9. 检测成就
        checkAndGrantAchievements(userId);

        return delta;
    }

    @Override
    public UserGrowthVO getUserGrowth(Long userId) {
        growthMapper.insertIfNotExists(userId);
        PortalUserGrowth growth = growthMapper.selectByUserId(userId);
        if (growth == null) {
            growth = new PortalUserGrowth();
            growth.setUserId(userId);
            growth.setGrowthValue(0);
            growth.setLevel(1);
            growth.setTitle(LEVEL_TITLES[0]);
            growth.setSeasonValue(0);
        }

        UserGrowthVO vo = new UserGrowthVO();
        vo.setUserId(userId);
        vo.setGrowthValue(growth.getGrowthValue());
        vo.setLevel(growth.getLevel());
        vo.setTitle(growth.getTitle());
        vo.setSeasonValue(growth.getSeasonValue());
        vo.setUpdateTime(growth.getUpdateTime());

        // 计算下一级信息
        int currentLevel = growth.getLevel();
        if (currentLevel < LEVEL_THRESHOLDS.length) {
            int nextThreshold = LEVEL_THRESHOLDS[currentLevel];
            vo.setNextLevelGrowth(nextThreshold - growth.getGrowthValue());
            vo.setNextLevelTitle(LEVEL_TITLES[currentLevel]);
        }

        // 赛季排名
        Integer rank = growthMapper.selectSeasonRank(userId);
        vo.setSeasonRank(rank);

        return vo;
    }

    @Override
    public UserStatsVO getUserStats(Long userId) {
        statsMapper.insertIfNotExists(userId);
        PortalUserStats stats = statsMapper.selectByUserId(userId);
        if (stats == null) {
            stats = new PortalUserStats();
            stats.setUserId(userId);
        }

        UserStatsVO vo = new UserStatsVO();
        vo.setUserId(userId);
        vo.setArticles(stats.getArticleCount() != null ? stats.getArticleCount() : 0);
        vo.setViews(stats.getArticleViewSum() != null ? stats.getArticleViewSum() : 0L);
        vo.setLikes(stats.getArticleLikeSum() != null ? stats.getArticleLikeSum() : 0L);
        vo.setBookmarks(stats.getArticleBookmarkSum() != null ? stats.getArticleBookmarkSum() : 0L);
        vo.setWordCount(stats.getArticleWordSum() != null ? stats.getArticleWordSum() : 0L);
        vo.setBookFinished(stats.getBookFinished() != null ? stats.getBookFinished() : 0);
        vo.setBooklistCount(stats.getBooklistCount() != null ? stats.getBooklistCount() : 0);
        vo.setQuoteCount(stats.getQuoteCount() != null ? stats.getQuoteCount() : 0);
        vo.setReadingMinutes(stats.getReadingMinutes() != null ? stats.getReadingMinutes() : 0L);
        vo.setQuestionSolved(stats.getQuestionSolved() != null ? stats.getQuestionSolved() : 0);
        vo.setNoteCount(stats.getNoteCount() != null ? stats.getNoteCount() : 0);
        vo.setExperienceCount(stats.getExperienceCount() != null ? stats.getExperienceCount() : 0);
        vo.setNoteAdopted(stats.getNoteAdopted() != null ? stats.getNoteAdopted() : 0);
        vo.setFollowers(stats.getFollowerCount() != null ? stats.getFollowerCount() : 0);
        vo.setFollowing(stats.getFollowingCount() != null ? stats.getFollowingCount() : 0);
        vo.setComments(stats.getCommentCount() != null ? stats.getCommentCount() : 0);
        vo.setTotalLikes(stats.getTotalLikeReceived() != null ? stats.getTotalLikeReceived() : 0L);
        vo.setCheckinStreak(stats.getCheckinStreak() != null ? stats.getCheckinStreak() : 0);
        return vo;
    }

    @Override
    public List<UserBadgeVO> getUserBadges(Long userId) {
        return badgeMapper.selectBadgesByUserId(userId);
    }

    @Override
    public List<AchievementVO> getAllAchievements(Long userId) {
        List<PortalAchievement> achievements = achievementMapper.selectAllEnabled();
        // 已达成的徽章记录
        List<UserBadgeVO> earned = userId == null ? java.util.Collections.emptyList() : badgeMapper.selectBadgesByUserId(userId);
        java.util.Map<Long, UserBadgeVO> earnedMap = new java.util.HashMap<>();
        for (UserBadgeVO b : earned) {
            if (b.getAchievementId() != null) earnedMap.put(b.getAchievementId(), b);
        }
        List<AchievementVO> result = new ArrayList<>();
        for (PortalAchievement a : achievements) {
            AchievementVO vo = new AchievementVO();
            vo.setId(a.getId());
            vo.setCode(a.getCode());
            vo.setName(a.getName());
            vo.setDescription(a.getDescription());
            vo.setIcon(a.getIcon());
            vo.setModule(a.getModule());
            vo.setGrowthReward(a.getGrowthReward());
            vo.setSort(a.getSort());
            UserBadgeVO ub = earnedMap.get(a.getId());
            vo.setEarned(ub != null);
            vo.setEarnedTime(ub == null ? null : ub.getCreateTime());
            result.add(vo);
        }
        return result;
    }

    @Override
    public List<UserGrowthVO> getRanking(int limit) {
        List<Map<String, Object>> list = growthMapper.selectSeasonRankingWithUser(limit);
        List<UserGrowthVO> result = new ArrayList<>();
        for (int i = 0; i < list.size(); i++) {
            Map<String, Object> row = list.get(i);
            UserGrowthVO vo = new UserGrowthVO();
            vo.setUserId(toLong(row.get("user_id")));
            vo.setGrowthValue(toInt(row.get("growth_value")));
            vo.setLevel(toInt(row.get("level")));
            Object titleObj = row.get("title");
            vo.setTitle(titleObj == null ? null : titleObj.toString());
            vo.setSeasonValue(toInt(row.get("season_value")));
            vo.setSeasonRank(i + 1);
            Object nickObj = row.get("nickname");
            vo.setNickname(nickObj == null ? null : nickObj.toString());
            Object avatarObj = row.get("avatar");
            vo.setAvatar(avatarObj == null ? null : avatarObj.toString());
            result.add(vo);
        }
        return result;
    }

    private static Integer toInt(Object o) {
        if (o == null) return 0;
        if (o instanceof Number) return ((Number) o).intValue();
        try { return Integer.parseInt(o.toString()); } catch (Exception e) { return 0; }
    }

    private static Long toLong(Object o) {
        if (o == null) return null;
        if (o instanceof Number) return ((Number) o).longValue();
        try { return Long.parseLong(o.toString()); } catch (Exception e) { return null; }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Map<String, Object> checkin(Long userId) {
        Map<String, Object> result = new HashMap<>();
        if (userId == null) {
            result.put("success", false);
            result.put("message", "用户未登录");
            return result;
        }

        statsMapper.insertIfNotExists(userId);
        PortalUserStats stats = statsMapper.selectByUserId(userId);
        LocalDate today = LocalDate.now();

        if (stats.getLastCheckinDate() != null && stats.getLastCheckinDate().equals(today)) {
            result.put("success", false);
            result.put("message", "今日已签到");
            result.put("streak", stats.getCheckinStreak());
            return result;
        }

        // 计算连续签到天数
        int newStreak = 1;
        if (stats.getLastCheckinDate() != null) {
            LocalDate yesterday = today.minusDays(1);
            if (stats.getLastCheckinDate().equals(yesterday)) {
                newStreak = (stats.getCheckinStreak() != null ? stats.getCheckinStreak() : 0) + 1;
            }
        }

        // 更新签到信息（需要自定义 SQL，这里用 BaseMapper 的 update）
        PortalUserStats update = new PortalUserStats();
        update.setId(stats.getId());
        update.setCheckinStreak(newStreak);
        update.setLastCheckinDate(today);
        statsMapper.updateById(update);

        // 记录成长事件
        int growth = recordEvent("all", "daily_checkin", userId, null, null);

        result.put("success", true);
        result.put("message", "签到成功");
        result.put("streak", newStreak);
        result.put("growth", growth);
        return result;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void checkAndGrantAchievements(Long userId) {
        if (userId == null) return;

        List<PortalAchievement> achievements = achievementMapper.selectAllEnabled();
        for (PortalAchievement achievement : achievements) {
            // 检查是否已获得
            int exists = badgeMapper.countByUserAndAchievement(userId, achievement.getId());
            if (exists > 0) continue;

            // 解析条件
            if (!checkCondition(userId, achievement)) continue;

            // 授予徽章
            badgeMapper.insertIfNotExists(userId, achievement.getId());

            // 奖励成长值
            if (achievement.getGrowthReward() != null && achievement.getGrowthReward() > 0) {
                growthMapper.insertIfNotExists(userId);
                growthMapper.addGrowth(userId, achievement.getGrowthReward());

                // 记录奖励流水
                PortalGrowthLog rewardLog = new PortalGrowthLog();
                rewardLog.setUserId(userId);
                rewardLog.setModule("all");
                rewardLog.setAction("achievement_reward");
                rewardLog.setEntityType("achievement");
                rewardLog.setEntityId(achievement.getId());
                rewardLog.setGrowthDelta(achievement.getGrowthReward());
                rewardLog.setDescription("获得成就: " + achievement.getName());
                rewardLog.setCreateTime(LocalDateTime.now());
                logMapper.insert(rewardLog);

                updateLevel(userId);
            }

            log.info("用户[{}]获得成就: {} ({})", userId, achievement.getName(), achievement.getCode());
        }
    }

    // ==================== 内部方法 ====================

    /**
     * 根据模块和行为更新统计聚合表
     */
    private void updateStats(String module, String action, Long userId, int delta) {
        switch (action) {
            // 文章模块
            case "publish_article":
                statsMapper.addArticleCount(userId, 1);
                break;
            case "receive_like":
                statsMapper.addArticleLikeSum(userId, delta);
                break;
            case "receive_bookmark":
                statsMapper.addArticleBookmarkSum(userId, delta);
                break;
            case "article_featured":
                // 精选不增加计数，只加成长值
                break;
            case "receive_comment":
                statsMapper.addCommentCount(userId, delta);
                break;
            case "receive_follow":
                statsMapper.addFollowerCount(userId, delta);
                break;
            // 读书空间
            case "finish_book":
                statsMapper.addBookFinished(userId, delta);
                break;
            case "write_quote":
                statsMapper.addQuoteCount(userId, delta);
                break;
            case "create_booklist":
                statsMapper.addBooklistCount(userId, delta);
                break;
            case "quote_liked":
            case "booklist_liked":
                statsMapper.addTotalLikeReceived(userId, delta);
                break;
            case "booklist_bookmarked":
                // 书单被收藏，归入总被赞/被收藏统计（复用 totalLikeReceived 聚合）
                statsMapper.addTotalLikeReceived(userId, delta);
                break;
            // 面试空间
            case "solve_question":
                statsMapper.addQuestionSolved(userId, delta);
                break;
            case "write_note":
                statsMapper.addNoteCount(userId, delta);
                break;
            case "note_adopted":
                statsMapper.addNoteAdopted(userId, delta);
                break;
            case "publish_experience":
                statsMapper.addExperienceCount(userId, delta);
                break;
            case "experience_liked":
                statsMapper.addTotalLikeReceived(userId, delta);
                break;
            default:
                break;
        }
    }

    /**
     * 更新用户等级
     */
    private void updateLevel(Long userId) {
        PortalUserGrowth growth = growthMapper.selectByUserId(userId);
        if (growth == null) return;

        int growthValue = growth.getGrowthValue() != null ? growth.getGrowthValue() : 0;
        int newLevel = 1;
        for (int i = LEVEL_THRESHOLDS.length - 1; i >= 0; i--) {
            if (growthValue >= LEVEL_THRESHOLDS[i]) {
                newLevel = i + 1;
                break;
            }
        }

        if (newLevel != growth.getLevel()) {
            String title = newLevel <= LEVEL_TITLES.length ? LEVEL_TITLES[newLevel - 1] : LEVEL_TITLES[LEVEL_TITLES.length - 1];
            growthMapper.updateLevel(userId, newLevel, title);
            log.info("用户[{}]等级提升: {} -> {} ({})", userId, growth.getLevel(), newLevel, title);
        }
    }

    /**
     * 检查成就条件是否满足
     */
    private boolean checkCondition(Long userId, PortalAchievement achievement) {
        if (achievement.getConditionJson() == null || achievement.getConditionJson().isBlank()) {
            return false;
        }

        try {
            JsonNode condition = objectMapper.readTree(achievement.getConditionJson());
            String action = condition.has("action") ? condition.get("action").asText() : null;
            int requiredCount = condition.has("count") ? condition.get("count").asInt() : 0;

            if (action == null || requiredCount <= 0) return false;

            // 特殊处理等级成就
            if ("level".equals(action)) {
                PortalUserGrowth growth = growthMapper.selectByUserId(userId);
                int level = growth != null && growth.getLevel() != null ? growth.getLevel() : 1;
                return level >= requiredCount;
            }

            // 查询行为累计次数
            int actualCount = logMapper.countAction(userId, action);
            return actualCount >= requiredCount;

        } catch (Exception e) {
            log.error("解析成就条件失败: {}", achievement.getConditionJson(), e);
            return false;
        }
    }

    /**
     * 判断用户是否VIP（通过查询数据库，避免依赖 SecurityContext）
     */
    private boolean isUserVip(Long userId) {
        try {
            com.moyun.portal.domain.model.PortalLoginUser loginUser = com.moyun.portal.util.PortalSecurityUtils.getLoginUser();
            if (loginUser != null && loginUser.getId() != null && loginUser.getId().equals(userId)) {
                return loginUser.getRoles() != null && loginUser.getRoles().contains("vip");
            }
        } catch (Exception e) {
            // SecurityContext 不可用时忽略
        }
        return false;
    }
}
