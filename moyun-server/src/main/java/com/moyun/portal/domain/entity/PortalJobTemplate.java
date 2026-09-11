package com.moyun.portal.domain.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.moyun.core.base.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 岗位模板对象 portal_job_template
 *
 * <p>JD/关键词/出题权重配置，支撑 QuestionPicker 智能出题（job 题源）。</p>
 *
 * @author moyun
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("portal_job_template")
public class PortalJobTemplate extends BaseEntity {
    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 模板名称（如：Java后端工程师） */
    private String name;

    /** 岗位类别（技术/产品/运营/设计等） */
    private String category;

    /** 岗位编码（对齐 portal_voice_interview.position） */
    private String positionCode;

    /** 模板描述 */
    private String description;

    /** 岗位 JD 原文（用于 LLM 关键词提取与出题上下文） */
    private String jdText;

    /** 岗位关键词，逗号分隔（LLM 提取 + 人工维护） */
    private String keywords;

    /** 难度:easy,medium,hard（对齐 portal_interview_question.difficulty） */
    private String difficulty;

    /** 默认出题数量 */
    private Integer questionCount;

    /** 出题权重 JSON（job岗位核心/resume简历深挖/weak薄弱点/random随机兜底） */
    private String weights;

    /** 状态:active 启用/inactive 停用 */
    private String status;
}