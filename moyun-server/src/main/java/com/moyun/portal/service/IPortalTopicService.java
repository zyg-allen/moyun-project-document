package com.moyun.portal.service;

import java.util.Map;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;

import com.moyun.portal.domain.entity.PortalTopic;
import com.moyun.portal.domain.vo.TopicListVO;
import com.moyun.portal.domain.vo.TopicVO;

/**
 * 话题 服务层
 *
 * @author moyun
 */
public interface IPortalTopicService extends IService<PortalTopic> {

    /**
     * 话题分页列表
     *
     * @param pageNum  页码
     * @param pageSize 每页数量
     * @param sort     排序：latest/hot/active
     * @param keyword  标题关键词（可选）
     * @return 分页结果
     */
    Page<TopicListVO> getTopicList(Integer pageNum, Integer pageSize, String sort, String keyword);

    /**
     * 话题详情（含 creator、stats、isLiked、isOwner）
     *
     * @param id            话题ID
     * @param currentUserId 当前登录用户ID（可为 null）
     * @return 话题详情 VO
     */
    TopicVO getTopicDetail(Long id, Long currentUserId);

    /**
     * 创建话题（校验 is_certified_creator=1，触发成长事件 create_topic，发布 Feed 事件 create_topic）
     *
     * @param topic  话题对象（title/description/cover 由前端传入）
     * @param userId 当前登录用户ID
     * @return 创建后的话题对象（含 id）
     */
    PortalTopic createTopic(PortalTopic topic, Long userId);

    /**
     * 编辑话题（仅 creator 可编辑）
     *
     * @param id     话题ID
     * @param topic  待更新字段
     * @param userId 当前登录用户ID
     * @return 更新后的话题对象
     */
    PortalTopic updateTopic(Long id, PortalTopic topic, Long userId);

    /**
     * 删除话题（creator 或 admin，软删 status=deleted）
     *
     * @param id     话题ID
     * @param userId 当前登录用户ID
     */
    void deleteTopic(Long id, Long userId);

    /**
     * 话题点赞/取消（幂等，触发成长事件 receive_topic_like）
     *
     * @param id     话题ID
     * @param userId 当前登录用户ID
     * @return 含 isLiked 和 likeCount
     */
    Map<String, Object> toggleTopicLike(Long id, Long userId);

    /**
     * 浏览数 +1
     *
     * @param id 话题ID
     */
    void incrementViewCount(Long id);

    /**
     * CMS 分页查询话题（所有状态）
     *
     * @param pageNum  页码
     * @param pageSize 每页数量
     * @param keyword  标题关键词（可选）
     * @param status   状态筛选（可选）
     * @return 分页结果
     */
    Page<TopicListVO> getCmsTopicList(Integer pageNum, Integer pageSize, String keyword, String status);

    /**
     * CMS 更新话题状态（active/archived/deleted）
     *
     * @param id     话题ID
     * @param status 新状态
     */
    void updateTopicStatus(Long id, String status);

    /**
     * CMS 置顶/取消置顶
     *
     * @param id     话题ID
     * @param pinned 0/1
     */
    void updateTopicPinned(Long id, Integer pinned);

    /**
     * CMS 精选话题（触发 topic_featured 成长事件）
     *
     * @param id 话题ID
     */
    void featureTopic(Long id);

    /**
     * 当前用户发起的话题分页
     *
     * @param pageNum  页码
     * @param pageSize 每页数量
     * @param userId   当前登录用户ID
     * @return 分页结果
     */
    Page<TopicListVO> getMyTopics(Integer pageNum, Integer pageSize, Long userId);
}
