package com.moyun.agent.dto;

import lombok.Data;
import java.util.List;

/**
 * 聊天请求DTO
 *
 * <p>用于接收前端发送的对话请求参数</p>
 *
 * @author laomao
 */
@Data
public class ChatRequest {

    /** 对话ID（已废弃，使用conversationId） */
    private Long memoryId;

    /** 用户问题 */
    private String message;

    /** 智能体ID */
    private Long agentId;

    /** 会话ID（用于加载历史对话） */
    private Long conversationId;

    /** 是否为系统自动打招呼（true时不保存用户消息） */
    private Boolean isGreeting;

    /** 多模态图片（Base64编码数组） */
    private List<String> images;
}
