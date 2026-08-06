package com.moyun.ext.ai.service;

import com.moyun.ext.ai.dto.DiagramChatDTO;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

/**
 * 架构图对话服务接口
 *
 * @author laomao
 */
public interface DiagramChatService {
    
    /**
     * 流式生成架构图响应
     *
     * @param dto 对话请求
     * @param emitter SSE 发送器
     */
    void generateStreamResponse(DiagramChatDTO dto, SseEmitter emitter);
}
