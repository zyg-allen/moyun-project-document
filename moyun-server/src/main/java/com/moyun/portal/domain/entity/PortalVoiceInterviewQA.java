package com.moyun.portal.domain.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 语音面试问答表 portal_voice_interview_qa（V10.1）
 *
 * <p>基础字段与主表完全对齐：id / create_time / update_time / del_flag。
 * 不再继承 BaseEntity，避免 create_by / update_by / remark 等不存在列导致 MP 报错。
 *
 * @author moyun
 */
@Data
@TableName("portal_voice_interview_qa")
public class PortalVoiceInterviewQA implements Serializable {
    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 面试会话ID */
    private Long interviewId;

    /** 关联题目ID */
    private Long questionId;

    /** 问题来源 bank=题库/resume_project=简历锚定/llm=智能体生成 */
    private String questionSource;

    /** 主问题目序号（从0开始，追问与主问共享序号） */
    private Integer questionIdx;

    /** 追问父问答ID（NULL=主问，非NULL=追问） */
    private Long parentQaId;

    /** 面试问题（快照自题目标题/追问生成） */
    private String question;

    /** 用户回答（ASR 转写后可编辑） */
    private String userAnswer;

    /** 转写是否被用户编辑（0=原样 1=已编辑） */
    private Integer transcriptionEdited;

    /** AI 反馈（规则化生成） */
    private String aiFeedback;

    /** AI 面试官话术（TTS 播报内容） */
    private String speakText;

    /** 本题评分（0-100） */
    private Integer score;

    /** 规则维度分 JSON（如 {"coverage":80,"length":60,"structure":70}） */
    private String ruleDimensionsJson;

    /** LLM 结构化评分 JSON（scores/total/strengths/weaknesses/comment，ScoringEngine 产出） */
    private String llmScoreJson;

    /** LLM深度分析 JSON（sentiment/fluency/redFlags/completeness） */
    private String llmAnalysisJson;

    /** 已使用提示次数（0~3） */
    private Integer hintUsed;

    /** 答题耗时（毫秒） */
    private Integer latencyMs;

    /** 下一步动作 followup/hint/next/report */
    private String nextAction;

    /** 创建时间 */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @TableField("create_time")
    private LocalDateTime createTime;

    /** 更新时间（与主表对齐，评分后更新） */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @TableField("update_time")
    private LocalDateTime updateTime;

    /** 删除标记（0=存在 2=删除） */
    @TableLogic
    @TableField("del_flag")
    private String delFlag;
}
