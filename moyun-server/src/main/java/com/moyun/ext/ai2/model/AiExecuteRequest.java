package com.moyun.ext.ai2.model;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

import java.util.Map;

/**
 * AI统一执行请求
 *
 * <p>所有业务场景的AI调用统一入参。依据《AI能力统一接入层 — 完整方案文档》V2.0 §4.1。</p>
 *
 * <p>使用方式：POST /api/ai/execute，body 示例：</p>
 * <pre>
 * {
 *   "sceneCode": "sensitive_word",
 *   "input": { "text": "待检测文本" }
 * }
 * </pre>
 *
 * @author laomao
 * @since 2026-09-09
 */
@Data
public class AiExecuteRequest {

    /** 场景代码（必填）：voice_interview/resume_parse/resume_optimize/question_generate/finance_analysis/sensitive_word/daily_topic */
    @NotBlank(message = "场景代码不能为空")
    private String sceneCode;

    /** 业务参数（各场景Handler自定义取值） */
    private Map<String, Object> input;

    /** 输出模式覆盖：sync/stream（null 则使用场景默认配置） */
    private String outputMode;

    /** 运行时配置覆盖（如 temperature/maxTokens） */
    private Map<String, Object> config;

    /** 是否异步执行（预留） */
    private Boolean async;

    // ===== 上下文（网关自动注入，调用方无需传） =====

    /** 请求ID（自动生成） */
    private String requestId;

    /** 当前用户ID（网关注入） */
    private Long userId;

    /** 会话ID（可选） */
    private String sessionId;
}
