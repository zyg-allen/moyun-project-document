package com.moyun.core.config;

import java.io.IOException;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/**
 * 请求链路追踪 + 慢接口日志。
 *
 * <ul>
 *   <li>优先复用上游传入的 {@code X-Trace-Id}（Nginx/网关），没有则生成 16 位；</li>
 *   <li>写入 MDC（logback pattern 里的 {@code [NO_TRACE]} 会自动变成 traceId）；</li>
 *   <li>响应头回传 {@code X-Trace-Id}，前端报错时可带上；</li>
 *   <li>耗时超过阈值（默认 500ms）打一条 WARN，便于定位慢接口。</li>
 * </ul>
 *
 * <p>放在最外层，保证即使后续过滤器抛错，traceId 也已写入日志上下文。</p>
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class TraceIdFilter extends OncePerRequestFilter {

    private static final Logger log = LoggerFactory.getLogger(TraceIdFilter.class);

    public static final String HEADER = "X-Trace-Id";
    public static final String MDC_KEY = "traceId";

    @Value("${moyun.trace.slow-ms:500}")
    private long slowMs;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                    FilterChain chain) throws ServletException, IOException {
        String traceId = request.getHeader(HEADER);
        if (traceId == null || traceId.isBlank() || traceId.length() > 64) {
            traceId = UUID.randomUUID().toString().replace("-", "").substring(0, 16);
        }
        MDC.put(MDC_KEY, traceId);
        response.setHeader(HEADER, traceId);
        response.setHeader("X-API-Version", "1");
        long start = System.currentTimeMillis();
        try {
            chain.doFilter(request, response);
        } finally {
            long cost = System.currentTimeMillis() - start;
            if (cost >= slowMs && !isStatic(request.getRequestURI())) {
                log.warn("慢接口 {} {} {}ms traceId={}", request.getMethod(),
                        request.getRequestURI(), cost, traceId);
            }
            MDC.remove(MDC_KEY);
        }
    }

    private boolean isStatic(String uri) {
        return uri.startsWith("/static") || uri.startsWith("/assets")
                || uri.endsWith(".js") || uri.endsWith(".css")
                || uri.endsWith(".png") || uri.endsWith(".jpg") || uri.endsWith(".svg");
    }
}
