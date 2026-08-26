package com.moyun.portal.domain.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.extension.handlers.JacksonTypeHandler;
import com.baomidou.mybatisplus.annotation.TableField;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 简历优化-岗位匹配报告（v10.13 简历优化重构）
 * <p>每次岗位匹配分析结果存档，可追溯历史评分。dimensions 为四维评分明细 JSON。</p>
 *
 * @author moyun
 */
@Data
@TableName(value = "portal_resume_job_match", autoResultMap = true)
public class PortalResumeJobMatch implements Serializable {
    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long userId;

    /** 简历ID（portal_user_resume.id） */
    private Long resumeId;

    /** 岗位目标ID */
    private Long jobTargetId;

    /** 综合匹配度 0-100 */
    private Integer matchScore;

    /** 评级：excellent/good/medium/poor */
    private String grade;

    /** 已匹配关键词（逗号分隔） */
    private String matchedKeywords;

    /** 缺失关键词（逗号分隔） */
    private String missingKeywords;

    /** 各维度评分明细 JSON（关键词/经验/技能/结构匹配） */
    @TableField(typeHandler = JacksonTypeHandler.class)
    private com.fasterxml.jackson.databind.JsonNode dimensions;

    /** AI 分析总结 */
    private String summary;

    /** 是否 LLM 生成：0=规则 1=LLM */
    private Integer aiPowered;

    private LocalDateTime createTime;
}
