package com.moyun.ledger.domain.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * AI 财务分析报告月度快照 ledger_ai_analysis_report（v11.36）
 *
 * <p>每月一条（uk user_id+period）：当月命中直接返回（零 token），显式刷新覆盖。
 *
 * @author moyun
 */
@Data
@TableName("ledger_ai_analysis_report")
public class LedgerAiAnalysisReport implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /** 报告ID */
    @TableId(type = IdType.AUTO)
    private Long id;

    /** 门户用户ID */
    private Long userId;

    /** 报告月份 yyyy-MM */
    private String period;

    /** 财务健康分 0-100 */
    private Integer healthScore;

    /** 核心指标 JSON */
    private String metricsJson;

    /** 收入来源 JSON */
    private String incomeJson;

    /** 债务风险 JSON */
    private String riskJson;

    /** 建议 JSON */
    private String adviceJson;

    /** LLM 综述（失败时为模板文案） */
    private String aiSummary;

    /** 综述是否为 LLM 生成：1=是 0=模板降级 */
    private Integer aiEnabled;

    /** 当次画像快照 */
    private String profileSnapshot;

    /** 数据指纹（v11.40：流水/资产/负债/画像变更痕迹，命中缓存前比对，变化自动重算） */
    private String dataFingerprint;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createTime;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime updateTime;
}