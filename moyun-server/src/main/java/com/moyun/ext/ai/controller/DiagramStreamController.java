package com.moyun.ext.ai.controller;

import com.moyun.core.base.AjaxResult;
import com.moyun.ext.ai.dto.DiagramChatDTO;
import com.moyun.ext.ai.service.DiagramChatService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

@Slf4j
@Tag(name = "AI架构图-流式")
@RestController
@RequestMapping("/cms/ai/diagram")
@RequiredArgsConstructor
public class DiagramStreamController {

    private final DiagramChatService diagramChatService;

    @Operation(summary = "流式生成架构图")
    @PostMapping(value = "/chat/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    @PreAuthorize("@ss.hasPermi('cms:ai:diagram:list')")
    public SseEmitter chatStream(@RequestBody DiagramChatDTO dto) {
        String msgPreview = "";
        if (dto.getMessage() != null && !dto.getMessage().isEmpty()) {
            msgPreview = dto.getMessage().substring(0, Math.min(50, dto.getMessage().length()));
        }
        log.info("📊 接收到流式架构图生成请求, 内容: {}, 风格: {}", msgPreview, dto.getStyle());
        
        SseEmitter emitter = new SseEmitter(300000L);
        
        diagramChatService.generateStreamResponse(dto, emitter);
        
        return emitter;
    }
}
