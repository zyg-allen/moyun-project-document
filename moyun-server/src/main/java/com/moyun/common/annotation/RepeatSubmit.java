package com.moyun.common.annotation;

import java.lang.annotation.*;
import java.util.concurrent.TimeUnit;

/**
 * 自定义注解防止表单重复提交
 *
 * @author ruoyi
 */
@Inherited
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface RepeatSubmit {
    /**
     * 间隔时间(ms)，小于此时间视为重复提交
     */
    public int interval() default 5000;

    /**
     * 提示消息 支持国际化 格式为 {key}
     */
    public String message() default "{repeat.submit.message}";

    /**
     * 时间单位
     */
    public TimeUnit timeUnit() default TimeUnit.MILLISECONDS;
}