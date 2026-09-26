package com.moyun.ledger.domain.vo;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * 财务健康评分 VO（规则引擎兜底核心）
 *
 * <p>5 项加权（资产负债率30 / 紧急预备金25 / 结余率20 / 负债收入比15 / 被动收入10），
 * 总分 0-100；等级 A(≥85)/B(70-84)/C(60-69)/D(&lt;60)；AI 不可用时等级文案即兜底结论。
 *
 * @author moyun
 */
@Data
public class ScoreVO {

    /** 总分（0-100；无数据指标按剩余权重归一化） */
    private Integer total = 0;

    /** 等级：A/B/C/D */
    private String grade = "D";

    /** 等级文案（优秀/良好/一般/预警） */
    private String gradeLabel = "预警";

    /** 兜底文案（规则引擎结论，AI 不可用时直接展示） */
    private String desc;

    /** 评分明细（逐项：name/weight/level/score/ratioValue/ratioLabel） */
    private List<Map<String, Object>> detail = new ArrayList<>();
}
