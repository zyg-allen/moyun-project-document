package com.moyun.community.auth.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.moyun.common.core.domain.entity.SysUser;
import com.moyun.common.utils.SecurityUtils;
import com.moyun.community.auth.model.dto.LoginDto;
import com.moyun.community.auth.model.dto.RegisterDto;
import com.moyun.community.auth.model.vo.UserVo;
import com.moyun.community.auth.service.AuthService;
import com.moyun.community.content.entity.UserProfile;
import com.moyun.community.content.mapper.UserProfileMapper;
import com.moyun.framework.web.service.SysLoginService;
import com.moyun.framework.web.service.TokenService;
import com.moyun.system.mapper.SysUserMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {

    private final SysLoginService loginService;
    private final TokenService tokenService;
    private final SysUserMapper sysUserMapper;
    private final UserProfileMapper userProfileMapper;
    private final PasswordEncoder passwordEncoder;

    @Override
    public Map<String, Object> login(LoginDto dto) {
        String token = loginService.login(dto.getUsername(), dto.getPassword(), null, null);

        SysUser sysUser = sysUserMapper.selectUserByUserName(dto.getUsername());
        UserProfile profile = userProfileMapper.selectByUserId(sysUser.getUserId());

        Map<String, Object> result = new HashMap<>();
        result.put("token", token);
        result.put("userId", sysUser.getUserId());
        result.put("username", sysUser.getUserName());
        result.put("nickname", profile != null ? profile.getNickname() : sysUser.getNickName());

        return result;
    }

    @Override
    @Transactional
    public Map<String, Object> register(RegisterDto dto) {
        if (!dto.getPassword().equals(dto.getConfirmPassword())) {
            throw new RuntimeException("两次密码输入不一致");
        }

        LambdaQueryWrapper<SysUser> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(SysUser::getUserName, dto.getUsername());
        if (sysUserMapper.selectCount(queryWrapper) > 0) {
            throw new RuntimeException("用户名已存在");
        }

        SysUser sysUser = new SysUser();
        sysUser.setUserName(dto.getUsername());
        sysUser.setNickName(dto.getNickname() != null ? dto.getNickname() : dto.getUsername());
        sysUser.setPassword(passwordEncoder.encode(dto.getPassword()));
        sysUser.setEmail(dto.getEmail());
        sysUser.setPhonenumber(dto.getPhonenumber());
        sysUser.setStatus("0");
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

        Map<String, Object> result = new HashMap<>();
        result.put("userId", sysUser.getUserId());
        result.put("username", sysUser.getUserName());
        result.put("success", true);

        return result;
    }

    @Override
    public void logout() {
        tokenService.delLoginUser(String.valueOf(SecurityUtils.getUserId()));
    }

    @Override
    public UserVo getCurrentUser() {
        Long userId = SecurityUtils.getUserId();
        if (userId == null) {
            throw new RuntimeException("用户未登录");
        }

        SysUser sysUser = sysUserMapper.selectUserById(userId);
        UserProfile profile = userProfileMapper.selectByUserId(userId);

        UserVo vo = new UserVo();
        vo.setUserId(sysUser.getUserId());
        vo.setUsername(sysUser.getUserName());
        vo.setNickname(profile != null ? profile.getNickname() : sysUser.getNickName());
        vo.setAvatar(profile != null ? profile.getAvatar() : null);
        vo.setEmail(sysUser.getEmail());
        vo.setPhonenumber(sysUser.getPhonenumber());

        if (profile != null) {
            vo.setGender(profile.getGender());
            vo.setBio(profile.getBio());
            vo.setPoints(profile.getPoints());
            vo.setLevel(profile.getLevel());
            vo.setInkBalance(profile.getInkBalance());
            vo.setArticleCount(profile.getArticleCount());
            vo.setFollowerCount(profile.getFollowerCount());
            vo.setFollowingCount(profile.getFollowingCount());
            vo.setLikeCount(profile.getLikeCount());
            vo.setIsAuthor(profile.getIsAuthor());
            vo.setAuthorLevel(profile.getAuthorLevel());
        }

        return vo;
    }
}
