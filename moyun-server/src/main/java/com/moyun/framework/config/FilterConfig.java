package com.moyun.framework.config;

import com.moyun.common.filter.RepeatableFilter;
import com.moyun.common.filter.XssFilter;
import com.moyun.common.utils.StringUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import jakarta.servlet.DispatcherType;
import jakarta.servlet.Filter;
import java.util.HashMap;
import java.util.Map;

/**
 * Filter配置
 *
 * @author ruoyi
 */
@Configuration
public class FilterConfig
{
    @Value("${xss.excludes:}")
    private String excludes;

    @Value("${xss.urlPatterns:/*}")
    private String urlPatterns;

    @Bean
    @ConditionalOnProperty(value = "xss.enabled", havingValue = "true")
    public FilterRegistrationBean<Filter> xssFilterRegistration()
    {
        FilterRegistrationBean<Filter> registration = new FilterRegistrationBean<>();
        registration.setDispatcherTypes(DispatcherType.REQUEST);
        registration.setFilter(new XssFilter());
        String[] patterns = StringUtils.split(urlPatterns, ",");
        if (patterns != null && patterns.length > 0)
        {
            registration.addUrlPatterns(patterns);
        }
        else
        {
            registration.addUrlPatterns("/*");
        }
        registration.setName("xssFilter");
        registration.setOrder(FilterRegistrationBean.HIGHEST_PRECEDENCE);
        Map<String, String> initParameters = new HashMap<>();
        initParameters.put("excludes", excludes);
        registration.setInitParameters(initParameters);
        return registration;
    }

    @Bean
    public FilterRegistrationBean<Filter> repeatableFilterRegistration()
    {
        FilterRegistrationBean<Filter> registration = new FilterRegistrationBean<>();
        registration.setFilter(new RepeatableFilter());
        registration.addUrlPatterns("/*");
        registration.setName("repeatableFilter");
        registration.setOrder(FilterRegistrationBean.LOWEST_PRECEDENCE);
        return registration;
    }

}
