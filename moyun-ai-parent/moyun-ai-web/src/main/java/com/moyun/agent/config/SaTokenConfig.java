package com.moyun.agent.config;

import cn.dev33.satoken.interceptor.SaInterceptor;
import cn.dev33.satoken.router.SaRouter;
import cn.dev33.satoken.stp.StpUtil;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Sa-Token 配置
 *
 * <p>配置登录拦截规则，放行登录相关接口和静态资源</p>
 *
 * @author laomao
 * @time 2025/11/25
 */
@Configuration
public class SaTokenConfig implements WebMvcConfigurer {

    /**
     * 注册Sa-Token拦截器
     *
     * <p>配置路由拦截规则：
     * <ul>
     *   <li>拦截所有路由，检查登录状态</li>
     *   <li>放行认证相关接口（/api/auth/**）</li>
     *   <li>放行Swagger文档接口</li>
     *   <li>放行静态资源</li>
     * </ul>
     * </p>
     *
     * @param registry 拦截器注册器
     */
    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(new SaInterceptor(handle -> {
            SaRouter
                .match("/**")                    // 拦截所有路由
                .notMatch("/api/auth/**")        // 放行登录相关接口
                .notMatch("/api/image/**", "/api/image")  // 放行图片接口
                .notMatch("/api/workflow/*/execute/stream")  // 放行SSE流式接口
                .notMatch("/api/workflow/share/**")  // 放行工作流分享接口
                .notMatch("/doc.html", "/webjars/**", "/swagger-resources/**", "/v3/api-docs/**")  // 放行Swagger
                .notMatch("/favicon.ico", "/error")  // 放行静态资源
                .check(r -> StpUtil.checkLogin());   // 检查是否登录
        })).addPathPatterns("/**");
    }
}
