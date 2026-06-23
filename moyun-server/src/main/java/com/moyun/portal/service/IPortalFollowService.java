package com.moyun.portal.service;

import java.util.List;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;

import com.moyun.portal.domain.entity.PortalFollow;
import com.moyun.portal.domain.query.FollowQuery;

/**
 * 门户关注 业务层
 *
 * @author moyun
 */
public interface IPortalFollowService {

    /**
     * 根据条件分页查询关注列表
     *
     * @param page 分页参数
     * @param query 查询条件
     * @return 分页结果
     */
    Page<PortalFollow> selectPortalFollowPage(Page<PortalFollow> page, FollowQuery query);

    /**
     * 根据条件查询关注列表（不分页，用于导出等场景）
     *
     * @param query 查询条件
     * @return 关注信息集合
     */
    List<PortalFollow> selectPortalFollowList(FollowQuery query);

    /**
     * 通过关注ID查询关注
     *
     * @param id 关注ID
     * @return 关注对象信息
     */
    public PortalFollow selectPortalFollowById(Long id);

    /**
     * 新增关注信息
     *
     * @param portalFollow 关注信息
     * @return 结果
     */
    public int insertPortalFollow(PortalFollow portalFollow);

    /**
     * 通过关注ID删除关注
     *
     * @param id 关注ID
     * @return 结果
     */
    public int deletePortalFollowById(Long id);

    /**
     * 修改关注信息
     *
     * @param portalFollow 关注信息
     * @return 结果
     */
    public int updatePortalFollow(PortalFollow portalFollow);

    /**
     * 批量删除关注信息
     *
     * @param ids 需要删除的关注ID
     * @return 结果
     */
    public int deletePortalFollowByIds(Long[] ids);

    /**
     * 切换关注状态（关注/取消关注）
     *
     * 同步更新 portal_user_stats 中的粉丝数和关注数，
     * 并为被关注者记录 receive_follow 成长事件。
     *
     * @param followerId  关注者ID（当前登录用户）
     * @param followingId 被关注者ID
     * @return 包含 followed（是否已关注）和 followerCount（被关注者粉丝数）的 Map
     */
    java.util.Map<String, Object> toggleFollow(Long followerId, Long followingId);

    /**
     * 检查当前用户是否已关注目标用户
     *
     * @param followerId  关注者ID
     * @param followingId 被关注者ID
     * @return true=已关注
     */
    boolean isFollowing(Long followerId, Long followingId);
}
