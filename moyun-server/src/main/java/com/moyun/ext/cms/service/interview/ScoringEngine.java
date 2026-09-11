package com.moyun.ext.cms.service.interview;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.moyun.ext.ai2.support.AiSceneJsonClient;
import com.moyun.util.string.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 评分引擎（v11.x 权重化重构）
 *
 * <p>自我介绍评分：LLM 结构化 4 维度（逻辑结构/自我认知/岗位匹配/表达流畅）
 * + 规则校验兜底（字数/结构词），权重来自面试配置 scoring_weights.selfIntro，
 * 默认 30/25/25/20。总分 = Σ(维度分 × 权重)。</p>
 *
 * <p>v11.58 P0-3c：LLM 评分收口 AI 网关（voice_interview 场景 task=self_intro 子任务，
 * 提示词收编至 VoiceInterviewHandler，本类只做解析与权重融合）。</p>
 *
 * <p>每题评分融合（LLM 70% + 规则 30%）随 C1 接入 submitAnswer 链路时启用。</p>
 *
 * @author moyun
 */
@Component
public class ScoringEngine {
    /** v11.39：本服务所属 AI 场景代码（绑定见 ai_scene_config，业务不感知模型选择） */
    private static final String SCENE_VOICE_INTERVIEW = "voice_interview";


    private static final Logger log = LoggerFactory.getLogger(ScoringEngine.class);

    private static final ObjectMapper MAPPER = new ObjectMapper();

    /** 自我介绍默认维度权重：逻辑结构30/自我认知25/岗位匹配25/表达流畅20 */
    private static final int[] DEFAULT_INTRO_WEIGHTS = {30, 25, 25, 20};

    /** 自我介绍结构词（规则维度校验用） */
    private static final String[] STRUCTURE_WORDS = {"首先", "其次", "然后", "最后", "第一", "第二", "目前", "曾经", "负责"};

    /** v11.58 P0-3c：LLM 直调收口网关 */
    @Autowired
    private AiSceneJsonClient aiSceneJsonClient;

    /**
     * 自我介绍评分（LLM 优先，失败回退规则）
     *
     * @param position           面试岗位（岗位匹配维度参考）
     * @param transcript         自我介绍转写文本
     * @param scoringWeightsJson 面试配置 scoring_weights JSON（读取 selfIntro 节点，可空）
     * @param userId             用户ID（网关限流身份/日志归属，可空走匿名桶）
     */
    public IntroScore evaluateSelfIntro(String position, String transcript, String scoringWeightsJson, Long userId) {
        int[] weights = resolveIntroWeights(scoringWeightsJson);
        IntroScore score = tryLlmSelfIntro(position, transcript, weights, userId);
        if (score == null) {
            score = ruleSelfIntro(transcript, weights);
        }
        score.recomputeTotal(weights);
        return score;
    }

    // ==================== LLM 评分（v11.58 P0-3c：经 AI 网关） ====================

    private IntroScore tryLlmSelfIntro(String position, String transcript, int[] weights, Long userId) {
        try {
            JsonNode node = aiSceneJsonClient.executeForJson(
                    SCENE_VOICE_INTERVIEW,
                    Map.of("task", "self_intro",
                            "context", position == null ? "" : position,
                            "transcript", transcript),
                    userId);
            if (node == null) {
                return null;
            }
            return parseIntroScore(node, weights);
        } catch (Exception e) {
            log.warn("[ScoringEngine] LLM 自我介绍评分失败，回退规则评分：{}", e.getMessage());
            return null;
        }
    }

    /** 解析网关结构化自我介绍评分（v11.58：Handler 已容错解析 JSON，此处只做字段映射）；失败返回 null 走规则 */
    private IntroScore parseIntroScore(JsonNode node, int[] weights) {
        try {
            IntroScore score = new IntroScore();
            Map<String, Integer> dims = new LinkedHashMap<>();
            dims.put("structure", clampInt(node.path("scores").path("structure").asInt(-1)));
            dims.put("awareness", clampInt(node.path("scores").path("awareness").asInt(-1)));
            dims.put("matching", clampInt(node.path("scores").path("matching").asInt(-1)));
            dims.put("fluency", clampInt(node.path("scores").path("fluency").asInt(-1)));
            score.setDimensions(dims);
            score.setComment(node.path("comment").asText(""));
            score.setFollowupWorth(node.path("followupWorth").asBoolean(false));
            score.setFollowupQuestion(node.path("followupQuestion").asText(""));
            for (JsonNode s : node.path("strengths")) {
                String t = s.asText("").trim();
                if (StringUtils.isNotEmpty(t) && score.getStrengths().size() < 3) {
                    score.getStrengths().add(t);
                }
            }
            for (JsonNode w : node.path("weaknesses")) {
                String t = w.asText("").trim();
                if (StringUtils.isNotEmpty(t) && score.getWeaknesses().size() < 3) {
                    score.getWeaknesses().add(t);
                }
            }
            return score;
        } catch (Exception e) {
            log.warn("[ScoringEngine] 自我介绍评分 JSON 解析失败：{}", e.getMessage());
            return null;
        }
    }

    // ==================== 规则兜底 ====================

    /** 规则评分：字数 + 结构词 + 岗位关键词命中 */
    private IntroScore ruleSelfIntro(String transcript, int[] weights) {
        IntroScore score = new IntroScore();
        String text = transcript == null ? "" : transcript.trim();
        int len = text.length();

        int structure = 40;
        int hits = 0;
        for (String word : STRUCTURE_WORDS) {
            if (text.contains(word)) {
                hits++;
            }
        }
        if (hits >= 3) {
            structure = 75;
        } else if (hits == 2) {
            structure = 65;
        } else if (hits == 1) {
            structure = 55;
        }
        if (len < 50) {
            structure = Math.min(structure, 40);
        }

        int awareness = len >= 200 ? 70 : (len >= 100 ? 60 : 45);
        int matching = 55;
        int fluency = len >= 150 ? 70 : (len >= 80 ? 60 : 45);

        Map<String, Integer> dims = new LinkedHashMap<>();
        dims.put("structure", structure);
        dims.put("awareness", awareness);
        dims.put("matching", matching);
        dims.put("fluency", fluency);
        score.setDimensions(dims);
        score.setComment(len < 50
                ? "自我介绍偏短，建议补充技术栈、项目亮点与求职方向。"
                : "自我介绍内容基本完整，建议进一步突出与目标岗位匹配的项目亮点。");
        return score;
    }

    // ==================== 融合评分（C1） ====================

    /** 每题默认 LLM 融合比例（%）：LLM 70% + 规则 30% */
    private static final int DEFAULT_LLM_RATIO = 70;

    /** 总分默认权重：自我介绍 20% + 技术问答 80% */
    private static final int[] DEFAULT_TOTAL_WEIGHTS = {20, 80};

    /**
     * 每题评分融合：LLM 分 × llmRatio% + 规则分 × (100-llmRatio)%。
     * 比例来自面试配置 scoring_weights.llmRatio（0-100，默认 70）。
     */
    public int fuseAnswerScore(int llmScore, int ruleScore, String scoringWeightsJson) {
        int ratio = resolveLlmRatio(scoringWeightsJson);
        double fused = llmScore * ratio / 100.0 + ruleScore * (100 - ratio) / 100.0;
        return clampInt((int) Math.round(fused));
    }

    /**
     * 面试总分融合：自我介绍分 × introWeight + 技术问答均分 × (1-introWeight)。
     * 权重来自面试配置 scoring_weights.total（{"intro":20,"tech":80}）。
     */
    public int fuseTotalScore(int introTotal, int techAvg, String scoringWeightsJson) {
        int[] weights = DEFAULT_TOTAL_WEIGHTS.clone();
        if (StringUtils.isNotEmpty(scoringWeightsJson)) {
            try {
                JsonNode node = MAPPER.readTree(scoringWeightsJson).path("total");
                if (!node.isMissingNode() && !node.isNull()) {
                    weights[0] = clampWeight(node.path("intro").asInt(weights[0]));
                    weights[1] = clampWeight(node.path("tech").asInt(weights[1]));
                }
            } catch (Exception e) {
                log.warn("[ScoringEngine] 总分权重解析失败，使用默认值：{}", e.getMessage());
            }
        }
        int wSum = weights[0] + weights[1];
        if (wSum <= 0) {
            return techAvg;
        }
        double fused = (double) introTotal * weights[0] / wSum + (double) techAvg * weights[1] / wSum;
        return clampInt((int) Math.round(fused));
    }

    private int resolveLlmRatio(String scoringWeightsJson) {
        if (StringUtils.isEmpty(scoringWeightsJson)) {
            return DEFAULT_LLM_RATIO;
        }
        try {
            JsonNode node = MAPPER.readTree(scoringWeightsJson).path("llmRatio");
            if (node.isMissingNode() || node.isNull()) {
                return DEFAULT_LLM_RATIO;
            }
            return clampWeight(node.asInt(DEFAULT_LLM_RATIO));
        } catch (Exception e) {
            return DEFAULT_LLM_RATIO;
        }
    }
    // ==================== 权重解析 ====================

    /**
     * 解析自我介绍维度权重：scoring_weights.selfIntro（{"structure":30,"awareness":25,"matching":25,"fluency":20}）
     */
    private int[] resolveIntroWeights(String scoringWeightsJson) {
        int[] weights = DEFAULT_INTRO_WEIGHTS.clone();
        if (StringUtils.isEmpty(scoringWeightsJson)) {
            return weights;
        }
        try {
            JsonNode node = MAPPER.readTree(scoringWeightsJson).path("selfIntro");
            if (node.isMissingNode() || node.isNull()) {
                return weights;
            }
            weights[0] = clampWeight(node.path("structure").asInt(weights[0]));
            weights[1] = clampWeight(node.path("awareness").asInt(weights[1]));
            weights[2] = clampWeight(node.path("matching").asInt(weights[2]));
            weights[3] = clampWeight(node.path("fluency").asInt(weights[3]));
        } catch (Exception e) {
            log.warn("[ScoringEngine] 自我介绍权重解析失败，使用默认值：{}", e.getMessage());
        }
        return weights;
    }

    private int clampWeight(int v) {
        return Math.max(0, Math.min(100, v));
    }

    private int clampInt(int v) {
        return v < 0 ? -1 : Math.min(100, v);
    }

    // ==================== 结果模型 ====================

    /**
     * 自我介绍评分结果（序列化到 portal_voice_interview.intro_score_json）
     */
    @lombok.Data
    public static class IntroScore {

        /** 4 维度分：structure/awareness/matching/fluency */
        private Map<String, Integer> dimensions = new LinkedHashMap<>();

        /** 总分（0-100，按权重加权） */
        private Integer total;

        /** 总评 */
        private String comment;

        /** 亮点（最多 3 条） */
        private List<String> strengths = new ArrayList<>();

        /** 不足（最多 3 条） */
        private List<String> weaknesses = new ArrayList<>();

        /** 是否值得追问 */
        private boolean followupWorth;

        /** 追问问题（followupWorth=true 时非空） */
        private String followupQuestion;

        /** 按权重重算总分（维度缺失 -1 时按 50 计） */
        void recomputeTotal(int[] weights) {
            int wSum = weights[0] + weights[1] + weights[2] + weights[3];
            if (wSum <= 0) {
                total = 0;
                return;
            }
            int[] keys = {0, 1, 2, 3};
            String[] names = {"structure", "awareness", "matching", "fluency"};
            double sum = 0;
            for (int i : keys) {
                Integer v = dimensions.get(names[i]);
                int val = (v == null || v < 0) ? 50 : v;
                sum += (double) val * weights[i] / wSum;
            }
            total = (int) Math.round(sum);
        }
    }
}