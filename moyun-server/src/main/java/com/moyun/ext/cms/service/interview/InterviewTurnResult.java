package com.moyun.ext.cms.service.interview;

import lombok.Data;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 面试单轮 LLM 分析结果（Agent 模式统一产出）
 *
 * <p>超集结构：既包含旧 {@code AnswerAnalysis} 的评分/漏洞/追问建议，
 * 也包含 Agent 模式新增的心态感知、流畅度评估、可疑信号、要点覆盖与出题决策。
 * 通过 {@code llm_analysis_json} 持久化，供报告聚合与前端展示。</p>
 *
 * @author moyun
 */
@Data
public class InterviewTurnResult {

    /** 面试官口头回应（自然语言前段，可直接 TTS 播报 / 流式输出） */
    private String reply = "";

    /** 综合评分（0-100，null = LLM 未给出，调用方回退规则分） */
    private Integer score;

    /** 维度分（relevance/professionalism/fluency/logic/confidence） */
    private Map<String, Integer> dimensions = new LinkedHashMap<>();

    /** 点评（先肯定再指出问题） */
    private String feedback = "";

    /** 回答暴露的具体漏洞 */
    private List<String> flaws = new ArrayList<>();

    /** 可疑信号（答非所问/背诵痕迹/前后矛盾/夸大数据） */
    private List<String> redFlags = new ArrayList<>();

    /** 心态感知 */
    private Sentiment sentiment;

    /** 表达流畅度评估 */
    private Fluency fluencyAssessment;

    /** 要点覆盖情况 */
    private Completeness completeness;

    /** 水平评估 junior/mid/senior */
    private String level = "";

    /** 是否值得追问 */
    private boolean followupWorth;

    /** 出题决策：deepen=深挖追问 / change_topic=换新话题 / wrap_up=收尾 */
    private String nextAction = "";

    /** 下一问（deepen/change_topic 时的问题；deepen 必须引用候选人原话） */
    private String nextQuestion = "";

    /** 采用的题库候选编号（null = LLM 自拟） */
    private Long candidateId;

    /** 换题/收尾过渡话术 */
    private String transition = "";

    /** 回答跑偏时的一句引导语 */
    private String guidance = "";

    /** 心态感知结构 */
    @Data
    public static class Sentiment {
        /** nervous/confident/hesitant/calm */
        private String state = "";
        /** 一句话判断依据 */
        private String note = "";
    }

    /** 流畅度评估结构 */
    @Data
    public static class Fluency {
        private Integer score;
        /** 口头禅/重复/停顿与连贯性评价 */
        private String comment = "";
    }

    /** 要点覆盖结构 */
    @Data
    public static class Completeness {
        private List<String> covered = new ArrayList<>();
        private List<String> missing = new ArrayList<>();
    }
}
