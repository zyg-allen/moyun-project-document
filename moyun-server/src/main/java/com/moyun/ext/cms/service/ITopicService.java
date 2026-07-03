package com.moyun.ext.cms.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.moyun.core.base.page.PageDomain;
import com.moyun.ext.cms.domain.query.TopicQuery;
import com.moyun.ext.cms.domain.vo.TopicListItemVO;
import com.moyun.ext.cms.domain.vo.TopicPostVO;
import com.moyun.ext.cms.domain.vo.TopicVO;
import com.moyun.portal.domain.entity.PortalTopic;

/**
 * 话题/超话 Service 接口（社交深化与商业化 4.2）
 *
 * @author moyun
 */
public interface ITopicService {

    /**
     * 话题列表（公开，分页，仅 active）
     */
    Page<TopicListItemVO> listTopics(TopicQuery query);

    /**
     * 热门话题（公开，分页，按关注数倒序）
     */
    Page<TopicListItemVO> hotTopics(PageDomain query);

    /**
     * 话题详情（公开，含当前用户是否关注）
     *
     * @param slug          话题别名
     * @param currentUserId 当前登录用户ID（未登录传 null）
     */
    TopicVO getTopicDetail(String slug, Long currentUserId);

    /**
     * 话题下的动态（基于 portal_entity_tag 聚合带该话题标签的文章）
     */
    Page<TopicPostVO> listTopicPosts(String slug, PageDomain query);

    /**
     * 关注/取消关注话题（toggle，原子更新关注数）
     *
     * @return 操作后的关注状态：true=已关注，false=已取消
     */
    boolean toggleFollow(Long topicId, Long userId);

    // ==================== 后台管理 ====================

    /**
     * 后台话题分页（含所有状态）
     */
    Page<TopicListItemVO> cmsListTopics(TopicQuery query);

    /**
     * 后台新增话题
     *
     * @return 话题ID
     */
    Long cmsAddTopic(PortalTopic topic);

    /**
     * 后台修改话题
     *
     * @return 影响行数
     */
    int cmsUpdateTopic(PortalTopic topic);

    /**
     * 后台删除话题（级联删除关注）
     *
     * @return 影响行数
     */
    int cmsDeleteTopic(Long id);
}
