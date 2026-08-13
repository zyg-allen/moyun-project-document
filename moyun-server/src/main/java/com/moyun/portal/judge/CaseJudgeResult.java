package com.moyun.portal.judge;

import lombok.Data;

/**
 * 单个测试用例的判题结果（v6.3 OJ 判题系统）
 *
 * @author moyun
 */
@Data
public class CaseJudgeResult {

    /** 用例 ID */
    private Long caseId;

    /** 用例序号（1-based，前端展示用 "用例 1"、"用例 2"） */
    private Integer caseIndex;

    /** 是否样例 */
    private Boolean isSample;

    /** 是否通过 */
    private Boolean passed;

    /** 运行耗时（毫秒） */
    private Integer runtime;

    /** 内存峰值（KB，当前实现为占位，未做实际统计） */
    private Integer memoryUsage;

    /** 实际输出（仅失败时回填，便于前端展示调试） */
    private String actualOutput;

    /** 错误信息（仅 RE/CE 时回填） */
    private String errorMessage;

    public static CaseJudgeResult pass(Long caseId, Integer caseIndex, Boolean isSample, int runtime) {
        CaseJudgeResult r = new CaseJudgeResult();
        r.caseId = caseId;
        r.caseIndex = caseIndex;
        r.isSample = isSample;
        r.passed = true;
        r.runtime = runtime;
        return r;
    }

    public static CaseJudgeResult fail(Long caseId, Integer caseIndex, Boolean isSample, int runtime,
                                      String actualOutput, String errorMessage) {
        CaseJudgeResult r = new CaseJudgeResult();
        r.caseId = caseId;
        r.caseIndex = caseIndex;
        r.isSample = isSample;
        r.passed = false;
        r.runtime = runtime;
        r.actualOutput = actualOutput;
        r.errorMessage = errorMessage;
        return r;
    }
}
