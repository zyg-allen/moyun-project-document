package com.moyun.ext.ai.service;

import com.moyun.ext.ai.dto.PromptGenerateResponse;

/**
 * 系统提示词生成服务
 *
 * <p>根据智能体描述，调用 LLM 生成专业的系统提示词</p>
 *
 * @author laomao
 */
public interface PromptGeneratorService {

    /**
     * 根据描述生成专业系统提示词
     *
     * @param description 智能体描述或简短提示词
     * @return 包含生成提示词的响应
     */
    PromptGenerateResponse generate(String description);
}
