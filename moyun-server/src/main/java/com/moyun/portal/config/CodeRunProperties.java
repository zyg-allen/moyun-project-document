package com.moyun.portal.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import lombok.Data;

/**
 * 在线代码运行（/portal/code/run）配置。
 *
 * <p><b>为什么默认关闭</b>：当前 {@code CodeExecutorService} 基于 ProcessBuilder 直接执行用户代码，
 * 未引入 Docker / cgroups 强隔离，存在 RCE 风险。在独立安全沙箱就绪前保持关闭——
 * 与 {@code PortalCodeRunController#run} 原先"无条件返回 503"的行为一致，但改为**配置驱动**。</p>
 *
 * <p>前端据 {@link #enabled} 把「运行代码」按钮置灰并展示常驻维护说明，
 * 避免用户点了才知道不可用；沙箱就绪后只需把 {@code moyun.code-run.enabled} 置 true
 * （Controller 与前端同时生效），无需改代码。</p>
 *
 * @author moyun
 */
@Data
@Component
@ConfigurationProperties(prefix = "moyun.code-run")
public class CodeRunProperties {

    /** 是否允许在线执行代码（默认 false：安全沙箱就绪前禁用） */
    private boolean enabled = false;
}
