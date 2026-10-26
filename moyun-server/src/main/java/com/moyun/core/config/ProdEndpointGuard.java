package com.moyun.core.config;

import java.io.IOException;
import java.util.List;

import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.core.Ordered;

/**
 * 生产环境端点硬拦截。
 *
 * <p>背景：`spring.profiles.group.prod=dev` 会让 dev 的结构性配置参与合并，
 * 导致 dev 中开启的 Druid 控制台（/druid/*）与 Knife4j（/doc.html）在 prod 下仍然生效，
 * 仅靠 YAML 的 `enabled: false` 无法保证关闭（属性优先级取决于 profile 合并顺序）。
 *
 * <p>因此这里用 {@code @Profile("prod")} 的过滤器做**最后一道闸**：
 * 无论配置怎么写，生产环境一律 404 这些排障/文档端点。
 * 需要临时排障时，请通过 SSH 隧道访问本机端口，而不是在公网放开。
 *
 * @author moyun
 */
@Configuration
@Profile("prod")
public class ProdEndpointGuard {

    private static final List<String> BLOCKED_PREFIXES = List.of(
            "/druid",
            "/doc.html",
            "/swagger-ui",
            "/v3/api-docs",
            "/webjars",
            "/swagger-resources"
    );

    @Bean
    public FilterRegistrationBean<Filter> prodEndpointBlockFilter() {
        Filter filter = new Filter() {
            @Override
            public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
                    throws IOException, ServletException {
                String uri = ((HttpServletRequest) request).getRequestURI();
                for (String prefix : BLOCKED_PREFIXES) {
                    if (uri.startsWith(prefix)) {
                        ((HttpServletResponse) response).sendError(HttpServletResponse.SC_NOT_FOUND);
                        return;
                    }
                }
                chain.doFilter(request, response);
            }
        };
        FilterRegistrationBean<Filter> registration = new FilterRegistrationBean<>(filter);
        registration.addUrlPatterns("/*");
        registration.setOrder(Ordered.HIGHEST_PRECEDENCE);
        registration.setName("prodEndpointBlockFilter");
        return registration;
    }
}
