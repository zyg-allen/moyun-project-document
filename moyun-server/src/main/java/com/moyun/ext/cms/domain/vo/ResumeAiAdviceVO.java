package com.moyun.ext.cms.domain.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 简历 AI 改进建议 VO（v5.9 阶段2）
 * <p>
 * 当前为规则化生成（基于评分明细 + 岗位匹配度子项），后期可替换为真实 AI 模型调用。
 * 结构设计兼容 AI 流式输出：advice 为分点建议列表，summary 为整体总结。
 *
 * @author moyun
 */
@Data
public class ResumeAiAdviceVO implements Serializable {
    private static final long serialVersionUID = 1L;

    /** 简历ID */
    private Long resumeId;

    /** 当前评分（0-115） */
    private Integer score;

    /** 评分等级：A/B/C/D（A≥90, B≥75, C≥60, D<60） */
    private String grade;

    /** 整体总结（1-2 句话） */
    private String summary;

    /** 改进建议列表（分点） */
    private List<AdviceItem> advices;

    /** 缺失岗位必备技能列表（来自岗位匹配度子项，便于用户针对性补充） */
    private List<String> missingSkills;

    /** 是否启用 AI 模型（当前 false，表示规则化生成；后期接入 AI 后置 true） */
    private Boolean aiPowered;

    /** 生成时间 */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime generatedTime;

    /** 单条改进建议 */
    @Data
    public static class AdviceItem implements Serializable {
        private static final long serialVersionUID = 1L;
        /** 建议维度（如 "基本信息"、"岗位匹配度"） */
        private String dimension;
        /** 优先级 high/medium/low */
        private String priority;
        /** 建议内容 */
        private String content;
        /** 建议类型 fill（补充缺失）/ refine（优化已有）/ match（岗位匹配） */
        private String type;
    }
}
