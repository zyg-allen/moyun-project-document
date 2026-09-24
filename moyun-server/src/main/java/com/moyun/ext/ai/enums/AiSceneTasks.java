package com.moyun.ext.ai.enums;

/**
 * AI 场景子任务常量（task 拆行，AI统一网关整改 2B.1）
 *
 * <p>task 拆行五处口径：</p>
 * <ul>
 *   <li>ai_scene_config.scene_code 存全码（{@code scene:task}，如 resume_optimize:advice）</li>
 *   <li>{@link AiSceneEnum} 只保留主场景枚举</li>
 *   <li>业务调用传主码 + {@code input.task}（本类常量，AiSceneJsonClient 签名不变）</li>
 *   <li>AiSceneRegistry 路由先全码后主码</li>
 *   <li>ai_execute_log.scene_code 记录全码（按 task 维度统计成本）</li>
 * </ul>
 *
 * <p>本类是 task 短码的唯一权威来源，业务 Service 组装 input 时引用，避免魔法字符串
 * 与配置行 scene_code 拆分口径漂移。新增子任务 = 本类加常量 + 全码配置行 + Service 组装。</p>
 *
 * @author laomao
 * @since 2026-09-24
 */
public final class AiSceneTasks {

    private AiSceneTasks() {
    }

    // ===== resume_optimize（简历优化）=====

    /** 评分明细 → 改进建议（ResumeAiAdviceService） */
    public static final String RESUME_ADVICE = "advice";
    /** JD × 简历 → 岗位匹配报告（ResumeJobMatchService） */
    public static final String RESUME_JOB_MATCH = "job_match";
    /** 字段级 3 版本优化（ResumeDeepOptimizeService） */
    public static final String RESUME_FIELD_ASSIST = "field_assist";
    /** 空字段初始草稿（ResumeDeepOptimizeService） */
    public static final String RESUME_DRAFT_EMPTY = "draft_empty";
    /** 整份简历逐项深度优化（ResumeDeepOptimizeGenerator） */
    public static final String RESUME_DEEP_OPTIMIZE = "deep_optimize";

    // ===== voice_interview（AI 语音面试）=====

    /** 面试预热：候选人画像 + 考察计划 + 开场白 + 首题（VoiceInterviewServiceImpl） */
    public static final String INTERVIEW_WARMUP = "warmup";
    /** 候选人回答深度分析：评分校正/6维/漏洞/追问建议（VoiceInterviewServiceImpl） */
    public static final String INTERVIEW_ANSWER_ANALYSIS = "answer_analysis";
    /** 自我介绍 4 维评分（ScoringEngine） */
    public static final String INTERVIEW_SELF_INTRO = "self_intro";
}
