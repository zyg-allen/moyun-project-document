package com.moyun.agent.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 生成系统提示词响应DTO
 *
 * <p>包含AI生成的专业系统提示词</p>
 *
 * @author laomao
 * @time 2025/11/23
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class PromptGenerateResponse {

    /**
     * 生成的系统提示词
     */
    private String systemPrompt;
}
