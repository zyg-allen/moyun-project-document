package com.moyun.ext.ai.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 工作流执行事件
 * 用于SSE实时推送执行进度
 *
 * @author laomao
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WorkflowExecutionEvent {
    
    /**
     * 事件类型
     * start - 开始执行
     * node_start - 节点开始
     * node_complete - 节点完成
     * node_error - 节点错误
     * complete - 工作流完成
     * error - 工作流错误
     */
    private String type;
    
    /**
     * 节点ID
     */
    private String nodeId;
    
    /**
     * 节点名称
     */
    private String nodeName;
    
    /**
     * 节点类型
     */
    private String nodeType;
    
    /**
     * 消息内容
     */
    private String message;
    
    /**
     * 节点输出（仅在node_complete时）
     */
    private Object output;
    
    /**
     * 执行耗时（毫秒）
     */
    private Long durationMs;
    
    /**
     * 错误信息（仅在error时）
     */
    private String error;
    
    /**
     * 最终输出（仅在complete时）
     */
    private Object finalOutput;
    
    /**
     * 时间戳
     */
    private Long timestamp;
    
    public static WorkflowExecutionEvent start(String message) {
        return WorkflowExecutionEvent.builder()
                .type("start")
                .message(message)
                .timestamp(System.currentTimeMillis())
                .build();
    }
    
    public static WorkflowExecutionEvent nodeStart(String nodeId, String nodeName, String nodeType) {
        return WorkflowExecutionEvent.builder()
                .type("node_start")
                .nodeId(nodeId)
                .nodeName(nodeName)
                .nodeType(nodeType)
                .message("开始执行节点: " + nodeName)
                .timestamp(System.currentTimeMillis())
                .build();
    }
    
    public static WorkflowExecutionEvent nodeComplete(String nodeId, String nodeName, Object output, long durationMs) {
        return WorkflowExecutionEvent.builder()
                .type("node_complete")
                .nodeId(nodeId)
                .nodeName(nodeName)
                .message("节点执行完成: " + nodeName)
                .output(output)
                .durationMs(durationMs)
                .timestamp(System.currentTimeMillis())
                .build();
    }
    
    public static WorkflowExecutionEvent nodeError(String nodeId, String nodeName, String error) {
        return WorkflowExecutionEvent.builder()
                .type("node_error")
                .nodeId(nodeId)
                .nodeName(nodeName)
                .message("节点执行失败: " + nodeName)
                .error(error)
                .timestamp(System.currentTimeMillis())
                .build();
    }
    
    public static WorkflowExecutionEvent complete(Object finalOutput, long durationMs) {
        return WorkflowExecutionEvent.builder()
                .type("complete")
                .message("工作流执行完成")
                .finalOutput(finalOutput)
                .durationMs(durationMs)
                .timestamp(System.currentTimeMillis())
                .build();
    }
    
    public static WorkflowExecutionEvent error(String error) {
        return WorkflowExecutionEvent.builder()
                .type("error")
                .message("工作流执行失败")
                .error(error)
                .timestamp(System.currentTimeMillis())
                .build();
    }
}
