package com.moyun.ext.aigateway.model;

import lombok.Data;

import java.util.List;

/**
 * AI统一执行响应元数据
 *
 * @author laomao
 * @since 2026-09-09
 */
@Data
public class AiMetadata {

    /** 使用的模型 */
    private String modelUsed;

    /** 使用的Agent */
    private String agentUsed;

    /** Token消耗 */
    private Integer tokenUsed;

    /** 输入 Token（成本核算用） */
    private Integer inputTokens;

    /** 输出 Token（成本核算用） */
    private Integer outputTokens;

    /**
     * Token 是否为**本地估算**（v13.3）：
     * {@code true} = 服务端未回传 usage，由 {@code TokenMeter} 本地分词估算；
     * {@code false}/null = 服务端真实值。流式调用因 langchain4j 未下发
     * {@code stream_options.include_usage} 而拿不到真实 usage，故流式场景通常为 {@code true}。
     */
    private Boolean tokenEstimated;

    /** 模型提供商 */
    private String modelProvider;

    /** 工具调用记录 */
    private List<ToolCallInfo> toolCalls;

    /** 是否来自缓存 */
    private Boolean fromCache;

    /** 重试次数 */
    private Integer retryCount;

    /**
     * 工具调用信息
     */
    @Data
    public static class ToolCallInfo {
        private String toolName;
        private Object arguments;
        private Object result;
    }
}
