package com.moyun.portal.mapper;

import java.util.List;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

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

    PortalUserTask selectByUserAndTask(@Param("userId") Long userId, @Param("taskId") Long taskId);

    /**
     * 查询用户所有任务进度（含任务定义信息，联表查询）
     */

    List<java.util.Map<String, Object>> selectUserTasksWithDetail(@Param("userId") Long userId);

    /**
     * 原子累加进度并判定是否完成
     *
     * @return 更新行数
     */

    int addProgress(@Param("userId") Long userId, @Param("taskId") Long taskId,
                    @Param("delta") int delta, @Param("targetCount") int targetCount);

    /**
     * 标记奖励已领取
     */

    int markClaimed(@Param("id") Long id, @Param("userId") Long userId);
}
