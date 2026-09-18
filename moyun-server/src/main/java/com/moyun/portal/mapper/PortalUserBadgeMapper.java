package com.moyun.portal.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

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

    int countByUserAndAchievement(@Param("userId") Long userId, @Param("achievementId") Long achievementId);

    /**
     * 插入徽章（如果不存在）
     */

    int insertIfNotExists(@Param("userId") Long userId, @Param("achievementId") Long achievementId);

    /**
     * 查询用户所有徽章（关联成就信息）
     */

    List<com.moyun.portal.domain.vo.UserBadgeVO> selectBadgesByUserId(@Param("userId") Long userId);
}
