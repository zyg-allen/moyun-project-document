package com.moyun.ext.aigateway.model;

import lombok.Data;

import java.util.List;

/**
 * 会话流式执行命令（网关会话通道入参）
 *
 * <p>与 {@link AiExecuteRequest} 的区别：会话通道由业务 Service 直接调用（非 HTTP 入口），
 * 携带记忆/模型路由/瞬态指令等会话专属语义；SSE 事件协议由调用方自定义，
 * 业务编排（落库、事件载荷）留在业务侧。</p>
 *
 * @author moyun
 * @since 2026-09-24
 */
@Data
public class ConversationStreamCommand {

    /** 治理场景码（限流/Token熔断/执行日志的参数与维度，如 voice_interview） */
    private String sceneCode;

    /** 会话记忆ID（业务侧定义，如 voice-interview:{interviewId}） */
    private String sessionId;

    /** 当前用户ID（限流身份；可空按 anonymous） */
    private Long userId;

    /** 本轮用户输入（入滑窗 + 注入防护清洗） */
    private String userInput;

    /** per-agent 模型路由的 agentId（agent 绑定链） */
    private Long agentId;

    /** 滑窗大小（null=默认40） */
    private Integer maxMessages;

    /** 本轮瞬态指令（不入滑窗，仅本轮发送，如话术风格约束） */
    private List<String> directives;
}
