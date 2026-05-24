package com.moyun.community.auth.controller.app;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.moyun.common.core.domain.entity.SysUser;
import com.moyun.common.utils.SecurityUtils;
import com.moyun.community.auth.model.dto.LoginDto;
import com.moyun.community.auth.model.dto.RegisterDto;
import com.moyun.community.auth.model.vo.LoginResponseVo;
import com.moyun.community.auth.model.vo.PortalUserVo;
import com.moyun.community.auth.service.AuthService;
import com.moyun.community.common.domain.ApiResponse;
import com.moyun.community.content.entity.UserProfile;
import com.moyun.community.content.mapper.UserProfileMapper;
import com.moyun.framework.web.service.SysLoginService;
import com.moyun.system.mapper.SysUserMapper;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Map;

/**
 * 前台用户管理Controller
 *
 * @author moyun
 */
@Tag(name = "用户模块", description = "用户登录、注册、个人信息等接口")
@RestController
@RequestMapping("/api/user")
@RequiredArgsConstructor
public class UserController {

    private final SysLoginService loginService;
    private final SysUserMapper sysUserMapper;
    private final UserProfileMapper userProfileMapper;
    private final PasswordEncoder passwordEncoder;

    /**
     * 用户登录
     */
    @Operation(summary = "用户登录", description = "用户登录，返回Token和用户信息")
    @PostMapping("/login")
    public ApiResponse<LoginResponseVo> login(
            @Parameter(description = "登录信息", required = true)
            @RequestBody LoginDto dto) {
        String token = loginService.login(dto.getUsername(), dto.getPassword(), null, null);

        SysUser sysUser = sysUserMapper.selectUserByUserName(dto.getUsername());
        PortalUserVo userVo = convertToPortalUserVo(sysUser);

        LoginResponseVo response = LoginResponseVo.builder()
                .token(token)
                .refreshToken(token)
                .user(userVo)
                .build();

        return ApiResponse.success(response);
    }

    /**
     * 用户注册
     */
    @Operation(summary = "用户注册", description = "新用户注册账号")
    @PostMapping("/register")
    @Transactional
    public ApiResponse<LoginResponseVo> register(
            @Parameter(description = "注册信息", required = true)
            @RequestBody RegisterDto dto) {
        if (!dto.getPassword().equals(dto.getConfirmPassword())) {
            return ApiResponse.error("两次密码输入不一致");
        }

        LambdaQueryWrapper<SysUser> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(SysUser::getUserName, dto.getUsername());
        if (sysUserMapper.selectCount(queryWrapper) > 0) {
            return ApiResponse.error("用户名已存在");
        }

        SysUser sysUser = new SysUser();
        sysUser.setUserName(dto.getUsername());
        sysUser.setNickName(dto.getNickname() != null ? dto.getNickname() : dto.getUsername());
        sysUser.setPassword(passwordEncoder.encode(dto.getPassword()));
        sysUser.setEmail(dto.getEmail());
        sysUser.setPhonenumber(dto.getPhonenumber());
        sysUser.setStatus("0");
        sysUser.setCreateTime(LocalDateTime.now());
        sysUserMapper.insert(sysUser);

        UserProfile profile = new UserProfile();
        profile.setUserId(sysUser.getUserId());
        profile.setNickname(dto.getNickname() != null ? dto.getNickname() : dto.getUsername());
        profile.setPoints(0);
        profile.setLevel(1);
        profile.setInkBalance(java.math.BigDecimal.ZERO);
        profile.setArticleCount(0);
        profile.setFollowerCount(0);
        profile.setFollowingCount(0);
        profile.setLikeCount(0L);
        profile.setIsAuthor(0);
        profile.setAuthorLevel(0);
        profile.setCreateTime(LocalDateTime.now());
        profile.setUpdateTime(LocalDateTime.now());
        userProfileMapper.insert(profile);

        String token = loginService.login(dto.getUsername(), dto.getPassword(), null, null);
        PortalUserVo userVo = convertToPortalUserVo(sysUser);

        LoginResponseVo response = LoginResponseVo.builder()
                .token(token)
                .refreshToken(token)
                .user(userVo)
                .build();

        return ApiResponse.success(response);
    }

    /**
     * 退出登录
     */
    @Operation(summary = "退出登录", description = "退出登录，清除Token")
    @PostMapping("/logout")
    public ApiResponse<Void> logout() {
        return ApiResponse.success();
    }

    /**
     * 获取当前用户信息
     */
    @Operation(summary = "获取当前用户", description = "获取当前登录用户的详细信息")
    @GetMapping("/current")
    public ApiResponse<PortalUserVo> getCurrentUser() {
        Long userId = SecurityUtils.getUserId();
        if (userId == null) {
            return ApiResponse.error(401, "未登录");
        }

        SysUser sysUser = sysUserMapper.selectUserById(userId);
        PortalUserVo userVo = convertToPortalUserVo(sysUser);

        return ApiResponse.success(userVo);
    }

    /**
     * 获取用户详情
     */
    @Operation(summary = "获取用户详情", description = "根据用户ID获取用户信息")
    @GetMapping("/{userId}")
    public ApiResponse<PortalUserVo> getUserById(
            @Parameter(description = "用户ID", required = true)
            @PathVariable String userId) {
        try {
            Long id = Long.parseLong(userId);
            SysUser sysUser = sysUserMapper.selectUserById(id);
            if (sysUser == null) {
                return ApiResponse.error(404, "用户不存在");
            }
            PortalUserVo userVo = convertToPortalUserVo(sysUser);
            return ApiResponse.success(userVo);
        } catch (NumberFormatException e) {
            return ApiResponse.error("无效的用户ID");
        }
    }

    /**
     * 获取用户统计信息
     */
    @Operation(summary = "获取用户统计", description = "获取用户统计数据")
    @GetMapping({"/stats", "/{userId}/stats"})
    public ApiResponse<PortalUserVo.UserStats> getUserStats(
            @Parameter(description = "用户ID")
            @PathVariable(required = false) String userId) {
        Long targetUserId;
        if (userId != null) {
            try {
                targetUserId = Long.parseLong(userId);
            } catch (NumberFormatException e) {
                return ApiResponse.error("无效的用户ID");
            }
        } else {
            targetUserId = SecurityUtils.getUserId();
            if (targetUserId == null) {
                return ApiResponse.error(401, "未登录");
            }
        }

        UserProfile profile = userProfileMapper.selectByUserId(targetUserId);
        PortalUserVo.UserStats stats = new PortalUserVo.UserStats();
        if (profile != null) {
            stats.setArticlesCount(profile.getArticleCount());
            stats.setFollowersCount(profile.getFollowerCount());
            stats.setFollowingCount(profile.getFollowingCount());
            stats.setLikesCount(profile.getLikeCount() != null ? profile.getLikeCount().intValue() : 0);
            stats.setViewsCount(0);
            stats.setBookmarksCount(0);
            stats.setTodayVisitors(0);
            stats.setTotalVisitors(0);
        }

        return ApiResponse.success(stats);
    }

    /**
     * 转换为前台用户VO
     */
    private PortalUserVo convertToPortalUserVo(SysUser sysUser) {
        PortalUserVo vo = new PortalUserVo();
        vo.setId(String.valueOf(sysUser.getUserId()));
        vo.setUsername(sysUser.getUserName());
        vo.setNickname(sysUser.getNickName());
        vo.setEmail(sysUser.getEmail());
        vo.setPhone(sysUser.getPhonenumber());
        vo.setRole("user");
        vo.setCreatedAt(sysUser.getCreateTime() != null 
                ? sysUser.getCreateTime().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME) 
                : null);
        vo.setUpdatedAt(sysUser.getUpdateTime() != null 
                ? sysUser.getUpdateTime().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME) 
                : null);
        vo.setIsPhoneVerified(sysUser.getPhonenumber() != null);
        vo.setIsWechatVerified(false);
        vo.setTwoFactorEnabled(false);

        UserProfile profile = userProfileMapper.selectByUserId(sysUser.getUserId());
        if (profile != null) {
            vo.setAvatar(profile.getAvatar());
            vo.setBio(profile.getBio());
            vo.setNickname(profile.getNickname());

            PortalUserVo.UserStats stats = new PortalUserVo.UserStats();
            stats.setArticlesCount(profile.getArticleCount());
            stats.setFollowersCount(profile.getFollowerCount());
            stats.setFollowingCount(profile.getFollowingCount());
            stats.setLikesCount(profile.getLikeCount() != null ? profile.getLikeCount().intValue() : 0);
            stats.setViewsCount(0);
            stats.setBookmarksCount(0);
            stats.setTodayVisitors(0);
            stats.setTotalVisitors(0);
            vo.setStats(stats);
        }

        return vo;
    }
}
