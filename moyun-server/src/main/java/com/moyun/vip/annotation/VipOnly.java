package com.moyun.vip.annotation;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * VIP 权益校验注解（与 @PreAuthorize 权限注解同层同构）
 *
 * <p>使用方式（方法级优先，类级兜底由扫描器支持）：
 * <pre>
 * &#64;VipOnly(platform = "portal", benefit = "interview_unlimited")
 * &#64;PostMapping("/start")
 * public AjaxResult start(...) { ... }
 * </pre>
 *
 * <p>链路：启动扫描（VipApiScanner）→ 注册表（vip_api_registry，后台可见可禁用）
 * → 运行时切面（VipOnlyAspect）：开关校验 → 注册表校验 → 权益校验/次数消耗。
 *
 * @author moyun
 */
@Target({ElementType.TYPE, ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface VipOnly {

    /** 端代码（sys_platform.platform_code） */
    String platform();

    /** 权益代码（vip_benefit.benefit_code） */
    String benefit();

    /** 是否消耗次数（true=校验并扣减额度，false=仅校验） */
    boolean consume() default true;

    /** 校验失败提示（HTTP 402 语义，前端据此引导开通） */
    String message() default "权益不足，请开通会员";
}
