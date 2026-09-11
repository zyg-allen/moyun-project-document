package com.moyun.ext.ai2.handler.impl;

import com.moyun.ext.ai.entity.AiSceneConfig;
import com.moyun.ext.ai2.constant.AiErrorCodes;
import com.moyun.ext.ai2.handler.AbstractAiSceneHandler;
import com.moyun.ext.ai2.model.AiExecuteRequest;
import com.moyun.ext.ai2.model.AiExecuteResponse;
import com.moyun.ext.ai2.model.data.TopicSceneData;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * 今日主题生成场景Handler（scene = daily_topic）
 *
 * <p>职责：日期/领域 → 每日主题（标题/描述/分类）。适用于首页话题、运营栏目等。</p>
 *
 * <p>输入参数：date(日期，可选，默认今天)、domain(领域，可选，如 技术/职场/生活)、
 * excludeTitles(已生成过的标题，可选，避免重复)</p>
 *
 * @author laomao
 * @since 2026-09-09
 */
@Slf4j
@Component("dailyTopicHandler")
public class DailyTopicHandler extends AbstractAiSceneHandler {

    @Override
    public String getSceneCode() {
        return "daily_topic";
    }

    @Override
    public void validate(AiExecuteRequest request) {
        // 全参数可选，跳过非空校验
    }

    @Override
    public AiExecuteResponse<?> execute(AiExecuteRequest request, AiSceneConfig config) {
        String date = getInputString(request, "date");
        String domain = getInputString(request, "domain");
        String excludeTitles = getInputString(request, "excludeTitles");

        String systemPrompt = """
                你是内容运营专家。为指定日期生成一个当日主题，只输出 JSON：
                {"title": "主题标题（15字内，有吸引力）",
                 "description": "主题描述（50字内）",
                 "category": "分类（技术/职场/生活/热点）"}
                标题避免与历史主题重复。禁止输出 JSON 以外内容。""";

        StringBuilder user = new StringBuilder();
        user.append("日期：").append(date != null ? date : java.time.LocalDate.now());
        if (domain != null && !domain.isBlank()) {
            user.append("\n领域：").append(domain);
        }
        if (excludeTitles != null && !excludeTitles.isBlank()) {
            user.append("\n已生成过的标题（避免重复）：").append(excludeTitles);
        }

        String raw = chat(getSceneCode(), systemPrompt, user.toString());
        if (raw == null || raw.isBlank()) {
            return AiExecuteResponse.failure(AiErrorCodes.AI_CALL_FAILED, "AI服务暂不可用");
        }

        Map<String, Object> parsed = parseOutput(raw, config);
        if (parsed == null) {
            return AiExecuteResponse.failure(AiErrorCodes.AI_PARSE_ERROR, "主题生成结果解析失败");
        }

        TopicSceneData data = new TopicSceneData();
        Object title = parsed.get("title");
        data.setTitle(title != null ? String.valueOf(title) : null);
        Object description = parsed.get("description");
        data.setDescription(description != null ? String.valueOf(description) : null);
        Object category = parsed.get("category");
        data.setCategory(category != null ? String.valueOf(category) : null);
        data.setSource("ai_generated");
        return AiExecuteResponse.success(data);
    }
}
