package com.moyun.ext.cms.domain.vo;

import lombok.Data;

import java.io.Serializable;
import java.util.List;

/**
 * 深度优化结果 VO（v10.13 简历优化重构）
 * <p>LLM 基于目标岗位 JD 对简历逐项生成的优化建议，前端展示前后对比、逐项采纳。</p>
 *
 * @author moyun
 */
@Data
public class ResumeDeepOptimizeVO implements Serializable {
    private static final long serialVersionUID = 1L;

    private Long resumeId;

    private Long jobTargetId;

    /** AI 分析总结 */
    private String summary;

    /** 建议列表 */
    private List<OptimizeItem> items;

    /** 是否 LLM 生成（false=规则兜底，无建议项） */
    private Boolean aiPowered;

    /** 单条优化建议：section+index+field 唯一定位简历字段 */
    @Data
    public static class OptimizeItem implements Serializable {
        private static final long serialVersionUID = 1L;

        /** 区块：objective(求职意向)/education/work/project/skills/selfIntro */
        private String section;
        /** 列表条目索引（标量字段为 0；skills 为技能名） */
        private Integer index;
        /** 字段名：position/description/name 等 */
        private String field;
        /** 优化前内容 */
        private String original;
        /** 优化后内容（可直接替换） */
        private String optimized;
        /** 优化理由 */
        private String reason;
    }
}
