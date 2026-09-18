package com.moyun.portal.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.moyun.portal.domain.entity.PortalStudyPlanLog;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

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

    int sumDoneCountByPlan(@Param("planId") Long planId);

    /**
     * 统计某用户在指定日期的累计完成数（跨所有计划，用于今日完成数）
     */

    int sumDoneCountByUserAndDate(@Param("userId") Long userId, @Param("logDate") java.time.LocalDate logDate);
}
