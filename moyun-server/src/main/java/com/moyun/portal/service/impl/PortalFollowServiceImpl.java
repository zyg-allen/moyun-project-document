package com.moyun.portal.service.impl;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.moyun.portal.domain.entity.PortalFollow;
import com.moyun.portal.domain.query.FollowQuery;
import com.moyun.portal.mapper.PortalFollowMapper;
import com.moyun.portal.mapper.PortalUserStatsMapper;
import com.moyun.portal.service.IPortalFollowService;
import com.moyun.portal.service.IPortalGrowthService;

/**
 * 门户关注 业务层处理
 *
 * @author moyun
 */
@Slf4j
@Service
public class PortalFollowServiceImpl extends ServiceImpl<PortalFollowMapper, PortalFollow> implements IPortalFollowService {

    @Autowired
    private PortalFollowMapper portalFollowMapper;

    @Autowired
    private PortalUserStatsMapper userStatsMapper;

    @Autowired
    private IPortalGrowthService portalGrowthService;

    /**
     * 根据条件分页查询关注列表
     *
     * @param page 分页参数
     * @param query 查询条件
     * @return 分页结果
     */
    @Override
    public Page<PortalFollow> selectPortalFollowPage(Page<PortalFollow> page, FollowQuery query) {
        return baseMapper.selectPortalFollowPage(page, query);
    }

    /**
     * 根据条件查询关注列表（不分页，用于导出等场景）
     *
     * @param query 查询条件
     * @return 关注信息集合
     */
    @Override
    public List<PortalFollow> selectPortalFollowList(FollowQuery query) {
        return baseMapper.selectPortalFollowList(query);
    }

    /**
     * 通过关注ID查询关注
     *
     * @param id 关注ID
     * @return 关注对象信息
     */
    @Override
    public PortalFollow selectPortalFollowById(Long id) {
        return portalFollowMapper.selectPortalFollowById(id);
    }

    /**
     * 新增关注信息
     *
     * @param portalFollow 关注信息
     * @return 结果
     */
    @Override
    public int insertPortalFollow(PortalFollow portalFollow) {
        return portalFollowMapper.insertPortalFollow(portalFollow);
    }

    /**
     * 修改关注信息
     *
     * @param portalFollow 关注信息
     * @return 结果
     */
    @Override
    public int updatePortalFollow(PortalFollow portalFollow) {
        return portalFollowMapper.updatePortalFollow(portalFollow);
    }

    /**
     * 通过关注ID删除关注
     *
     * @param id 关注ID
     * @return 结果
     */
    @Override
    public int deletePortalFollowById(Long id) {
        return portalFollowMapper.deletePortalFollowById(id);
    }

    /**
     * 批量删除关注信息
     *
     * @param ids 需要删除的关注ID
     * @return 结果
     */
    @Override
    public int deletePortalFollowByIds(Long[] ids) {
        return portalFollowMapper.deletePortalFollowByIds(ids);
    }

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
    @Override
    @Transactional(rollbackFor = Exception.class)
    public Map<String, Object> toggleFollow(Long followerId, Long followingId) {
        Map<String, Object> result = new HashMap<>();
        if (followerId == null || followingId == null) {
            result.put("followed", false);
            result.put("message", "参数不能为空");
            return result;
        }
        if (followerId.equals(followingId)) {
            result.put("followed", false);
            result.put("message", "不能关注自己");
            return result;
        }

        // 确保双方统计记录存在
        userStatsMapper.insertIfNotExists(followerId);
        userStatsMapper.insertIfNotExists(followingId);

        // 查询是否已关注
        PortalFollow existing = baseMapper.selectOne(
                new LambdaQueryWrapper<PortalFollow>()
                        .eq(PortalFollow::getFollowerId, followerId)
                        .eq(PortalFollow::getFollowingId, followingId)
        );

        if (existing == null) {
            // 未关注 → 关注
            PortalFollow follow = new PortalFollow();
            follow.setFollowerId(followerId);
            follow.setFollowingId(followingId);
            follow.setCreateTime(LocalDateTime.now());
            baseMapper.insert(follow);

            // 原子更新统计：关注者关注数+1，被关注者粉丝数+1
            userStatsMapper.addFollowingCount(followerId, 1);
            userStatsMapper.addFollowerCount(followingId, 1);

            // 为被关注者记录成长事件
            portalGrowthService.recordEventWithTarget("article", "receive_follow",
                    followingId, followerId, "user", followerId);

            result.put("followed", true);
            result.put("message", "关注成功");
        } else {
            // 已关注 → 取消关注
            baseMapper.deleteById(existing.getId());

            // 原子更新统计：关注者关注数-1，被关注者粉丝数-1
            userStatsMapper.addFollowingCount(followerId, -1);
            userStatsMapper.addFollowerCount(followingId, -1);

            result.put("followed", false);
            result.put("message", "已取消关注");
        }

        // 返回被关注者最新粉丝数
        com.moyun.portal.domain.entity.PortalUserStats stats = userStatsMapper.selectByUserId(followingId);
        result.put("followerCount", stats != null && stats.getFollowerCount() != null ? stats.getFollowerCount() : 0);
        return result;
    }

    /**
     * 检查当前用户是否已关注目标用户
     *
     * @param followerId  关注者ID
     * @param followingId 被关注者ID
     * @return true=已关注
     */
    @Override
    public boolean isFollowing(Long followerId, Long followingId) {
        if (followerId == null || followingId == null) {
            return false;
        }
        Long count = baseMapper.selectCount(
                new LambdaQueryWrapper<PortalFollow>()
                        .eq(PortalFollow::getFollowerId, followerId)
                        .eq(PortalFollow::getFollowingId, followingId)
        );
        return count != null && count > 0;
    }

    /**
     * 关注用户（幂等：若已关注则直接返回已关注状态，不重复插入）
     *
     * @param followerId  关注者ID（当前登录用户）
     * @param followingId 被关注者ID
     * @return 包含 followed、followerCount、message 的结果 Map
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public Map<String, Object> follow(Long followerId, Long followingId) {
        Map<String, Object> result = new HashMap<>();
        if (followerId == null || followingId == null) {
            result.put("followed", false);
            result.put("message", "参数不能为空");
            return result;
        }
        if (followerId.equals(followingId)) {
            result.put("followed", false);
            result.put("message", "不能关注自己");
            return result;
        }

        // 已关注则幂等返回
        if (isFollowing(followerId, followingId)) {
            result.put("followed", true);
            result.put("message", "已关注");
            result.put("followerCount", getFollowerCount(followingId));
            return result;
        }

        // 确保双方统计记录存在
        userStatsMapper.insertIfNotExists(followerId);
        userStatsMapper.insertIfNotExists(followingId);

        // 插入关注记录
        PortalFollow follow = new PortalFollow();
        follow.setFollowerId(followerId);
        follow.setFollowingId(followingId);
        follow.setCreateTime(LocalDateTime.now());
        baseMapper.insert(follow);

        // 原子更新统计：关注者关注数+1，被关注者粉丝数+1
        userStatsMapper.addFollowingCount(followerId, 1);
        userStatsMapper.addFollowerCount(followingId, 1);

        // 为被关注者记录成长事件
        portalGrowthService.recordEventWithTarget("article", "receive_follow",
                followingId, followerId, "user", followerId);

        result.put("followed", true);
        result.put("message", "关注成功");
        result.put("followerCount", getFollowerCount(followingId));
        return result;
    }

    /**
     * 取消关注用户（幂等：若未关注则直接返回未关注状态，不报错）
     *
     * @param followerId  关注者ID（当前登录用户）
     * @param followingId 被关注者ID
     * @return 包含 followed、followerCount、message 的结果 Map
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public Map<String, Object> unfollow(Long followerId, Long followingId) {
        Map<String, Object> result = new HashMap<>();
        if (followerId == null || followingId == null) {
            result.put("followed", false);
            result.put("message", "参数不能为空");
            return result;
        }

        // 未关注则幂等返回
        if (!isFollowing(followerId, followingId)) {
            result.put("followed", false);
            result.put("message", "未关注");
            result.put("followerCount", getFollowerCount(followingId));
            return result;
        }

        // 删除关注记录
        baseMapper.delete(
                new LambdaQueryWrapper<PortalFollow>()
                        .eq(PortalFollow::getFollowerId, followerId)
                        .eq(PortalFollow::getFollowingId, followingId)
        );

        // 确保统计记录存在（防止取消关注时统计为空）
        userStatsMapper.insertIfNotExists(followerId);
        userStatsMapper.insertIfNotExists(followingId);

        // 原子更新统计：关注者关注数-1，被关注者粉丝数-1
        userStatsMapper.addFollowingCount(followerId, -1);
        userStatsMapper.addFollowerCount(followingId, -1);

        result.put("followed", false);
        result.put("message", "已取消关注");
        result.put("followerCount", getFollowerCount(followingId));
        return result;
    }

    /**
     * 查询指定用户的粉丝列表（分页，按关注时间倒序）
     */
    @Override
    public Page<PortalFollow> selectFollowerPage(Page<PortalFollow> page, Long userId) {
        return baseMapper.selectPage(page,
                new LambdaQueryWrapper<PortalFollow>()
                        .eq(PortalFollow::getFollowingId, userId)
                        .orderByDesc(PortalFollow::getCreateTime)
        );
    }

    /**
     * 查询指定用户的关注列表（分页，按关注时间倒序）
     */
    @Override
    public Page<PortalFollow> selectFollowingPage(Page<PortalFollow> page, Long userId) {
        return baseMapper.selectPage(page,
                new LambdaQueryWrapper<PortalFollow>()
                        .eq(PortalFollow::getFollowerId, userId)
                        .orderByDesc(PortalFollow::getCreateTime)
        );
    }

    /**
     * 获取指定用户的粉丝数（内部复用）
     */
    private Integer getFollowerCount(Long userId) {
        com.moyun.portal.domain.entity.PortalUserStats stats = userStatsMapper.selectByUserId(userId);
        return stats != null && stats.getFollowerCount() != null ? stats.getFollowerCount() : 0;
    }
}
