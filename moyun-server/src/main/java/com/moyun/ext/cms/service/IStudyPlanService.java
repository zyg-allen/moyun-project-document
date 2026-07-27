package com.moyun.ext.cms.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.moyun.ext.cms.domain.vo.StudyPlanVO;

import java.util.List;

/**
 * 学习计划 Service 接口（任务 3.2）
 *
 * @author moyun
 */
public interface IStudyPlanService {

    /**
     * 创建/修改学习计划。
     * 创建时校验标题非空与计划数量上限；修改时校验归属。
     *
     * @return 计划ID
     */
    Long savePlan(StudyPlanVO vo, Long userId);

    /**
     * 我的计划列表（分页，含进度统计）
     *
     * @param status 状态筛选，可为 null
     */
    Page<StudyPlanVO> listMyPlans(Long userId, String status, Integer pageNum, Integer pageSize);

    /**
     * 计划进度（含进度统计）
     */
    StudyPlanVO getPlanProgress(Long planId, Long userId);

    /**
     * 记录今日完成数（用于今日任务打卡）
     *
     * @return 操作后的今日完成数
     */
    int recordTodayProgress(Long planId, Long userId, int delta);

    /**
     * 切换计划状态（active/completed/abandoned）
     */
    int changeStatus(Long planId, Long userId, String status);

    /**
     * 删除计划（仅作者本人，级联删除日志）
     */
    int deletePlan(Long planId, Long userId);

    /**
     * 基于用户画像自动生成学习计划（v5.9 阶段3）
     * <p>
     * 根据用户画像快照（薄弱点 + 岗位必备技能）生成针对性学习计划：
     * - 薄弱点：每个生成一个 daily_question 计划，targetCategory=标签名，targetCount=10
     * - 岗位必备技能未掌握：每个生成一个 daily_question 计划，targetCategory=技能名，targetCount=15
     * - 自动去重：跳过已存在同 targetCategory + planType 的 active 计划
     * - 受 MAX_PLAN_PER_USER 限制，超限时停止生成
     *
     * @param userId 当前用户ID
     * @return 生成的计划列表（含进度统计）；无画像或无薄弱点时返回空列表
     */
    List<StudyPlanVO> generatePlansFromProfile(Long userId);
}
