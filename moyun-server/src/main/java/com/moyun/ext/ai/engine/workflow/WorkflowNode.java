package com.moyun.ext.ai.engine.workflow;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

/**
 * 工作流节点定义
 *
 * @author laomao
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WorkflowNode {

    /** 节点ID */
    private String id;

    /** 节点类型: start, end, llm, condition, tool, code, http */
    private String type;

    /** 节点名称 */
    private String name;

    /** 节点配置 */
    private Map<String, Object> config;

    /** 节点位置X */
    private Double positionX;

    /** 节点位置Y */
    private Double positionY;
}
