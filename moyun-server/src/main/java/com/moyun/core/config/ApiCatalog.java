package com.moyun.core.config;

import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.TreeSet;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.mvc.method.RequestMappingInfo;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerMapping;

/**
 * 启动时收集全部接口清单，用于和 {@link ApiStatsFilter} 的调用计数做差集，
 * 找出"从未被调用"的接口。
 *
 * <p>路径统一归一化：{@code /a/123/b} 与 {@code /a/{id}/b} 视为同一个 key。</p>
 */
@Component
public class ApiCatalog {

    private final Set<String> endpoints = new TreeSet<>();

    @Autowired
    @Qualifier("requestMappingHandlerMapping")
    private RequestMappingHandlerMapping handlerMapping;

    @EventListener(ApplicationReadyEvent.class)
    public void init() {
        handlerMapping.getHandlerMethods().forEach((info, method) -> {
            Set<String> patterns = patterns(info);
            Set<String> methods = info.getMethodsCondition().getMethods().isEmpty()
                    ? Collections.singleton("ANY")
                    : info.getMethodsCondition().getMethods().stream().map(Enum::name).collect(java.util.stream.Collectors.toSet());
            for (String p : patterns) {
                for (String m : methods) {
                    endpoints.add(m + " " + normalize(p));
                }
            }
        });
    }

    private Set<String> patterns(RequestMappingInfo info) {
        Set<String> result = new LinkedHashSet<>();
        if (info.getPathPatternsCondition() != null) {
            info.getPathPatternsCondition().getPatterns()
                    .forEach(p -> result.add(p.getPatternString()));
        } else if (info.getPatternsCondition() != null) {
            result.addAll(info.getPatternsCondition().getPatterns());
        }
        return result;
    }

    public Set<String> all() {
        return Collections.unmodifiableSet(endpoints);
    }

    /** 与调用计数做差集：没有任何调用记录的接口 */
    public Set<String> zeroCall(Set<String> called) {
        Set<String> zero = new TreeSet<>(endpoints);
        zero.removeAll(called);
        return zero;
    }

    public static String normalize(String path) {
        return path.replaceAll("\\{[^}]+}", "{id}").replaceAll("/\\d+", "/{id}");
    }
}
