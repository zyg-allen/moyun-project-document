package com.moyun.portal.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import com.moyun.portal.domain.entity.PortalUserBadge;

import java.util.List;

/**
 * 用户徽章 数据层
 *
 * @author moyun
 */
@Mapper
public interface PortalUserBadgeMapper extends BaseMapper<PortalUserBadge> {

    /**
     * 查询用户是否已获得某成就
     */
    @Select("SELECT COUNT(*) FROM portal_user_badge WHERE user_id = #{userId} AND achievement_id = #{achievementId}")
    int countByUserAndAchievement(@Param("userId") Long userId, @Param("achievementId") Long achievementId);

    /**
     * 插入徽章（如果不存在）
     */
    @Update("INSERT IGNORE INTO portal_user_badge (user_id, achievement_id) VALUES (#{userId}, #{achievementId})")
    int insertIfNotExists(@Param("userId") Long userId, @Param("achievementId") Long achievementId);

    /**
     * 查询用户所有徽章（关联成就信息）
     */
    @Select("SELECT b.id, b.achievement_id, a.code, a.name, a.description, a.icon, a.module, a.growth_reward, b.create_time " +
            "FROM portal_user_badge b " +
            "INNER JOIN portal_achievement a ON b.achievement_id = a.id " +
            "WHERE b.user_id = #{userId} " +
            "ORDER BY b.create_time DESC")
    List<com.moyun.portal.domain.vo.UserBadgeVO> selectBadgesByUserId(@Param("userId") Long userId);
}
