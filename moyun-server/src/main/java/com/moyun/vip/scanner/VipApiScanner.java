package com.moyun.vip.scanner;

import com.moyun.vip.annotation.VipOnly;
import com.moyun.vip.domain.entity.VipApiRegistry;
import com.moyun.vip.mapper.VipApiRegistryMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.AnnotatedElementUtils;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.mvc.method.RequestMappingInfo;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerMapping;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * {@link VipOnly} 接口启动扫描器
 *
 * <p>启动后遍历全部 HandlerMethod，方法注解优先、类注解兜底，
 * 多路径映射（@GetMapping({"/a","/b"})）逐 pattern 注册，
 * upsert 只更新代码侧字段，运营字段（api_desc/enabled）不覆盖。
 * 扫描失败仅告警不阻塞启动；后台提供手动重扫入口。
 *
 * <p>路径 pattern 化：注册统一存 pattern 路径（URI 模板变量去掉内联正则约束，
 * 如 {id:[0-9]+} → {id}），保证 VipOnlyAspect 可用 Ant 通配正确命中带路径参数的接口；
 * 启动时先迁移存量旧格式行（保留运营字段），再 upsert 本次扫描结果，
 * 迁移 + upsert 在同一事务内完成。
 *
 * @author moyun
 */
@Component
public class VipApiScanner implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(VipApiScanner.class);

    /** URI 模板变量内联正则约束（{var:regex}）→ 仅保留变量名 */
    private static final Pattern REGEX_VAR_PATTERN =
            Pattern.compile("\\{([a-zA-Z][a-zA-Z0-9_]*)\\s*:\\s*[^{}]*}");

    @Autowired
    private VipApiRegistryMapper registryMapper;

    /** 按名注入 MVC 主映射（actuate 的 controllerEndpointHandlerMapping 会产生第二个候选） */
    @Autowired
    @Qualifier("requestMappingHandlerMapping")
    private RequestMappingHandlerMapping handlerMapping;

    @Autowired
    private TransactionTemplate transactionTemplate;

    @Override
    public void run(ApplicationArguments args) {
        try {
            int count = scan();
            log.info("[VipApiScanner] VIP 接口扫描完成，共 {} 条注册", count);
        } catch (Exception e) {
            log.error("[VipApiScanner] 扫描失败，不影响启动", e);
        }
    }

    /**
     * 手动重扫入口（后台"接口重新扫描"按钮调用）
     *
     * @return 本次 upsert 条数
     */
    public int scan() {
        Map<RequestMappingInfo, HandlerMethod> handlers = handlerMapping.getHandlerMethods();
        // 存量迁移 + 批量 upsert 同事务（扫描入口可能是 ApplicationRunner 自调用，无法依赖代理事务）
        Integer count = transactionTemplate.execute(status -> doScan(handlers));
        return count == null ? 0 : count;
    }

    private int doScan(Map<RequestMappingInfo, HandlerMethod> handlers) {
        // 1. 存量迁移：旧格式（{var:regex}）行 → pattern 化，运营字段（api_desc/enabled）保留
        migrateLegacyRows();
        int count = 0;
        for (Map.Entry<RequestMappingInfo, HandlerMethod> entry : handlers.entrySet()) {
            HandlerMethod method = entry.getValue();

            VipOnly anno = AnnotatedElementUtils.findMergedAnnotation(method.getMethod(), VipOnly.class);
            if (anno == null) {
                anno = AnnotatedElementUtils.findMergedAnnotation(method.getBeanType(), VipOnly.class);
            }
            if (anno == null) {
                continue;
            }

            // 多 pattern 逐个注册（§12-2：@GetMapping({"/a","/b"}) 不漏注册），统一 pattern 化
            Set<String> patterns = entry.getKey().getPatternValues();
            String httpMethod = extractHttpMethod(entry.getKey());
            for (String path : patterns) {
                registryMapper.upsert(toPatternPath(path), httpMethod,
                        method.getBeanType().getName(), method.getMethod().getName(),
                        anno.platform(), anno.benefit(),
                        anno.consume() ? 1 : 0, anno.message());
                count++;
            }
        }
        return count;
    }

    /**
     * 存量迁移：把旧格式 api_path（{var:regex}）改写为 pattern 化路径（{var}）。
     * 若 pattern 化行已存在（新旧格式并存），删除旧格式行；
     * 改写仅动 api_path，运营字段原样保留，随后 upsert 命中唯一键续更代码侧字段。
     */
    private void migrateLegacyRows() {
        List<VipApiRegistry> rows = registryMapper.selectList(null);
        Map<String, Long> keys = new HashMap<>();
        for (VipApiRegistry row : rows) {
            keys.put(rowKey(row.getApiPath(), row.getHttpMethod()), row.getId());
        }
        for (VipApiRegistry row : rows) {
            String normalized = toPatternPath(row.getApiPath());
            if (normalized.equals(row.getApiPath())) {
                continue;
            }
            String key = rowKey(normalized, row.getHttpMethod());
            if (keys.containsKey(key)) {
                registryMapper.deleteById(row.getId());
                log.info("[VipApiScanner] 存量覆盖：删除旧格式注册 {} {}",
                        row.getHttpMethod(), row.getApiPath());
            } else {
                VipApiRegistry update = new VipApiRegistry();
                update.setId(row.getId());
                update.setApiPath(normalized);
                registryMapper.updateById(update);
                keys.put(key, row.getId());
                log.info("[VipApiScanner] 存量迁移：{} {} → {}",
                        row.getHttpMethod(), row.getApiPath(), normalized);
            }
        }
    }

    /**
     * pattern 化：{var:regex} → {var}（保留变量名，去掉内联正则约束），如
     * /portal/interview/voice/{id:[0-9]+}/start → /portal/interview/voice/{id}/start
     */
    static String toPatternPath(String path) {
        if (path == null) {
            return null;
        }
        return REGEX_VAR_PATTERN.matcher(path).replaceAll("{$1}");
    }

    private String rowKey(String apiPath, String httpMethod) {
        return apiPath + "|" + (httpMethod == null ? "" : httpMethod);
    }

    private String extractHttpMethod(RequestMappingInfo info) {
        return info.getMethodsCondition().getMethods().stream()
                .findFirst().map(Enum::name).orElse("ALL");
    }
}
