package com.moyun.ext.ai.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * AI 生成系统提示词请求
 *
 * @author laomao
 */
@Data
public class PromptGenerateRequest {

    /** 智能体描述或简短提示词，AI 据此润色成专业系统提示词 */
    @NotBlank(message = "描述不能为空")
    @Size(min = 2, max = 2000, message = "描述长度需在2-2000字符之间")
    private String description;
}
