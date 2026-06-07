package com.moyun.portal.util;

import com.moyun.portal.domain.entity.PortalUser;
import com.moyun.portal.domain.model.PortalLoginUser;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

/**
 * 门户安全工具类
 * 
 * 专门用于获取前台用户信息，与后台 SecurityUtils 完全独立
 * 
 * @author moyun
 */
public class PortalSecurityUtils {

    /**
     * 获取当前登录的门户用户ID
     * 
     * @return 门户用户ID
     */
    public static Long getUserId() {
        PortalLoginUser loginUser = getLoginUser();
        return loginUser != null ? loginUser.getId() : null;
    }

    /**
     * 获取当前登录的门户用户名
     * 
     * @return 用户名
     */
    public static String getUsername() {
        PortalLoginUser loginUser = getLoginUser();
        return loginUser != null ? loginUser.getUsername() : null;
    }

    /**
     * 获取当前登录的门户用户信息
     * 
     * @return PortalLoginUser
     */
    public static PortalLoginUser getLoginUser() {
        try {
            Authentication authentication = getAuthentication();
            if (authentication != null && authentication.getPrincipal() instanceof PortalLoginUser) {
                return (PortalLoginUser) authentication.getPrincipal();
            }
        } catch (Exception e) {
            throw new RuntimeException("获取门户用户信息异常", e);
        }
        return null;
    }

    /**
     * 获取当前登录的门户用户实体
     * 
     * @return PortalUser
     */
    public static PortalUser getUser() {
        PortalLoginUser loginUser = getLoginUser();
        return loginUser != null ? loginUser.getUser() : null;
    }

    /**
     * 获取认证对象
     * 
     * @return Authentication
     */
    public static Authentication getAuthentication() {
        return SecurityContextHolder.getContext().getAuthentication();
    }

    /**
     * 判断当前是否为管理员
     * 
     * @return 是否为管理员
     */
    public static boolean isAdmin() {
        PortalLoginUser loginUser = getLoginUser();
        if (loginUser == null || loginUser.getRoles() == null) {
            return false;
        }
        return loginUser.getRoles().contains("admin");
    }

    /**
     * 判断当前是否为VIP用户
     * 
     * @return 是否为VIP
     */
    public static boolean isVip() {
        PortalLoginUser loginUser = getLoginUser();
        if (loginUser == null || loginUser.getRoles() == null) {
            return false;
        }
        return loginUser.getRoles().contains("vip");
    }
}
