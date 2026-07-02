package com.moyun.ext.cms.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.moyun.core.base.page.PageDomain;

import com.moyun.ext.cms.domain.vo.FeedEventVO;

/**
 * 动态/Feed 流 Service 接口（推拉结合，先实现"读时拉"模式）
 *
 * @author moyun
 */
public interface IFeedService {

    /**
     * 关注的人的动态（分页，读时拉模式：JOIN portal_follow）
     *
     * @param userId 当前登录用户ID
     * @param query  分页参数
     * @return 动态事件分页
     */
    Page<FeedEventVO> getFollowingFeed(Long userId, PageDomain query);

    /**
     * 全站热门动态（最近 7 天事件，按时间倒序，分页）
     *
     * @param query 分页参数
     * @return 动态事件分页
     */
    Page<FeedEventVO> getHotFeed(PageDomain query);

    /**
     * 发布动态事件：写入 portal_feed_event，并同步推送给粉丝收件箱（关注者数量 &lt; 1000 时同步写）。
     *
     * @param userId     事件发布者ID
     * @param eventType  事件类型：publish_article/publish_experience/new_column/checkin 等
     * @param targetType 目标类型：article/experience/column/book 等
     * @param targetId   目标对象ID
     * @param title      目标标题
     * @param summary    动态摘要
     * @param cover      封面图
     * @return 动态事件ID（失败返回 null）
     */
    Long publishEvent(Long userId, String eventType, String targetType, Long targetId,
                      String title, String summary, String cover);

    /**
     * 删除事件（如文章被删除）：同步清理 portal_feed_event 与 portal_feed_inbox
     *
     * @param targetType 目标类型
     * @param targetId   目标对象ID
     * @return 影响行数（删除的事件数）
     */
    int deleteEvent(String targetType, Long targetId);
}
