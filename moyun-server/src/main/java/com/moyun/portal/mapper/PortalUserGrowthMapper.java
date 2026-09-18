package com.moyun.portal.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

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

    PortalUserGrowth selectByUserId(@Param("userId") Long userId);

    /**
     * 原子增加成长值（同时更新赛季值）
     */

    int addGrowth(@Param("userId") Long userId, @Param("delta") int delta);

    /**
     * 更新等级和头衔
     */

    int updateLevel(@Param("userId") Long userId, @Param("level") int level, @Param("title") String title);

    /**
     * 赛季排行榜（Top N）
     */

    List<PortalUserGrowth> selectSeasonRanking(@Param("limit") int limit);

    /**
     * 赛季排行榜（Top N，含用户昵称/头像）
     * <p>修复 JOIN 条件原为 g.user_id = u.user_id，导致 nickname/avatar 全部 NULL。
     * portal_user_growth.user_id 关联的是 portal_user.id（自身主键），不是 portal_user.user_id（后台 sys_user 主键）。
     * 同时增加 coalesce(nickname, username) 兜底，与 PortalFeedEventMapper 保持一致。</p>
     */

    List<Map<String, Object>> selectSeasonRankingWithUser(@Param("limit") int limit);

    /**
     * 查询用户赛季排名
     */

    Integer selectSeasonRank(@Param("userId") Long userId);

    /**
     * 插入（如果不存在）
     */

    int insertIfNotExists(@Param("userId") Long userId);

    /**
     * 原子增加积分（任务奖励）
     */

    int addPoints(@Param("userId") Long userId, @Param("delta") int delta);

    /**
     * 原子扣减积分（积分商城兑换，先校验余额）
     *
     * @return 影响行数，0 表示余额不足
     */

    int deductPoints(@Param("userId") Long userId, @Param("delta") int delta);

    /**
     * 查询用户积分余额
     */

    Long selectPoints(@Param("userId") Long userId);

    /**
     * 原子扣减成长值（审核驳回等场景回滚，带下界保护避免负数）
     * 注意：season_value 同步扣减，保持赛季值与成长值一致
     *
     * @return 影响行数，0 表示余额不足或用户不存在
     */

    int deductGrowth(@Param("userId") Long userId, @Param("delta") int delta);
}
