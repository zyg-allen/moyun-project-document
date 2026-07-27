package com.moyun.agent.engine.workflow;

import com.moyun.agent.dto.WorkflowExecutionEvent;
import lombok.Data;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;

/**
 * 工作流执行上下文
 *
 * <p>在工作流执行过程中传递，存储变量和执行日志</p>
 *
 * @author laomao
 */
@Data
public class WorkflowContext {

    /** 工作流ID */
    private Long workflowId;

    /** 执行ID */
    private Long executionId;

    /** 输入参数 */
    private Map<String, Object> input = new HashMap<>();

    /** 变量存储(节点间传递数据) */
    private Map<String, Object> variables = new HashMap<>();

    /** 每个节点的输出 */
    private Map<String, Object> nodeOutputs = new HashMap<>();

    /** 执行日志 */
    private List<NodeExecutionLog> logs = new ArrayList<>();

    /** 当前节点ID */
    private String currentNodeId;

    /** 是否已停止 */
    private boolean stopped = false;

    /** 最终输出 */
    private Object finalOutput;
    
    /** 事件回调（用于SSE实时推送） */
    private Consumer<WorkflowExecutionEvent> eventCallback;

    /**
     * 设置变量
     */
    public void setVariable(String key, Object value) {
        variables.put(key, value);
    }

    /**
     * 获取变量
     * 优先从 variables 获取，其次从 input 获取
     */
    public Object getVariable(String key) {
        // 先从变量中获取
        if (variables.containsKey(key)) {
            return variables.get(key);
        }
        // 特殊处理 input 变量
        if ("input".equals(key)) {
            // 如果 input 是 Map 且只有一个 input 键，返回其值
            if (input.size() == 1 && input.containsKey("input")) {
                return input.get("input");
            }
            // 否则返回整个 input 或其字符串表示
            if (input.size() == 1) {
                return input.values().iterator().next();
            }
            return input;
        }
        // 从 input 中获取
        if (input.containsKey(key)) {
            return input.get(key);
        }
        return null;
    }

    /**
     * 获取变量(带默认值)
     */
    public Object getVariable(String key, Object defaultValue) {
        Object value = getVariable(key);
        return value != null ? value : defaultValue;
    }

    /**
     * 移除变量
     */
    public void removeVariable(String key) {
        variables.remove(key);
    }

    /**
     * 设置节点输出
     */
    public void setNodeOutput(String nodeId, Object output) {
        nodeOutputs.put(nodeId, output);
    }

    /**
     * 获取节点输出
     */
    public Object getNodeOutput(String nodeId) {
        return nodeOutputs.get(nodeId);
    }

    /**
     * 添加执行日志
     */
    public synchronized void addLog(NodeExecutionLog log) {
        logs.add(log);
    }

    /**
     * 创建分支上下文（用于并行执行）
     * 
     * <p>复制当前上下文的变量，但使用独立的日志列表</p>
     * 
     * @param branchId 分支标识
     * @return 分支上下文
     */
    public WorkflowContext createBranchContext(String branchId) {
        WorkflowContext branchContext = new WorkflowContext();
        branchContext.setWorkflowId(this.workflowId);
        branchContext.setExecutionId(this.executionId);
        branchContext.setEventCallback(this.eventCallback);
        
        // 复制输入（只读）
        branchContext.setInput(new HashMap<>(this.input));
        
        // 复制变量（可修改，但独立于主上下文）
        branchContext.setVariables(new ConcurrentHashMap<>(this.variables));
        
        // 独立的节点输出
        branchContext.setNodeOutputs(new ConcurrentHashMap<>());
        
        // 独立的日志列表
        branchContext.setLogs(new ArrayList<>());
        
        // 记录分支ID
        branchContext.setVariable("_branch_id", branchId);
        
        return branchContext;
    }

    /**
     * 合并分支变量到主上下文
     * 
     * <p>将分支产生的变量合并到主上下文，使用分支前缀避免冲突</p>
     * 
     * @param branchContext 分支上下文
     * @param branchPrefix 分支前缀
     */
    public void mergeBranchVariables(WorkflowContext branchContext, String branchPrefix) {
        // 合并分支产生的新变量
        for (Map.Entry<String, Object> entry : branchContext.getVariables().entrySet()) {
            String key = entry.getKey();
            // 跳过内部变量
            if (key.startsWith("_")) {
                continue;
            }
            // 如果主上下文没有这个变量，或者分支修改了它，则合并
            if (!this.variables.containsKey(key) || 
                !java.util.Objects.equals(this.variables.get(key), entry.getValue())) {
                // 使用带分支前缀的变量名
                this.variables.put(branchPrefix + "_" + key, entry.getValue());
                // 也保留不带前缀的（最后一个分支的值）
                this.variables.put(key, entry.getValue());
            }
        }
        
        // 合并节点输出
        for (Map.Entry<String, Object> entry : branchContext.getNodeOutputs().entrySet()) {
            this.nodeOutputs.put(entry.getKey(), entry.getValue());
        }
    }

    /**
     * 合并分支日志到主上下文
     * 
     * @param branchContext 分支上下文
     */
    public synchronized void mergeLogs(WorkflowContext branchContext) {
        this.logs.addAll(branchContext.getLogs());
    }

    /**
     * 节点执行日志
     */
    @Data
    @lombok.Builder
    @lombok.NoArgsConstructor
    @lombok.AllArgsConstructor
    public static class NodeExecutionLog {
        private String nodeId;
        private String nodeName;
        private String nodeType;
        private String status; // running, completed, failed, skipped
        private Object input;
        private Object output;
        private String errorMessage;
        private Long durationMs;
        private Long timestamp;
    }
}
