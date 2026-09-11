package com.moyun.ext.ai2.handler.impl;

import com.moyun.ext.ai.entity.AiSceneConfig;
import com.moyun.ext.ai2.constant.AiErrorCodes;
import com.moyun.ext.ai2.handler.AbstractAiSceneHandler;
import com.moyun.ext.ai2.model.AiExecuteRequest;
import com.moyun.ext.ai2.model.AiExecuteResponse;
import com.moyun.ext.ai2.model.data.SensitiveWordSceneData;
import com.moyun.ext.ai2.support.PromptInjectionGuard;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

/**
 * 敏感词检测场景Handler（scene = sensitive_word）
 *
 * <p>职责：文本 → 敏感词识别 + 风险分级（同步分类场景，文档 §6.2 示例的落地实现）。</p>
 *
 * <p>输入参数：input.text（数据通道，v11.57 P0-3 收口修正——待检测文本是**不可信数据**
 * 而非用户指令：走 userInput 会被网关意图分类器误判低置信度而打断，且语义上与
 * v11.57 双通道防护设计（指令=指令通道 / 数据=数据通道）相悖）</p>
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
        // 快速失败：待检测文本缺失直接拒绝（分类场景唯一必填参数）
        requireInputString(request, "text");
    }

    @Override
    public AiExecuteResponse<?> execute(AiExecuteRequest request, AiSceneConfig config) {
        String text = requireInputString(request, "text");

        String systemPrompt = """
                你是内容安全审核专家。检测文本是否包含敏感内容（涉政/色情/暴恐/辱骂/违法广告等），只输出 JSON：
                {"hasSensitive": true/false,
                 "words": ["命中的敏感词或类别"],
                 "riskLevel": "high/medium/low",
                 "suggestion": "处理建议（正常内容给'无风险'）"}
                无敏感内容时 hasSensitive=false、words=[]、riskLevel="low"。禁止输出 JSON 以外内容。""";

        // v11.57：数据通道隔离——待检测文本为不可信数据，分隔符包裹防注入
        String raw = chat(getSceneCode(), systemPrompt,
                PromptInjectionGuard.wrapData("待检测文本", text));
        if (raw == null || raw.isBlank()) {
            return AiExecuteResponse.failure(AiErrorCodes.AI_CALL_FAILED, "AI服务暂不可用");
        }

        Map<String, Object> parsed = parseOutput(raw, config);
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
