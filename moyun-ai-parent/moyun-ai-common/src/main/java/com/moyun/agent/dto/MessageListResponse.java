package com.moyun.agent.dto;

import com.moyun.agent.entity.ConversationMessage;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 消息列表响应DTO
 *
 * <p>包含会话的历史消息列表</p>
 *
 * @author laomao
 * @time 2025/11/23
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class MessageListResponse {

    /**
     * 消息列表
     */
    private List<ConversationMessage> messages;

    /**
     * 消息总数
     */
    private Integer total;
}
