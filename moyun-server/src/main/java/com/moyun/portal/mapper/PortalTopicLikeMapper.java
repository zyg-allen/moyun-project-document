package com.moyun.portal.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Delete;

import com.moyun.portal.domain.entity.PortalTopicLike;

/**
 * 话题点赞 数据层
 *
 * @author moyun
 */
@Mapper
public interface PortalTopicLikeMapper extends BaseMapper<PortalTopicLike> {

    /**
     * 查询用户是否已点赞某话题
     */
    @Select("SELECT * FROM portal_topic_like WHERE topic_id = #{topicId} AND user_id = #{userId} LIMIT 1")
    PortalTopicLike selectByTopicAndUser(@Param("topicId") Long topicId, @Param("userId") Long userId);

    /**
     * 删除点赞记录（取消点赞）
     */
    @Delete("DELETE FROM portal_topic_like WHERE topic_id = #{topicId} AND user_id = #{userId}")
    int deleteByTopicAndUser(@Param("topicId") Long topicId, @Param("userId") Long userId);
}
