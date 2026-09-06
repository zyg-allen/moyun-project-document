package com.moyun.ext.cms.service.interview;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * 面试单轮 LLM 输出解析器
 *
 * <p>LLM 输出协议：前段自然语言（面试官口头回应）+ 后段 ```json 围栏块（结构化分析+决策）。
 * 三级解析兜底：围栏切取 → 整段截取 JSON → 返回 null（调用方降级规则评分）。</p>
 *
 * <p>字段全部容错：score 缺省返回 null 由调用方回退规则分，dimensions 缺省为空 Map。</p>
 *
 * @author moyun
 */
@Component
public class InterviewAnalysisParser {

    private final ObjectMapper objectMapper;

    public InterviewAnalysisParser(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    /**
     * 解析 LLM 单轮输出
     *
     * @param raw LLM 原始输出
     * @return 解析结果；完全无 JSON 且无文本时返回 null
     */
    public InterviewTurnResult parse(String raw) {
        if (raw == null || raw.isBlank()) {
            return null;
        }
        String text = raw.trim();
        InterviewTurnResult result = new InterviewTurnResult();

        String json = extractJson(text);
        if (json != null) {
            // reply = 围栏起点之前的全部文本（不含围栏与语言标记）
            String reply = text.substring(0, text.indexOf("```")).trim();
            result.setReply(reply);
            JsonNode node = readTree(json);
            if (node != null) {
                fillStructured(result, node);
                return result;
            }
            // JSON 截取了但解析失败：reply 已提取，结构化降级为空
            return result;
        }

        // 无围栏：尝试整段裸 JSON
        JsonNode node = readTree(text);
        if (node != null && node.isObject() && node.hasNonNull("score")) {
            fillStructured(result, node);
            return result;
        }

        // 纯文本（无 JSON）：仅提取 reply，score 保持 null
        result.setReply(stripFenceMarks(text));
        return result;
    }

    /** 切取 ```json ... ``` 或 ``` ... ``` 围栏内容；无围栏返回 null */
    private String extractJson(String text) {
        int fenceStart = text.indexOf("```");
        if (fenceStart < 0) {
            return null;
        }
        int contentStart = fenceStart + 3;
        // 跳过语言标记（json）
        if (text.startsWith("json", contentStart)) {
            contentStart += 4;
        }
        int fenceEnd = text.indexOf("```", contentStart);
        String content = fenceEnd > contentStart
                ? text.substring(contentStart, fenceEnd)
                : text.substring(contentStart);
        return content.trim();
    }

    private JsonNode readTree(String json) {
        try {
            JsonNode node = objectMapper.readTree(json);
            return node.isObject() ? node : null;
        } catch (Exception e) {
            // 兜底：截取第一个 { 到最后一个 }
            int st = json.indexOf('{');
            int en = json.lastIndexOf('}');
            if (st >= 0 && en > st) {
                try {
                    JsonNode node = objectMapper.readTree(json.substring(st, en + 1));
                    return node.isObject() ? node : null;
                } catch (Exception ignored) {
                }
            }
            return null;
        }
    }

    private void fillStructured(InterviewTurnResult r, JsonNode node) {
        r.setScore(clampOrNull(node.path("score").asInt(-1)));

        JsonNode dims = node.path("dimensions");
        if (dims.isObject()) {
            for (String key : new String[]{"relevance", "professionalism", "fluency", "logic", "confidence"}) {
                int v = dims.path(key).asInt(-1);
                if (v >= 0) {
                    r.getDimensions().put(key, Math.min(100, v));
                }
            }
        }

        if (r.getReply().isEmpty()) {
            String feedback = node.path("feedback").asText("").trim();
            r.setReply(feedback);
        }
        r.setFeedback(node.path("feedback").asText("").trim());

        r.setFlaws(readStringList(node.path("flaws"), 3));
        r.setRedFlags(readStringList(node.path("redFlags"), 2));

        JsonNode sentiment = node.path("sentiment");
        if (sentiment.isObject()) {
            InterviewTurnResult.Sentiment s = new InterviewTurnResult.Sentiment();
            s.setState(sentiment.path("state").asText("").trim());
            s.setNote(sentiment.path("note").asText("").trim());
            if (!s.getState().isEmpty()) {
                r.setSentiment(s);
            }
        }

        JsonNode fluency = node.path("fluencyAssessment");
        if (!fluency.isObject()) {
            fluency = node.path("fluency");
        }
        if (fluency.isObject()) {
            InterviewTurnResult.Fluency f = new InterviewTurnResult.Fluency();
            int fs = fluency.path("score").asInt(-1);
            if (fs >= 0) {
                f.setScore(Math.min(100, fs));
            }
            f.setComment(fluency.path("comment").asText("").trim());
            r.setFluencyAssessment(f);
        }

        JsonNode completeness = node.path("completeness");
        if (completeness.isObject()) {
            InterviewTurnResult.Completeness c = new InterviewTurnResult.Completeness();
            c.setCovered(readStringList(completeness.path("covered"), 6));
            c.setMissing(readStringList(completeness.path("missing"), 6));
            r.setCompleteness(c);
        }

        r.setLevel(node.path("level").asText("").trim());
        r.setFollowupWorth(node.path("followupWorth").asBoolean(false));
        r.setNextAction(node.path("nextAction").asText("").trim());
        r.setNextQuestion(node.path("nextQuestion").asText("").trim());
        long cid = node.path("candidateId").asLong(-1);
        r.setCandidateId(cid > 0 ? cid : null);
        r.setTransition(node.path("transition").asText("").trim());
        r.setGuidance(node.path("guidance").asText("").trim());
    }

    private List<String> readStringList(JsonNode node, int limit) {
        List<String> list = new ArrayList<>();
        if (node != null && node.isArray()) {
            for (JsonNode item : node) {
                String t = item.asText("").trim();
                if (!t.isEmpty() && list.size() < limit) {
                    list.add(t);
                }
            }
        }
        return list;
    }

    private Integer clampOrNull(int v) {
        if (v < 0) {
            return null;
        }
        return Math.min(100, v);
    }

    /** 清理围栏符号与首尾空白 */
    private String stripFenceMarks(String s) {
        return s.replace("```", "").trim();
    }
}
