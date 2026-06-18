package com.moyun.portal.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import com.moyun.portal.domain.entity.PortalGrowthLog;

import java.time.LocalDate;
import java.util.List;

/**
 * 成长事件流水 数据层
 *
 * @author moyun
 */
@Mapper
public interface PortalGrowthLogMapper extends BaseMapper<PortalGrowthLog> {

    /**
     * 查询今日某行为已获得成长值次数（用于每日上限校验）
     */
    @Select("SELECT COUNT(*) FROM portal_growth_log WHERE user_id = #{userId} AND module = #{module} AND action = #{action} AND DATE(create_time) = #{today}")
    int countTodayAction(@Param("userId") Long userId, @Param("module") String module, @Param("action") String action, @Param("today") LocalDate today);

    /**
     * 查询某行为的累计次数（用于成就检测）
     */
    @Select("SELECT COUNT(*) FROM portal_growth_log WHERE user_id = #{userId} AND action = #{action}")
    int countAction(@Param("userId") Long userId, @Param("action") String action);

    /**
     * 查询用户成长记录列表
     */
    @Select("SELECT * FROM portal_growth_log WHERE user_id = #{userId} ORDER BY create_time DESC LIMIT #{limit}")
    List<PortalGrowthLog> selectRecentLogs(@Param("userId") Long userId, @Param("limit") int limit);
}
