package com.moyun.portal.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import com.moyun.portal.domain.entity.PortalBookClubRecordLike;

/**
 * 打卡点赞表 数据层
 *
 * @author moyun
 */
@Mapper
public interface PortalBookClubRecordLikeMapper extends BaseMapper<PortalBookClubRecordLike> {

    /**
     * 查询某用户是否已点赞某打卡记录
     */
    PortalBookClubRecordLike selectByRecordAndUser(@Param("recordId") Long recordId,
                                                   @Param("userId") Long userId);

    /**
     * 统计某打卡记录的点赞数
     */
    long countByRecord(@Param("recordId") Long recordId);
}
