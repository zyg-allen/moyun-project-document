package com.moyun.ext.ai2.model.data;

import lombok.Data;

/**
 * 场景1：面试交互数据（scene = voice_interview / interview）
 *
 * @author laomao
 * @since 2026-09-09
 */
@Data
public class InterviewSceneData {

    /** 当前问题 */
    private String question;

    /** 问题ID */
    private String questionId;

    /** 问题类型：八股/算法/项目 */
    private String questionType;

    /** 当前轮次 */
    private Integer round;

    /** 总轮次 */
    private Integer totalRounds;

    /** 下一步动作：ask/followup/next/end */
    private String nextAction;

    /** 提示 */
    private String hint;

    /** 建议思考时间（秒） */
    private Long thinkTime;

    /** 评估反馈（answer评估场景） */
    private String evaluation;

    /** 评分（0-100） */
    private Integer score;
}
