package com.moyun.portal.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.moyun.portal.domain.entity.PortalFreeTrial;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Update;

/**
 * 免费体验次数 Mapper（v11.85）
 *
 * <p>消耗走原子 SQL（UPDATE 条件自增 / INSERT 冲突即失败），防并发多刷。
 *
 * @author moyun
 */
@Mapper
public interface PortalFreeTrialMapper extends BaseMapper<PortalFreeTrial> {

    /** 原子消耗一次：已有记录且未达上限时自增（返回影响行数，1=成功 0=无资格） */
    @Update("UPDATE portal_free_trial SET used_count = used_count + 1, update_time = NOW() "
            + "WHERE user_id = #{userId} AND scene = #{scene} AND used_count < #{maxTimes}")
    int consumeExisting(@Param("userId") Long userId, @Param("scene") String scene, @Param("maxTimes") int maxTimes);

    /** 首次消耗：插入记录（唯一键冲突说明并发已有行，返回 0 由调用方回退 UPDATE） */
    @Insert("INSERT INTO portal_free_trial (user_id, scene, used_count, create_time, update_time) "
            + "VALUES (#{userId}, #{scene}, 1, NOW(), NOW())")
    int insertFirst(@Param("userId") Long userId, @Param("scene") String scene);
}
