package com.moyun.ext.ai2.model.data;

import lombok.Data;

import java.util.Map;

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

    /**
     * 子任务结构化结果（v11.58 P0-3c 业务收口）：
     * answer_analysis/knowledge_desc 存 LLM 输出的完整 JSON Map；
     * candidate_ask/speak_text 存 {"text": 清洗后文本}。
     * 业务侧经 AiSceneJsonClient.executeForJson 解包。
     */
    private Map<String, Object> structured;
}
