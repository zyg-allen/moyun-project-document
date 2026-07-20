package com.moyun.portal.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import com.moyun.portal.domain.entity.PortalTopicPost;

/**
 * 话题观点（楼层） 数据层
 *
 * @author moyun
 */
@Mapper
public interface PortalTopicPostMapper extends BaseMapper<PortalTopicPost> {

    /**
     * 原子增加观点点赞数
     */
    @Update("UPDATE portal_topic_post SET like_count = like_count + #{delta} WHERE id = #{id} AND like_count + #{delta} >= 0")
    int incrementLikeCount(@Param("id") Long id, @Param("delta") int delta);

    /**
     * 原子增加观点评论数（仅一级评论计入）
     */
    @Update("UPDATE portal_topic_post SET comment_count = comment_count + #{delta} WHERE id = #{id} AND comment_count + #{delta} >= 0")
    int incrementCommentCount(@Param("id") Long id, @Param("delta") int delta);

    /**
     * 软删观点
     */
    @Update("UPDATE portal_topic_post SET is_deleted = 1 WHERE id = #{id} AND is_deleted = 0")
    int softDelete(@Param("id") Long id);

    /**
     * 按话题级联软删所有观点（用于话题删除时同步软删其下观点）
     */
    @Update("UPDATE portal_topic_post SET is_deleted = 1 WHERE topic_id = #{topicId} AND is_deleted = 0")
    int softDeleteByTopic(@Param("topicId") Long topicId);

    /**
     * 查询当前话题下一楼层号（基于 MAX(floor) + 1）
     * 配合 portal_topic 行级锁可保证楼层号不重复
     */
    @Select("SELECT COALESCE(MAX(floor), 0) + 1 FROM portal_topic_post WHERE topic_id = #{topicId}")
    Integer selectNextFloor(@Param("topicId") Long topicId);
}
