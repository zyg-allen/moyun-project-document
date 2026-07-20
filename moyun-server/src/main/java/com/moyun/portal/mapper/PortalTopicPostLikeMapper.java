package com.moyun.portal.mapper;

import java.util.List;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Delete;

import com.moyun.portal.domain.entity.PortalTopicPostLike;

/**
 * 话题观点点赞 数据层
 *
 * @author moyun
 */
@Mapper
public interface PortalTopicPostLikeMapper extends BaseMapper<PortalTopicPostLike> {

    /**
     * 查询用户是否已点赞某观点
     */
    @Select("SELECT * FROM portal_topic_post_like WHERE post_id = #{postId} AND user_id = #{userId} LIMIT 1")
    PortalTopicPostLike selectByPostAndUser(@Param("postId") Long postId, @Param("userId") Long userId);

    /**
     * 删除点赞记录（取消点赞）
     */
    @Delete("DELETE FROM portal_topic_post_like WHERE post_id = #{postId} AND user_id = #{userId}")
    int deleteByPostAndUser(@Param("postId") Long postId, @Param("userId") Long userId);

    /**
     * 批量查询当前用户已点赞的观点 ID 集合
     */
    @Select("<script>SELECT post_id FROM portal_topic_post_like WHERE user_id = #{userId} AND post_id IN " +
            "<foreach item='id' collection='postIds' open='(' separator=',' close=')'>#{id}</foreach></script>")
    List<Long> selectLikedByUser(@Param("userId") Long userId, @Param("postIds") List<Long> postIds);
}
