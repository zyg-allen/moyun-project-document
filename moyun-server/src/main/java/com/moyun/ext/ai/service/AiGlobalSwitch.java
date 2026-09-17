package com.moyun.ext.ai.service;

import com.moyun.system.service.ISysConfigService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * AI 全局运行时开关（sys_config 热配置化）
 *
 * <p><strong>背景</strong>：原 {@code moyun.ai.enabled} / {@code moyun.ai.resume-advice-enabled}
 * 为 yaml 静态配置——改值需重启、各环境 profile 不一致时会静默关闭 AI 链路（环境陷阱）。
 * 收口为 sys_config 运行时开关，管理台「参数设置」可直接修改、即时生效。</p>
 *
 * <p><strong>键约定</strong>（均缺省 true，未配置 = 开启）：</p>
 * <ul>
 *   <li>{@code ai.global.enabled}：AI 能力全局开关——关闭后面试 agent 直连 / 场景网关
 *       逐题分析 / 预热 / 简历 AI 建议等全部业务判断点回退规则化兜底</li>
 *   <li>{@code ai.resume.advice.enabled}：简历 AI 建议子开关（叠加在全局开关之上）</li>
 * </ul>
 *
 * <p><strong>与 yaml 的分工</strong>：yaml 的 {@code moyun.ai.enabled} 仅保留
 * {@code @ConditionalOnProperty} bean 装配职责（AiModuleLlmClient / NoopLlmClient 二选一），
 * 基础配置中固定为 true 消除 profile 缺失陷阱；运行时业务判断一律走本组件。</p>
 *
 * <p>读取走 RuoYi {@link ISysConfigService}（Redis 缓存 + 管理台更新自动失效），
 * 任何异常兜底返回缺省值（开关永不阻塞业务）。</p>
 *
 * @author moyun
 * @since 2026-09-17
 */
@Component
public class AiGlobalSwitch {

    /** AI 能力全局开关键 */
    public static final String KEY_GLOBAL = "ai.global.enabled";

    /** 简历 AI 建议子开关键 */
    public static final String KEY_RESUME_ADVICE = "ai.resume.advice.enabled";

    @Autowired
    private ISysConfigService sysConfigService;

    /** AI 能力全局开关（缺省 true；管理台置 false 全链路回退规则化兜底） */
    public boolean isEnabled() {
        return readAsBool(KEY_GLOBAL, true);
    }

    /** 简历 AI 建议子开关（缺省 true；需与全局开关同时开启才生效） */
    public boolean isResumeAdviceEnabled() {
        return readAsBool(KEY_RESUME_ADVICE, true);
    }

    /** sys_config 布尔读取：未配置/读取异常返回缺省值；接受 true/1（大小写不敏感） */
    private boolean readAsBool(String key, boolean defaultValue) {
        try {
            String value = sysConfigService.selectConfigByKey(key);
            if (value == null || value.isBlank()) {
                return defaultValue;
            }
            String trimmed = value.trim();
            return "true".equalsIgnoreCase(trimmed) || "1".equals(trimmed);
        } catch (Exception e) {
            return defaultValue;
        }
    }
}
