package com.moyun.ext.cms.support;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.moyun.ext.cms.domain.vo.VoiceInterviewReportVO;
import com.moyun.util.json.LlmJsonExtractor;
import org.apache.commons.lang3.StringUtils;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

/**
 * 面试文本与 JSON 处理工具（v13.54 批次 4 四 / 第①步）。
 *
 * <p><b>为什么抽出来</b>：{@code VoiceInterviewServiceImpl} 曾达 <b>2943 行 / 64 私有方法 / 24 依赖</b>，
 * 四职责（编排 / 提示词装配 / 评分 / 报告）混杂。拆分按「风险从低到高」渐进：
 * <b>第①步先抽无状态纯函数</b> —— 它们不读实例字段（只依赖入参），
 * 抽走后行为**逐字不变**、可被单测直接覆盖，是风险最低的一刀。</p>
 *
 * <p><b>本类只放纯函数</b>：全部 {@code static}，不持有状态、不注入依赖、不做 IO。
 * 需要 {@code ObjectMapper} 的方法由调用方传入（不在此 new，避免与 Spring 配置的
 * 序列化特性分叉）。</p>
 *
 * <p><b>不属于本类</b>：任何读实例字段（{@code interviewMapper}/{@code lockUtil}/缓存…）
 * 或做远程 IO 的方法 —— 它们留在 Service 内，待后续步骤按职责抽取。</p>
 *
 * @author laomao
 */
public final class InterviewTextUtils {

    private InterviewTextUtils() {
    }

    /** 追问预测条数上限（提示词已约束，代码再兜底一次） */
    public static final int MAX_PREDICTED_QUESTIONS = 6;

    /**
     * 数值钳制到 {@code [min, max]}。
     *
     * @param v   原值
     * @param min 下界
     * @param max 上界
     * @return 钳制后的值
     */
    public static int clamp(int v, int min, int max) {
        return Math.max(min, Math.min(max, v));
    }

    /**
     * 文本截断（压缩空白后超长加省略号）。
     *
     * @param text   原文（可空）
     * @param maxLen 最大长度
     * @return 压缩并截断后的文本；入参为空时返回空串（非 null）
     */
    public static String truncateText(String text, int maxLen) {
        if (text == null) {
            return "";
        }
        String flat = text.replaceAll("\\s+", " ").trim();
        return flat.length() > maxLen ? flat.substring(0, maxLen) + "…" : flat;
    }

    /**
     * 技能 JSON 格式化。
     *
     * <p>{@code {"Java":{"level":"了解"}}} 或数组 → {@code "Java·了解 / Python·了解"}；
     * 解析失败原样返回（兜底存量/异构数据，不抛异常）。</p>
     *
     * @param mapper   调用方提供的 ObjectMapper（避免与 Spring 配置分叉）
     * @param rawSkills 技能 JSON 原文（可为普通文本，此时原样返回）
     * @return 可读的技能串
     */
    public static String formatSkills(ObjectMapper mapper, String rawSkills) {
        if (StringUtils.isEmpty(rawSkills) || !rawSkills.trim().startsWith("{")) {
            return rawSkills;
        }
        try {
            JsonNode node = mapper.readTree(rawSkills);
            if (!node.isObject()) {
                return rawSkills;
            }
            List<String> parts = new ArrayList<>();
            Iterator<Map.Entry<String, JsonNode>> it = node.fields();
            while (it.hasNext()) {
                Map.Entry<String, JsonNode> entry = it.next();
                String name = entry.getKey();
                String level = entry.getValue().isContainerNode()
                        ? entry.getValue().path("level").asText("") : entry.getValue().asText("");
                parts.add(StringUtils.isEmpty(level) ? name : name + "·" + level);
            }
            return String.join(" / ", parts);
        } catch (Exception e) {
            return rawSkills;
        }
    }

    /**
     * 容错提取 JSON 对象主体（统一走 {@link LlmJsonExtractor} ——
     * 剥围栏/前后杂文本/括号配平一处实现）。
     *
     * @param mapper 调用方提供的 ObjectMapper
     * @param raw    LLM 原始输出
     * @return 解析出的 JSON 节点；无法解析返回 {@code null}
     */
    public static JsonNode extractJsonObject(ObjectMapper mapper, String raw) {
        return LlmJsonExtractor.extractNode(mapper, raw);
    }

    /**
     * 解析复盘产出的「追问预测」（上限 {@value #MAX_PREDICTED_QUESTIONS} 条）。
     *
     * <p>解析失败或空数组返回**空列表** → 调用方不设置该字段 → 前端**整 tab 隐藏**
     * （对齐「字段为空按缺失隐藏」惯例，报告其余部分照常）。</p>
     *
     * <p>字段口径：{@code question/briefAnswer/analysis/knowledgePoint/askedThisRound/askedScore}，
     * 其中 {@code askedThisRound=true} 的条目在前端归入分组 A「本次已问」（复盘视角），
     * 其余归入分组 B「未被问到」（预警视角，核心价值）。</p>
     *
     * @param arr LLM 产出的 predictedQuestions 节点（可能缺失/非数组/元素非对象）
     * @return 解析成功的预测列表（可能为空，**永不为 null**）
     */
    public static List<VoiceInterviewReportVO.PredictedQuestionView> parsePredictedQuestions(JsonNode arr) {
        List<VoiceInterviewReportVO.PredictedQuestionView> out = new ArrayList<>();
        if (arr == null || !arr.isArray()) {
            return out;
        }
        for (JsonNode item : arr) {
            if (out.size() >= MAX_PREDICTED_QUESTIONS) {
                break; // 硬约束：上限 6 条（提示词已约束，此处再兜底一次）
            }
            if (item == null || !item.isObject()) {
                continue;
            }
            String question = item.path("question").asText("").trim();
            if (StringUtils.isEmpty(question)) {
                continue; // 没有问题文本的条目无价值
            }
            VoiceInterviewReportVO.PredictedQuestionView v = new VoiceInterviewReportVO.PredictedQuestionView();
            v.setQuestion(question);
            v.setBriefAnswer(item.path("briefAnswer").asText(""));
            v.setAnalysis(item.path("analysis").asText(""));
            v.setKnowledgePoint(item.path("knowledgePoint").asText(""));
            boolean asked = item.path("askedThisRound").asBoolean(false);
            v.setAskedThisRound(asked);
            if (asked) {
                int s = item.path("askedScore").asInt(-1);
                if (s >= 0) {
                    v.setAskedScore(clamp(s, 0, 100));
                }
            }
            out.add(v);
        }
        return out;
    }
}
