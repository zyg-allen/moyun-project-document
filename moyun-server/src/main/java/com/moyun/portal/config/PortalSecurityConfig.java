package com.moyun.portal.config;

import com.moyun.core.security.handle.AuthenticationEntryPointImpl;
import com.moyun.core.security.handle.LogoutSuccessHandlerImpl;
import com.moyun.portal.security.filter.PortalJwtAuthenticationTokenFilter;
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
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.web.authentication.logout.LogoutFilter;
import org.springframework.web.filter.CorsFilter;

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
                .csrf(csrf -> csrf.disable())
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
                        .requestMatchers("/portal/login", "/portal/register", "/portal/captchaImage").permitAll()
                        // 门户公开资源允许匿名访问（GET请求）
                        .requestMatchers(HttpMethod.GET,
                                "/portal/article/list",
                                "/portal/article/hot",
                                "/portal/article/featured",
                                "/portal/article/carousel",
                                "/portal/article/**",
                                "/portal/category/**",
                                "/portal/tag/**",
                                "/portal/comment/**",
                                "/portal/friendLink/**",
                                "/portal/vipPackage/**",
                                "/portal/user/profile/**"
                        ).permitAll()
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