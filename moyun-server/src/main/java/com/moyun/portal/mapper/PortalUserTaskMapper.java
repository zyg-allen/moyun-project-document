package com.moyun.portal.mapper;

import java.util.List;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import com.moyun.portal.domain.entity.PortalUserTask;

/**
 * 用户任务进度 数据层
 *
 * @author moyun
 */
@Mapper
public interface PortalUserTaskMapper extends BaseMapper<PortalUserTask> {

    /**
     * 查询用户某任务进度
     */
    @Select("SELECT * FROM portal_user_task WHERE user_id = #{userId} AND task_id = #{taskId}")
    PortalUserTask selectByUserAndTask(@Param("userId") Long userId, @Param("taskId") Long taskId);

    /**
     * 查询用户所有任务进度（含任务定义信息，联表查询）
     */
    @Select("SELECT ut.*, t.code AS task_code, t.name AS task_name, t.task_type, t.reward_points, t.target_count, t.icon AS task_icon " +
            "FROM portal_user_task ut LEFT JOIN portal_task t ON ut.task_id = t.id " +
            "WHERE ut.user_id = #{userId} ORDER BY t.task_type ASC, t.id ASC")
    List<java.util.Map<String, Object>> selectUserTasksWithDetail(@Param("userId") Long userId);

    /**
     * 原子累加进度并判定是否完成
     *
     * @return 更新行数
     */
    @Update("UPDATE portal_user_task SET progress = LEAST(progress + #{delta}, #{targetCount}), " +
            "completed = IF(progress + #{delta} >= #{targetCount}, 1, completed), " +
            "completed_time = IF(progress + #{delta} >= #{targetCount} AND completed = 0, NOW(), completed_time) " +
            "WHERE user_id = #{userId} AND task_id = #{taskId} AND claimed = 0")
    int addProgress(@Param("userId") Long userId, @Param("taskId") Long taskId,
                    @Param("delta") int delta, @Param("targetCount") int targetCount);

    /**
     * 标记奖励已领取
     */
    @Update("UPDATE portal_user_task SET claimed = 1 WHERE id = #{id} AND user_id = #{userId} AND completed = 1 AND claimed = 0")
    int markClaimed(@Param("id") Long id, @Param("userId") Long userId);
}
