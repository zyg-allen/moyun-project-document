package com.moyun.ext.cms.service.interview;

import com.moyun.util.string.StringUtils;

/**
 * 面试流程阶段（v11.x 6阶段状态机）
 *
 * <p>由 {@code portal_voice_interview.phase} 字段驱动：
 * {@code INTRO_WAITING → INTRO_RECEIVED → INTRO_FOLLOWUP → TECH_QUESTION →
 * PROJECT_DEEP → SYSTEM_DESIGN → CANDIDATE_ASK → FINISHED}</p>
 *
 * <p>兼容规则：
 * <ul>
 *   <li>phase 为 NULL 的旧会话走原流程（{@link #isLegacy}）</li>
 *   <li>enable_self_intro=0 时 start 直接进入 TECH_QUESTION（跳过 INTRO 系列）</li>
 *   <li>TECH/PROJECT/SYSTEM 按题单 question_type / 题源动态落位，非固定顺序</li>
 * </ul>
 *
 * @author moyun
 */
public enum InterviewPhase {

    /** 等待自我介绍（start 后、提交自我介绍前） */
    INTRO_WAITING("等待自我介绍"),

    /** 自我介绍已接收（评分完成，瞬时态） */
    INTRO_RECEIVED("自我介绍已接收"),

    /** 自我介绍追问（LLM 识别到值得追问的点，最多 1 轮） */
    INTRO_FOLLOWUP("自我介绍追问"),

    /** 技术问答（默认阶段） */
    TECH_QUESTION("技术问答"),

    /** 项目深挖（简历锚定题 / question_type=project） */
    PROJECT_DEEP("项目深挖"),

    /** 系统设计（question_type=system / 场景=系统设计） */
    SYSTEM_DESIGN("系统设计"),

    /** 候选人反问（题单耗尽后进入，最多 3 问） */
    CANDIDATE_ASK("候选人反问"),

    /** 已结束（finish 落位） */
    FINISHED("已结束");

    private final String label;

    InterviewPhase(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }

    /** 数据库存储代码（枚举名） */
    public String code() {
        return name();
    }

    /** 宽容解析：NULL/未知值返回 null（旧流程） */
    public static InterviewPhase fromCode(String code) {
        if (StringUtils.isEmpty(code)) {
            return null;
        }
        try {
            return valueOf(code.trim());
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    /** 是否旧流程会话（phase 为空 → 完全走原逻辑） */
    public static boolean isLegacy(String phaseCode) {
        return StringUtils.isEmpty(phaseCode);
    }

    /** 是否自我介绍系列阶段 */
    public static boolean isIntro(String phaseCode) {
        InterviewPhase p = fromCode(phaseCode);
        return p == INTRO_WAITING || p == INTRO_RECEIVED || p == INTRO_FOLLOWUP;
    }
}