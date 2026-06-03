package com.moyun.portal.security.filter;

import com.moyun.portal.domain.model.PortalLoginUser;
import com.moyun.portal.security.auth.PortalTokenService;
import com.moyun.util.string.StringUtils;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;

/**
 * 门户token过滤器 验证token有效性
 *
 * @author moyun
 */
@Component
public class PortalJwtAuthenticationTokenFilter extends OncePerRequestFilter {
    private static final Logger log = LoggerFactory.getLogger(PortalJwtAuthenticationTokenFilter.class);

    @Autowired
    @Qualifier("portalTokenService")
    private PortalTokenService portalTokenService;

    /**
     * 公开路径列表（不需要登录即可访问）
     */
    private static final List<String> PUBLIC_PATHS = Arrays.asList(
            "/portal/login",
            "/portal/register",
            "/portal/captchaImage",
            "/portal/article/list",
            "/portal/article/hot",
            "/portal/article/featured",
            "/portal/article/carousel",
            "/portal/article/home",
            "/portal/article/byCategory",
            "/portal/category/",
            "/portal/tag/",
            "/portal/comment/list",
            "/portal/friendLink/",
            "/portal/vipPackage/",
            "/portal/user/profile/",
            "/portal/book/",
            "/portal/bookList/",
            "/portal/bookQuote/",
            "/portal/interviewCategory/",
            "/portal/interviewQuestion/",
            "/portal/interviewExperience/",
            "/portal/interviewResumeTemplate/",
            "/portal/debug/"
    );

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        String requestURI = request.getRequestURI();

        // 只处理 /portal 路径的请求
        if (!requestURI.startsWith("/portal/")) {
            chain.doFilter(request, response);
            return;
        }

        // 跳过公开路径的token验证
        if (isPublicPath(requestURI, request.getMethod())) {
            log.debug("跳过公开路径的token验证: {}", requestURI);
            chain.doFilter(request, response);
            return;
        }

        log.debug("处理门户请求: {}", requestURI);
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

    /**
     * 判断是否为公开路径
     */
    private boolean isPublicPath(String requestURI, String method) {
        // 检查精确匹配的路径
        for (String publicPath : PUBLIC_PATHS) {
            if (requestURI.equals(publicPath) || requestURI.startsWith(publicPath)) {
                // 对于文章相关的POST请求（view/like）也需要允许
                if (requestURI.startsWith("/portal/article/") &&
                        ("POST".equals(method) || "DELETE".equals(method))) {
                    // 检查是否是 view 或 like 操作
                    if (requestURI.contains("/view") || requestURI.contains("/like")) {
                        return true;
                    }
                }
                // GET请求的文章详情
                if ("GET".equals(method) && requestURI.startsWith("/portal/article/")) {
                    return true;
                }
                // 评论的POST请求
                if (requestURI.startsWith("/portal/comment/") && "POST".equals(method)) {
                    return true;
                }
                return true;
            }
        }
        return false;
    }
}
