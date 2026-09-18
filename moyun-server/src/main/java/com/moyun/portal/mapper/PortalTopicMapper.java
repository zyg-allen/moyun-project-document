package com.moyun.portal.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import com.moyun.portal.domain.entity.PortalTopic;

/**
 * 话题主表 数据层
 *
 * @author moyun
 */
@Mapper
public interface PortalTopicMapper extends BaseMapper<PortalTopic> {

    /**
     * 原子增加浏览数
     */

    int incrementViewCount(@Param("id") Long id, @Param("delta") int delta);

    /**
     * 原子增加观点数 + 同步最后观点时间/用户
     */

    int incrementPostCount(@Param("id") Long id, @Param("delta") int delta,
                           @Param("postTime") java.time.LocalDateTime postTime,
                           @Param("posterId") Long posterId);

    /**
     * 仅原子减少观点数，不触碰 last_post_time / last_poster_id
     * 用于删除观点时仅同步计数，保留最后观点时间/用户信息
     */

    int decrementPostCount(@Param("topicId") Long topicId);

    /**
     * 标记话题为精选（is_featured = 1）
     */

    int markFeatured(@Param("topicId") Long topicId);

    /**
     * 原子增加点赞数
     */

    int incrementLikeCount(@Param("id") Long id, @Param("delta") int delta);

    /**
     * 原子增加评论数（仅一级评论计入）
     */

    int incrementCommentCount(@Param("id") Long id, @Param("delta") int delta);

    /**
     * 行级锁：SELECT FOR UPDATE，用于 createPost 时获取楼层号的并发安全
     */

    PortalTopic selectForUpdate(@Param("id") Long id);
}
