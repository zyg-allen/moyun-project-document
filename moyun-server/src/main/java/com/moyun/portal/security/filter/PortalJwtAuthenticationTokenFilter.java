package com.moyun.portal.security.filter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import com.moyun.portal.domain.model.PortalLoginUser;
import com.moyun.portal.security.auth.PortalTokenService;
import com.moyun.util.string.StringUtils;

/**
 * 门户 token 过滤器：透明处理 token，不做路径判断
 *
 * <p>设计原则（与核心模块 {@code JwtAuthenticationTokenFilter} 一致）：</p>
 * <ul>
 *   <li>如果没有 token，直接跳过，由 {@code SecurityConfig} 决定是否允许访问；</li>
 *   <li>如果有 token，解析并设置认证信息到 {@code SecurityContext}。</li>
 * </ul>
 *
 * <p>访问控制完全交给 {@code PortalSecurityConfig.authorizeHttpRequests}，
 * 避免维护"第二套路径白名单"导致规则不一致的 bug（如 {@code /portal/article/my}
 * 既是 GET 又需登录，曾被白名单误伤）。</p>
 *
 * @author moyun
 */
@Component
@Slf4j
public class PortalJwtAuthenticationTokenFilter extends OncePerRequestFilter {

    @Autowired
    @Qualifier("portalTokenService")
    private PortalTokenService portalTokenService;

    /** 后台管理接口路径前缀（由核心 SecurityConfig 的 admin token 处理，本 Filter 跳过） */
    private static final String ADMIN_PATH_PREFIX = "/portal/admin/";

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        String requestURI = request.getRequestURI();

        // 只处理 /portal 路径的请求
        if (!requestURI.startsWith("/portal/")) {
            chain.doFilter(request, response);
            return;
        }

        // 后台管理接口由核心 SecurityConfig（admin token）处理，本 Filter 跳过
        if (requestURI.startsWith(ADMIN_PATH_PREFIX)) {
            chain.doFilter(request, response);
            return;
        }

        // 透明处理：有 token 就解析设置 SecurityContext，无 token 跳过
        // 访问控制完全交给 SecurityConfig.authorizeHttpRequests
        PortalLoginUser loginUser = portalTokenService.getLoginUser(request);
        if (StringUtils.isNotNull(loginUser)) {
            portalTokenService.verifyToken(loginUser);
            UsernamePasswordAuthenticationToken authenticationToken =
                    new UsernamePasswordAuthenticationToken(loginUser, null, loginUser.getAuthorities());
            authenticationToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
            SecurityContextHolder.getContext().setAuthentication(authenticationToken);
        }
        chain.doFilter(request, response);
    }
}
