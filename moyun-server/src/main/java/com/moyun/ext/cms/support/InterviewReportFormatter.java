package com.moyun.ext.cms.support;

import com.fasterxml.jackson.databind.JsonNode;
import com.moyun.ext.cms.domain.vo.VoiceInterviewReportVO;
import org.apache.commons.lang3.StringUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

/**
 * 面试报告「纯格式化」工具。
 *
 * <p><b>为什么单独一类</b>：{@link InterviewTextUtils} 收的是**文本/JSON 通用处理**，
 * 本类收的是**报告语义的纯格式化**（亮点/薄弱点视图、改进建议、概要文案、总结建议）
 * —— 两者关注点不同，混在一起会让"文本工具"承担业务措辞。</p>
 *
 * <p><b>本类只放纯函数</b>：全部 {@code static}，不持有状态、不注入依赖、不做 IO。
 * 任何读实例字段（Mapper/锁/缓存）或做远程 IO 的报告逻辑**留在 Service**，
 * 待后续步骤按职责抽取（第③步才动编排主流程，含 SSE/事务/锁，最危险）。</p>
 *
 * @author laomao
 */
public final class InterviewReportFormatter {

    /** 亮点/薄弱点结构化视图的条数上限（与 LLM 提示词的 2-4 条口径一致） */
    public static final int MAX_POINT_VIEWS = 4;

    /** 薄弱点转改进建议的条数上限 */
    public static final int MAX_WEAK_SUGGESTIONS = 3;

    /** 含自我介绍不足在内的改进建议总条数上限 */
    public static final int MAX_TOTAL_SUGGESTIONS = 5;

    private InterviewReportFormatter() {
    }

    /**
     * 解析亮点/薄弱点数组 → 结构化视图（setter 注入）+ 返回标题列表（旧字段双写）。
     *
     * <p>设计意图：报告同时保留「结构化视图」（新前端）与「字符串标题数组」（旧字段/兼容），
     * 故本方法既通过 {@code setter} 回填结构化视图，又返回标题列表供调用方写旧字段。</p>
     *
     * <p>降级：{@code arr} 为 null/非数组时返回**空列表**（不抛）；单条缺少 {@code title} 时跳过
     * （无标题的条目对用户无价值）。</p>
     *
     * @param arr    LLM 产出的 highlights / weakPoints 节点
     * @param setter 结构化视图回填回调（仅在解析出至少一条时调用）
     * @return 标题列表（可能为空，永不为 null）
     */
    public static List<String> parsePointViews(
            JsonNode arr,
            Consumer<List<VoiceInterviewReportVO.PointView>> setter) {
        List<String> titles = new ArrayList<>();
        if (arr == null || !arr.isArray()) {
            return titles;
        }
        List<VoiceInterviewReportVO.PointView> views = new ArrayList<>();
        for (JsonNode item : arr) {
            if (views.size() >= MAX_POINT_VIEWS) {
                break;
            }
            String title = item.path("title").asText("").trim();
            if (StringUtils.isEmpty(title)) {
                continue;
            }
            VoiceInterviewReportVO.PointView view = new VoiceInterviewReportVO.PointView();
            view.setTitle(title);
            view.setDetail(item.path("detail").asText("").trim());
            views.add(view);
            titles.add(title);
        }
        if (!views.isEmpty()) {
            setter.accept(views);
        }
        return titles;
    }

    /**
     * 规则兜底的改进建议（LLM 建议缺失或为空时使用）。
     *
     * <p>来源两级：① 薄弱点（最多 {@value #MAX_WEAK_SUGGESTIONS} 条）；
     * ② 自我介绍不足（补足到 {@value #MAX_TOTAL_SUGGESTIONS} 条）。
     * 两者皆无时给一条「均衡」正向文案 —— **保证建议区永不空白**（报告不出现空板块）。</p>
     *
     * @param weakPoints 薄弱点标题列表（可空）
     * @param introScore 自我介绍评分视图（可空；其 weaknesses 会被转为建议）
     * @return 非空的建议列表
     */
    public static List<String> buildImprovementSuggestions(
            List<String> weakPoints,
            VoiceInterviewReportVO.IntroScoreView introScore) {
        List<String> suggestions = new ArrayList<>();
        if (weakPoints != null) {
            for (String wp : weakPoints) {
                if (suggestions.size() >= MAX_WEAK_SUGGESTIONS) {
                    break;
                }
                suggestions.add("针对薄弱点「" + wp + "」做专项复习，可结合错题本巩固。");
            }
        }
        if (introScore != null && introScore.getWeaknesses() != null) {
            for (String wk : introScore.getWeaknesses()) {
                if (StringUtils.isNotEmpty(wk) && suggestions.size() < MAX_TOTAL_SUGGESTIONS) {
                    suggestions.add("自我介绍改进：" + wk);
                }
            }
        }
        if (suggestions.isEmpty()) {
            suggestions.add("整体表现均衡，建议挑战更高难度的面试场景以突破上限。");
        }
        return suggestions;
    }

    /**
     * 概要文案（按均分分档：≥80 优秀 / ≥60 良好 / 其余一般）。
     *
     * @param total    题目总数
     * @param answered 已作答题数
     * @param avg      均分（0-100）
     * @return 概要文案
     */
    public static String buildSummary(int total, int answered, int avg) {
        StringBuilder sb = new StringBuilder();
        sb.append("本次面试共 ").append(total).append(" 题，作答 ").append(answered).append(" 题。");
        if (avg >= 80) {
            sb.append("整体表现优秀，知识点掌握扎实。");
        } else if (avg >= 60) {
            sb.append("整体表现良好，部分知识点需加强。");
        } else {
            sb.append("整体表现一般，建议针对薄弱点深入复习。");
        }
        return sb.toString();
    }

    /**
     * 总结建议（有薄弱点则列出，否则给正向文案）。
     *
     * @param avg        均分（0-100；保留入参以兼容既有调用口径）
     * @param weakPoints 薄弱点标题列表（非 null）
     * @return 总结建议文案
     */
    public static String buildSuggestion(int avg, List<String> weakPoints) {
        if (weakPoints == null || weakPoints.isEmpty()) {
            return "继续保持，挑战更高难度的题目。";
        }
        return "建议重点复习以下薄弱知识点：" + String.join("、", weakPoints) + "。可通过错题本针对性练习。";
    }
}
