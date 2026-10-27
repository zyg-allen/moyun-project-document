package com.moyun.core.config;

import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.LongAdder;
import java.util.stream.Collectors;

import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/**
 * 接口调用计数（按 方法 + URI 模板）。
 *
 * <p>目的：为"哪些接口没人用"提供真实数据，替代容易误判的静态扫描。
 * 计数常驻内存，重启清零；通过 {@code GET /cms/monitor/api-stats} 查看。</p>
 *
 * <p>URI 中的数字段会归一化为 {@code {id}}，避免按 id 爆炸成成千上万个 key。</p>
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE + 10)
public class ApiStatsFilter extends OncePerRequestFilter {

    private static final Map<String, LongAdder> COUNTERS = new ConcurrentHashMap<>();
    private static final LongAdder TOTAL = new LongAdder();

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                    FilterChain chain) throws ServletException, IOException {
        try {
            chain.doFilter(request, response);
        } finally {
            if (!isStatic(request.getRequestURI())) {
                String key = request.getMethod() + " " + normalize(request.getRequestURI());
                COUNTERS.computeIfAbsent(key, k -> new LongAdder()).increment();
                TOTAL.increment();
            }
        }
    }

    /** 已产生调用记录的接口 key */
    public static java.util.Set<String> calledKeys() {
        return new java.util.HashSet<>(COUNTERS.keySet());
    }

    /** 返回调用次数最多的接口 */
    public static Map<String, Object> snapshot(int limit) {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("total", TOTAL.sum());
        result.put("items", COUNTERS.entrySet().stream()
                .sorted((a, b) -> Long.compare(b.getValue().sum(), a.getValue().sum()))
                .limit(Math.max(1, Math.min(limit, 500)))
                .map(e -> {
                    Map<String, Object> m = new LinkedHashMap<>();
                    m.put("api", e.getKey());
                    m.put("count", e.getValue().sum());
                    return m;
                })
                .collect(Collectors.toList()));
        return result;
    }

    private static String normalize(String uri) {
        return uri.replaceAll("/\\d+", "/{id}");
    }

    private boolean isStatic(String uri) {
        return uri.startsWith("/static") || uri.startsWith("/assets")
                || uri.endsWith(".js") || uri.endsWith(".css")
                || uri.endsWith(".png") || uri.endsWith(".jpg") || uri.endsWith(".svg");
    }
}
