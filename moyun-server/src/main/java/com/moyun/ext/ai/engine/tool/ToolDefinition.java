package com.moyun.ext.ai.engine.tool;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 工具定义（给LLM使用）
 *
 * @author laomao
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class ToolDefinition {

    /**
     * 工具名称
     */
    private String name;

    /**
     * 显示名称
     */
    private String displayName;

    /**
     * 工具描述
     */
    private String description;

    /**
     * 参数定义（JSON Schema）
     */
    private String parameters;

    /**
     * 转换为LLM可理解的格式
     */
    public String toPromptFormat() {
        StringBuilder sb = new StringBuilder();
        sb.append("工具名称: ").append(name).append("\n");
        sb.append("功能描述: ").append(description).append("\n");
        sb.append("参数格式: ").append(parameters).append("\n");
        return sb.toString();
    }
}
