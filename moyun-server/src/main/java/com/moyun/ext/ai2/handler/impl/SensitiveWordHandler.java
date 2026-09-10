package com.moyun.ext.ai2.handler.impl;

import com.moyun.ext.ai2.constant.AiErrorCodes;
import com.moyun.ext.ai2.handler.AbstractAiSceneHandler;
import com.moyun.ext.ai2.model.AiExecuteRequest;
import com.moyun.ext.ai2.model.AiExecuteResponse;
import com.moyun.ext.ai2.model.data.SensitiveWordSceneData;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

/**
 * 敏感词检测场景Handler（scene = sensitive_word）
 *
 * <p>职责：文本 → 敏感词识别 + 风险分级（同步分类场景，文档 §6.2 示例的落地实现）。</p>
 *
 * <p>输入参数：userInput（顶层字段，v11.52 契约——用户自由文本统一走 userInput，
 * 网关意图判断/语义缓存键同步消费）</p>
 *
 * @author laomao
 * @since 2026-09-09
 */
@Slf4j
@Component("sensitiveWordHandler")
public class SensitiveWordHandler extends AbstractAiSceneHandler {

    @Override
    public String getSceneCode() {
        return "sensitive_word";
    }

    @Override
    public void validate(AiExecuteRequest request) {
        // 快速失败：顶层 userInput 缺失直接拒绝（分类场景唯一必填参数）
        requireUserInput(request);
    }

    @Override
    public AiExecuteResponse<?> execute(AiExecuteRequest request) {
        String text = requireUserInput(request);

        String systemPrompt = """
                你是内容安全审核专家。检测文本是否包含敏感内容（涉政/色情/暴恐/辱骂/违法广告等），只输出 JSON：
                {"hasSensitive": true/false,
                 "words": ["命中的敏感词或类别"],
                 "riskLevel": "high/medium/low",
                 "suggestion": "处理建议（正常内容给'无风险'）"}
                无敏感内容时 hasSensitive=false、words=[]、riskLevel="low"。禁止输出 JSON 以外内容。""";

        String raw = chat(getSceneCode(), systemPrompt, "待检测文本：\n" + text);
        if (raw == null || raw.isBlank()) {
            return AiExecuteResponse.failure(AiErrorCodes.AI_CALL_FAILED, "AI服务暂不可用");
        }

        Map<String, Object> parsed = parseJsonMap(raw);
        if (parsed == null) {
            return AiExecuteResponse.failure(AiErrorCodes.AI_PARSE_ERROR, "敏感词检测结果解析失败");
        }

        SensitiveWordSceneData data = new SensitiveWordSceneData();
        data.setHasSensitive(Boolean.TRUE.equals(parsed.get("hasSensitive")));
        if (parsed.get("words") instanceof List<?> words) {
            data.setWords(words.stream().map(String::valueOf).toList());
        }
        Object riskLevel = parsed.get("riskLevel");
        data.setRiskLevel(riskLevel != null ? String.valueOf(riskLevel) : "low");
        Object suggestion = parsed.get("suggestion");
        data.setSuggestion(suggestion != null ? String.valueOf(suggestion) : null);
        return AiExecuteResponse.success(data);
    }
}
