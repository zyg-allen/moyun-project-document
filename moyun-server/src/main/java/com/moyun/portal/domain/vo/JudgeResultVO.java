package com.moyun.portal.domain.vo;

import java.io.Serializable;
import java.util.List;

import lombok.Data;

/**
 * 判题结果 VO（v6.3 OJ 判题系统）
 * <p>
 * 判题提交后立即返回，前端按逐用例结果展示通过情况。
 *
 * @author moyun
 */
@Data
public class JudgeResultVO implements Serializable {
    private static final long serialVersionUID = 1L;

    /** 关联提交记录 ID */
    private Long submissionId;

    /** 判题状态码（AC/WA/TLE/MLE/RE/CE/SE/PENDING） */
    private String status;

    /** 判题状态展示名（Accepted/Wrong Answer/...） */
    private String statusName;

    /** 是否通过 */
    private Boolean accepted;

    /** 通过用例数 */
    private Integer passedCount;

    /** 总用例数 */
    private Integer totalCount;

    /** 最大运行时间（毫秒） */
    private Integer maxRuntime;

    /** 内存峰值（KB） */
    private Integer maxMemory;

    /** 首个失败用例 ID */
    private Long failedCaseId;

    /** 首个失败用例输入（仅失败且为样例时回填，隐藏用例仅回填序号） */
    private String failedCaseInput;

    /** 首个失败用例期望输出 */
    private String failedCaseExpected;

    /** 首个失败用例实际输出 */
    private String failedCaseActual;

    /** 编译/运行错误信息 */
    private String errorMessage;

    /** 逐用例结果（仅样例用例的失败详情可见，隐藏用例仅展示通过/失败） */
    private List<CaseResultItem> caseResults;

    /**
     * 单用例结果项
     */
    @Data
    public static class CaseResultItem implements Serializable {
        private static final long serialVersionUID = 1L;

        private Long caseId;

        /** 用例序号（1-based） */
        private Integer caseIndex;

        private Boolean isSample;

        private Boolean passed;

        /** 运行时间（毫秒） */
        private Integer runtime;

        /** 实际输出（仅失败且为样例时回填） */
        private String actualOutput;

        /** 错误信息（仅 RE/TLE 时回填） */
        private String errorMessage;
    }
}
