package com.moyun.common.utils;

import com.moyun.common.constant.Constants;
import com.moyun.common.core.domain.entity.SysRole;
import com.moyun.common.core.domain.model.LoginUser;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

import java.util.List;

/**
 * 安全工具类
 *
 * @author ruoyi
 */
public class SecurityUtils {

    /**
     * 用户ID
     **/
    public static Long getUserId() {
        return getLoginUser().getUserId();
    }

    /**
     * 获取部门ID
     **/
    public static Long getDeptId() {
        return getLoginUser().getDeptId();
    }

    /**
     * 获取用户账户
     **/
    public static String getUsername() {
        return getLoginUser().getUsername();
    }

    /**
     * 获取用户
     **/
    public static LoginUser getLoginUser() {
        try {
            return (LoginUser) getAuthentication().getPrincipal();
        } catch (Exception e) {
            throw new RuntimeException("获取用户信息异常");
        }
    }

    /**
     * 获取Authentication
     */
    public static Authentication getAuthentication() {
        return SecurityContextHolder.getContext().getAuthentication();
    }

    /**
     * 生成BCryptPasswordEncoder密码
     *
     * @param password 密码
     * @return 加密字符串
     */
    public static String encryptPassword(String password) {
        BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();
        return passwordEncoder.encode(password);
    }

    /**
     * 判断密码是否相同
     *
     * @param rawPassword     真实密码
     * @param encodedPassword 加密后字符
     * @return 结果
     */
    public static boolean matchesPassword(String rawPassword, String encodedPassword) {
        BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();
        return passwordEncoder.matches(rawPassword, encodedPassword);
    }

    /**
     * 是否为管理员
     *
     * @param userId 用户ID
     * @return 结果
     */
    public static boolean isAdmin(Long userId) {
        return userId != null && 1L == userId;
    }

    /**
     * 验证用户是否具备某权限
     *
     * @param permission 权限字符
     * @return 用户是否具备某权限
     */
    public static boolean hasPermission(String permission) {
        return hasPermission(getAuthentication(), permission);
    }

    /**
     * 验证用户是否具备某权限
     *
     * @param authentication 用户身份
     * @param permission     权限字符
     * @return 用户是否具备某权限
     */
    public static boolean hasPermission(Authentication authentication, String permission) {
        if (authentication == null) {
            return false;
        }
        LoginUser loginUser = (LoginUser) authentication.getPrincipal();
//        if (loginUser.isSuperAdmin()) {
//            return true;
//        }
        return loginUser.getPermissions() != null && loginUser.getPermissions().contains(permission);
    }

    /**
     * 验证用户是否拥有某个角色
     *
     * @param role 角色标识
     * @return 用户是否具备某角色
     */
    public static boolean hasRole(String role) {
        return hasRole(getAuthentication(), role);
    }

    /**
     * 验证用户是否拥有某个角色
     *
     * @param authentication 用户身份
     * @param role           角色标识
     * @return 用户是否具备某角色
     */
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