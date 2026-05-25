package com.moyun.util.security;

import com.moyun.common.constant.Constants;
import com.moyun.core.base.entity.SysRole;
import com.moyun.core.base.model.LoginUser;
import org.apache.commons.lang3.StringUtils;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

import java.util.List;

public class SecurityUtils {

    public static Long getUserId() {
        return getLoginUser().getUserId();
    }

    public static Long getDeptId() {
        return getLoginUser().getDeptId();
    }

    public static String getUsername() {
        return getLoginUser().getUsername();
    }

    public static LoginUser getLoginUser() {
        try {
            return (LoginUser) getAuthentication().getPrincipal();
        } catch (Exception e) {
            throw new RuntimeException("获取用户信息异常");
        }
    }

    public static Authentication getAuthentication() {
        return SecurityContextHolder.getContext().getAuthentication();
    }

    public static String encryptPassword(String password) {
        BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();
        return passwordEncoder.encode(password);
    }

    public static boolean matchesPassword(String rawPassword, String encodedPassword) {
        BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();
        return passwordEncoder.matches(rawPassword, encodedPassword);
    }

    public static boolean isAdmin(Long userId) {
        return userId != null && 1L == userId;
    }

    public static boolean hasPermission(String permission) {
        return hasPermission(getAuthentication(), permission);
    }

    public static boolean hasPermission(Authentication authentication, String permission) {
        if (authentication == null) {
            return false;
        }
        LoginUser loginUser = (LoginUser) authentication.getPrincipal();
        return loginUser.getPermissions() != null && loginUser.getPermissions().contains(permission);
    }

    public static boolean hasRole(String role) {
        return hasRole(getAuthentication(), role);
    }

    public static boolean hasRole(Authentication authentication, String role) {
        if (authentication == null) {
            return false;
        }
        LoginUser loginUser = (LoginUser) authentication.getPrincipal();
        List<SysRole> roles = loginUser.getUser().getRoles();
        if (roles == null || roles.isEmpty()) {
            return false;
        }
        for (SysRole sysRole : roles) {
            String roleKey = sysRole.getRoleKey();
            if (Constants.SUPER_ADMIN.equals(roleKey) || roleKey.equals(StringUtils.trim(role))) {
                return true;
            }
        }
        return false;
    }
}
