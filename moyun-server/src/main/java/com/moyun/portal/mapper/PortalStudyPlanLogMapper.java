package com.moyun.portal.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.moyun.portal.domain.entity.PortalStudyPlanLog;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

/**
 * 计划每日进度 Mapper
 *
 * @author moyun
 */
@Mapper
public interface PortalStudyPlanLogMapper extends BaseMapper<PortalStudyPlanLog> {

    /**
     * 统计某计划的累计完成数
     */
    @Select("SELECT COALESCE(SUM(done_count), 0) FROM portal_study_plan_log WHERE plan_id = #{planId}")
    int sumDoneCountByPlan(@Param("planId") Long planId);

    /**
     * 统计某用户在指定日期的累计完成数（跨所有计划，用于今日完成数）
     */
    @Select("SELECT COALESCE(SUM(done_count), 0) FROM portal_study_plan_log WHERE user_id = #{userId} AND log_date = #{logDate}")
    int sumDoneCountByUserAndDate(@Param("userId") Long userId, @Param("logDate") java.time.LocalDate logDate);
}
