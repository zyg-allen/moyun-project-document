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
import com.moyun.core.config.properties.PermitAllUrlProperties;
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
     * 允许匿名访问的地址（扫描 @Anonymous 注解收集）
     * 门户链也消费此列表，使 Controller 上的 @Anonymous 注解在 /portal/** 路径上生效
     */
    @Autowired
    private PermitAllUrlProperties permitAllUrl;

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
                .authorizeHttpRequests(authorizeRequests -> {
                        // 先消费 @Anonymous 注解收集的 URL，使 Controller 上的 @Anonymous 在门户链生效
                        // 必须放在 authenticated() 窄规则之前，但 @Anonymous 标注的都是公开方法，不会与需登录的窄规则冲突
                        permitAllUrl.getUrls().forEach(url -> authorizeRequests.requestMatchers(url).permitAll());
                        // 门户登录、注册、验证码允许匿名访问
                        authorizeRequests.requestMatchers("/portal/login", "/portal/register", "/portal/captchaImage", "/portal/debug/**").permitAll()
                        // 文章查看、点赞、浏览允许所有人访问（GET 全放开，POST view/like 公开，写操作需登录）
                        // 注意：/portal/article/my 是 GET 但需登录，必须在 permitAll 之前声明
                        .requestMatchers(HttpMethod.GET, "/portal/article/my").authenticated()
                        .requestMatchers(HttpMethod.GET, "/portal/article/**").permitAll()
                        .requestMatchers(HttpMethod.POST, "/portal/article/*/view").permitAll()
                        .requestMatchers(HttpMethod.POST, "/portal/article/*/like").permitAll()
                        // 评论查看公开；发布评论需登录（authorId 由 Service 强制覆写，不再信任前端）
                        .requestMatchers(HttpMethod.GET, "/portal/comment/**").permitAll()
                        // POST /portal/comment/** 默认落入 anyRequest().authenticated() 链，强制登录态
                        // 评论点赞虽标 permitAll，但实际由 Controller 内部校验 getUserId 后再操作
                        .requestMatchers(HttpMethod.POST, "/portal/comment/*/like").permitAll()
                        // 话题模块：列表/详情/观点列表/评论列表对游客公开；
                        // 我的话题/观点需登录；写操作由 Controller 内部 PortalSecurityUtils.getUserId() 校验
                        // 注意：/portal/topic/my/** 必须在 /portal/topic/** permitAll 之前声明，否则会被覆盖
                        .requestMatchers(HttpMethod.GET, "/portal/topic/my/**").authenticated()
                        .requestMatchers(HttpMethod.GET, "/portal/topic/**").permitAll()
                        // 点赞接口（话题/观点/评论）放行到 Controller，由 Controller 校验登录态后操作
                        .requestMatchers(HttpMethod.POST, "/portal/topic/*/like").permitAll()
                        .requestMatchers(HttpMethod.POST, "/portal/topic/post/*/like").permitAll()
                        .requestMatchers(HttpMethod.POST, "/portal/topic/comment/*/like").permitAll()
                        // 分类查询公开（仅 GET），写操作（POST/PUT/DELETE）需登录 + 管理员角色
                        .requestMatchers(HttpMethod.GET, "/portal/category/**").permitAll()
                        // 标签查询公开（GET），创建/绑定需登录（Controller 内部校验 getUserId）
                        .requestMatchers(HttpMethod.GET, "/portal/tag/**").permitAll()
                        // 友情链接接口（支持驼峰和连字符两种命名）
                        .requestMatchers("/portal/friendLink/**").permitAll()
                        .requestMatchers("/portal/friend-link/**").permitAll()
                        // 自研广告位前台展示接口（详情页底部广告卡片，公开访问）
                        .requestMatchers("/portal/ad/**").permitAll()
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
                        // 作者主页：按 ID 查询用户公开信息，允许游客访问（/portal/user/{id}）
                        .requestMatchers(HttpMethod.GET, "/portal/user/{id}").permitAll()
                        // 用户统计（/portal/user/{id}/stats 或 /portal/user/stats）公开
                        .requestMatchers(HttpMethod.GET, "/portal/user/{id}/stats").permitAll()
                        // 读书空间前台公开接口（书籍、书单、金句列表、详情、点赞）
                        // 注意：阅读进度、书架、阅读偏好的写接口（POST/PUT/DELETE）需登录，
                        // 只读 GET 接口对游客放行（控制器已做 null 兜底，未登录返回空/false，无数据泄露）
                        .requestMatchers(HttpMethod.GET, "/portal/reading/progress/recent").permitAll()
                        .requestMatchers(HttpMethod.GET, "/portal/reading/progress/{bookId}").permitAll()
                        .requestMatchers(HttpMethod.GET, "/portal/reading/bookshelf/check/**").permitAll()
                        .requestMatchers("/portal/reading/progress/**").authenticated()
                        .requestMatchers("/portal/reading/bookshelf/**").authenticated()
                        .requestMatchers("/portal/reading/preference/**").authenticated()
                        .requestMatchers("/portal/reading/**").permitAll()
                        .requestMatchers("/portal/book/**").permitAll()
                        .requestMatchers("/portal/bookList/**").permitAll()
                        .requestMatchers("/portal/bookQuote/**").permitAll()
                        // 面试指南公开接口
                        .requestMatchers("/portal/interview/**").permitAll()
                        // 帮助中心公开接口
                        .requestMatchers("/portal/help/**").permitAll()
                        // 成长体系公开接口（排行榜、指定用户成长/统计/徽章/成就）
                        .requestMatchers(HttpMethod.GET, "/portal/growth/ranking").permitAll()
                        .requestMatchers(HttpMethod.GET, "/portal/growth/user/**").permitAll()
                        .requestMatchers(HttpMethod.GET, "/portal/growth/stats").permitAll()
                        .requestMatchers(HttpMethod.GET, "/portal/growth/badges").permitAll()
                        .requestMatchers(HttpMethod.GET, "/portal/growth/achievements").permitAll()
                        // 关注公开接口（检查关注状态、粉丝列表、关注列表，允许游客浏览作者主页）
                        .requestMatchers(HttpMethod.GET, "/portal/follow/check/**").permitAll()
                        .requestMatchers(HttpMethod.GET, "/portal/follow/{userId}/followers").permitAll()
                        .requestMatchers(HttpMethod.GET, "/portal/follow/{userId}/following").permitAll()
                        // 后台管理接口（admin token 认证，由核心 SecurityConfig 处理）
                        .requestMatchers("/portal/admin/**").permitAll()
                        // 其他门户请求需要认证
                        .anyRequest().authenticated();
                })
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
