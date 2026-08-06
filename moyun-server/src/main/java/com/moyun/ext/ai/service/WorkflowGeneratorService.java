package com.moyun.ext.ai.service;

/**
 * 工作流自然语言生成服务接口
 * 
 * <p>通过自然语言描述自动生成完整的工作流定义</p>
 * 
 * @author laomao
 * @since 2025-11-30
 */
public interface WorkflowGeneratorService {
    
    /**
     * 根据自然语言描述生成工作流
     * 
     * @param description 用户的自然语言描述
     * @return 生成结果，包含工作流JSON和说明
     */
    GenerateResult generate(String description);
    
    /**
     * 根据自然语言描述生成工作流，并自动保存
     * 
     * @param description 用户的自然语言描述
     * @param workflowName 工作流名称（可选，为空则自动生成）
     * @return 保存后的工作流ID
     */
    Long generateAndSave(String description, String workflowName);
    
    /**
     * 优化/修改已有工作流
     * 
     * @param workflowId 现有工作流ID
     * @param instruction 修改指令
     * @return 优化后的工作流JSON
     */
    GenerateResult optimize(Long workflowId, String instruction);
    
    /**
     * 生成结果
     */
    @lombok.Data
    @lombok.Builder
    @lombok.NoArgsConstructor
    @lombok.AllArgsConstructor
    class GenerateResult {
        /** 是否成功 */
        private boolean success;
        
        /** 生成的工作流JSON（包含nodes和edges） */
        private String graphData;
        
        /** 工作流名称 */
        private String workflowName;
        
        /** 工作流描述 */
        private String workflowDescription;
        
        /** 节点数量 */
        private int nodeCount;
        
        /** 生成说明（给用户看的） */
        private String explanation;
        
        /** 错误信息（如果失败） */
        private String errorMessage;
        
        /** 使用的Token数 */
        private int tokensUsed;
        
        public static GenerateResult success(String graphData, String name, String description, 
                                             int nodeCount, String explanation) {
            return GenerateResult.builder()
                    .success(true)
                    .graphData(graphData)
                    .workflowName(name)
                    .workflowDescription(description)
                    .nodeCount(nodeCount)
                    .explanation(explanation)
                    .build();
        }
        
        public static GenerateResult fail(String errorMessage) {
            return GenerateResult.builder()
                    .success(false)
                    .errorMessage(errorMessage)
                    .build();
        }
    }
}
