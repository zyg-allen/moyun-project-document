package com.moyun.portal.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

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
    @Update("UPDATE portal_topic SET view_count = view_count + #{delta} WHERE id = #{id} AND view_count + #{delta} >= 0")
    int incrementViewCount(@Param("id") Long id, @Param("delta") int delta);

    /**
     * 原子增加观点数 + 同步最后观点时间/用户
     */
    @Update("UPDATE portal_topic SET post_count = post_count + #{delta}, last_post_time = #{postTime}, last_poster_id = #{posterId} " +
            "WHERE id = #{id} AND post_count + #{delta} >= 0")
    int incrementPostCount(@Param("id") Long id, @Param("delta") int delta,
                           @Param("postTime") java.time.LocalDateTime postTime,
                           @Param("posterId") Long posterId);

    /**
     * 仅原子减少观点数，不触碰 last_post_time / last_poster_id
     * 用于删除观点时仅同步计数，保留最后观点时间/用户信息
     */
    @Update("UPDATE portal_topic SET post_count = GREATEST(0, post_count - 1) WHERE id = #{topicId}")
    int decrementPostCount(@Param("topicId") Long topicId);

    /**
     * 标记话题为精选（is_featured = 1）
     */
    @Update("UPDATE portal_topic SET is_featured = 1 WHERE id = #{topicId}")
    int markFeatured(@Param("topicId") Long topicId);

    /**
     * 原子增加点赞数
     */
    @Update("UPDATE portal_topic SET like_count = like_count + #{delta} WHERE id = #{id} AND like_count + #{delta} >= 0")
    int incrementLikeCount(@Param("id") Long id, @Param("delta") int delta);

    /**
     * 原子增加评论数（仅一级评论计入）
     */
    @Update("UPDATE portal_topic SET comment_count = comment_count + #{delta} WHERE id = #{id} AND comment_count + #{delta} >= 0")
    int incrementCommentCount(@Param("id") Long id, @Param("delta") int delta);

    /**
     * 行级锁：SELECT FOR UPDATE，用于 createPost 时获取楼层号的并发安全
     */
    @Select("SELECT * FROM portal_topic WHERE id = #{id} FOR UPDATE")
    PortalTopic selectForUpdate(@Param("id") Long id);
}
