package com.moyun.ext.cms.domain.vo;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 语音面试单条问答 VO（V10.1）
 *
 * @author moyun
 */
@Data
public class VoiceInterviewQaVO {

    private Long id;
    private Long interviewId;
    private Long questionId;
    /** 问题来源 bank=题库/resume_project=简历锚定/llm=智能体生成 */
    private String questionSource;
    private Integer questionIdx;
    private Long parentQaId;

    /** 面试问题 */
    private String question;

    /** 用户回答 */
    private String userAnswer;

    /** 转写是否被编辑 */
    private Integer transcriptionEdited;

    /** AI 反馈 */
    private String aiFeedback;

    /** AI 面试官话术（TTS 播报内容） */
    private String speakText;

    /** 本题评分 */
    private Integer score;

    /** 规则维度分 JSON */
    private String ruleDimensionsJson;

    /** 已使用提示次数 */
    private Integer hintUsed;

    /** 答题耗时 */
    private Integer latencyMs;

    /** 下一步动作 followup/hint/next/report */
    private String nextAction;

    private LocalDateTime createTime;
}
