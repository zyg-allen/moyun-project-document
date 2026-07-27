package com.moyun.agent.dto;

import lombok.Data;
import java.util.List;

/**
 * 架构图对话请求 DTO
 *
 * @author laomao
 */
@Data
public class DiagramChatDTO {
    
    /**
     * 用户消息
     */
    private String message;
    
    /**
     * 图表风格: normal(普通) / enterprise(企业级)
     */
    private String style;
    
    /**
     * 对话历史（用于上下文理解）
     */
    private List<ChatMessage> history;
    
    /**
     * 当前架构图 XML（用于增量修改）
     */
    private String currentDiagramXml;
    
    @Data
    public static class ChatMessage {
        private String role;  // user / assistant
        private String content;
    }
}
