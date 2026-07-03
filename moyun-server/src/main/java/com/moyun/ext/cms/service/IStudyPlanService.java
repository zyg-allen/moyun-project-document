package com.moyun.ext.cms.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.moyun.ext.cms.domain.vo.StudyPlanVO;

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
}
