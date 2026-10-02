package com.moyun.ext.cms.domain.vo;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

import java.util.List;
import java.util.Map;

/**
 * 语音面试报告 VO
 *
 * <p>结束面试时生成，含总分、维度雷达、亮点/薄弱点、逐题点评。
 *
 * @author moyun
 */
@Data
public class VoiceInterviewReportVO {

    /** 面试ID */
    private Long interviewId;

    /** 总分（0-100） */
    private Integer totalScore;

    /** 维度分（coverage 覆盖率/length 长度/structure 结构/completeness 完整度） */
    private Map<String, Integer> dimensions;

    /** 亮点列表 */
    private List<String> highlights;

    /** 薄弱点列表 */
    private List<String> weakPoints;

    /** 逐题点评（questionIdx -> 点评文本） */
    private List<QuestionReview> questionReviews;

    /** AI 总结 */
    private String summary;

    /** 下次练习建议 */
    private String suggestion;

    /** 心态趋势（逐轮：nervous/confident/hesitant/calm，Agent 模式产出） */
    private List<String> sentimentTrend;

    /** 全场可疑信号汇总（答非所问/背诵痕迹/前后矛盾/夸大数据） */
    private List<String> redFlags;

    /** 表达流畅度均分（0-100，Agent 模式产出） */
    private Integer fluencyAvg;

    /** 自我介绍独立评分（v11.x 6阶段流程产出，旧会话为 null 前端隐藏） */
    private IntroScoreView introScore;

    /** 针对性改进建议（v11.x：来自薄弱点/自我介绍不足/错题，最多 5 条） */
    private List<String> improvementSuggestions;
    private List<KnowledgePointView> knowledgePoints;

    /**
     * 面试者简介（V2 报告三段式第一栏）
     * <p>key：name 姓名 / skills 技能 / resumeSelfIntro 简历自我介绍 /
     * interviewSelfIntro 面试口头自我介绍 / aiScore 简历AI评分
     */
    private Map<String, String> candidate;

    /**
     * 岗位信息（V2 报告三段式第二栏）
     * <p>key：position 岗位 / jobRequirements 岗位要求JD / matchRate 岗位匹配度(%)
     */
    private Map<String, String> jobInfo;

    /** 整场 LLM 复盘总评（3-5 句，基于简历+岗位+对话；旧报告为 null 前端回退 summary） */
    private String overallComment;

    /** LLM 岗位匹配度评估（旧报告为 null 前端回退 jobInfo.matchRate） */
    private JobMatchView jobMatch;

    /** 结构化亮点（旧报告为 null 前端回退 highlights） */
    private List<PointView> highlightViews;

    /** 结构化薄弱点（旧报告为 null 前端回退 weakPoints） */
    private List<PointView> weakPointViews;

    /**
     * 水平定级（结构化字段）。
     *
     * <p>取值 {@code junior} / {@code mid} / {@code senior}（来源：预热阶段
     * {@code voice_interview:warmup} 的画像产物 → {@code portal_interview_config.levelEstimate}）。
     * 前端用于：概要 tab 定级徽章、发展方向 tab 个人化锚点。</p>
     */
    private String levelEstimate;

    /**
     * 追问预测（前端「追问预测」tab 的数据源）。
     *
     * <p>由 {@code voice_interview:report_review} 的 {@code predictedQuestions} 字段产出，
     * 上限 6 条（已问 + 未问合计）。<b>解析失败时该字段为 null</b>，
     * 前端据此**整 tab 隐藏**（对齐「字段为空按缺失隐藏」惯例，报告其余部分照常）。</p>
     *
     * <p>产品定位：分组 A「本次已问」= 复盘视角（答得对不对）；
     * 分组 B「未被问到」= 预警视角（真面试官下次会问什么）——**后者是核心价值**。</p>
     */
    private List<PredictedQuestionView> predictedQuestions;

    /**
     * 发展方向分析（懒生成；用户不打开 tab 则为 null）。
     *
     * <p>写入路径：{@code generateIndustryInsight} 调用场景
     * {@code voice_interview:industry_insight} 后，把结果写入报告 JSON 再整体落库
     * （报告是整段 JSON 存储，故新增字段自动持久化）。</p>
     */
    private IndustryInsightView industryInsight;

    /**
     * 单条追问预测
     */
    @Data
    public static class PredictedQuestionView {
        /** 预测的面试问题 */
        private String question;
        /** 要点式简答（≤80 字） */
        private String briefAnswer;
        /** 为什么会被问（≤60 字） */
        private String analysis;
        /** 考点标签（2-6 字） */
        private String knowledgePoint;
        /** 本场是否已问过（true → 归入「本次已问」分组） */
        private Boolean askedThisRound;
        /** 本场得分（仅 askedThisRound=true 时有值） */
        private Integer askedScore;
    }

    /**
     * 逐题点评项
     */
    @Data
    public static class QuestionReview {
        private Integer questionIdx;
        private String question;
        private Integer score;
        private String feedback;
        /** 候选人原始作答（前端折叠展开展示） */
        private String userAnswer;
        /** 问答ID（加入错题本用） */
        private Long qaId;
    }

    /**
     * 岗位匹配度视图（整场 LLM 复盘产出）
     */
    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class JobMatchView {
        /** 匹配度（0-100） */
        private Integer rate;
        /** 匹配依据（对照 JD 与实际作答，1-2 句） */
        private String reason;
    }

    /**
     * 结构化亮点/薄弱点视图（整场 LLM 复盘产出）
     */
    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class PointView {
        /** 短标题 */
        private String title;
        /** 具体依据/不足说明（引用作答内容） */
        private String detail;
    }

    /**
     * 相关知识点视图（题库 tags 聚合 + LLM 简介增强）
     */
    @Data
    public static class KnowledgePointView {
        private String title;
        private String desc;
    }

    /**
     * 自我介绍评分视图（v11.x）
     */
    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class IntroScoreView {
        /** 4 维度分：structure/awareness/matching/fluency */
        private Map<String, Integer> dimensions;
        /** 总分（0-100，按权重加权） */
        private Integer total;
        /** 总评 */
        private String comment;
        /** 亮点（最多 3 条） */
        private List<String> strengths;
        /** 不足（最多 3 条） */
        private List<String> weaknesses;
    }

    /**
     * 发展方向分析（懒生成，写入报告 JSON 的 {@code industryInsight} 字段）。
     *
     * <p>懒生成：用户不打开 tab 就不产生调用（V1.2 §1 原则 3「成本按需发生」）。
     * 生成一次后随报告 JSON 持久化，前端按 {@code generatedAt} 提示「生成于 X 日 · 刷新」。</p>
     *
     * <p><b>口径诚实</b>：LLM 无实时行业数据，输出为**方向性判断**
     * （技术趋势 / 技能供需结构 / 结合个人短板的行动建议），
     * <b>不承诺实时行业动态</b>——提示词硬约束禁止标注具体百分比、薪酬数字或时效性季度数据。</p>
     */
    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class IndustryInsightView {
        /** 生成时间（毫秒时间戳；前端据此算「生成于 X 日」） */
        private Long generatedAt;
        /** 技术趋势 3-4 条 */
        private List<TrendView> trends;
        /** 技能供需结构：已具备 vs 建议补充 */
        private SupplyDemandView supplyDemand;
        /** 行动建议 3 条（每条强制引用本场真实薄弱点） */
        private List<ActionView> actions;
    }

    /** 技术趋势单条 */
    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class TrendView {
        /** 趋势标题（≤12 字） */
        private String title;
        /** 说明（≤60 字） */
        private String detail;
        /** 成熟度：成熟期 / 上升期 / 早期 */
        private String maturity;
    }

    /** 技能供需结构 */
    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class SupplyDemandView {
        /** 简历已具备的技能标签（前端标 ✅） */
        private List<String> existing;
        /** 建议补充的技能标签（前端标 ⚠️ 欠缺） */
        private List<String> missing;
    }

    /** 行动建议单条 */
    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class ActionView {
        /** 可执行建议（≤80 字） */
        private String content;
        /** 对应的本场真实薄弱点（提示词要求强制引用；前端做锚点关联展示） */
        private String relatedWeakPoint;
    }
}
