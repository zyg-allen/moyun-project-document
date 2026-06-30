package com.moyun.portal.config;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.ProviderManager;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.web.authentication.logout.LogoutFilter;
import org.springframework.web.filter.CorsFilter;

import com.moyun.core.security.handle.AuthenticationEntryPointImpl;
import com.moyun.core.security.handle.LogoutSuccessHandlerImpl;
import com.moyun.portal.security.filter.PortalJwtAuthenticationTokenFilter;

/**
 * 门户spring security配置（独立认证）
 *
 * @author moyun
 */
@Configuration
@EnableWebSecurity
@EnableMethodSecurity(prePostEnabled = true, securedEnabled = true)
@Order(1) // 更高的优先级，先匹配门户路径
public class PortalSecurityConfig {

    /**
     * 门户自定义用户认证逻辑
     */
    @Autowired
    @Qualifier("portalUserDetailsServiceImpl")
    private UserDetailsService userDetailsService;

    /**
     * 密码编码器（复用核心模块的Bean）
     */
    @Autowired
    private BCryptPasswordEncoder bCryptPasswordEncoder;

    /**
     * 认证失败处理类
     */
    @Autowired
    private AuthenticationEntryPointImpl unauthorizedHandler;

    /**
     * 退出处理类
     */
    @Autowired
    private LogoutSuccessHandlerImpl logoutSuccessHandler;

    /**
     * 门户token认证过滤器
     */
    @Autowired
    private PortalJwtAuthenticationTokenFilter portalAuthenticationTokenFilter;

    /**
     * 跨域过滤器
     */
    @Autowired
    private CorsFilter corsFilter;

    /**
     * 门户身份验证实现
     */
    @Bean(name = "portalAuthenticationManager")
    public AuthenticationManager portalAuthenticationManager() {
        DaoAuthenticationProvider daoAuthenticationProvider = new DaoAuthenticationProvider();
        daoAuthenticationProvider.setUserDetailsService(userDetailsService);
        daoAuthenticationProvider.setPasswordEncoder(bCryptPasswordEncoder);
        return new ProviderManager(daoAuthenticationProvider);
    }

    @Bean(name = "portalSecurityFilterChain")
    protected SecurityFilterChain portalSecurityFilterChain(HttpSecurity httpSecurity) throws Exception {
        return httpSecurity
                .securityMatcher("/portal/**") // 只匹配门户路径
                .csrf(AbstractHttpConfigurer::disable)
                .headers(headers -> headers
                        .cacheControl(cache -> cache.disable())
                        .frameOptions(frameOptions -> frameOptions.sameOrigin())
                )
                .exceptionHandling(exceptionHandling -> exceptionHandling
                        .authenticationEntryPoint(unauthorizedHandler)
                )
                .sessionManagement(session -> session
                        .sessionCreationPolicy(SessionCreationPolicy.STATELESS)
                )
                .authorizeHttpRequests(authorizeRequests -> authorizeRequests
                        // 门户登录、注册、验证码允许匿名访问
                        .requestMatchers("/portal/login", "/portal/register", "/portal/captchaImage", "/portal/debug/**").permitAll()
                        // 文章查看、点赞、浏览允许所有人访问（GET 全放开，POST view/like 公开，写操作需登录）
                        // 注意：/portal/article/my 是 GET 但需登录，必须在 permitAll 之前声明
                        .requestMatchers(HttpMethod.GET, "/portal/article/my").authenticated()
                        .requestMatchers(HttpMethod.GET, "/portal/article/**").permitAll()
                        .requestMatchers(HttpMethod.POST, "/portal/article/*/view").permitAll()
                        .requestMatchers(HttpMethod.POST, "/portal/article/*/like").permitAll()
                        // 评论查看、发布、点赞公开（点赞需登录由 Controller 内部校验）
                        .requestMatchers(HttpMethod.GET, "/portal/comment/**").permitAll()
                        .requestMatchers(HttpMethod.POST, "/portal/comment/**").permitAll()
                        .requestMatchers(HttpMethod.POST, "/portal/comment/*/like").permitAll()
                        // 分类查询公开（仅 GET），写操作（POST/PUT/DELETE）需登录 + 管理员角色
                        .requestMatchers(HttpMethod.GET, "/portal/category/**").permitAll()
                        // 标签查询公开（GET），创建/绑定需登录（Controller 内部校验 getUserId）
                        .requestMatchers(HttpMethod.GET, "/portal/tag/**").permitAll()
                        // 友情链接接口（支持驼峰和连字符两种命名）
                        .requestMatchers("/portal/friendLink/**").permitAll()
                        .requestMatchers("/portal/friend-link/**").permitAll()
                        // VIP 套餐查询公开（售卖页展示），写操作需登录 + 管理员
                        .requestMatchers(HttpMethod.GET, "/portal/vip-package/**").permitAll()
                        .requestMatchers(HttpMethod.GET, "/portal/vipPackage/**").permitAll()
                        // 通知查询公开（未登录返回空列表/0，由 Controller 内部处理），标记已读需登录
                        .requestMatchers(HttpMethod.GET, "/portal/notification/**").permitAll()
                        // 用户相关公开接口（作者列表、公开资料等）
                        .requestMatchers("/portal/user/authors").permitAll()
                        .requestMatchers("/portal/user/profile/**").permitAll()
                        // 当前用户信息接口允许匿名访问（未登录返回null）
                        .requestMatchers("/portal/user/me").permitAll()
                        // 读书空间前台公开接口（书籍、书单、金句列表、详情、点赞）
                        .requestMatchers("/portal/reading/**").permitAll()
                        .requestMatchers("/portal/book/**").permitAll()
                        .requestMatchers("/portal/bookList/**").permitAll()
                        .requestMatchers("/portal/bookQuote/**").permitAll()
                        // 面试指南公开接口
                        .requestMatchers("/portal/interview/**").permitAll()
                        // 帮助中心公开接口
                        .requestMatchers("/portal/help/**").permitAll()
                        // 成长体系公开接口（排行榜、指定用户成长/统计/徽章）
                        .requestMatchers(HttpMethod.GET, "/portal/growth/ranking").permitAll()
                        .requestMatchers(HttpMethod.GET, "/portal/growth/user/**").permitAll()
                        .requestMatchers(HttpMethod.GET, "/portal/growth/stats").permitAll()
                        .requestMatchers(HttpMethod.GET, "/portal/growth/badges").permitAll()
                        // 关注公开接口（检查关注状态，允许游客浏览作者主页）
                        .requestMatchers(HttpMethod.GET, "/portal/follow/check/**").permitAll()
                        // 后台管理接口（admin token 认证，由核心 SecurityConfig 处理）
                        .requestMatchers("/portal/admin/**").permitAll()
                        // 其他门户请求需要认证
                        .anyRequest().authenticated()
                )
                .logout(logout -> logout
                        .logoutUrl("/portal/logout")
                        .logoutSuccessHandler(logoutSuccessHandler)
                )
                .addFilterBefore(portalAuthenticationTokenFilter, UsernamePasswordAuthenticationFilter.class)
                .addFilterBefore(corsFilter, PortalJwtAuthenticationTokenFilter.class)
                .addFilterBefore(corsFilter, LogoutFilter.class)
                .build();
    }
}
