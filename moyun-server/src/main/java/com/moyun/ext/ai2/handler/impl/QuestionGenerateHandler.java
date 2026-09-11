package com.moyun.ext.ai2.handler.impl;

import com.moyun.ext.ai.entity.AiSceneConfig;
import com.moyun.ext.ai2.constant.AiErrorCodes;
import com.moyun.ext.ai2.handler.AbstractAiSceneHandler;
import com.moyun.ext.ai2.model.AiExecuteRequest;
import com.moyun.ext.ai2.model.AiExecuteResponse;
import com.moyun.ext.ai2.model.data.QuestionSceneData;
import com.moyun.ext.ai2.model.data.QuestionSceneData.GeneratedQuestion;
import com.moyun.ext.ai2.support.PromptInjectionGuard;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * 智能出题场景Handler（scene = question_generate）
 *
 * <p><strong>v11.58 P0-3 业务收口——双子任务契约：</strong></p>
 * <ul>
 *   <li><b>task=jd_keywords</b>：JD 文本 → 面试考察关键词数组（原
 *       {@code PortalJobTemplateServiceImpl.extractByLlm} 提示词逐字收编），
 *       结果置于 {@link QuestionSceneData#getKeywords()}。</li>
 *   <li><b>task 缺省</b>（管理台场景调试/开放入口）：岗位 + 技能标签 → 面试题单。</li>
 * </ul>
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

    /** JD 关键词提取上限（与原业务 PortalJobTemplateServiceImpl.MAX_LLM_KEYWORDS 一致） */
    private static final int MAX_JD_KEYWORDS = 15;

    @Override
    public String getSceneCode() {
        return "question_generate";
    }

    @Override
    public void validate(AiExecuteRequest request) {
        if ("jd_keywords".equals(getInputString(request, "task"))) {
            requireInputString(request, "context");
            return;
        }
        requireInputString(request, "position");
    }

    @Override
    public AiExecuteResponse<?> execute(AiExecuteRequest request, AiSceneConfig config) {
        if ("jd_keywords".equals(getInputString(request, "task"))) {
            return executeJdKeywords(request);
        }
        return executeQuestionGenerate(request, config);
    }

    // ==================== 子任务：JD 关键词提取（v11.58 业务收口） ====================

    private AiExecuteResponse<?> executeJdKeywords(AiExecuteRequest request) {
        String jdText = requireInputString(request, "context");

        // 与 PortalJobTemplateServiceImpl.extractByLlm 原提示词逐字一致
        String systemPrompt = "从岗位JD中提取面试考察关键词。规则："
                + "1.只提取技术栈、专业能力、业务领域三类实词；"
                + "2.每个关键词2-20个字符，保留英文原文大小写（如 Spring Boot）；"
                + "3.最多" + MAX_JD_KEYWORDS + "个，按重要性降序；"
                + "4.禁止编造JD中不存在的内容。"
                + "只输出JSON数组本体，如 [\"Java\",\"MySQL\"]，禁止markdown代码块。";

        // 数据通道隔离：JD 为外部输入的不可信数据
        String raw = chat(getSceneCode(), systemPrompt,
                PromptInjectionGuard.wrapData("岗位JD", jdText));
        if (raw == null || raw.isBlank()) {
            return AiExecuteResponse.failure(AiErrorCodes.AI_CALL_FAILED, "AI服务暂不可用");
        }

        List<String> keywords = parseJsonStringArray(raw);
        if (keywords == null) {
            return AiExecuteResponse.failure(AiErrorCodes.AI_PARSE_ERROR, "关键词提取结果解析失败");
        }

        QuestionSceneData data = new QuestionSceneData();
        data.setKeywords(keywords);
        return AiExecuteResponse.success(data);
    }

    /** 容错解析 JSON 字符串数组（剥离围栏/截取首尾中括号），失败返回 null */
    private List<String> parseJsonStringArray(String raw) {
        if (raw == null || raw.isBlank()) {
            return null;
        }
        try {
            return MAPPER.readValue(extractJsonArray(raw),
                    new com.fasterxml.jackson.core.type.TypeReference<List<String>>() {
                    });
        } catch (Exception e) {
            log.warn("[ai2:{}] JSON(数组)解析失败: {}", getSceneCode(), e.getMessage());
            return null;
        }
    }

    private String extractJsonArray(String raw) {
        String text = raw.trim();
        if (text.contains("```")) {
            int start = text.indexOf("```");
            int contentStart = text.indexOf('\n', start);
            int end = text.lastIndexOf("```");
            if (contentStart > 0 && end > contentStart) {
                text = text.substring(contentStart + 1, end).trim();
            }
        }
        int begin = text.indexOf('[');
        int end = text.lastIndexOf(']');
        if (begin >= 0 && end > begin) {
            return text.substring(begin, end + 1);
        }
        return text;
    }

    // ==================== 通用模式：面试题生成 ====================

    private AiExecuteResponse<?> executeQuestionGenerate(AiExecuteRequest request, AiSceneConfig config) {
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

        String raw = chat(getSceneCode(), systemPrompt,
                PromptInjectionGuard.wrapData("出题要求", user.toString()));
        if (raw == null || raw.isBlank()) {
            return AiExecuteResponse.failure(AiErrorCodes.AI_CALL_FAILED, "AI服务暂不可用");
        }

        Map<String, Object> parsed = parseOutput(raw, config);
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
