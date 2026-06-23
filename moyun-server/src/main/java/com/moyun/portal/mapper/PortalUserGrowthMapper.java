package com.moyun.portal.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import com.moyun.portal.domain.entity.PortalUserGrowth;

import java.util.List;
import java.util.Map;

/**
 * 用户成长值 数据层
 *
 * @author moyun
 */
@Mapper
public interface PortalUserGrowthMapper extends BaseMapper<PortalUserGrowth> {

    /**
     * 根据用户ID查询成长信息
     */
    @Select("SELECT * FROM portal_user_growth WHERE user_id = #{userId}")
    PortalUserGrowth selectByUserId(@Param("userId") Long userId);

    /**
     * 原子增加成长值（同时更新赛季值）
     */
    @Update("UPDATE portal_user_growth SET growth_value = growth_value + #{delta}, season_value = season_value + #{delta} WHERE user_id = #{userId}")
    int addGrowth(@Param("userId") Long userId, @Param("delta") int delta);

    /**
     * 更新等级和头衔
     */
    @Update("UPDATE portal_user_growth SET level = #{level}, title = #{title} WHERE user_id = #{userId}")
    int updateLevel(@Param("userId") Long userId, @Param("level") int level, @Param("title") String title);

    /**
     * 赛季排行榜（Top N）
     */
    @Select("SELECT * FROM portal_user_growth ORDER BY season_value DESC LIMIT #{limit}")
    List<PortalUserGrowth> selectSeasonRanking(@Param("limit") int limit);

    /**
     * 赛季排行榜（Top N，含用户昵称/头像）
     */
    @Select("SELECT g.*, u.nickname, u.avatar FROM portal_user_growth g " +
            "LEFT JOIN portal_user u ON g.user_id = u.user_id " +
            "ORDER BY g.season_value DESC LIMIT #{limit}")
    List<Map<String, Object>> selectSeasonRankingWithUser(@Param("limit") int limit);

    /**
     * 查询用户赛季排名
     */
    @Select("SELECT COUNT(*) + 1 FROM portal_user_growth WHERE season_value > (SELECT season_value FROM portal_user_growth WHERE user_id = #{userId})")
    Integer selectSeasonRank(@Param("userId") Long userId);

    /**
     * 插入（如果不存在）
     */
    @Update("INSERT IGNORE INTO portal_user_growth (user_id, growth_value, level, title, season_value) VALUES (#{userId}, 0, 1, '初出茅庐', 0)")
    int insertIfNotExists(@Param("userId") Long userId);
}
