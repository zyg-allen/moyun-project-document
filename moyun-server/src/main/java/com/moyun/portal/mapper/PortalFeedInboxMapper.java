package com.moyun.portal.mapper;

import java.util.List;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import com.moyun.portal.domain.entity.PortalFeedInbox;

/**
 * 动态收件箱 数据层（推模式：写入关注者收件箱）
 *
 * @author moyun
 */
@Mapper
public interface PortalFeedInboxMapper extends BaseMapper<PortalFeedInbox> {

    /**
     * 批量插入收件箱记录（向多个粉丝推送同一条事件）
     *
     * @param eventId     动态事件ID
     * @param followerIds 粉丝ID列表
     * @param createdTime 入箱时间
     * @return 影响行数
     */
    int batchInsert(@Param("eventId") Long eventId,
                    @Param("followerIds") List<Long> followerIds,
                    @Param("createdTime") java.time.LocalDateTime createdTime);

    /**
     * 根据事件ID删除收件箱记录（事件删除时同步清理）
     *
     * @param eventId 动态事件ID
     * @return 影响行数
     */
    int deleteByEventId(@Param("eventId") Long eventId);
}
