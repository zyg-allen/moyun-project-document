package com.moyun.community.growth.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.moyun.community.growth.entity.CmsPointsLog;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface CmsPointsLogMapper extends BaseMapper<CmsPointsLog> {

    @Select("SELECT COALESCE(SUM(points), 0) FROM cms_points_log WHERE user_id = #{userId} AND status = 'completed'")
    Long selectSumPoints(@Param("userId") Long userId);
}
