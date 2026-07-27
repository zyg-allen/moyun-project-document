package com.moyun.agent.engine.workflow;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.*;

/**
 * 工作流验证器
 * 
 * <p>验证工作流定义的正确性：</p>
 * <ul>
 *   <li>必须有且只有一个开始节点</li>
 *   <li>必须有至少一个结束节点</li>
 *   <li>所有节点必须可达</li>
 *   <li>不能有孤立节点</li>
 *   <li>节点配置完整性检查</li>
 *   <li>边连接正确性检查</li>
 * </ul>
 *
 * @author laomao
 * @since 2025-12-12
 */
@Slf4j
@Component
public class WorkflowValidator {

    /**
     * 验证结果
     */
    public static class ValidationResult {
        private final boolean valid;
        private final List<ValidationError> errors;
        private final List<ValidationWarning> warnings;

        private ValidationResult(boolean valid, List<ValidationError> errors, List<ValidationWarning> warnings) {
            this.valid = valid;
            this.errors = errors;
            this.warnings = warnings;
        }

        public static ValidationResult success() {
            return new ValidationResult(true, Collections.emptyList(), Collections.emptyList());
        }

        public static ValidationResult fail(List<ValidationError> errors, List<ValidationWarning> warnings) {
            return new ValidationResult(false, errors, warnings);
        }

        public boolean isValid() { return valid; }
        public List<ValidationError> getErrors() { return errors; }
        public List<ValidationWarning> getWarnings() { return warnings; }
    }

    /**
     * 验证错误
     */
    public static class ValidationError {
        private final String nodeId;
        private final String nodeName;
        private final String message;
        private final String errorCode;

        public ValidationError(String nodeId, String nodeName, String message, String errorCode) {
            this.nodeId = nodeId;
            this.nodeName = nodeName;
            this.message = message;
            this.errorCode = errorCode;
        }

        public String getNodeId() { return nodeId; }
        public String getNodeName() { return nodeName; }
        public String getMessage() { return message; }
        public String getErrorCode() { return errorCode; }

        @Override
        public String toString() {
            return String.format("[%s] %s: %s", errorCode, nodeName != null ? nodeName : nodeId, message);
        }
    }

    /**
     * 验证警告
     */
    public static class ValidationWarning {
        private final String nodeId;
        private final String message;

        public ValidationWarning(String nodeId, String message) {
            this.nodeId = nodeId;
            this.message = message;
        }

        public String getNodeId() { return nodeId; }
        public String getMessage() { return message; }
    }

    /**
     * 验证工作流
     */
    public ValidationResult validate(List<WorkflowNode> nodes, List<WorkflowEdge> edges) {
        List<ValidationError> errors = new ArrayList<>();
        List<ValidationWarning> warnings = new ArrayList<>();

        if (nodes == null || nodes.isEmpty()) {
            errors.add(new ValidationError(null, null, "工作流没有节点", "NO_NODES"));
            return ValidationResult.fail(errors, warnings);
        }

        // 构建节点映射
        Map<String, WorkflowNode> nodeMap = new HashMap<>();
        for (WorkflowNode node : nodes) {
            nodeMap.put(node.getId(), node);
        }

        // 1. 检查开始节点
        List<WorkflowNode> startNodes = new ArrayList<>();
        for (WorkflowNode node : nodes) {
            if ("start".equals(node.getType())) {
                startNodes.add(node);
            }
        }

        if (startNodes.isEmpty()) {
            errors.add(new ValidationError(null, null, "工作流缺少开始节点", "NO_START_NODE"));
        } else if (startNodes.size() > 1) {
            errors.add(new ValidationError(null, null, 
                "工作流有多个开始节点: " + startNodes.size(), "MULTIPLE_START_NODES"));
        }

        // 2. 检查结束节点
        List<WorkflowNode> endNodes = new ArrayList<>();
        for (WorkflowNode node : nodes) {
            if ("end".equals(node.getType())) {
                endNodes.add(node);
            }
        }

        if (endNodes.isEmpty()) {
            warnings.add(new ValidationWarning(null, "工作流没有结束节点，可能导致流程无法正常结束"));
        }

        // 3. 检查边的有效性
        Set<String> connectedNodeIds = new HashSet<>();
        if (edges != null) {
            for (WorkflowEdge edge : edges) {
                String sourceId = edge.getSource();
                String targetId = edge.getTarget();

                if (!nodeMap.containsKey(sourceId)) {
                    errors.add(new ValidationError(sourceId, null, 
                        "边的源节点不存在: " + sourceId, "INVALID_EDGE_SOURCE"));
                }

                if (!nodeMap.containsKey(targetId)) {
                    errors.add(new ValidationError(targetId, null, 
                        "边的目标节点不存在: " + targetId, "INVALID_EDGE_TARGET"));
                }

                connectedNodeIds.add(sourceId);
                connectedNodeIds.add(targetId);
            }
        }

        // 4. 检查孤立节点（除了开始节点）
        for (WorkflowNode node : nodes) {
            if (!"start".equals(node.getType()) && !connectedNodeIds.contains(node.getId())) {
                warnings.add(new ValidationWarning(node.getId(), 
                    "节点 '" + node.getName() + "' 没有任何连接，可能是孤立节点"));
            }
        }

        // 5. 检查可达性（从开始节点）
        if (!startNodes.isEmpty()) {
            Set<String> reachable = findReachableNodes(startNodes.get(0).getId(), edges);
            for (WorkflowNode node : nodes) {
                if (!"start".equals(node.getType()) && !reachable.contains(node.getId())) {
                    warnings.add(new ValidationWarning(node.getId(),
                        "节点 '" + node.getName() + "' 从开始节点不可达"));
                }
            }
        }

        // 6. 检查节点配置完整性
        for (WorkflowNode node : nodes) {
            validateNodeConfig(node, errors, warnings);
        }

        // 7. 检查条件节点的分支
        for (WorkflowNode node : nodes) {
            if ("condition".equals(node.getType())) {
                validateConditionBranches(node, edges, errors, warnings);
            }
        }

        // 8. 检查循环节点的连接
        for (WorkflowNode node : nodes) {
            if ("loop".equals(node.getType()) || "while".equals(node.getType())) {
                validateLoopConnections(node, edges, errors, warnings);
            }
        }

        if (errors.isEmpty()) {
            return ValidationResult.success();
        }

        return ValidationResult.fail(errors, warnings);
    }

    /**
     * 查找从指定节点可达的所有节点
     */
    private Set<String> findReachableNodes(String startId, List<WorkflowEdge> edges) {
        Set<String> reachable = new HashSet<>();
        Queue<String> queue = new LinkedList<>();
        queue.add(startId);
        reachable.add(startId);

        while (!queue.isEmpty()) {
            String current = queue.poll();
            if (edges != null) {
                for (WorkflowEdge edge : edges) {
                    if (edge.getSource().equals(current) && !reachable.contains(edge.getTarget())) {
                        reachable.add(edge.getTarget());
                        queue.add(edge.getTarget());
                    }
                }
            }
        }

        return reachable;
    }

    /**
     * 验证节点配置
     */
    private void validateNodeConfig(WorkflowNode node, List<ValidationError> errors, List<ValidationWarning> warnings) {
        String type = node.getType();
        Map<String, Object> config = node.getConfig();

        switch (type) {
            case "llm":
                if (config == null || config.get("modelId") == null) {
                    errors.add(new ValidationError(node.getId(), node.getName(),
                        "LLM节点未配置模型", "LLM_NO_MODEL"));
                }
                if (config == null || config.get("userPrompt") == null) {
                    warnings.add(new ValidationWarning(node.getId(),
                        "LLM节点 '" + node.getName() + "' 未配置用户提示词"));
                }
                break;

            case "condition":
                if (config == null || config.get("expression") == null) {
                    errors.add(new ValidationError(node.getId(), node.getName(),
                        "条件节点未配置表达式", "CONDITION_NO_EXPRESSION"));
                }
                break;

            case "http":
                if (config == null || config.get("url") == null) {
                    errors.add(new ValidationError(node.getId(), node.getName(),
                        "HTTP节点未配置URL", "HTTP_NO_URL"));
                }
                break;

            case "code":
                if (config == null || config.get("code") == null) {
                    errors.add(new ValidationError(node.getId(), node.getName(),
                        "代码节点未配置代码", "CODE_NO_CODE"));
                }
                break;

            case "loop":
            case "iterator":
                if (config == null || config.get("listVariable") == null) {
                    warnings.add(new ValidationWarning(node.getId(),
                        "循环节点 '" + node.getName() + "' 未配置列表变量"));
                }
                break;

            case "knowledge":
                if (config == null || config.get("knowledgeBaseId") == null) {
                    errors.add(new ValidationError(node.getId(), node.getName(),
                        "知识库节点未配置知识库", "KNOWLEDGE_NO_KB"));
                }
                break;

            case "tool":
                if (config == null || config.get("toolName") == null) {
                    errors.add(new ValidationError(node.getId(), node.getName(),
                        "工具节点未配置工具名称", "TOOL_NO_NAME"));
                }
                break;

            case "subflow":
                if (config == null || config.get("workflowId") == null) {
                    errors.add(new ValidationError(node.getId(), node.getName(),
                        "子流程节点未配置工作流", "SUBFLOW_NO_WORKFLOW"));
                }
                break;

            case "database":
                if (config == null || config.get("sql") == null || ((String) config.get("sql")).trim().isEmpty()) {
                    errors.add(new ValidationError(node.getId(), node.getName(),
                        "数据库节点未配置SQL语句", "DATABASE_NO_SQL"));
                }
                break;

            case "email":
                if (config == null || config.get("to") == null || ((String) config.get("to")).trim().isEmpty()) {
                    errors.add(new ValidationError(node.getId(), node.getName(),
                        "邮件节点未配置收件人", "EMAIL_NO_TO"));
                }
                if (config == null || config.get("subject") == null || ((String) config.get("subject")).trim().isEmpty()) {
                    warnings.add(new ValidationWarning(node.getId(),
                        "邮件节点 '" + node.getName() + "' 未配置主题"));
                }
                break;

            case "cache":
                if (config == null || config.get("key") == null || ((String) config.get("key")).trim().isEmpty()) {
                    errors.add(new ValidationError(node.getId(), node.getName(),
                        "缓存节点未配置缓存键", "CACHE_NO_KEY"));
                }
                break;

            case "webhook":
                if (config == null || config.get("url") == null || ((String) config.get("url")).trim().isEmpty()) {
                    errors.add(new ValidationError(node.getId(), node.getName(),
                        "Webhook节点未配置URL", "WEBHOOK_NO_URL"));
                }
                break;
        }
    }

    /**
     * 验证条件节点分支
     */
    private void validateConditionBranches(WorkflowNode node, List<WorkflowEdge> edges, 
            List<ValidationError> errors, List<ValidationWarning> warnings) {
        
        boolean hasTrueBranch = false;
        boolean hasFalseBranch = false;

        if (edges != null) {
            for (WorkflowEdge edge : edges) {
                if (edge.getSource().equals(node.getId())) {
                    String handle = edge.getSourceHandle();
                    if ("true".equals(handle) || "yes".equals(handle)) {
                        hasTrueBranch = true;
                    } else if ("false".equals(handle) || "no".equals(handle)) {
                        hasFalseBranch = true;
                    }
                }
            }
        }

        if (!hasTrueBranch) {
            warnings.add(new ValidationWarning(node.getId(),
                "条件节点 '" + node.getName() + "' 缺少 true 分支"));
        }
        if (!hasFalseBranch) {
            warnings.add(new ValidationWarning(node.getId(),
                "条件节点 '" + node.getName() + "' 缺少 false 分支"));
        }
    }

    /**
     * 验证循环节点连接
     */
    private void validateLoopConnections(WorkflowNode node, List<WorkflowEdge> edges,
            List<ValidationError> errors, List<ValidationWarning> warnings) {
        
        boolean hasLoopBranch = false;
        boolean hasDoneBranch = false;

        if (edges != null) {
            for (WorkflowEdge edge : edges) {
                if (edge.getSource().equals(node.getId())) {
                    String handle = edge.getSourceHandle();
                    if ("loop".equals(handle) || "body".equals(handle)) {
                        hasLoopBranch = true;
                    } else if ("done".equals(handle) || "exit".equals(handle) || handle == null) {
                        hasDoneBranch = true;
                    }
                }
            }
        }

        if (!hasLoopBranch) {
            warnings.add(new ValidationWarning(node.getId(),
                "循环节点 '" + node.getName() + "' 缺少循环体分支"));
        }
        if (!hasDoneBranch) {
            warnings.add(new ValidationWarning(node.getId(),
                "循环节点 '" + node.getName() + "' 缺少完成分支"));
        }
    }

    /**
     * 快速验证（只检查严重错误）
     */
    public boolean quickValidate(List<WorkflowNode> nodes, List<WorkflowEdge> edges) {
        if (nodes == null || nodes.isEmpty()) return false;

        boolean hasStart = false;
        for (WorkflowNode node : nodes) {
            if ("start".equals(node.getType())) {
                hasStart = true;
                break;
            }
        }

        return hasStart;
    }
}
