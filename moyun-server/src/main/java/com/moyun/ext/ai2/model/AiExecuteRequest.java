package com.moyun.ext.ai2.model;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
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
 *   "userInput": "待检测的用户文本",
 *   "input": { "position": "Java工程师" }
 * }
 * </pre>
 *
 * <p>入参语义分层（v11.52 确立）：</p>
 * <ul>
 *   <li>{@link #userInput} —— 用户自由文本（人打的原始输入）。对话/检测/生成类场景的主要原料，
 *       网关横切关注点（意图判断路由、语义缓存键）统一消费此字段</li>
 *   <li>{@link #input} —— 结构化业务参数（强类型键值：userId/range/position/questionId 等），
 *       由各场景 Handler 自定义取值，网关不解释其内容</li>
 * </ul>
 *
 * @author laomao
 * @since 2026-09-09
 */
@Data
public class AiExecuteRequest {

    /**
     * 场景代码（必填）：voice_interview/resume_parse/resume_optimize/question_generate/finance_analysis/sensitive_word/daily_topic
     */
    @NotBlank(message = "场景代码不能为空")
    private String sceneCode;

    /**
     * 用户自由文本输入（对话/检测/生成类场景的主要原料）：
     * 网关意图判断（IntentClassifier）与语义缓存键（primaryInputText）优先消费此字段。
     * 业务结构化参数请放 input，不要塞进本字段。
     */
    @Size(max = 8000, message = "用户输入文本不能超过8000字符")
    private String userInput;

    /**
     * 业务参数（各场景Handler自定义取值）
     */
    private Map<String, Object> input;

    /**
     * 输出模式覆盖：sync/stream（null 则使用场景默认配置）
     */
    private String outputMode;

    /**
     * 运行时配置覆盖（如 temperature/maxTokens）
     */
    private Map<String, Object> config;

    /**
     * 是否异步执行（预留）
     */
    private Boolean async;

    // ===== 上下文（网关自动注入，调用方无需传） =====

    /**
     * 请求ID（自动生成）
     */
    private String requestId;

    /**
     * 当前用户ID（网关注入）
     */
    private Long userId;

    /**
     * 会话ID（可选）
     */
    private String sessionId;
}
