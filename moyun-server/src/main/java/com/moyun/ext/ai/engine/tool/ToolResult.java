package com.moyun.ext.ai.engine.tool;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 工具执行结果
 *
 * @author laomao
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class ToolResult {

    /**
     * 是否执行成功
     */
    private boolean success;

    /**
     * 文本结果（给LLM整合用）
     */
    private String content;

    /**
     * 结构化数据（可选）
     */
    private Object data;

    /**
     * 错误信息
     */
    private String errorMessage;

    /**
     * 执行耗时（毫秒）
     */
    private long durationMs;

    /**
     * 创建成功结果
     */
    public static ToolResult success(String content) {
        return ToolResult.builder()
                .success(true)
                .content(content)
                .build();
    }

    /**
     * 创建成功结果（带数据）
     */
    public static ToolResult success(String content, Object data) {
        return ToolResult.builder()
                .success(true)
                .content(content)
                .data(data)
                .build();
    }

    /**
     * 创建失败结果
     */
    public static ToolResult fail(String errorMessage) {
        return ToolResult.builder()
                .success(false)
                .errorMessage(errorMessage)
                .content("工具执行失败: " + errorMessage)
                .build();
    }
}
