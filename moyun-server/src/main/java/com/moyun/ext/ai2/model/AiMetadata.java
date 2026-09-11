package com.moyun.ext.ai2.model;

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

    /** 输入 Token（v11.57 P0-2 成本核算用） */
    private Integer inputTokens;

    /** 输出 Token（v11.57 P0-2 成本核算用） */
    private Integer outputTokens;

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
