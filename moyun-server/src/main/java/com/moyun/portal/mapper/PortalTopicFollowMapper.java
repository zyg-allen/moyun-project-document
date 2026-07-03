package com.moyun.portal.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.moyun.portal.domain.entity.PortalTopicFollow;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

/**
 * 话题关注 Mapper
 *
 * @author moyun
 */
@Mapper
public interface PortalTopicFollowMapper extends BaseMapper<PortalTopicFollow> {

    /**
     * 查询用户对某话题的关注记录（用于判断是否已关注）
     */
    @Select("SELECT * FROM portal_topic_follow WHERE topic_id = #{topicId} AND user_id = #{userId}")
    PortalTopicFollow selectByTopicAndUser(@Param("topicId") Long topicId, @Param("userId") Long userId);
}
