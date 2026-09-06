package com.moyun.ext.cms.service.interview;

import lombok.Data;

/**
 * 面试出题决策策略（纯函数，无副作用）
 *
 * <p>LLM 只有建议权，本策略持决定权：基于追问链深、追问预算、主问进度
 * 对 LLM 建议的 nextAction 做硬约束裁决，防止死循环追问、防止提前收尾。</p>
 *
 * <p>规则：
 * <ul>
 *   <li>wrap_up 合法 ⇔ roundsDone ≥ totalPlanned（强制收尾）或 ≥ ceil(0.8×totalPlanned)（允许提前收尾）</li>
 *   <li>deepen 合法 ⇔ followupDepth &lt; 2 且 followupUsed &lt; 4 且 roundsDone &lt; totalPlanned</li>
 *   <li>change_topic 合法 ⇔ roundsDone &lt; totalPlanned</li>
 *   <li>拒绝时按 deepen → change_topic → wrap_up 降级</li>
 * </ul></p>
 *
 * @author moyun
 */
public final class InterviewDecisionPolicy {

    public static final String DEEPEN = "deepen";
    public static final String CHANGE_TOPIC = "change_topic";
    public static final String WRAP_UP = "wrap_up";

    /** 同一主题允许的最大追问链深度 */
    public static final int FOLLOWUP_MAX_DEPTH = 2;

    /** 单场面试追问总预算 */
    public static final int FOLLOWUP_BUDGET = 4;

    private InterviewDecisionPolicy() {
    }

    /**
     * 裁决 LLM 建议的下一步动作
     *
     * @param llmAction     LLM 建议动作（deepen/change_topic/wrap_up，可为空）
     * @param followupDepth 当前 QA 的追问链深（主问为 0）
     * @param followupUsed  本场已消耗追问总数
     * @param roundsDone    已完成主问轮数（distinct questionIdx 已作答数）
     * @param totalPlanned  主问总预算
     */
    public static Decision resolve(String llmAction, int followupDepth, int followupUsed,
                                   int roundsDone, int totalPlanned) {
        Decision d = new Decision();
        int earlyWrapThreshold = (int) Math.ceil(totalPlanned * 0.8);

        boolean wrapOk = totalPlanned <= 0 || roundsDone >= totalPlanned
                || roundsDone >= earlyWrapThreshold;
        boolean deepenOk = followupDepth < FOLLOWUP_MAX_DEPTH
                && followupUsed < FOLLOWUP_BUDGET
                && roundsDone < totalPlanned;
        boolean changeOk = roundsDone < totalPlanned;

        String requested = llmAction == null ? "" : llmAction.trim();
        switch (requested) {
            case DEEPEN:
                if (deepenOk) {
                    d.setAction(DEEPEN);
                } else {
                    d.setAction(changeOk ? CHANGE_TOPIC : WRAP_UP);
                    d.setOverrideReason("追问链深/预算超限：" + requested + "→" + d.getAction());
                }
                break;
            case CHANGE_TOPIC:
                if (changeOk) {
                    d.setAction(CHANGE_TOPIC);
                } else {
                    d.setAction(WRAP_UP);
                    d.setOverrideReason("主问已全部完成：" + requested + "→" + WRAP_UP);
                }
                break;
            case WRAP_UP:
                if (wrapOk) {
                    d.setAction(WRAP_UP);
                } else {
                    d.setAction(changeOk ? CHANGE_TOPIC : WRAP_UP);
                    d.setOverrideReason("提前收尾被拒（进度 " + roundsDone + "/" + totalPlanned + "）："
                            + requested + "→" + d.getAction());
                }
                break;
            default:
                // LLM 未给建议：保守推进——有剩余主问换题，否则收尾
                d.setAction(changeOk ? CHANGE_TOPIC : WRAP_UP);
                if (requested.isEmpty()) {
                    d.setOverrideReason("LLM 未给出动作，默认推进");
                } else {
                    d.setOverrideReason("未知动作 " + requested + "，默认推进");
                }
        }
        return d;
    }

    /** 裁决结果：最终动作 + 覆盖原因（用于日志观测 LLM 决策质量） */
    @Data
    public static class Decision {
        private String action;
        private String overrideReason;
    }
}
