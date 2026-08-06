package com.moyun.ext.ai.service.impl;

import com.moyun.ext.ai.dto.PromptGenerateResponse;
import com.moyun.ext.ai.exception.BusinessException;
import com.moyun.ext.ai.exception.ErrorCode;
import com.moyun.ext.ai.service.LLMService;
import com.moyun.ext.ai.service.PromptGeneratorService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * 系统提示词生成服务实现
 *
 * <p>调用 LLM 将用户简短描述润色为专业的智能体系统提示词</p>
 *
 * @author laomao
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PromptGeneratorServiceImpl implements PromptGeneratorService {

    private final LLMService llmService;

    @Override
    public PromptGenerateResponse generate(String description) {
        log.info("🪄 开始生成系统提示词，描述长度: {} 字符", description.length());

        try {
            String prompt = buildPrompt(description);
            String response = llmService.generate(prompt);
            String systemPrompt = cleanResponse(response);

            log.info("✅ 系统提示词生成完成，长度: {} 字符", systemPrompt.length());
            return new PromptGenerateResponse(systemPrompt);
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            log.error("❌ 系统提示词生成失败", e);
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "生成失败: " + e.getMessage());
        }
    }

    /**
     * 构建发送给 LLM 的指令
     */
    private String buildPrompt(String description) {
        return """
            你是一位资深的 AI 智能体设计专家。请根据以下描述，为智能体生成一段专业、完整的系统提示词（System Prompt）。

            【用户描述】
            %s

            【生成要求】
            1. 系统提示词应明确智能体的角色定位、能力边界和行为准则
            2. 语言专业、简洁，直接可用，不要包含"以下是系统提示词"等元描述
            3. 如果描述中包含具体业务场景，请结合场景细化行为指引
            4. 控制在 300-800 字以内
            5. 直接输出提示词正文，不要包裹在代码块或引号中
            """.formatted(description);
    }

    /**
     * 清理 LLM 响应：去除代码块包裹、首尾引号等
     */
    private String cleanResponse(String response) {
        if (response == null || response.isBlank()) {
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "LLM 返回空内容");
        }
        String cleaned = response.trim();
        // 去除 markdown 代码块包裹
        if (cleaned.startsWith("```")) {
            cleaned = cleaned.replaceAll("^```[a-zA-Z]*\\n?", "").replaceAll("\\n?```$", "");
        }
        // 去除首尾引号
        if ((cleaned.startsWith("\"") && cleaned.endsWith("\""))
                || (cleaned.startsWith("「") && cleaned.endsWith("」"))) {
            cleaned = cleaned.substring(1, cleaned.length() - 1);
        }
        return cleaned.trim();
    }
}
