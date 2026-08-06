package com.moyun.ext.ai.controller;

import com.moyun.ext.ai.dto.PromptGenerateRequest;
import com.moyun.ext.ai.dto.PromptGenerateResponse;
import com.moyun.ext.ai.service.PromptGeneratorService;
import com.moyun.core.base.AjaxResult;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 系统提示词生成接口
 *
 * <p>供智能体配置页调用，根据描述 AI 生成专业系统提示词</p>
 *
 * @author laomao
 */
@Slf4j
@RestController
@RequestMapping("/cms/ai/prompt")
@RequiredArgsConstructor
public class PromptGeneratorController {

    private final PromptGeneratorService promptGeneratorService;

    /**
     * 根据描述生成专业系统提示词
     */
    @PostMapping("/generate")
    @PreAuthorize("@ss.hasPermi('cms:ai:agent:list')")
    public AjaxResult generate(@Valid @RequestBody PromptGenerateRequest request) {
        log.info("🪄 收到系统提示词生成请求，描述长度: {} 字符", request.getDescription().length());
        PromptGenerateResponse response = promptGeneratorService.generate(request.getDescription());
        return AjaxResult.success(response);
    }
}
