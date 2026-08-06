package com.moyun.ext.ai.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.moyun.core.base.AjaxResult;
import com.moyun.ext.ai.dto.DiagramGenerateDTO;
import com.moyun.ext.ai.service.DiagramService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@Slf4j
@Tag(name = "AI架构图")
@RestController
@RequestMapping("/cms/ai/diagram")
@RequiredArgsConstructor
public class DiagramController {

    private final DiagramService diagramService;
    private final ObjectMapper objectMapper;

    @Operation(summary = "生成架构图")
    @PostMapping("/generate")
    @PreAuthorize("@ss.hasPermi('cms:ai:diagram:query')")
    public AjaxResult generate(@RequestBody DiagramGenerateDTO dto) {
        try {
            log.info("📊 接收到架构图生成请求, 内容长度: {}, 风格: {}", 
                    dto.getContent() != null ? dto.getContent().length() : 0,
                    dto.getStyle());

            String jsonResult = diagramService.generateDiagram(dto.getContent(), dto.getStyle());

            Object data = objectMapper.readValue(jsonResult, Object.class);

            log.info("✅ 架构图生成成功");
            return AjaxResult.success(data);

        } catch (IllegalArgumentException e) {
            log.warn("⚠️ 参数错误: {}", e.getMessage());
            return AjaxResult.error(e.getMessage());

        } catch (Exception e) {
            log.error("❌ 架构图生成失败: {}", e.getMessage(), e);
            return AjaxResult.error("架构图生成失败: " + e.getMessage());
        }
    }
}
