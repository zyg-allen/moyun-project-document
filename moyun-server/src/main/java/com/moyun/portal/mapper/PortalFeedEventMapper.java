package com.moyun.portal.mapper;

import java.util.List;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import com.moyun.ext.cms.domain.vo.FeedEventVO;
import com.moyun.portal.domain.entity.PortalFeedEvent;

/**
 * 动态事件流 数据层
 *
 * @author moyun
 */
@Mapper
public interface PortalFeedEventMapper extends BaseMapper<PortalFeedEvent> {

    /**
     * 查询我关注的人的最近动态（读时拉模式：JOIN portal_follow）
     *
     * @param page   分页参数
     * @param userId 当前登录用户ID（follower_id）
     * @return 动态事件分页（含发布者昵称/头像）
     */
    Page<FeedEventVO> selectFollowingEvents(Page<FeedEventVO> page, @Param("userId") Long userId);

    /**
     * 全站热门动态（最近 7 天事件，按时间倒序）
     *
     * @param page 分页参数
     * @return 动态事件分页（含发布者昵称/头像）
     */
    Page<FeedEventVO> selectHotEvents(Page<FeedEventVO> page);

    /**
     * 新增动态事件（返回自增主键）
     *
     * @param event 动态事件
     * @return 影响行数
     */
    int insertFeedEvent(PortalFeedEvent event);

    /**
     * 根据目标对象查询事件（用于删除场景定位事件）
     *
     * @param targetType 目标类型
     * @param targetId   目标对象ID
     * @return 事件列表
     */
    List<PortalFeedEvent> selectByTarget(@Param("targetType") String targetType, @Param("targetId") Long targetId);

    /**
     * 根据目标对象删除事件（如文章被删除）
     *
     * @param targetType 目标类型
     * @param targetId   目标对象ID
     * @return 影响行数
     */
    int deleteByTarget(@Param("targetType") String targetType, @Param("targetId") Long targetId);
}
