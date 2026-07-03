package com.moyun.ext.cms.service.impl;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.moyun.common.exception.system.ServiceException;
import com.moyun.ext.cms.service.ITaskService;
import com.moyun.portal.domain.entity.PortalTask;
import com.moyun.portal.domain.entity.PortalUserTask;
import com.moyun.portal.mapper.PortalTaskMapper;
import com.moyun.portal.mapper.PortalUserGrowthMapper;
import com.moyun.portal.mapper.PortalUserTaskMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 任务系统 Service 实现（阶段四 4.4）
 *
 * 设计要点：
 * 1. 任务进度通过 portal_user_task 表记录，UNIQUE(user_id, task_id) 保证幂等
 * 2. 进度累加使用原子 SQL（LEST 防超目标），并自动判定 completed
 * 3. 领取奖励需 completed=1 且 claimed=0，原子更新 claimed 并加积分
 * 4. 积分独立于成长值（portal_user_growth.points），兑换不影响等级
 *
 * @author moyun
 */
@Slf4j
@Service
public class TaskServiceImpl implements ITaskService {

    @Autowired private PortalTaskMapper taskMapper;
    @Autowired private PortalUserTaskMapper userTaskMapper;
    @Autowired private PortalUserGrowthMapper growthMapper;

    @Override
    public List<Map<String, Object>> listTasks(Long currentUserId) {
        List<PortalTask> tasks = taskMapper.selectAllActive();
        List<Map<String, Object>> result = new ArrayList<>(tasks.size());
        for (PortalTask task : tasks) {
            Map<String, Object> item = toTaskMap(task);
            if (currentUserId != null) {
                PortalUserTask ut = userTaskMapper.selectByUserAndTask(currentUserId, task.getId());
                item.put("progress", ut != null ? ut.getProgress() : 0);
                item.put("completed", ut != null ? ut.getCompleted() : 0);
                item.put("claimed", ut != null ? ut.getClaimed() : 0);
                item.put("userTaskId", ut != null ? ut.getId() : null);
            } else {
                item.put("progress", 0);
                item.put("completed", 0);
                item.put("claimed", 0);
                item.put("userTaskId", null);
            }
            result.add(item);
        }
        return result;
    }

    @Override
    public List<Map<String, Object>> myTasks(Long userId) {
        if (userId == null) {
            throw new ServiceException("请先登录");
        }
        // 确保当日 daily 任务进度行存在
        refreshDaily(userId);
        return userTaskMapper.selectUserTasksWithDetail(userId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public int claimReward(Long userId, Long userTaskId) {
        if (userId == null) {
            throw new ServiceException("请先登录");
        }
        PortalUserTask ut = userTaskMapper.selectById(userTaskId);
        if (ut == null || !userId.equals(ut.getUserId())) {
            throw new ServiceException("任务记录不存在");
        }
        if (ut.getCompleted() == null || ut.getCompleted() != 1) {
            throw new ServiceException("任务尚未完成");
        }
        if (ut.getClaimed() != null && ut.getClaimed() == 1) {
            throw new ServiceException("奖励已领取");
        }
        // 原子标记领取，防止重复领取
        int updated = userTaskMapper.markClaimed(userTaskId, userId);
        if (updated == 0) {
            throw new ServiceException("奖励领取失败，可能已被领取");
        }
        PortalTask task = taskMapper.selectById(ut.getTaskId());
        if (task == null || task.getRewardPoints() == null || task.getRewardPoints() <= 0) {
            return 0;
        }
        // 确保成长记录存在并加积分
        growthMapper.insertIfNotExists(userId);
        growthMapper.addPoints(userId, task.getRewardPoints());
        return task.getRewardPoints();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void refreshDaily(Long userId) {
        if (userId == null) {
            return;
        }
        growthMapper.insertIfNotExists(userId);
        // 为所有 active 的 daily 任务创建进度行（如不存在）
        List<PortalTask> dailyTasks = taskMapper.selectList(
                Wrappers.<PortalTask>lambdaQuery().eq(PortalTask::getTaskType, "daily").eq(PortalTask::getStatus, "active"));
        for (PortalTask task : dailyTasks) {
            PortalUserTask exist = userTaskMapper.selectByUserAndTask(userId, task.getId());
            if (exist == null) {
                PortalUserTask ut = new PortalUserTask();
                ut.setUserId(userId);
                ut.setTaskId(task.getId());
                ut.setProgress(0);
                ut.setCompleted(0);
                ut.setClaimed(0);
                try {
                    userTaskMapper.insert(ut);
                } catch (org.springframework.dao.DuplicateKeyException e) {
                    // 并发情况下已存在，忽略
                }
            }
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void recordProgress(Long userId, String taskCode, int delta) {
        if (userId == null || taskCode == null || delta <= 0) {
            return;
        }
        PortalTask task = taskMapper.selectByCode(taskCode);
        if (task == null || !"active".equals(task.getStatus())) {
            return;
        }
        // 确保进度行存在
        PortalUserTask ut = userTaskMapper.selectByUserAndTask(userId, task.getId());
        if (ut == null) {
            ut = new PortalUserTask();
            ut.setUserId(userId);
            ut.setTaskId(task.getId());
            ut.setProgress(0);
            ut.setCompleted(0);
            ut.setClaimed(0);
            try {
                userTaskMapper.insert(ut);
            } catch (org.springframework.dao.DuplicateKeyException e) {
                // 并发情况下已存在，继续更新
            }
        }
        int target = task.getTargetCount() != null ? task.getTargetCount() : 1;
        userTaskMapper.addProgress(userId, task.getId(), delta, target);
    }

    private Map<String, Object> toTaskMap(PortalTask task) {
        Map<String, Object> map = new HashMap<>();
        map.put("id", task.getId());
        map.put("code", task.getCode());
        map.put("name", task.getName());
        map.put("description", task.getDescription());
        map.put("taskType", task.getTaskType());
        map.put("rewardPoints", task.getRewardPoints());
        map.put("targetCount", task.getTargetCount());
        map.put("icon", task.getIcon());
        return map;
    }
}
