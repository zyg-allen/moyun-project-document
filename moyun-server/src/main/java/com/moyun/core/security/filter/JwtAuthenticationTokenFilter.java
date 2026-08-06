package com.moyun.core.security.filter;

import com.moyun.core.base.model.LoginUser;
import com.moyun.core.security.auth.TokenService;
import com.moyun.util.security.SecurityUtils;
import com.moyun.util.string.StringUtils;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * token过滤器 验证token有效性
 * <p>
 * 设计原则：透明处理token，不做路径判断
 * - 如果没有token，直接跳过，由SecurityConfig决定是否允许访问
 * - 如果有token，解析并设置认证信息
 * </p>
 *
 * @author ruoyi
 */
@Component
public class JwtAuthenticationTokenFilter extends OncePerRequestFilter {

    @Autowired
    private TokenService tokenService;

    /**
     * 对 ASYNC 分发也执行过滤。
     * <p>SseEmitter / DeferredResult 等异步返回值在 complete() 时会触发 ASYNC 分发，
     * 此时原始请求的 SecurityContext 已被清除，默认 OncePerRequestFilter 不处理 ASYNC 分发，
     * 导致 AuthorizationFilter 检查到无认证信息抛 AccessDeniedException。
     * 此处覆盖为 false，使 ASYNC 分发时也重新从 JWT token 恢复认证上下文。</p>
     */
    @Override
    protected boolean shouldNotFilterAsyncDispatch() {
        return false;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        // 透明处理：如果没有token或解析失败，直接跳过
        // 由SecurityConfig的permitAll()和authenticated()决定是否允许访问
        LoginUser loginUser = tokenService.getLoginUser(request);
        if (StringUtils.isNotNull(loginUser) && StringUtils.isNull(SecurityUtils.getAuthentication())) {
            tokenService.verifyToken(loginUser);
            UsernamePasswordAuthenticationToken authenticationToken =
                    new UsernamePasswordAuthenticationToken(loginUser, null, loginUser.getAuthorities());
            authenticationToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
            SecurityContextHolder.getContext().setAuthentication(authenticationToken);
        }
        chain.doFilter(request, response);
    }
}
