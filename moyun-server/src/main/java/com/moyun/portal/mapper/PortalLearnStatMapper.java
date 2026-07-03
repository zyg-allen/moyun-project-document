package com.moyun.portal.mapper;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

/**
 * 学习中心聚合统计 Mapper（任务 3.1）
 * <p>
 * 专门用于聚合 portal_interview_submission 表的统计查询，
 * 不修改已有的 PortalInterviewSubmissionMapper，保持职责单一。
 *
 * @author moyun
 */
@Mapper
public interface PortalLearnStatMapper {

    /**
     * 用户累计答题数（按 user_id 统计）
     */
    @Select("SELECT COUNT(*) FROM portal_interview_submission WHERE user_id = #{userId}")
    Long countSubmissionsByUser(@Param("userId") Long userId);

    /**
     * 用户累计通过数（is_success = 1）
     */
    @Select("SELECT COUNT(*) FROM portal_interview_submission WHERE user_id = #{userId} AND is_success = 1")
    Long countSuccessByUser(@Param("userId") Long userId);

    /**
     * 用户今日答题数
     */
    @Select("SELECT COUNT(*) FROM portal_interview_submission " +
            "WHERE user_id = #{userId} AND DATE(create_time) = CURDATE()")
    Long countTodaySubmissionsByUser(@Param("userId") Long userId);

    /**
     * 统计最近 N 天每日刷题数（用于计算连续打卡天数）
     * 返回有答题记录的去重日期数（按日期倒序）
     */
    @Select("SELECT DISTINCT DATE(create_time) FROM portal_interview_submission " +
            "WHERE user_id = #{userId} AND create_time >= DATE_SUB(CURDATE(), INTERVAL #{days} DAY) " +
            "ORDER BY DATE(create_time) DESC")
    java.util.List<java.time.LocalDate> selectRecentActiveDates(@Param("userId") Long userId, @Param("days") int days);
}
