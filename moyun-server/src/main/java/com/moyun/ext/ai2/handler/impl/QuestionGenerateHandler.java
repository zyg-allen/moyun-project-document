package com.moyun.ext.ai2.handler.impl;

import com.moyun.ext.ai2.constant.AiErrorCodes;
import com.moyun.ext.ai2.handler.AbstractAiSceneHandler;
import com.moyun.ext.ai2.model.AiExecuteRequest;
import com.moyun.ext.ai2.model.AiExecuteResponse;
import com.moyun.ext.ai2.model.data.QuestionSceneData;
import com.moyun.ext.ai2.model.data.QuestionSceneData.GeneratedQuestion;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * 智能出题场景Handler（scene = question_generate）
 *
 * <p>职责：岗位 + 技能标签 + 难度 → 面试题单。
 * 与业务模块 PortalJobTemplateServiceImpl 的提示词语义对齐。</p>
 *
 * <p>输入参数：position(岗位)、skills(技能标签，逗号分隔或列表)、count(题量，默认5)、
 * difficulty(难度 easy/medium/hard，可选)、questionType(题型，可选)</p>
 *
 * @author laomao
 * @since 2026-09-09
 */
@Slf4j
@Component("questionGenerateHandler")
public class QuestionGenerateHandler extends AbstractAiSceneHandler {

    @Override
    public String getSceneCode() {
        return "question_generate";
    }

    @Override
    public AiExecuteResponse<?> execute(AiExecuteRequest request) {
        String position = requireInputString(request, "position");
        Object skillsRaw = request.getInput() != null ? request.getInput().get("skills") : null;
        int count = getInputInteger(request, "count", 5);
        String difficulty = getInputString(request, "difficulty");

        String skills = skillsRaw instanceof List<?> list
                ? String.join(",", list.stream().map(String::valueOf).toList())
                : (skillsRaw != null ? String.valueOf(skillsRaw) : "");

        String systemPrompt = """
                你是技术面试出题专家。基于岗位和技能生成面试题，只输出 JSON：
                {"questions": [{"question": "题目", "type": "八股/算法/场景/项目",
                 "difficulty": "easy/medium/hard", "answer": "参考答案要点",
                 "knowledgePoints": ["考察点"]}]}
                题目要贴合岗位实际要求，覆盖不同层次。禁止输出 JSON 以外内容。""";

        StringBuilder user = new StringBuilder("岗位：").append(position);
        if (!skills.isBlank()) {
            user.append("\n技能要求：").append(skills);
        }
        user.append("\n题量：").append(count).append(" 道");
        if (difficulty != null && !difficulty.isBlank()) {
            user.append("\n难度：").append(difficulty);
        }

        String raw = chat(getSceneCode(), systemPrompt, user.toString());
        if (raw == null || raw.isBlank()) {
            return AiExecuteResponse.failure(AiErrorCodes.AI_CALL_FAILED, "AI服务暂不可用");
        }

        Map<String, Object> parsed = parseJsonMap(raw);
        if (parsed == null || !(parsed.get("questions") instanceof List<?> questions)) {
            return AiExecuteResponse.failure(AiErrorCodes.AI_PARSE_ERROR, "出题结果解析失败");
        }

        List<GeneratedQuestion> result = new ArrayList<>();
        for (Object item : questions) {
            if (item instanceof Map<?, ?> q) {
                GeneratedQuestion gq = new GeneratedQuestion();
                gq.setQuestion(str(q, "question"));
                gq.setType(str(q, "type"));
                gq.setDifficulty(str(q, "difficulty"));
                gq.setAnswer(str(q, "answer"));
                if (q.get("knowledgePoints") instanceof List<?> kps) {
                    gq.setKnowledgePoints(kps.stream().map(String::valueOf).toList());
                }
                result.add(gq);
            }
        }

        QuestionSceneData data = new QuestionSceneData();
        data.setQuestions(result);
        data.setTotalCount(result.size());
        data.setDifficulty(difficulty);
        return AiExecuteResponse.success(data);
    }

    private String str(Map<?, ?> map, String key) {
        Object value = map.get(key);
        return value != null ? String.valueOf(value) : null;
    }
}
