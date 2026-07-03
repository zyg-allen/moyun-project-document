package com.moyun.portal.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.moyun.ext.cms.domain.query.TopicQuery;
import com.moyun.ext.cms.domain.vo.TopicListItemVO;
import com.moyun.ext.cms.domain.vo.TopicPostVO;
import com.moyun.ext.cms.domain.vo.TopicVO;
import com.moyun.portal.domain.entity.PortalTopic;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Update;

/**
 * 话题 Mapper
 *
 * @author moyun
 */
@Mapper
public interface PortalTopicMapper extends BaseMapper<PortalTopic> {

    /**
     * 话题列表分页（公开，仅 active）
     */
    Page<TopicListItemVO> selectListPage(Page<TopicListItemVO> page, @Param("query") TopicQuery query);

    /**
     * 热门话题（按关注数倒序，默认 active）
     */
    Page<TopicListItemVO> selectHotPage(Page<TopicListItemVO> page);

    /**
     * 话题详情（按 slug 查询）
     */
    TopicVO selectDetailBySlug(@Param("slug") String slug);

    /**
     * 话题详情（按 ID 查询）
     */
    TopicVO selectDetailById(@Param("id") Long id);

    /**
     * 话题下的动态（基于 portal_entity_tag + portal_tag 聚合带该话题标签的文章）
     */
    Page<TopicPostVO> selectTopicPosts(Page<TopicPostVO> page, @Param("topicName") String topicName);

    /**
     * 后台话题分页查询（含所有状态）
     */
    Page<TopicListItemVO> selectCmsListPage(Page<TopicListItemVO> page, @Param("query") TopicQuery query);

    /**
     * 原子更新关注数
     */
    @Update("UPDATE portal_topic SET follow_count = GREATEST(follow_count + #{delta}, 0) WHERE id = #{id}")
    int updateFollowCount(@Param("id") Long id, @Param("delta") int delta);

    /**
     * 原子更新关联内容数
     */
    @Update("UPDATE portal_topic SET post_count = GREATEST(post_count + #{delta}, 0) WHERE id = #{id}")
    int updatePostCount(@Param("id") Long id, @Param("delta") int delta);
}
