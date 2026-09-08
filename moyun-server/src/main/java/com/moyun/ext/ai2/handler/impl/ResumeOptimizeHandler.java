package com.moyun.ext.ai2.handler.impl;

import com.moyun.ext.ai2.constant.AiErrorCodes;
import com.moyun.ext.ai2.handler.AbstractAiSceneHandler;
import com.moyun.ext.ai2.model.AiExecuteRequest;
import com.moyun.ext.ai2.model.AiExecuteResponse;
import com.moyun.ext.ai2.model.data.ResumeSceneData;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

/**
 * 简历优化场景Handler（scene = resume_optimize）
 *
 * <p>职责：简历内容 + 目标岗位 → 优化建议/评分/关键词。
 * 与业务模块 ResumeAiAdviceService/ResumeJobMatchService 的提示词语义对齐。</p>
 *
 * <p>输入参数：resumeText(简历内容)、targetPosition(目标岗位，可选)</p>
 *
 * @author laomao
 * @since 2026-09-09
 */
@Slf4j
@Component("resumeOptimizeHandler")
public class ResumeOptimizeHandler extends AbstractAiSceneHandler {

    @Override
    public String getSceneCode() {
        return "resume_optimize";
    }

    @Override
    public AiExecuteResponse<?> execute(AiExecuteRequest request) {
        String resumeText = requireInputString(request, "resumeText");
        String targetPosition = getInputString(request, "targetPosition");

        String systemPrompt = """
                你是资深简历优化顾问。结合目标岗位评估简历并给出优化建议，只输出 JSON：
                {"score": 0到100整数（岗位匹配度）,
                 "suggestions": ["具体可执行的优化建议，按重要性排序，3-6条"],
                 "keywords": ["建议补充的关键词"],
                 "optimizedText": "优化后的核心内容片段（可选，重点段落改写）"}
                建议要具体到 STAR 法则、量化成果、技能匹配。禁止输出 JSON 以外内容。""";

        StringBuilder user = new StringBuilder("简历内容：\n").append(resumeText);
        if (targetPosition != null && !targetPosition.isBlank()) {
            user.append("\n\n目标岗位：").append(targetPosition);
        }

        String raw = chat(getSceneCode(), systemPrompt, user.toString());
        if (raw == null || raw.isBlank()) {
            return AiExecuteResponse.failure(AiErrorCodes.AI_CALL_FAILED, "AI服务暂不可用");
        }

        Map<String, Object> parsed = parseJsonMap(raw);
        if (parsed == null) {
            return AiExecuteResponse.failure(AiErrorCodes.AI_PARSE_ERROR, "简历优化结果解析失败");
        }

        ResumeSceneData data = new ResumeSceneData();
        data.setOriginalText(resumeText);
        if (parsed.get("score") instanceof Number number) {
            data.setScore(number.intValue());
        }
        if (parsed.get("suggestions") instanceof List<?> suggestions) {
            data.setSuggestions(suggestions.stream().map(String::valueOf).toList());
        }
        if (parsed.get("keywords") instanceof List<?> keywords) {
            data.setKeywords(keywords.stream().map(String::valueOf).toList());
        }
        if (parsed.get("optimizedText") instanceof String optimized) {
            data.setOptimizedText(optimized);
        }
        return AiExecuteResponse.success(data);
    }
}
