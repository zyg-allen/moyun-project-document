package com.moyun.ext.cms.domain.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 面试题目详情 VO
 *
 * @author moyun
 */
@Data
public class InterviewQuestionDetailVO implements Serializable {
    private static final long serialVersionUID = 1L;

    private Long id;

    private String title;

    private String description;

    private String difficulty;

    private Long categoryId;

    private String categoryName;

    private List<String> tags;

    private List<com.moyun.portal.domain.vo.TagVO> tagList;

    private List<InterviewCompanyVO> companies;

    private BigDecimal acceptanceRate;

    private Long submissionCount;

    private Long likeCount;

    /** 提示 */
    private String hint;

    /** 参考答案（代码题参考代码片段，兼容旧字段） */
    private String solution;

    private Integer sort;

    private String status;

    // ============ 结构化字段（v6.3 题目结构化） ============

    /**
     * 题目类型：bagwen 八股 / algorithm 算法 / system_design 系统设计 / project 项目 / hr HR
     */
    private String questionType;

    /**
     * 考察点列表（由 entity.examinePoints JSON 字符串解析得到）
     * 前端详情页以"考察点"列表形式展示，便于面试者对齐面试官关注点
     */
    private List<String> examinePoints;

    /**
     * 答题大纲（Markdown，结构化答题思路/步骤/框架）
     */
    private String answerOutline;

    /**
     * 评分标准列表（由 entity.scoringCriteria JSON 字符串解析得到）
     * 每项含 dimension 维度 / weight 权重 / description 说明
     */
    private List<ScoringCriterionItem> scoringCriteria;

    /**
     * 官方参考答案（Markdown），八股/设计/项目/HR 类题目完整答案
     */
    private String referenceAnswer;

    /**
     * 前置题目 ID 列表（由 entity.prerequisiteIds 逗号分隔字符串解析得到）
     * 用于学习路径推荐：未通过前置题时提示先做前置题
     */
    private List<Long> prerequisiteIds;

    // ============ 练习模式扩展字段（v10.6 题库重构·阶段2） ============

    /**
     * 练习模式：reading 展示阅读 / choice 选择题 / coding 编程题
     */
    private String practiceMode;

    /**
     * 选择题选项（JSON 字符串，前端 JSON.parse 为 [{label,text,is_correct}]）
     * 仅 practice_mode=choice 时有值
     */
    private String options;

    /**
     * 正确答案（选择题：选项 label 如 B；编程题：null）
     */
    private String correctAnswer;

    /**
     * 题目解析（做题后展示）
     */
    private String analysis;

    /**
     * 知识点标签（逗号分隔字符串）
     */
    private String knowledgeTags;

    /**
     * 评分标准单条项
     */
    @lombok.Data
    public static class ScoringCriterionItem implements Serializable {
        private static final long serialVersionUID = 1L;

        /** 评分维度名称，如"完整性"、"深度"、"代码质量" */
        private String dimension;

        /** 权重（百分比 0-100，或任意数值，前端按权重展示进度条） */
        private Integer weight;

        /** 评分说明 */
        private String description;
    }

    /** 我是否已点赞 */
    private Boolean liked;

    /** 我是否已收藏 */
    private Boolean bookmarked;

    /** 我的做题状态 */
    private String attemptStatus;

    /** 我的最近提交（历史） */
    private List<InterviewSubmissionVO> mySubmissions;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createTime;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime updateTime;
}
