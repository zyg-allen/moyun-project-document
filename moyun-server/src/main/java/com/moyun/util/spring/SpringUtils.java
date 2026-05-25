package com.moyun.util.spring;

import com.moyun.util.string.StringUtils;
import org.springframework.aop.framework.Advised;
import org.springframework.aop.framework.AopContext;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.NoSuchBeanDefinitionException;
import org.springframework.beans.factory.config.BeanFactoryPostProcessor;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.stereotype.Component;

@Component
public final class SpringUtils implements BeanFactoryPostProcessor, ApplicationContextAware 
{
    private static ConfigurableListableBeanFactory beanFactory;

    private static ApplicationContext applicationContext;

    @Override
    public void postProcessBeanFactory(ConfigurableListableBeanFactory beanFactory) throws BeansException 
    {
        SpringUtils.beanFactory = beanFactory;
    }

    @Override
    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException 
    {
        SpringUtils.applicationContext = applicationContext;
    }

    @SuppressWarnings("unchecked")
    public static <T> T getBean(String name) throws BeansException
    {
        return (T) beanFactory.getBean(name);
    }

    public static <T> T getBean(Class<T> clz) throws BeansException
    {
        T result = (T) beanFactory.getBean(clz);
        return result;
    }

    public static boolean containsBean(String name)
    {
        return beanFactory.containsBean(name);
    }

    public static boolean isSingleton(String name) throws NoSuchBeanDefinitionException
    {
        return beanFactory.isSingleton(name);
    }

    public static Class<?> getType(String name) throws NoSuchBeanDefinitionException
    {
        return beanFactory.getType(name);
    }

    public static String[] getAliases(String name) throws NoSuchBeanDefinitionException
    {
        return beanFactory.getAliases(name);
    }

    @SuppressWarnings("unchecked")
    public static <T> T getAopProxy(T invoker)
    {
        Object proxy = AopContext.currentProxy();
        if (((Advised) proxy).getTargetSource().getTargetClass() == invoker.getClass())
        {
            return (T) proxy;
        }
        return invoker;
    }

    public static String[] getActiveProfiles()
    {
        return applicationContext.getEnvironment().getActiveProfiles();
    }

    public static String getActiveProfile()
    {
        final String[] activeProfiles = getActiveProfiles();
        return StringUtils.isNotEmpty(activeProfiles) ? activeProfiles[0] : null;
    }

    public static String getRequiredProperty(String key)
    {
        return applicationContext.getEnvironment().getRequiredProperty(key);
    }
}
