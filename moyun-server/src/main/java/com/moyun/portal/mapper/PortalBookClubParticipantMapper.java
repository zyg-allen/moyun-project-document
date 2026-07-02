package com.moyun.portal.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import com.moyun.portal.domain.entity.PortalBookClubParticipant;

/**
 * 共读参与表 数据层
 *
 * @author moyun
 */
@Mapper
public interface PortalBookClubParticipantMapper extends BaseMapper<PortalBookClubParticipant> {

    /**
     * 查询某用户是否已加入某活动
     */
    PortalBookClubParticipant selectByActivityAndUser(@Param("activityId") Long activityId,
                                                      @Param("userId") Long userId);

    /**
     * 统计某活动的实际参与人数
     */
    long countByActivity(@Param("activityId") Long activityId);
}
