package com.moyun.ext.cms.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.moyun.common.exception.system.ServiceException;
import com.moyun.ext.cms.domain.vo.StudyPlanVO;
import com.moyun.ext.cms.service.IStudyPlanService;
import com.moyun.portal.domain.entity.PortalStudyPlan;
import com.moyun.portal.domain.entity.PortalStudyPlanLog;
import com.moyun.portal.mapper.PortalStudyPlanLogMapper;
import com.moyun.portal.mapper.PortalStudyPlanMapper;
import com.moyun.util.bean.PageUtils;
import com.moyun.util.string.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * 学习计划 Service 实现（任务 3.2）
 *
 * @author moyun
 */
@Service
public class StudyPlanServiceImpl implements IStudyPlanService {

    /** 单用户计划数量上限 */
    private static final int MAX_PLAN_PER_USER = 20;

    @Autowired private PortalStudyPlanMapper studyPlanMapper;
    @Autowired private PortalStudyPlanLogMapper studyPlanLogMapper;

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
