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

    // ===== question_generate（智能出题）=====

    /**
     * JD 原文 → 岗位关键词提取（PortalJobTemplateServiceImpl）。
     *
     * <p><b>v13.43 补常量（报告七 P0-4）</b>：原实现用**裸字面量** {@code "jd_keywords"}，
     * 不在本白名单内 —— 既无法防「乱加子任务」，改名/重构也容易漏。
     * 补齐后 {@code validate()} 的两段式校验才能覆盖它。</p>
     */
    public static final String QUESTION_JD_KEYWORDS = "jd_keywords";

    // ===== voice_interview 新增子任务（方案 V1.2 批次 1）=====

    /**
     * 开场降级：warmup 失败后的简版开场白 + 首题（VoiceInterviewServiceImpl）。
     *
     * <p>方案 V1.1#1「收编不删除」：warmup 失败多为**瞬时网络抖动**（长期存在），
     * 删除兜底 = 一次抖动一场面试开不了头；而模型能力缺失才属配置错误（修一次永绝）。
     * 两类失败性质不同，故保留兜底但把提示词从代码迁到配置行。</p>
     */
    public static final String INTERVIEW_OPENING_FALLBACK = "opening_fallback";

    /** 思考提示：一句话引导（不泄答案），用户点击触发（VoiceInterviewServiceImpl#requestHint） */
    public static final String INTERVIEW_HINT = "hint";

    /**
     * 整场复盘 + 追问预测（一次调用双产出）。
     *
     * <p>方案 V1.2 §3.2 裁决：追问预测**并入**复盘调用而**不单开** task ——
     * 两触点输入完全同源（简历摘要 + 岗位 + JD + qaList），单开等于输入 token 翻倍，
     * 且模型一次看完对话后「顺带」产出预测问题质量更高（它知道哪些问过哪些没问）。</p>
     */
    public static final String INTERVIEW_REPORT_REVIEW = "report_review";

    /** 发展方向建议（懒生成：首次打开 tab 才调用；配置行预留 RAG 知识库绑定） */
    public static final String INTERVIEW_INDUSTRY_INSIGHT = "industry_insight";

    // ==================== 白名单（v13.51 批次 4：两段式校验用） ====================

    /**
     * 全部合法 task 短码（白名单）。
     *
     * <p><b>为什么需要它</b>：场景配置的 {@code scene_code} 支持两段式
     * {@code main:task} 写法，但原先管理端校验用 {@link AiSceneEnum#of(String)} 做**整串精确匹配**
     * ⇒ 合法的子场景（如 {@code voice_interview:warmup}）反而**存不了**，只能靠 SQL 维护。
     * 批次 4 把校验改为两段式后，task 段需要一份权威白名单 —— 就是本方法。</p>
     *
     * <p>白名单由本类常量派生（反射读取 {@code public static final String} 字段），
     * <b>新增子任务只需在本类加常量</b>，白名单自动跟随，不会再出现"加了常量但校验不认"的漂移。</p>
     *
     * @return 全部合法 task 短码（去重、无序但稳定）
     */
    public static java.util.Set<String> all() {
        java.util.Set<String> set = new java.util.LinkedHashSet<>();
        for (java.lang.reflect.Field f : AiSceneTasks.class.getDeclaredFields()) {
            if (!java.lang.reflect.Modifier.isStatic(f.getModifiers())) {
                continue;
            }
            if (f.getType() != String.class) {
                continue;
            }
            try {
                Object v = f.get(null);
                if (v != null && !String.valueOf(v).isBlank()) {
                    set.add(String.valueOf(v));
                }
            } catch (IllegalAccessException ignored) {
                // 常量字段不可访问在正常 JVM 下不会发生；忽略即可
            }
        }
        return set;
    }

    /**
     * 是否为合法 task 短码。
     *
     * @param task 待校验短码（可空）
     * @return true 表示在白名单内
     */
    public static boolean isValid(String task) {
        return task != null && all().contains(task);
    }
}
