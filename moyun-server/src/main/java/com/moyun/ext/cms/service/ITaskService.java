package com.moyun.ext.cms.service;

import com.moyun.portal.domain.entity.PortalTask;

import java.util.List;
import java.util.Map;

/**
 * 任务系统 Service（阶段四 4.4）
 *
 * 提供任务列表、用户进度、领取奖励、进度埋点能力。
 *
 * @author moyun
 */
public interface ITaskService {

    /**
     * 任务列表（公开，未登录时不返回进度）
     *
     * @param currentUserId 当前登录用户ID（可为 null）
     * @return 任务列表（含当前用户进度）
     */
    List<Map<String, Object>> listTasks(Long currentUserId);

    /**
     * 我的任务进度（需登录）
     *
     * @param userId 用户ID
     * @return 任务进度列表（含任务定义）
     */
    List<Map<String, Object>> myTasks(Long userId);

    /**
     * 领取任务奖励（需登录）
     *
     * @param userId     用户ID
     * @param userTaskId 用户任务进度ID
     * @return 实际发放的积分
     */
    int claimReward(Long userId, Long userTaskId);

    /**
     * 刷新每日任务（首次访问任务中心时为用户新建当日 daily 任务进度行）
     *
     * @param userId 用户ID
     */
    void refreshDaily(Long userId);

    /**
     * 任务进度埋点（供签到/发文/评论/点赞/刷题等行为调用）
     *
     * @param userId   用户ID
     * @param taskCode 任务编码（对应 portal_task.code）
     * @param delta    进度增量
     */
    void recordProgress(Long userId, String taskCode, int delta);
}
