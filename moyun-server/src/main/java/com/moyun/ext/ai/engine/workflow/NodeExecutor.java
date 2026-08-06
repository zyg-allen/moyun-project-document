package com.moyun.ext.ai.engine.workflow;

/**
 * 节点执行器接口
 *
 * <p>所有工作流节点类型都需要实现此接口</p>
 *
 * @author laomao
 */
public interface NodeExecutor {

    /**
     * 获取节点类型
     *
     * @return 节点类型标识
     */
    String getType();

    /**
     * 执行节点
     *
     * @param node 节点定义
     * @param context 执行上下文
     * @return 执行结果
     */
    NodeResult execute(WorkflowNode node, WorkflowContext context);

    /**
     * 节点执行结果
     */
    @lombok.Data
    @lombok.Builder
    @lombok.NoArgsConstructor
    @lombok.AllArgsConstructor
    class NodeResult {
        /** 是否成功 */
        private boolean success;

        /** 输出数据 */
        private Object output;

        /** 错误信息 */
        private String errorMessage;

        /** 下一个节点的输出句柄(用于条件分支) */
        private String nextHandle;

        /** 是否终止流程 */
        private boolean terminate;

        /** 是否并行执行 */
        private boolean parallel;

        /** 等待合并的分支数 */
        private int waitBranches;

        public static NodeResult success(Object output) {
            return NodeResult.builder()
                    .success(true)
                    .output(output)
                    .build();
        }

        public static NodeResult success(Object output, String nextHandle) {
            return NodeResult.builder()
                    .success(true)
                    .output(output)
                    .nextHandle(nextHandle)
                    .build();
        }

        public static NodeResult fail(String errorMessage) {
            return NodeResult.builder()
                    .success(false)
                    .errorMessage(errorMessage)
                    .build();
        }

        public static NodeResult terminate(Object output) {
            return NodeResult.builder()
                    .success(true)
                    .output(output)
                    .terminate(true)
                    .build();
        }
    }
}
