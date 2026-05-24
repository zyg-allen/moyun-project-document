package com.moyun.community.content.controller.app;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.moyun.common.utils.SecurityUtils;
import com.moyun.community.common.domain.ApiResponse;
import com.moyun.community.common.domain.PageResult;
import com.moyun.community.content.entity.UserProfile;
import com.moyun.community.content.mapper.UserProfileMapper;
import com.moyun.community.content.model.vo.PortalFollowVo;
import com.moyun.common.core.domain.entity.SysUser;
import com.moyun.system.mapper.SysUserMapper;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 前台关注 Controller
 *
 * @author moyun
 */
@Tag(name = "关注模块", description = "用户关注管理接口")
@RestController
@RequestMapping("/api/follow")
@RequiredArgsConstructor
public class PortalFollowController {

    private final SysUserMapper sysUserMapper;
    private final UserProfileMapper userProfileMapper;

    // 模拟关注关系存储（实际应该有数据库表）
    private static final Map<Long, List<Long>> FOLLOW_RELATIONS = new HashMap<>();

    /**
     * 关注用户
     */
    @Operation(summary = "关注用户", description = "关注某个用户")
    @PostMapping("/{targetUserId}")
    public ApiResponse<Void> followUser(@PathVariable Long targetUserId) {
        Long userId = SecurityUtils.getUserId();
        if (userId == null) {
            return ApiResponse.error(401, "未登录");
        }

        if (userId.equals(targetUserId)) {
            return ApiResponse.error("不能关注自己");
        }

        // 检查是否已关注
        List<Long> following = FOLLOW_RELATIONS.getOrDefault(userId, new ArrayList<>());
        if (following.contains(targetUserId)) {
            return ApiResponse.error("已经关注该用户");
        }

        following.add(targetUserId);
        FOLLOW_RELATIONS.put(userId, following);

        return ApiResponse.success();
    }

    /**
     * 取消关注
     */
    @Operation(summary = "取消关注", description = "取消关注某个用户")
    @DeleteMapping("/{targetUserId}")
    public ApiResponse<Void> unfollowUser(@PathVariable Long targetUserId) {
        Long userId = SecurityUtils.getUserId();
        if (userId == null) {
            return ApiResponse.error(401, "未登录");
        }

        List<Long> following = FOLLOW_RELATIONS.getOrDefault(userId, new ArrayList<>());
        following.remove(targetUserId);
        FOLLOW_RELATIONS.put(userId, following);

        return ApiResponse.success();
    }

    /**
     * 检查是否已关注
     */
    @Operation(summary = "检查关注状态", description = "检查当前用户是否已关注某个用户")
    @GetMapping("/check/{targetUserId}")
    public ApiResponse<Boolean> checkFollow(@PathVariable Long targetUserId) {
        Long userId = SecurityUtils.getUserId();
        if (userId == null) {
            return ApiResponse.success(false);
        }

        List<Long> following = FOLLOW_RELATIONS.getOrDefault(userId, new ArrayList<>());
        boolean isFollowing = following.contains(targetUserId);

        return ApiResponse.success(isFollowing);
    }

    /**
     * 获取粉丝列表
     */
    @Operation(summary = "获取粉丝列表", description = "获取某个用户的粉丝列表")
    @GetMapping("/{targetUserId}/followers")
    public ApiResponse<PageResult<PortalFollowVo>> getFollowers(
            @PathVariable Long targetUserId,
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "10") Integer pageSize) {

        List<Long> followers = new ArrayList<>();
        for (Map.Entry<Long, List<Long>> entry : FOLLOW_RELATIONS.entrySet()) {
            if (entry.getValue().contains(targetUserId)) {
                followers.add(entry.getKey());
            }
        }

        // 分页处理
        int total = followers.size();
        int start = (page - 1) * pageSize;
        int end = Math.min(start + pageSize, followers.size());
        List<Long> pageFollowers = followers.subList(start, end);

        List<PortalFollowVo> voList = new ArrayList<>();
        Long currentUserId = SecurityUtils.getUserId();

        for (Long followerId : pageFollowers) {
            PortalFollowVo vo = new PortalFollowVo();
            vo.setId(followerId);
            vo.setUserId(followerId);
            vo.setTargetUserId(targetUserId);

            SysUser user = sysUserMapper.selectById(followerId);
            UserProfile profile = userProfileMapper.selectByUserId(followerId);

            if (user != null) {
                vo.setUserNickname(user.getNickName());
            }
            if (profile != null) {
                vo.setUserAvatar(profile.getAvatar());
                vo.setUserBio(profile.getBio());
            }

            if (currentUserId != null) {
                List<Long> currentFollowing = FOLLOW_RELATIONS.getOrDefault(currentUserId, new ArrayList<>());
                vo.setIsFollowing(currentFollowing.contains(followerId));
            } else {
                vo.setIsFollowing(false);
            }

            voList.add(vo);
        }

        PageResult<PortalFollowVo> pageResult = PageResult.of(voList, (long) total, page, pageSize);
        return ApiResponse.success(pageResult);
    }

    /**
     * 获取关注列表
     */
    @Operation(summary = "获取关注列表", description = "获取某个用户的关注列表")
    @GetMapping("/{targetUserId}/following")
    public ApiResponse<PageResult<PortalFollowVo>> getFollowing(
            @PathVariable Long targetUserId,
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "10") Integer pageSize) {

        List<Long> following = FOLLOW_RELATIONS.getOrDefault(targetUserId, new ArrayList<>());

        // 分页处理
        int total = following.size();
        int start = (page - 1) * pageSize;
        int end = Math.min(start + pageSize, following.size());
        List<Long> pageFollowing = following.subList(start, end);

        List<PortalFollowVo> voList = new ArrayList<>();
        Long currentUserId = SecurityUtils.getUserId();

        for (Long followId : pageFollowing) {
            PortalFollowVo vo = new PortalFollowVo();
            vo.setId(followId);
            vo.setUserId(targetUserId);
            vo.setTargetUserId(followId);

            SysUser user = sysUserMapper.selectById(followId);
            UserProfile profile = userProfileMapper.selectByUserId(followId);

            if (user != null) {
                vo.setUserNickname(user.getNickName());
            }
            if (profile != null) {
                vo.setUserAvatar(profile.getAvatar());
                vo.setUserBio(profile.getBio());
            }

            if (currentUserId != null) {
                List<Long> currentFollowing = FOLLOW_RELATIONS.getOrDefault(currentUserId, new ArrayList<>());
                vo.setIsFollowing(currentFollowing.contains(followId));
            } else {
                vo.setIsFollowing(false);
            }

            voList.add(vo);
        }

        PageResult<PortalFollowVo> pageResult = PageResult.of(voList, (long) total, page, pageSize);
        return ApiResponse.success(pageResult);
    }

    /**
     * 初始化一些测试数据
     */
    static {
        // 测试用户 2 关注用户 1
        List<Long> following = new ArrayList<>();
        following.add(1L);
        FOLLOW_RELATIONS.put(2L, following);
    }
}
