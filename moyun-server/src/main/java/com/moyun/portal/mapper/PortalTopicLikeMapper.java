package com.moyun.portal.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

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

    PortalTopicLike selectByTopicAndUser(@Param("topicId") Long topicId, @Param("userId") Long userId);

    /**
     * 删除点赞记录（取消点赞）
     */

    int deleteByTopicAndUser(@Param("topicId") Long topicId, @Param("userId") Long userId);
}
