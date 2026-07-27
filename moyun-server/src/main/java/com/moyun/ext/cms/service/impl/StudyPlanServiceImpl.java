package com.moyun.ext.cms.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.moyun.common.exception.system.ServiceException;
import com.moyun.ext.cms.domain.vo.StudyPlanVO;
import com.moyun.ext.cms.domain.vo.UserProfileSnapshotVO;
import com.moyun.ext.cms.service.IStudyPlanService;
import com.moyun.ext.cms.service.IUserProfileSnapshotService;
import com.moyun.portal.domain.entity.PortalStudyPlan;
import com.moyun.portal.domain.entity.PortalStudyPlanLog;
import com.moyun.portal.mapper.PortalStudyPlanLogMapper;
import com.moyun.portal.mapper.PortalStudyPlanMapper;
import com.moyun.util.bean.PageUtils;
import com.moyun.util.string.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * 学习计划 Service 实现（任务 3.2）
 *
 * @author moyun
 */
@Service
public class StudyPlanServiceImpl implements IStudyPlanService {

    private static final Logger log = LoggerFactory.getLogger(StudyPlanServiceImpl.class);

    /** 单用户计划数量上限 */
    private static final int MAX_PLAN_PER_USER = 20;
    /** 单次画像生成计划数上限（避免一次生成过多） */
    private static final int MAX_GENERATE_PER_CALL = 6;
    /** 薄弱点计划目标题数 */
    private static final int WEAK_TAG_TARGET_COUNT = 10;
    /** 岗位必备技能计划目标题数 */
    private static final int REQUIRED_SKILL_TARGET_COUNT = 15;
    /** 生成的计划默认持续天数 */
    private static final int GENERATED_PLAN_DAYS = 30;

    @Autowired private PortalStudyPlanMapper studyPlanMapper;
    @Autowired private PortalStudyPlanLogMapper studyPlanLogMapper;
    @Autowired private IUserProfileSnapshotService profileSnapshotService;

    // ========================================================================
    // 创建 / 修改
    // ========================================================================
    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long savePlan(StudyPlanVO vo, Long userId) {
        if (userId == null) {
            throw new ServiceException("请登录后操作");
        }
        if (vo == null || StringUtils.isEmpty(vo.getTitle())) {
            throw new ServiceException("计划标题不能为空");
        }

        boolean isNew = vo.getId() == null || vo.getId() <= 0;
        PortalStudyPlan entity;
        if (isNew) {
            // 创建：校验同用户计划数量上限
            long existCount = studyPlanMapper.selectCount(
                    Wrappers.<PortalStudyPlan>lambdaQuery().eq(PortalStudyPlan::getUserId, userId));
            if (existCount >= MAX_PLAN_PER_USER) {
                throw new ServiceException("计划数量已达上限（" + MAX_PLAN_PER_USER + " 个）");
            }
            entity = new PortalStudyPlan();
            entity.setUserId(userId);
            entity.setStatus(StringUtils.isNotEmpty(vo.getStatus()) ? vo.getStatus() : "active");
            entity.setCreatedTime(LocalDateTime.now());
        } else {
            // 修改：校验归属
            entity = studyPlanMapper.selectById(vo.getId());
            if (entity == null) {
                throw new ServiceException("计划不存在");
            }
            if (!entity.getUserId().equals(userId)) {
                throw new ServiceException("无权修改该计划");
            }
            if (vo.getStatus() != null) {
                entity.setStatus(vo.getStatus());
            }
        }

        entity.setTitle(vo.getTitle());
        entity.setPlanType(vo.getPlanType());
        entity.setTargetCount(vo.getTargetCount());
        entity.setTargetCategory(vo.getTargetCategory());
        entity.setStartDate(vo.getStartDate());
        entity.setEndDate(vo.getEndDate());

        if (isNew) {
            studyPlanMapper.insert(entity);
        } else {
            studyPlanMapper.updateById(entity);
        }
        return entity.getId();
    }

    // ========================================================================
    // 我的计划列表
    // ========================================================================
    @Override
    public Page<StudyPlanVO> listMyPlans(Long userId, String status, Integer pageNum, Integer pageSize) {
        Page<PortalStudyPlan> page = PageUtils.buildPage(pageNum, pageSize);
        LambdaQueryWrapper<PortalStudyPlan> qw = Wrappers.<PortalStudyPlan>lambdaQuery()
                .eq(PortalStudyPlan::getUserId, userId)
                .orderByDesc(PortalStudyPlan::getCreatedTime);
        if (StringUtils.isNotEmpty(status)) {
            qw.eq(PortalStudyPlan::getStatus, status);
        }
        Page<PortalStudyPlan> resultPage = studyPlanMapper.selectPage(page, qw);

        Page<StudyPlanVO> voPage = new Page<>(resultPage.getCurrent(), resultPage.getSize(), resultPage.getTotal());
        List<StudyPlanVO> voList = new ArrayList<>((int) resultPage.getRecords().size());
        for (PortalStudyPlan plan : resultPage.getRecords()) {
            voList.add(toVOWithProgress(plan));
        }
        voPage.setRecords(voList);
        return voPage;
    }

    // ========================================================================
    // 计划进度
    // ========================================================================
    @Override
    public StudyPlanVO getPlanProgress(Long planId, Long userId) {
        PortalStudyPlan plan = mustOwnPlan(planId, userId);
        return toVOWithProgress(plan);
    }

    // ========================================================================
    // 记录今日完成数（upsert uk_plan_date）
    // ========================================================================
    @Override
    @Transactional(rollbackFor = Exception.class)
    public int recordTodayProgress(Long planId, Long userId, int delta) {
        mustOwnPlan(planId, userId);
        if (delta == 0) {
            return 0;
        }
        LocalDate today = LocalDate.now();
        LambdaQueryWrapper<PortalStudyPlanLog> qw = Wrappers.<PortalStudyPlanLog>lambdaQuery()
                .eq(PortalStudyPlanLog::getPlanId, planId)
                .eq(PortalStudyPlanLog::getLogDate, today);
        PortalStudyPlanLog log = studyPlanLogMapper.selectOne(qw);
        if (log == null) {
            log = new PortalStudyPlanLog();
            log.setPlanId(planId);
            log.setUserId(userId);
            log.setLogDate(today);
            log.setDoneCount(Math.max(0, delta));
            log.setCreatedTime(LocalDateTime.now());
            try {
                studyPlanLogMapper.insert(log);
            } catch (DuplicateKeyException e) {
                // 并发兜底：uk_plan_date 触发，回退到更新
                log = studyPlanLogMapper.selectOne(qw);
                if (log == null) {
                    throw e;
                }
                log.setDoneCount(Math.max(0, (log.getDoneCount() == null ? 0 : log.getDoneCount()) + delta));
                studyPlanLogMapper.updateById(log);
            }
        } else {
            log.setDoneCount(Math.max(0, (log.getDoneCount() == null ? 0 : log.getDoneCount()) + delta));
            studyPlanLogMapper.updateById(log);
        }
        return log.getDoneCount();
    }

    // ========================================================================
    // 切换状态
    // ========================================================================
    @Override
    @Transactional(rollbackFor = Exception.class)
    public int changeStatus(Long planId, Long userId, String status) {
        if (StringUtils.isEmpty(status)) {
            throw new ServiceException("状态不能为空");
        }
        PortalStudyPlan plan = mustOwnPlan(planId, userId);
        plan.setStatus(status);
        return studyPlanMapper.updateById(plan);
    }

    // ========================================================================
    // 删除（级联日志）
    // ========================================================================
    @Override
    @Transactional(rollbackFor = Exception.class)
    public int deletePlan(Long planId, Long userId) {
        mustOwnPlan(planId, userId);
        LambdaQueryWrapper<PortalStudyPlanLog> logQw = Wrappers.<PortalStudyPlanLog>lambdaQuery()
                .eq(PortalStudyPlanLog::getPlanId, planId);
        studyPlanLogMapper.delete(logQw);
        return studyPlanMapper.deleteById(planId);
    }

    // ========================================================================
    // v5.9 阶段3：基于画像自动生成学习计划
    // ========================================================================
    @Override
    @Transactional(rollbackFor = Exception.class)
    public List<StudyPlanVO> generatePlansFromProfile(Long userId) {
        if (userId == null) {
            throw new ServiceException("请登录后操作");
        }

        // 1. 构建用户画像快照
        UserProfileSnapshotVO snapshot;
        try {
            snapshot = profileSnapshotService.buildSnapshot(userId, null, null);
        } catch (Exception e) {
            log.warn("[StudyPlan] 用户 {} 画像构建失败：{}", userId, e.getMessage());
            return Collections.emptyList();
        }
        if (snapshot == null || !snapshot.isPersonalized()) {
            log.info("[StudyPlan] 用户 {} 画像未个性化，跳过生成", userId);
            return Collections.emptyList();
        }

        // 2. 收集候选目标分类（薄弱点 + 岗位必备技能），去重
        List<PlanCandidate> candidates = new ArrayList<>();
        Set<String> seen = new HashSet<>();
        if (snapshot.getWeakTags() != null) {
            for (UserProfileSnapshotVO.WeakTagItem wt : snapshot.getWeakTags()) {
                if (StringUtils.isNotEmpty(wt.getTagName()) && seen.add(wt.getTagName())) {
                    candidates.add(new PlanCandidate(wt.getTagName(), WEAK_TAG_TARGET_COUNT, "weak"));
                }
            }
        }
        if (snapshot.getRequiredSkills() != null) {
            for (String skill : snapshot.getRequiredSkills()) {
                if (StringUtils.isNotEmpty(skill) && seen.add(skill)) {
                    candidates.add(new PlanCandidate(skill, REQUIRED_SKILL_TARGET_COUNT, "required"));
                }
            }
        }
        if (candidates.isEmpty()) {
            log.info("[StudyPlan] 用户 {} 无薄弱点与必备技能，跳过生成", userId);
            return Collections.emptyList();
        }

        // 3. 查询已存在的 active 计划的 targetCategory 集合，避免重复
        Set<String> existingCategories = new HashSet<>();
        LambdaQueryWrapper<PortalStudyPlan> existQw = Wrappers.<PortalStudyPlan>lambdaQuery()
                .eq(PortalStudyPlan::getUserId, userId)
                .eq(PortalStudyPlan::getStatus, "active")
                .eq(PortalStudyPlan::getPlanType, "daily_question");
        List<PortalStudyPlan> existPlans = studyPlanMapper.selectList(existQw);
        for (PortalStudyPlan p : existPlans) {
            if (StringUtils.isNotEmpty(p.getTargetCategory())) {
                existingCategories.add(p.getTargetCategory());
            }
        }

        // 4. 校验计划总数上限
        long totalCount = studyPlanMapper.selectCount(
                Wrappers.<PortalStudyPlan>lambdaQuery().eq(PortalStudyPlan::getUserId, userId));
        long remaining = MAX_PLAN_PER_USER - totalCount;
        if (remaining <= 0) {
            log.info("[StudyPlan] 用户 {} 计划数已达上限 {}，跳过生成", userId, MAX_PLAN_PER_USER);
            return Collections.emptyList();
        }

        // 5. 生成计划（受剩余配额与单次上限双重限制）
        int maxGenerate = (int) Math.min(remaining, MAX_GENERATE_PER_CALL);
        List<StudyPlanVO> generated = new ArrayList<>();
        LocalDate today = LocalDate.now();
        LocalDate endDate = today.plusDays(GENERATED_PLAN_DAYS);

        for (PlanCandidate c : candidates) {
            if (generated.size() >= maxGenerate) break;
            // 跳过已存在的同分类计划
            if (existingCategories.contains(c.category)) continue;

            PortalStudyPlan plan = new PortalStudyPlan();
            plan.setUserId(userId);
            plan.setTitle("[画像推荐] 攻克 " + c.category);
            plan.setPlanType("daily_question");
            plan.setTargetCount(c.targetCount);
            plan.setTargetCategory(c.category);
            plan.setStartDate(today);
            plan.setEndDate(endDate);
            plan.setStatus("active");
            plan.setCreatedTime(LocalDateTime.now());
            studyPlanMapper.insert(plan);

            generated.add(toVOWithProgress(plan));
            log.info("[StudyPlan] 用户 {} 生成计划：{}（来源={}）", userId, plan.getTitle(), c.source);
        }

        return generated;
    }

    /** 计划候选项（内部数据结构） */
    private static class PlanCandidate {
        final String category;
        final int targetCount;
        final String source; // weak / required
        PlanCandidate(String category, int targetCount, String source) {
            this.category = category;
            this.targetCount = targetCount;
            this.source = source;
        }
    }

    // ========================================================================
    // 内部工具
    // ========================================================================

    /**
     * 校验计划存在且归属当前用户
     */
    private PortalStudyPlan mustOwnPlan(Long planId, Long userId) {
        if (userId == null) {
            throw new ServiceException("请登录后操作");
        }
        PortalStudyPlan entity = studyPlanMapper.selectById(planId);
        if (entity == null) {
            throw new ServiceException("计划不存在");
        }
        if (!entity.getUserId().equals(userId)) {
            throw new ServiceException("无权操作该计划");
        }
        return entity;
    }

    /**
     * 实体转 VO 并填充进度统计
     */
    private StudyPlanVO toVOWithProgress(PortalStudyPlan plan) {
        StudyPlanVO vo = new StudyPlanVO();
        vo.setId(plan.getId());
        vo.setUserId(plan.getUserId());
        vo.setTitle(plan.getTitle());
        vo.setPlanType(plan.getPlanType());
        vo.setTargetCount(plan.getTargetCount());
        vo.setTargetCategory(plan.getTargetCategory());
        vo.setStartDate(plan.getStartDate());
        vo.setEndDate(plan.getEndDate());
        vo.setStatus(plan.getStatus());
        vo.setCreatedTime(plan.getCreatedTime());

        int doneCount = studyPlanLogMapper.sumDoneCountByPlan(plan.getId());
        vo.setDoneCount(doneCount);

        int todayDone = studyPlanLogMapper.sumDoneCountByUserAndDate(plan.getUserId(), LocalDate.now());
        vo.setTodayDoneCount(todayDone);

        if (plan.getTargetCount() != null && plan.getTargetCount() > 0) {
            int pct = (int) Math.min(100, Math.round(doneCount * 100.0 / plan.getTargetCount()));
            vo.setProgressPercent(pct);
        } else {
            vo.setProgressPercent(0);
        }

        vo.setStreakDays(computePlanStreak(plan.getId()));
        return vo;
    }

    /**
     * 计算计划的连续打卡天数（从今日向前回溯，遇到 done_count=0 或无记录则停止）
     */
    private int computePlanStreak(Long planId) {
        LambdaQueryWrapper<PortalStudyPlanLog> qw = Wrappers.<PortalStudyPlanLog>lambdaQuery()
                .eq(PortalStudyPlanLog::getPlanId, planId)
                .gt(PortalStudyPlanLog::getDoneCount, 0)
                .orderByDesc(PortalStudyPlanLog::getLogDate)
                .last("LIMIT 400");
        List<PortalStudyPlanLog> logs = studyPlanLogMapper.selectList(qw);
        if (logs == null || logs.isEmpty()) {
            return 0;
        }
        int streak = 0;
        LocalDate cursor = LocalDate.now();
        for (PortalStudyPlanLog log : logs) {
            if (log.getLogDate() == null) {
                continue;
            }
            if (log.getLogDate().equals(cursor)) {
                streak++;
                cursor = cursor.minusDays(1);
            } else if (log.getLogDate().isBefore(cursor)) {
                // 中间有断档，停止
                break;
            }
            // log.logDate 在 cursor 之后（未来记录）忽略，继续遍历
        }
        return streak;
    }
}
