package com.moyun.ext.ai2.handler.impl;

import com.moyun.ext.ai2.constant.AiErrorCodes;
import com.moyun.ext.ai2.handler.AbstractAiSceneHandler;
import com.moyun.ext.ai2.model.AiExecuteRequest;
import com.moyun.ext.ai2.model.AiExecuteResponse;
import com.moyun.ext.ai2.model.data.ResumeSceneData;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * 简历解析场景Handler（scene = resume_parse）
 *
 * <p>职责：简历文本 → 结构化 JSON（基本信息/教育/工作/项目/技能）。
 * 与业务模块 ResumeParseService 的提示词语义对齐，通过统一网关复用。</p>
 *
 * <p>输入参数：text(简历原文)</p>
 *
 * @author laomao
 * @since 2026-09-09
 */
@Slf4j
@Component("resumeParseHandler")
public class ResumeParseHandler extends AbstractAiSceneHandler {

    @Override
    public String getSceneCode() {
        return "resume_parse";
    }

    @Override
    public AiExecuteResponse<?> execute(AiExecuteRequest request) {
        String text = requireInputString(request, "text");

        String systemPrompt = """
                你是简历解析专家。从简历文本中提取结构化信息，只输出 JSON：
                {"structured": {"basic": {"name":"", "phone":"", "email":"", "city":""},
                 "education": [{"school":"","major":"","degree":"","period":""}],
                 "work": [{"company":"","position":"","period":"","description":""}],
                 "projects": [{"name":"","role":"","description":""}],
                 "skills": [""]},
                 "keywords": ["核心技能关键词"]}
                无法识别的字段留空字符串或空数组。禁止输出 JSON 以外内容。""";

        String raw = chat(getSceneCode(), systemPrompt, "简历原文：\n" + text);
        if (raw == null || raw.isBlank()) {
            return AiExecuteResponse.failure(AiErrorCodes.AI_CALL_FAILED, "AI服务暂不可用");
        }

        Map<String, Object> parsed = parseJsonMap(raw);
        if (parsed == null) {
            return AiExecuteResponse.failure(AiErrorCodes.AI_PARSE_ERROR, "简历解析结果格式错误");
        }

        ResumeSceneData data = new ResumeSceneData();
        if (parsed.get("structured") instanceof Map<?, ?> structured) {
            @SuppressWarnings("unchecked")
            Map<String, Object> structuredMap = (Map<String, Object>) structured;
            data.setStructured(structuredMap);
        }
        if (parsed.get("keywords") instanceof java.util.List<?> keywords) {
            data.setKeywords(keywords.stream().map(String::valueOf).toList());
        }
        return AiExecuteResponse.success(data);
    }
}
