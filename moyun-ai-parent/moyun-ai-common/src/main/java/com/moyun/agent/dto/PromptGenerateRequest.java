package com.moyun.agent.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 生成系统提示词请求DTO
 *
 * <p>用于接收前端传入的智能体描述信息，生成专业的系统提示词</p>
 *
 * @author laomao
 * @time 2025/11/23
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class PromptGenerateRequest {

    /**
     * 用户输入的简单描述
     * 例如："一个专业的IT技术顾问"
     */
    private String description;

    /**
     * 使用的模型配置ID（可选，不传则使用默认对话模型）
     */
    private Long modelConfigId;
}
