package com.moyun.ext.cms.domain.vo;

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

    /**
     * 逐题点评项
     */
    @Data
    public static class QuestionReview {
        private Integer questionIdx;
        private String question;
        private Integer score;
        private String feedback;
    }
}
