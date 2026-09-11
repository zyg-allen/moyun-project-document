package com.moyun.core.config;

import com.moyun.common.config.RuoYiConfig;
import com.moyun.common.constant.Constants;
import com.moyun.core.mvc.interceptors.RepeatSubmitInterceptor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.CacheControl;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import org.springframework.web.filter.CorsFilter;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.util.concurrent.TimeUnit;

/**
 * 通用配置
 *
 * @author allen-zyg
 */
@Configuration
public class ResourcesConfig implements WebMvcConfigurer
{
    private static final Logger log = LoggerFactory.getLogger(ResourcesConfig.class);

    @Autowired
    private RepeatSubmitInterceptor repeatSubmitInterceptor;

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry)
    {
        /** 本地文件上传路径 */
        registry.addResourceHandler(Constants.RESOURCE_PREFIX + "/**")
                .addResourceLocations("file:" + RuoYiConfig.getProfile() + "/");

        /** swagger配置 */
        registry.addResourceHandler("/swagger-ui/**")
                .addResourceLocations("classpath:/META-INF/resources/webjars/springfox-swagger-ui/")
                .setCacheControl(CacheControl.maxAge(5, TimeUnit.HOURS).cachePublic());
        
        /** knife4j文档配置 */
        registry.addResourceHandler("doc.html")
                .addResourceLocations("classpath:/META-INF/resources/");
        
        registry.addResourceHandler("/webjars/**")
                .addResourceLocations("classpath:/META-INF/resources/webjars/");
    }

    /**
     * 自定义拦截规则
     */
    @Override
    public void addInterceptors(InterceptorRegistry registry)
    {
        registry.addInterceptor(repeatSubmitInterceptor)
                .addPathPatterns("/**")
                .excludePathPatterns("/doc.html", "/swagger-ui/**", "/v3/api-docs/**", "/webjars/**", "/swagger-resources/**");
    }

    /**
     * 跨域配置
     * 通过环境变量 CORS_ALLOWED_ORIGINS 配置允许的源（逗号分隔），默认仅允许本地开发地址
     * 注意：Origin 头中默认端口（80/443）通常被省略，需同时放行带端口通配符和无端口两种模式
     */
    @Bean
    public CorsFilter corsFilter()
    {
        CorsConfiguration config = new CorsConfiguration();
        // 从环境变量读取允许的源，默认仅本地开发
        String allowedOrigins = System.getenv("CORS_ALLOWED_ORIGINS");
        if (allowedOrigins != null && !allowedOrigins.trim().isEmpty()) {
            for (String origin : allowedOrigins.split(",")) {
                String trimmed = origin.trim();
                if (!trimmed.isEmpty()) {
                    config.addAllowedOriginPattern(trimmed);
                }
            }
            log.info("CORS allowed origins 已从环境变量加载：{}", config.getAllowedOriginPatterns());
        } else {
            // 本地开发默认白名单：同时覆盖带端口与无端口两种 Origin 头形式
            config.addAllowedOriginPattern("http://localhost:*");
            config.addAllowedOriginPattern("http://localhost");
            config.addAllowedOriginPattern("http://127.0.0.1:*");
            config.addAllowedOriginPattern("http://127.0.0.1");
            config.addAllowedOriginPattern("https://localhost:*");
            config.addAllowedOriginPattern("https://localhost");
            // 手机等局域网设备经 vite dev server（https + host）访问时，Origin 为局域网地址，
            // 代理不重写 Origin 头，需放行内网网段（仅开发默认值；生产务必配置 CORS_ALLOWED_ORIGINS）
            config.addAllowedOriginPattern("http://192.168.*:*");
            config.addAllowedOriginPattern("http://192.168.*");
            config.addAllowedOriginPattern("https://192.168.*:*");
            config.addAllowedOriginPattern("https://192.168.*");
            config.addAllowedOriginPattern("http://10.*:*");
            config.addAllowedOriginPattern("https://10.*:*");
            log.info("CORS 未配置 CORS_ALLOWED_ORIGINS，使用本地开发白名单：{}", config.getAllowedOriginPatterns());
        }
        // 设置访问源请求头
        config.addAllowedHeader("*");
        // 设置访问源请求方法
        config.addAllowedMethod("*");
        // 允许携带 Cookie
        config.setAllowCredentials(true);
        // 有效期 1800秒
        config.setMaxAge(1800L);
        // 添加映射路径，拦截一切请求
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);
        // 返回新的 CorsFilter
        return new CorsFilter(source);
    }
}