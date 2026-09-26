package com.moyun.ledger.domain.vo;

import lombok.Data;

/**
 * 关键财务比率 VO（确定性计算，6 项）
 *
 * <p>口径与阈值（评分卡一致）：
 * 资产负债率 &lt;50% A / 50-70% B / &gt;70% C；
 * 紧急预备金 ≥6月 A / 3-6月 B / &lt;3月 C；
 * 结余率 ≥30% A / 10-30% B / &lt;10% C；
 * 负债收入比 &lt;30% A / 30-50% B / &gt;50% C；
 * 被动收入占比 ≥20% A / 10-20% B / &lt;10% C；
 * 投资资产占比 ≥20% A / 10-20% B / &lt;10% C（辅助指标，不参与评分）。
 *
 * @author moyun
 */
@Data
public class RatiosVO {

    /** 资产负债率（%；无资产时为 null） */
    private Double debtToAsset;

    /** 紧急预备金月数（月；无支出数据时为 null） */
    private Double emergencyFundMonths;

    /** 结余率（%；无收入时为 null） */
    private Double surplusRatio;

    /** 负债收入比（月供/期间收入，%；无收入时为 null） */
    private Double debtToIncome;

    /** 被动收入占比（%；无收入时为 null） */
    private Double passiveIncomeRatio;

    /** 投资资产占比（%；无资产时为 null） */
    private Double investmentAssetRatio;

    /** 资产负债率评级：A/B/C（null=无数据） */
    private String debtToAssetLevel;

    /** 紧急预备金评级：A/B/C（null=无数据） */
    private String emergencyFundLevel;

    /** 结余率评级：A/B/C（null=无数据） */
    private String surplusLevel;

    /** 负债收入比评级：A/B/C（null=无数据） */
    private String debtToIncomeLevel;

    /** 被动收入占比评级：A/B/C（null=无数据） */
    private String passiveIncomeLevel;

    /** 投资资产占比评级：A/B/C（null=无数据） */
    private String investmentAssetLevel;
}
