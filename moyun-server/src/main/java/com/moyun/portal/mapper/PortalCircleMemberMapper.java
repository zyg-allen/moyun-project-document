package com.moyun.portal.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.moyun.portal.domain.entity.PortalCircleMember;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

/**
 * 圈子成员 Mapper
 *
 * @author moyun
 */
@Mapper
public interface PortalCircleMemberMapper extends BaseMapper<PortalCircleMember> {

    /**
     * 查询用户在指定圈子中的成员记录（用于判断是否已加入）
     */
    @Select("SELECT * FROM portal_circle_member WHERE circle_id = #{circleId} AND user_id = #{userId}")
    PortalCircleMember selectByCircleAndUser(@Param("circleId") Long circleId, @Param("userId") Long userId);

    /**
     * 统计用户加入的圈子数量（用于校验上限）
     */
    @Select("SELECT COUNT(*) FROM portal_circle_member WHERE user_id = #{userId}")
    int countByUserId(@Param("userId") Long userId);
}
