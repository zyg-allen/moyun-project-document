package com.moyun.portal.domain.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.moyun.core.base.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 面试配置对象 portal_interview_config
 *
 * <p>人设/提示词模板/评分权重/追问策略/自我介绍环节，全局或按需绑定到面试会话。</p>
 *
 * @author moyun
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("portal_interview_config")
public class PortalInterviewConfig extends BaseEntity {
    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 配置名称（如：标准技术面） */
    private String configName;

    /** 面试官人设:professional/friendly/strict（对齐 voice_interview_style） */
    private String personaType;

    /** 面试官提示词模板（支持 {{position}}/{{resumeDigest}} 等占位符，空则用 Agent 人设） */
    private String promptTemplate;

    /** 评分权重 JSON（5技术维度+LLM融合比例+自我介绍4维度） */
    private String scoringWeights;

    /** 出题权重 JSON（job/resume/weak/random） */
    private String questionWeights;

    /** 每题最大追问次数 */
    private Integer maxFollowups;

    /** 追问触发条件 JSON（vague_answer/contradiction/depth_needed） */
    private String followupTriggers;

    /** 是否启用自我介绍环节（0=旧流程兼容默认） */
    private Integer enableSelfIntro;

    /** 自我介绍建议时长（秒） */
    private Integer selfIntroDuration;

    /** 是否默认配置（互斥，全局唯一） */
    private Integer isDefault;

    /** 状态:active 启用/inactive 停用 */
    private String status;
}