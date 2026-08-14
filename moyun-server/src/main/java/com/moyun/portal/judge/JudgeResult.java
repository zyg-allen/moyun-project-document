package com.moyun.portal.judge;

import java.util.Collections;
import java.util.List;

import lombok.Data;

/**
 * 一次判题的整体结果（v6.3 OJ 判题系统）
 * <p>
 * 判题引擎对单次提交运行所有用例后返回，由 Service 层落库并下发前端。
 *
 * @author moyun
 */
@Data
public class JudgeResult {

    /** 判题状态（AC/WA/TLE/MLE/RE/CE/SE） */
    private JudgeStatus status;

    /** 通过用例数 */
    private int passedCount;

    /** 总用例数 */
    private int totalCount;

    /** 总运行时间（取最大单用例耗时，毫秒） */
    private int maxRuntimeMs;

    /** 内存峰值（KB，占位） */
    private int maxMemoryKb;

    /** 首个失败用例 ID（全 AC 时为 null） */
    private Long failedCaseId;

    /** 首个失败用例输入 */
    private String failedCaseInput;

    /** 首个失败用例期望输出 */
    private String failedCaseExpected;

    /** 首个失败用例实际输出 */
    private String failedCaseActual;

    /** 编译/运行错误信息（CE/RE/SE 时填写） */
    private String errorMessage;

    /** 逐用例结果（前端展示用例通过情况） */
    private List<CaseJudgeResult> caseResults;

    public static JudgeResult of(JudgeStatus status) {
        JudgeResult r = new JudgeResult();
        r.status = status;
        r.caseResults = Collections.emptyList();
        return r;
    }
}
