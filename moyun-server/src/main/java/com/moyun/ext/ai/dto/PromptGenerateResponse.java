package com.moyun.ext.ai.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * AI 生成系统提示词响应
 *
 * @author laomao
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class PromptGenerateResponse {

    /** AI 生成的专业系统提示词 */
    private String systemPrompt;
}
