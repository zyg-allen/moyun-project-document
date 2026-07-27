package com.moyun.ext.cms.service;

import com.moyun.ext.cms.domain.vo.UserProfileSnapshotVO;

/**
 * 用户画像快照 Service（v5.9 阶段0）
 * <p>
 * 为 AI 模拟面试官提供"画像驱动抽题"所需的数据：
 * 1. 薄弱知识点（基于答题历史计算 + Redis 缓存）
 * 2. 岗位必备技能（来自岗位字典）
 * 3. 面试统计（次数/平均分）
 *
 * @author moyun
 */
public interface IUserProfileSnapshotService {

    /**
     * 构建用户画像快照。
     * <p>
     * 若 position 命中岗位字典，则附带必备技能；否则 requiredSkills 为空。
     * 若用户无答题历史，则 weakTags 为空，调用方应降级为随机抽题。
     *
     * @param userId   门户用户ID
     * @param position 用户选择的目标岗位（可空）
     * @param scene    面试场景（可空）
     */
    UserProfileSnapshotVO buildSnapshot(Long userId, String position, String scene);

    /**
     * 刷新用户薄弱知识点（基于答题历史重新计算并写入 portal_user_stats.weak_tags）。
     * <p>
     * 触发时机：模拟面试结束时异步刷新；或定时任务每日刷新。
     *
     * @param userId 门户用户ID
     */
    void refreshWeakTags(Long userId);

    /**
     * 模拟面试结束后，更新面试统计（次数 + 平均分）。
     * <p>
     * 触发时机：finishMockInterview 完成后调用。
     *
     * @param userId 门户用户ID
     */
    void updateMockInterviewStats(Long userId);
}
