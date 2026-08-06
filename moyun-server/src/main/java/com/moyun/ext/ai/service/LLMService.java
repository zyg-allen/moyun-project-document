package com.moyun.ext.ai.service;

/**
 * LLM服务接口
 * 
 * <p>封装LLM调用，简化业务代码</p>
 * 
 * @author laomao
 */
public interface LLMService {
    
    /**
     * 使用默认模型生成文本
     * 
     * @param prompt 提示词
     * @return 生成的文本
     */
    String generate(String prompt);
    
    /**
     * 使用指定模型生成文本
     * 
     * @param modelConfigId 模型配置ID
     * @param prompt 提示词
     * @return 生成的文本
     */
    String generate(Long modelConfigId, String prompt);
    
    /**
     * 流式生成文本（SSE）
     * 
     * @param prompt 提示词
     * @param onToken 每个token的回调
     * @param onComplete 完成回调
     * @param onError 错误回调
     */
    void generateStream(String prompt, 
                        java.util.function.Consumer<String> onToken,
                        Runnable onComplete,
                        java.util.function.Consumer<Throwable> onError);
}
