package com.moyun.vip.scanner;

import com.moyun.vip.annotation.VipOnly;
import com.moyun.vip.mapper.VipApiRegistryMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.AnnotatedElementUtils;
import org.springframework.stereotype.Component;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.mvc.method.RequestMappingInfo;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerMapping;

import java.util.Map;
import java.util.Set;

/**
 * {@link VipOnly} 接口启动扫描器
 *
 * <p>启动后遍历全部 HandlerMethod，方法注解优先、类注解兜底，
 * 多路径映射（@GetMapping({"/a","/b"})）逐 pattern 注册，
 * upsert 只更新代码侧字段，运营字段（api_desc/enabled）不覆盖。
 * 扫描失败仅告警不阻塞启动；后台提供手动重扫入口。
 *
 * @author moyun
 */
@Component
public class VipApiScanner implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(VipApiScanner.class);

    @Autowired
    private VipApiRegistryMapper registryMapper;

    /** 按名注入 MVC 主映射（actuate 的 controllerEndpointHandlerMapping 会产生第二个候选） */
    @Autowired
    @Qualifier("requestMappingHandlerMapping")
    private RequestMappingHandlerMapping handlerMapping;

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

            // 多 pattern 逐个注册（§12-2：@GetMapping({"/a","/b"}) 不漏注册）
            Set<String> patterns = entry.getKey().getPatternValues();
            String httpMethod = extractHttpMethod(entry.getKey());
            for (String path : patterns) {
                registryMapper.upsert(path, httpMethod,
                        method.getBeanType().getName(), method.getMethod().getName(),
                        anno.platform(), anno.benefit(),
                        anno.consume() ? 1 : 0, anno.message());
                count++;
            }
        }
        return count;
    }

    private String extractHttpMethod(RequestMappingInfo info) {
        return info.getMethodsCondition().getMethods().stream()
                .findFirst().map(Enum::name).orElse("ALL");
    }
}
