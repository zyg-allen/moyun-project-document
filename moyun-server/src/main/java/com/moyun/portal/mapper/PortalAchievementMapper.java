package com.moyun.portal.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import com.moyun.portal.domain.entity.PortalAchievement;

import java.util.List;

/**
 * 成就定义 数据层
 *
 * @author moyun
 */
@Mapper
public interface PortalAchievementMapper extends BaseMapper<PortalAchievement> {

    /**
     * 查询所有启用的成就
     */
    @Select("SELECT * FROM portal_achievement WHERE status = '0' ORDER BY sort")
    List<PortalAchievement> selectAllEnabled();

    /**
     * 根据编码查询成就
     */
    @Select("SELECT * FROM portal_achievement WHERE code = #{code}")
    PortalAchievement selectByCode(@Param("code") String code);
}
