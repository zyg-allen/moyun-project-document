package com.moyun.agent.controller;

import com.moyun.agent.dto.DiagramChatDTO;
import com.moyun.agent.service.DiagramChatService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

/**
 * AI 架构图流式生成控制器
 *
 * <p>使用 SSE (Server-Sent Events) 实现流式输出</p>
 *
 * @author laomao
 */
@Slf4j
@Tag(name = "AI架构图-流式")
@RestController
@RequestMapping("/api/diagram")
@RequiredArgsConstructor
public class DiagramStreamController {

    private final DiagramChatService diagramChatService;

    @Operation(summary = "流式生成架构图")
    @PostMapping(value = "/chat/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter chatStream(@RequestBody DiagramChatDTO dto) {
        String msgPreview = "";
        if (dto.getMessage() != null && !dto.getMessage().isEmpty()) {
            msgPreview = dto.getMessage().substring(0, Math.min(50, dto.getMessage().length()));
        }
        log.info("📊 接收到流式架构图生成请求, 内容: {}, 风格: {}", msgPreview, dto.getStyle());
        
        // 创建 SSE Emitter，超时时间 5 分钟
        SseEmitter emitter = new SseEmitter(300000L);
        
        // 异步处理
        diagramChatService.generateStreamResponse(dto, emitter);
        
        return emitter;
    }
}
