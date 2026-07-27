package com.moyun.agent.engine.workflow;

import com.moyun.agent.exception.BusinessException;
import com.moyun.agent.exception.ErrorCode;
import com.moyun.agent.dto.WorkflowExecutionEvent;
import com.moyun.agent.entity.Workflow;
import com.moyun.agent.entity.WorkflowExecution;
import com.moyun.agent.mapper.WorkflowExecutionMapper;
import com.moyun.agent.mapper.WorkflowMapper;
import com.moyun.agent.util.JsonUtils;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;

/**
 * 工作流执行引擎
 * 
 * <p>核心功能：</p>
 * <ul>
 *     <li>解析工作流JSON定义（nodes和edges）</li>
 *     <li>按拓扑顺序执行节点（从开始节点开始）</li>
 *     <li>处理条件分支和循环逻辑</li>
 *     <li>变量管理和上下文传递</li>
 *     <li>执行日志和结果记录</li>
 * </ul>
 * 
 * <p>安全保护机制：</p>
 * <ul>
 *     <li>最大执行节点数限制：{@value #MAX_NODE_EXECUTIONS}，防止无限循环</li>
 *     <li>执行超时时间：{@value #EXECUTION_TIMEOUT_MS}ms，防止长时间阻塞</li>
 *     <li>异常捕获和记录，保障系统稳定性</li>
 * </ul>
 * 
 * <p>执行流程：</p>
 * <ol>
 *     <li>验证工作流存在和启用状态</li>
 *     <li>创建执行记录</li>
 *     <li>解析工作流图结构</li>
 *     <li>从开始节点开始递归执行</li>
 *     <li>更新执行状态和结果</li>
 * </ol>
 *
 * @author laomao
 * @since 2025-11-30
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class WorkflowEngine {

    private final WorkflowMapper workflowMapper;
    private final WorkflowExecutionMapper executionMapper;
    private final List<NodeExecutor> nodeExecutors;

    /** 节点执行器映射表 */
    private final Map<String, NodeExecutor> executorMap = new ConcurrentHashMap<>();

    /** 最大执行节点数（防止无限循环）*/
    private static final int MAX_NODE_EXECUTIONS = 1000;

    /** 执行超时时间（毫秒）*/
    private static final long EXECUTION_TIMEOUT_MS = 5 * 60 * 1000; // 5分钟

    /** 并行执行线程池 */
    private static final ExecutorService PARALLEL_EXECUTOR = Executors.newFixedThreadPool(
        Runtime.getRuntime().availableProcessors() * 2,
        r -> {
            Thread t = new Thread(r, "workflow-parallel-" + System.currentTimeMillis());
            t.setDaemon(true);
            return t;
        }
    );

    /** 并行执行默认超时时间（秒）*/
    private static final int PARALLEL_TIMEOUT_SECONDS = 120;

    /**
     * 初始化工作流引擎
     * 
     * <p>应用启动时自动执行，注册所有节点执行器</p>
     * <p>通过Spring自动注入所有NodeExecutor实现类，并按类型建立映射</p>
     */
    @PostConstruct
    public void init() {
        if (nodeExecutors != null) {
            for (NodeExecutor executor : nodeExecutors) {
                executorMap.put(executor.getType(), executor);
                log.info("📦 注册节点执行器: {}", executor.getType());
            }
        }
        log.info("✅ 工作流引擎初始化完成，已注册 {} 个节点类型", executorMap.size());
    }

    /**
     * 执行工作流
     * 
     * <p>主流程：</p>
     * <ol>
     *     <li>加载工作流定义并验证</li>
     *     <li>创建执行记录（状态：running）</li>
     *     <li>初始化执行上下文</li>
     *     <li>解析工作流图结构</li>
     *     <li>从开始节点递归执行</li>
     *     <li>更新执行状态（completed/failed）</li>
     *     <li>返回执行结果</li>
     * </ol>
     *
     * @param workflowId 工作流ID
     * @param input 输入参数Map，可为null
     * @return 执行结果，包含输出、耗时、节点日志等
     */
    public WorkflowResult execute(Long workflowId, Map<String, Object> input) {
        return execute(workflowId, input, false);
    }
    
    /**
     * 执行工作流（支持测试模式）
     *
     * @param workflowId 工作流ID
     * @param input 输入参数
     * @param isTestRun 是否为测试运行（测试运行跳过enabled检查）
     * @return 执行结果
     */
    public WorkflowResult execute(Long workflowId, Map<String, Object> input, boolean isTestRun) {
        long startTime = System.currentTimeMillis();

        // 获取工作流定义
        Workflow workflow = workflowMapper.selectById(workflowId);
        if (workflow == null) {
            return WorkflowResult.fail("工作流不存在: " + workflowId);
        }

        // 测试运行跳过enabled检查
        if (!isTestRun && !Boolean.TRUE.equals(workflow.getEnabled())) {
            return WorkflowResult.fail("工作流未启用");
        }

        log.info("🚀 开始执行工作流: id={}, name={}, isTestRun={}", workflowId, workflow.getName(), isTestRun);

        // 创建执行记录
        WorkflowExecution execution = WorkflowExecution.builder()
                .workflowId(workflowId)
                .status("running")
                .inputData(JsonUtils.toJson(input))
                .startTime(LocalDateTime.now())
                .createTime(LocalDateTime.now())
                .build();
        executionMapper.insert(execution);

        // 创建执行上下文
        WorkflowContext context = new WorkflowContext();
        context.setWorkflowId(workflowId);
        context.setExecutionId(execution.getId());
        context.setInput(input != null ? input : new HashMap<>());

        try {
            // 解析工作流图
            WorkflowGraph graph = parseGraph(workflow.getGraphData());
            if (graph == null || graph.getNodes().isEmpty()) {
                throw new BusinessException(ErrorCode.WORKFLOW_PARSE_FAILED, "工作流图定义为空或无效");
            }

            // 找到开始节点
            WorkflowNode startNode = findStartNode(graph);
            if (startNode == null) {
                throw new BusinessException(ErrorCode.WORKFLOW_PARSE_FAILED, "未找到开始节点");
            }

            // 初始化执行计数器
            int[] executionCounter = {0};

            // 执行工作流
            executeNode(startNode, graph, context, startTime, executionCounter);

            // 更新执行记录
            long duration = System.currentTimeMillis() - startTime;
            execution.setStatus("completed");
            execution.setOutputData(JsonUtils.toJson(context.getFinalOutput()));
            execution.setExecutionLog(JsonUtils.toJson(context.getLogs()));
            execution.setDurationMs(duration);
            execution.setEndTime(LocalDateTime.now());
            executionMapper.updateById(execution);

            log.info("✅ 工作流执行完成: id={}, duration={}ms", workflowId, duration);

            // 构建节点执行日志
            List<NodeLog> nodeLogs = context.getLogs().stream()
                    .map(log -> NodeLog.builder()
                            .nodeId(log.getNodeId())
                            .nodeName(log.getNodeName())
                            .status(log.getStatus())
                            .output(log.getOutput())
                            .durationMs(log.getDurationMs())
                            .error(log.getErrorMessage())
                            .build())
                    .collect(java.util.stream.Collectors.toList());

            return WorkflowResult.success(context.getFinalOutput(), execution.getId(), duration, nodeLogs);

        } catch (Exception e) {
            log.error("❌ 工作流执行失败: id={}", workflowId, e);

            execution.setStatus("failed");
            execution.setErrorMessage(e.getMessage());
            execution.setExecutionLog(JsonUtils.toJson(context.getLogs()));
            execution.setDurationMs(System.currentTimeMillis() - startTime);
            execution.setEndTime(LocalDateTime.now());
            executionMapper.updateById(execution);

            return WorkflowResult.fail(e.getMessage(), execution.getId());
        }
    }
    
    /**
     * 带事件回调的执行工作流（用于SSE实时推送）
     * 
     * @param workflowId 工作流ID
     * @param input 输入参数
     * @param isTestRun 是否为测试运行
     * @param eventCallback 事件回调函数
     * @return 执行结果
     */
    public WorkflowResult executeWithCallback(Long workflowId, Map<String, Object> input, 
            boolean isTestRun, Consumer<WorkflowExecutionEvent> eventCallback) {
        long startTime = System.currentTimeMillis();
        
        // 获取工作流定义
        Workflow workflow = workflowMapper.selectById(workflowId);
        if (workflow == null) {
            return WorkflowResult.fail("工作流不存在: " + workflowId);
        }
        
        if (!isTestRun && !Boolean.TRUE.equals(workflow.getEnabled())) {
            return WorkflowResult.fail("工作流未启用");
        }
        
        log.info("🚀 开始执行工作流(SSE): id={}, name={}", workflowId, workflow.getName());
        
        // 创建执行记录
        WorkflowExecution execution = WorkflowExecution.builder()
                .workflowId(workflowId)
                .status("running")
                .inputData(JsonUtils.toJson(input))
                .startTime(LocalDateTime.now())
                .createTime(LocalDateTime.now())
                .build();
        executionMapper.insert(execution);
        
        // 创建执行上下文
        WorkflowContext context = new WorkflowContext();
        context.setWorkflowId(workflowId);
        context.setExecutionId(execution.getId());
        context.setInput(input != null ? input : new HashMap<>());
        context.setEventCallback(eventCallback);  // 设置回调
        
        try {
            // 解析工作流图
            WorkflowGraph graph = parseGraph(workflow.getGraphData());
            if (graph == null || graph.getNodes().isEmpty()) {
                throw new BusinessException(ErrorCode.WORKFLOW_PARSE_FAILED, "工作流图定义为空或无效");
            }
            
            // 🔍 诊断输出：工作流图结构
            log.info("🔍 工作流图结构 - 节点数: {}, 边数: {}", 
                graph.getNodes().size(), 
                graph.getEdges() != null ? graph.getEdges().size() : 0);
            log.info("🔍 节点列表: {}", 
                graph.getNodes().stream()
                    .map(n -> n.getId() + "(" + n.getName() + ":" + n.getType() + ")")
                    .collect(java.util.stream.Collectors.joining(", ")));
            if (graph.getEdges() != null) {
                log.info("🔍 边列表: {}", 
                    graph.getEdges().stream()
                        .map(e -> e.getSource() + " -> " + e.getTarget())
                        .collect(java.util.stream.Collectors.joining(", ")));
            }
            
            // 找到开始节点
            WorkflowNode startNode = findStartNode(graph);
            if (startNode == null) {
                throw new BusinessException(ErrorCode.WORKFLOW_PARSE_FAILED, "未找到开始节点");
            }
            
            // 初始化执行计数器
            int[] executionCounter = {0};
            
            // 执行工作流（会触发事件回调）
            executeNode(startNode, graph, context, startTime, executionCounter);
            
            // 更新执行记录
            long duration = System.currentTimeMillis() - startTime;
            execution.setStatus("completed");
            execution.setOutputData(JsonUtils.toJson(context.getFinalOutput()));
            execution.setExecutionLog(JsonUtils.toJson(context.getLogs()));
            execution.setDurationMs(duration);
            execution.setEndTime(LocalDateTime.now());
            executionMapper.updateById(execution);
            
            log.info("✅ 工作流执行完成: id={}, duration={}ms", workflowId, duration);
            
            // 构建节点日志
            List<NodeLog> nodeLogs = context.getLogs().stream()
                    .map(log -> NodeLog.builder()
                            .nodeId(log.getNodeId())
                            .nodeName(log.getNodeName())
                            .status(log.getStatus())
                            .output(log.getOutput())
                            .durationMs(log.getDurationMs())
                            .error(log.getErrorMessage())
                            .build())
                    .collect(java.util.stream.Collectors.toList());
            
            return WorkflowResult.success(context.getFinalOutput(), execution.getId(), duration, nodeLogs);
            
        } catch (Exception e) {
            log.error("❌ 工作流执行失败: id={}", workflowId, e);
            
            execution.setStatus("failed");
            execution.setErrorMessage(e.getMessage());
            execution.setExecutionLog(JsonUtils.toJson(context.getLogs()));
            execution.setDurationMs(System.currentTimeMillis() - startTime);
            execution.setEndTime(LocalDateTime.now());
            executionMapper.updateById(execution);
            
            return WorkflowResult.fail(e.getMessage(), execution.getId());
        }
    }

    /**
     * 执行单个节点（递归方法）
     * 
     * <p>执行逻辑：</p>
     * <ol>
     *     <li>检查停止标志</li>
     *     <li>循环保护：检查执行次数</li>
     *     <li>超时保护：检查执行时间</li>
     *     <li>获取并调用节点执行器</li>
     *     <li>记录执行日志</li>
     *     <li>保存输出到上下文</li>
     *     <li>查找并执行下一个节点</li>
     * </ol>
     * 
     * @param node 当前节点
     * @param graph 工作流图结构
     * @param context 执行上下文
     * @param workflowStartTime 工作流开始时间戳
     * @param executionCounter 执行计数器（数组用于传递引用）
     */
    private void executeNode(WorkflowNode node, WorkflowGraph graph, WorkflowContext context,
                            long workflowStartTime, int[] executionCounter) {
        if (context.isStopped()) {
            return;
        }

        // 循环保护：检查执行次数
        executionCounter[0]++;
        if (executionCounter[0] > MAX_NODE_EXECUTIONS) {
            throw new BusinessException(ErrorCode.WORKFLOW_EXECUTE_FAILED, "工作流执行超过最大节点数限制（" + MAX_NODE_EXECUTIONS + "），可能存在无限循环");
        }

        // 超时保护：检查执行时间
        if (System.currentTimeMillis() - workflowStartTime > EXECUTION_TIMEOUT_MS) {
            throw new BusinessException(ErrorCode.WORKFLOW_EXECUTE_FAILED, "工作流执行超时（" + (EXECUTION_TIMEOUT_MS / 1000) + "秒）");
        }

        context.setCurrentNodeId(node.getId());
        long nodeStartTime = System.currentTimeMillis();
        
        // ⭐ 发送节点开始事件
        if (context.getEventCallback() != null) {
            context.getEventCallback().accept(
                WorkflowExecutionEvent.nodeStart(node.getId(), node.getName(), node.getType())
            );
        }

        log.info("▶ 执行节点: id={}, type={}, name={}", node.getId(), node.getType(), node.getName());

        // 获取节点执行器
        NodeExecutor executor = executorMap.get(node.getType());
        if (executor == null) {
            throw new BusinessException(ErrorCode.WORKFLOW_NODE_FAILED, "未知节点类型: " + node.getType());
        }

        // 执行节点
        NodeExecutor.NodeResult result;
        try {
            result = executor.execute(node, context);
        } catch (Exception e) {
            result = NodeExecutor.NodeResult.fail(e.getMessage());
        }

        long nodeDuration = System.currentTimeMillis() - nodeStartTime;

        // 记录执行日志
        WorkflowContext.NodeExecutionLog nodeLog = new WorkflowContext.NodeExecutionLog();
        nodeLog.setNodeId(node.getId());
        nodeLog.setNodeName(node.getName());
        nodeLog.setNodeType(node.getType());
        nodeLog.setStatus(result.isSuccess() ? "completed" : "failed");
        nodeLog.setInput(context.getInput());
        nodeLog.setOutput(result.getOutput());
        nodeLog.setErrorMessage(result.getErrorMessage());
        nodeLog.setDurationMs(nodeDuration);
        nodeLog.setTimestamp(System.currentTimeMillis());
        context.addLog(nodeLog);

        // 保存节点输出
        context.setNodeOutput(node.getId(), result.getOutput());

        if (!result.isSuccess()) {
            // ⭐ 发送节点错误事件
            if (context.getEventCallback() != null) {
                context.getEventCallback().accept(
                    WorkflowExecutionEvent.nodeError(node.getId(), node.getName(), result.getErrorMessage())
                );
            }
            throw new BusinessException(ErrorCode.WORKFLOW_NODE_FAILED, "节点执行失败[" + node.getName() + "]: " + result.getErrorMessage());
        }
        
        // ⭐ 发送节点完成事件
        if (context.getEventCallback() != null) {
            context.getEventCallback().accept(
                WorkflowExecutionEvent.nodeComplete(node.getId(), node.getName(), result.getOutput(), nodeDuration)
            );
        }

        if (result.isTerminate()) {
            context.setStopped(true);
            return;
        }

        // 查找下一个节点
        List<WorkflowNode> nextNodes = findNextNodes(node, graph, result.getNextHandle());
        
        log.info("🔍 节点 {} 的下一个节点: {} 个", node.getId(), nextNodes.size());
        if (!nextNodes.isEmpty()) {
            log.info("  下一步执行: {}", 
                nextNodes.stream()
                    .map(n -> n.getId() + "(" + n.getName() + ")")
                    .collect(java.util.stream.Collectors.joining(", ")));
        }

        // 检查是否需要并行执行
        if (result.isParallel() && nextNodes.size() > 1) {
            executeNodesInParallel(nextNodes, graph, context, workflowStartTime, executionCounter);
        } else {
            for (WorkflowNode nextNode : nextNodes) {
                executeNode(nextNode, graph, context, workflowStartTime, executionCounter);
            }
        }
    }

    /**
     * 查找当前节点的下一个节点列表
     * 
     * <p>根据边（edge）关系查找，支持条件分支：</p>
     * <ul>
     *     <li>普通节点：查找所有source为当前节点的边</li>
     *     <li>条件节点：只查找匹配sourceHandle的边（如"true"/"false"）</li>
     * </ul>
     * 
     * @param currentNode 当前节点
     * @param graph 工作流图结构
     * @param handle 输出句柄（用于条件分支），可为null
     * @return 下一个节点列表，可能为空
     */
    private List<WorkflowNode> findNextNodes(WorkflowNode currentNode, WorkflowGraph graph, String handle) {
        List<WorkflowNode> nextNodes = new ArrayList<>();

        for (WorkflowEdge edge : graph.getEdges()) {
            if (edge.getSource().equals(currentNode.getId())) {
                // 检查句柄匹配
                if (handle != null && edge.getSourceHandle() != null) {
                    if (!handle.equals(edge.getSourceHandle())) {
                        continue;
                    }
                }

                // 找到目标节点
                WorkflowNode targetNode = findNodeById(graph, edge.getTarget());
                if (targetNode != null) {
                    nextNodes.add(targetNode);
                }
            }
        }

        return nextNodes;
    }

    /**
     * 并行执行多个节点
     * 
     * <p>使用CompletableFuture实现真正的并行执行</p>
     * <p>所有分支完成后才继续执行后续节点</p>
     * 
     * @param nodes 要并行执行的节点列表
     * @param graph 工作流图
     * @param context 执行上下文
     * @param workflowStartTime 工作流开始时间
     * @param executionCounter 执行计数器
     */
    private void executeNodesInParallel(
            List<WorkflowNode> nodes,
            WorkflowGraph graph,
            WorkflowContext context,
            long workflowStartTime,
            int[] executionCounter) {
        
        log.info("⚡ 开始并行执行 {} 个分支", nodes.size());
        
        // 获取超时配置
        int timeout = PARALLEL_TIMEOUT_SECONDS;
        Object timeoutConfig = context.getVariable("_parallel_timeout");
        if (timeoutConfig instanceof Number) {
            timeout = ((Number) timeoutConfig).intValue();
        }
        
        // 为每个分支创建独立的上下文副本（避免并发修改问题）
        List<WorkflowContext> branchContexts = new ArrayList<>();
        for (int i = 0; i < nodes.size(); i++) {
            WorkflowContext branchContext = context.createBranchContext("branch_" + i);
            branchContexts.add(branchContext);
        }
        
        // 记录每个分支的异常
        AtomicReference<Throwable> firstError = new AtomicReference<>();
        
        // 创建并行任务
        List<CompletableFuture<Void>> futures = new ArrayList<>();
        for (int i = 0; i < nodes.size(); i++) {
            final int branchIndex = i;
            final WorkflowNode node = nodes.get(i);
            final WorkflowContext branchContext = branchContexts.get(i);
            
            CompletableFuture<Void> future = CompletableFuture.runAsync(() -> {
                try {
                    log.info("⚡ 分支 {} 开始执行: {}", branchIndex, node.getName());
                    executeNode(node, graph, branchContext, workflowStartTime, executionCounter);
                    log.info("✅ 分支 {} 执行完成: {}", branchIndex, node.getName());
                } catch (Exception e) {
                    log.error("❌ 分支 {} 执行失败: {}", branchIndex, e.getMessage());
                    firstError.compareAndSet(null, e);
                    throw new RuntimeException(e);
                }
            }, PARALLEL_EXECUTOR);
            
            futures.add(future);
        }
        
        try {
            // 等待所有分支完成（带超时）
            CompletableFuture.allOf(futures.toArray(new CompletableFuture[0]))
                .get(timeout, TimeUnit.SECONDS);
            
            log.info("✅ 所有 {} 个并行分支执行完成", nodes.size());
            
            // 合并所有分支的变量到主上下文
            for (int i = 0; i < branchContexts.size(); i++) {
                WorkflowContext branchContext = branchContexts.get(i);
                context.mergeBranchVariables(branchContext, "branch_" + i);
            }
            
            // 合并所有分支的日志
            for (WorkflowContext branchContext : branchContexts) {
                context.mergeLogs(branchContext);
            }
            
        } catch (java.util.concurrent.TimeoutException e) {
            log.error("❌ 并行执行超时（{}秒）", timeout);
            // 取消所有未完成的任务
            futures.forEach(f -> f.cancel(true));
            throw new BusinessException(ErrorCode.WORKFLOW_EXECUTION_TIMEOUT, "并行执行超时");
        } catch (Exception e) {
            Throwable cause = firstError.get();
            if (cause != null) {
                log.error("❌ 并行执行失败", cause);
                if (cause instanceof BusinessException) {
                    throw (BusinessException) cause;
                }
                throw new BusinessException(ErrorCode.WORKFLOW_NODE_FAILED, "并行执行失败: " + cause.getMessage());
            }
            throw new BusinessException(ErrorCode.WORKFLOW_NODE_FAILED, "并行执行失败: " + e.getMessage());
        }
    }

    /**
     * 根据ID查找节点
     * 
     * <p>简单的线性搜索，适用于小规模工作流</p>
     * <p>TODO: 如果节点数量较多，可考虑使用HashMap缓存节点映射</p>
     * 
     * @param graph 工作流图
     * @param nodeId 节点ID
     * @return 节点对象，不存在则返回null
     */
    private WorkflowNode findNodeById(WorkflowGraph graph, String nodeId) {
        for (WorkflowNode node : graph.getNodes()) {
            if (node.getId().equals(nodeId)) {
                return node;
            }
        }
        return null;
    }

    /**
     * 查找开始节点
     * 
     * <p>查找type="start"的节点，每个工作流必须有且只有一个开始节点</p>
     * 
     * @param graph 工作流图
     * @return 开始节点，不存在则返回null
     */
    private WorkflowNode findStartNode(WorkflowGraph graph) {
        for (WorkflowNode node : graph.getNodes()) {
            if ("start".equals(node.getType())) {
                return node;
            }
        }
        return null;
    }

    /**
     * 解析工作流图结构
     * 
     * <p>将JSON字符串反序列化为WorkflowGraph对象</p>
     * 
     * @param graphData JSON格式的工作流图数据
     * @return 解析后的图对象，解析失败返回null
     */
    private WorkflowGraph parseGraph(String graphData) {
        if (graphData == null || graphData.isEmpty()) {
            return null;
        }
        return JsonUtils.fromJson(graphData, WorkflowGraph.class);
    }

    /**
     * 工作流图结构
     */
    @lombok.Data
    public static class WorkflowGraph {
        private List<WorkflowNode> nodes = new ArrayList<>();
        private List<WorkflowEdge> edges = new ArrayList<>();
    }

    /**
     * 工作流执行结果
     */
    @lombok.Data
    @lombok.Builder
    @lombok.NoArgsConstructor
    @lombok.AllArgsConstructor
    public static class WorkflowResult {
        private boolean success;
        private Object output;
        private String errorMessage;
        private Long executionId;
        private Long durationMs;
        private List<NodeLog> nodeLogs;

        public static WorkflowResult success(Object output, Long executionId, Long durationMs, List<NodeLog> nodeLogs) {
            return WorkflowResult.builder()
                    .success(true)
                    .output(output)
                    .executionId(executionId)
                    .durationMs(durationMs)
                    .nodeLogs(nodeLogs)
                    .build();
        }

        public static WorkflowResult success(Object output, Long executionId, Long durationMs) {
            return success(output, executionId, durationMs, null);
        }

        public static WorkflowResult fail(String errorMessage) {
            return WorkflowResult.builder()
                    .success(false)
                    .errorMessage(errorMessage)
                    .build();
        }

        public static WorkflowResult fail(String errorMessage, Long executionId) {
            return WorkflowResult.builder()
                    .success(false)
                    .errorMessage(errorMessage)
                    .executionId(executionId)
                    .build();
        }
    }

    /**
     * 节点执行日志
     */
    @lombok.Data
    @lombok.Builder
    @lombok.NoArgsConstructor
    @lombok.AllArgsConstructor
    public static class NodeLog {
        private String nodeId;
        private String nodeName;
        private String status;
        private Object output;
        private Long durationMs;
        private String error;
    }
}
