package com.moyun.agent.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.moyun.agent.common.ApiResponse;
import com.moyun.agent.dto.DiagramGenerateDTO;
import com.moyun.agent.service.DiagramService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * AI 架构图生成控制器
 *
 * <p>提供通过 AI 生成架构图的功能</p>
 *
 * @author laomao
 */
@Slf4j
@Tag(name = "AI架构图")
@RestController
@RequestMapping("/api/diagram")
@RequiredArgsConstructor
public class DiagramController {

    private final DiagramService diagramService;
    private final ObjectMapper objectMapper;

    @Operation(summary = "生成架构图")
    @PostMapping("/generate")
    public ResponseEntity<ApiResponse<Object>> generate(@RequestBody DiagramGenerateDTO dto) {
        try {
            log.info("📊 接收到架构图生成请求, 内容长度: {}, 风格: {}", 
                    dto.getContent() != null ? dto.getContent().length() : 0,
                    dto.getStyle());

            // 调用服务生成架构图
            String jsonResult = diagramService.generateDiagram(dto.getContent(), dto.getStyle());

            // 将 JSON 字符串转换为对象返回
            Object data = objectMapper.readValue(jsonResult, Object.class);

            log.info("✅ 架构图生成成功");
            return ResponseEntity.ok(ApiResponse.success(data));

        } catch (IllegalArgumentException e) {
            log.warn("⚠️ 参数错误: {}", e.getMessage());
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error(e.getMessage()));

        } catch (Exception e) {
            log.error("❌ 架构图生成失败: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError()
                    .body(ApiResponse.error("架构图生成失败: " + e.getMessage()));
        }
    }
}
