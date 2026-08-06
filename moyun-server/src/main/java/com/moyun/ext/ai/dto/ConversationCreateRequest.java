package com.moyun.ext.ai.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 创建会话请求DTO
 *
 * <p>用于创建新的对话会话</p>
 *
 * @author laomao
 * @time 2025/11/23
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ConversationCreateRequest {

    /**
     * 智能体ID
     */
    private Long agentId;

    /**
     * 会话标题（可选）
     */
    private String title;
}
