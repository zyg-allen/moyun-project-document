package com.moyun.ext.ai.engine.workflow;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 工作流连线定义
 *
 * @author laomao
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WorkflowEdge {

    /** 连线ID */
    private String id;

    /** 源节点ID */
    private String source;

    /** 目标节点ID */
    private String target;

    /** 源节点输出句柄 */
    private String sourceHandle;

    /** 目标节点输入句柄 */
    private String targetHandle;

    /** 条件表达式(用于条件分支) */
    private String condition;

    /** 连线标签 */
    private String label;
}
