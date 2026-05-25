package com.moyun.core.config.properties;

import com.moyun.common.annotation.Anonymous;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.AnnotationUtils;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.mvc.method.RequestMappingInfo;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerMapping;

import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * 设置Anonymous注解允许匿名访问的url
 *
 * @author ruoyi
 */
@Configuration
public class PermitAllUrlProperties implements InitializingBean, ApplicationContextAware {
    private ApplicationContext applicationContext;

    private List<String> urls = new CopyOnWriteArrayList<>();

    @Override
    public void afterPropertiesSet() {
        //RequestMappingHandlerMapping mapping = applicationContext.getBean(RequestMappingHandlerMapping.class);
        RequestMappingHandlerMapping mapping = applicationContext.getBean("requestMappingHandlerMapping", RequestMappingHandlerMapping.class);
        Map<RequestMappingInfo, HandlerMethod> map = mapping.getHandlerMethods();

        map.keySet().forEach(info -> {
            HandlerMethod handlerMethod = map.get(info);

            // 获取方法上边的注解 替代path variable 为 *
            Anonymous method = AnnotationUtils.findAnnotation(handlerMethod.getMethod(), Anonymous.class);
            if (method != null) {
                addUrls(info);
            }

            // 获取类上边的注解, 替代path variable 为 *
            Anonymous controller = AnnotationUtils.findAnnotation(handlerMethod.getBeanType(), Anonymous.class);
            if (controller != null) {
                addUrls(info);
            }
        });
    }

    /**
     * 添加匿名访问URL
     *
     * @param info 请求映射信息
     */
    private void addUrls(RequestMappingInfo info) {
        // Spring Boot 3.x 使用 getPathPatternsCondition() 替代 getPatternsCondition()
        var pathPatterns = info.getPathPatternsCondition();
        if (pathPatterns != null) {
            pathPatterns.getPatterns().forEach(pattern -> {
                String patternString = pattern.getPatternString();
                // Spring Boot 3.x PathPattern 不支持将 {xxx} 替换为 *
                // 直接使用原始模式，因为 @Anonymous 标注的接口应该允许匿名访问
                urls.add(patternString);
            });
        }   else {
            var directPaths = info.getDirectPaths();
            for (String directPath : directPaths) {
                urls.add(directPath);
            }
        }

    }

    @Override
    public void setApplicationContext(ApplicationContext context) throws BeansException {
        this.applicationContext = context;
    }

    public List<String> getUrls() {
        return urls;
    }
}
