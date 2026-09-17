package com.moyun.ext.cms.domain.vo;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

import java.util.List;
import java.util.Map;

/**
 * 语音面试报告 VO（V10.1）
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
     * 面试者简介（v11.90 V2 报告三段式第一栏）
     * <p>key：name 姓名 / skills 技能 / resumeSelfIntro 简历自我介绍 /
     * interviewSelfIntro 面试口头自我介绍 / aiScore 简历AI评分
     */
    private Map<String, String> candidate;

    /**
     * 岗位信息（v11.90 V2 报告三段式第二栏）
     * <p>key：position 岗位 / jobRequirements 岗位要求JD / matchRate 岗位匹配度(%)
     */
    private Map<String, String> jobInfo;

    /** v11.97：整场 LLM 复盘总评（3-5 句，基于简历+岗位+对话；旧报告为 null 前端回退 summary） */
    private String overallComment;

    /** v11.97：LLM 岗位匹配度评估（旧报告为 null 前端回退 jobInfo.matchRate） */
    private JobMatchView jobMatch;

    /** v11.97：结构化亮点（旧报告为 null 前端回退 highlights） */
    private List<PointView> highlightViews;

    /** v11.97：结构化薄弱点（旧报告为 null 前端回退 weakPoints） */
    private List<PointView> weakPointViews;

    /**
     * 逐题点评项
     */
    @Data
    public static class QuestionReview {
        private Integer questionIdx;
        private String question;
        private Integer score;
        private String feedback;
        /** v11.97：候选人原始作答（前端折叠展开展示） */
        private String userAnswer;
        /** v11.97：问答ID（加入错题本用） */
        private Long qaId;
    }

    /**
     * v11.97：岗位匹配度视图（整场 LLM 复盘产出）
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
     * v11.97：结构化亮点/薄弱点视图（整场 LLM 复盘产出）
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
     * 相关知识点视图（v11.30.4：题库 tags 聚合 + LLM 简介增强）
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
}
