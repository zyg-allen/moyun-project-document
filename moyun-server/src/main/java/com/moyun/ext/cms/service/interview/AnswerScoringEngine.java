package com.moyun.ext.cms.service.interview;

import com.moyun.portal.domain.entity.PortalInterviewQuestion;
import com.moyun.util.string.StringUtils;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 答题评分引擎（v11.47 自 VoiceInterviewServiceImpl 抽出，规则评分无 LLM 依赖）
 *
 * <p>职责：单题作答的规则化评分——关键词覆盖率 + 长度/结构词/互动信号
 * 计算 6 维连续维度分（relevance/professionalism/fluency/interactivity/confidence/logic，
 * 对齐前端雷达图维度键）+ 文本反馈。LLM 融合评分仍留在主服务（依赖场景模型）。</p>
 *
 * <p>抽取动机：VoiceInterviewServiceImpl 3169 行上帝类拆分第一步（评分/题目选择/文案构建
 * 三块逐步剥离），本类与 ScoringEngine（自我介绍 LLM 评分）平行。</p>
 *
 * @author moyun
 */
@Component
public class AnswerScoringEngine {

    /** 关键词最大提取数 */
    private static final int MAX_KEYWORDS = 12;

    /** 中文/英文停用词 */
    private static final Set<String> STOPWORDS = new HashSet<>(Arrays.asList(
            "的", "了", "是", "在", "和", "与", "或", "等", "为", "对", "由", "及",
            "一个", "一种", "可以", "通过", "使用", "进行", "实现", "the", "a", "an",
            "is", "are", "to", "of", "in", "on", "for", "and", "or", "with", "by"
    ));

    /**
     * 规则评分结果：总分 + 反馈 + 6 维度分
     */
    public static class ScoreResult {
        public final int score;
        public final String feedback;
        public final Map<String, Integer> dimensions;

        public ScoreResult(int score, String feedback, Map<String, Integer> dimensions) {
            this.score = score;
            this.feedback = feedback;
            this.dimensions = dimensions;
        }
    }

    /**
     * 单题作答规则评分（无 LLM 依赖，同步可测）
     *
     * @param question 题目（可 null：LLM 动态题/自我介绍，此时从题干或长度推断）
     * @param answer   转写答案
     */
    public ScoreResult scoreAnswer(PortalInterviewQuestion question, String answer) {
        List<String> keywords = question == null
                ? new ArrayList<>()
                : extractKeywords(question.getTags(), question.getSolution());
        // v11.30.2：LLM 动态题（追问/系统设计/自我介绍）无 tags/solution，从题干提取关键词，
        // 保证 matched/coverage 有意义（此前恒为 0 导致 professionalism/interactivity/logic 输出固定值）
        if (keywords.isEmpty() && question != null && StringUtils.isNotEmpty(question.getTitle())) {
            keywords = extractKeywords(null, question.getTitle());
        }
        String lowerAnswer = answer == null ? "" : answer.toLowerCase();
        int matched = 0;
        for (String kw : keywords) {
            if (lowerAnswer.contains(kw.toLowerCase())) {
                matched++;
            }
        }

        double coverage;
        if (keywords.isEmpty()) {
            coverage = answer.length() >= 50 ? 0.6 : 0.2;
        } else {
            coverage = (double) matched / keywords.size();
        }
        double lengthBonus = Math.min(answer.length() / 200.0, 1.0) * 20;
        int score = (int) Math.min(100, Math.round(coverage * 80 + lengthBonus));

        // 维度分（6 维连续计算，v11.30.2 重构：以覆盖率/长度/结构词/互动信号连续映射，
        // 消除旧版二值阈值导致的固定值；对齐前端雷达图维度键）
        Map<String, Integer> dimensions = new LinkedHashMap<>();
        int len = answer.length();
        int coverageScore = (int) Math.round(coverage * 100);
        double matchRatio = coverage; // 命中比例（0-1）
        int structureWordHits = countStructureWords(answer);

        // relevance 回答相关性：关键词覆盖率（连续）
        dimensions.put("relevance", coverageScore);
        // professionalism 专业度：覆盖率为主 + 长度稳健加成（连续）
        int professionalism = (int) Math.round(30 + matchRatio * 50 + Math.min(len / 6.0, 20));
        dimensions.put("professionalism", clamp(professionalism, 0, 100));
        // fluency 表达流畅度：长度分段连续（<40 偏短 30-50；40-300 线性升至 95；>300 饱和微降防冗长）
        int fluency;
        if (len < 40) {
            fluency = 30 + len / 2;
        } else if (len <= 300) {
            fluency = 50 + (len - 40) * 45 / 260;
        } else {
            fluency = (int) Math.max(80, 95 - (len - 300) / 40);
        }
        dimensions.put("fluency", clamp(fluency, 0, 100));
        // interactivity 面试互动性：覆盖率 + 互动信号（举例/对比/承认不确定/反问）+ 长度参与度（连续）
        int interactiveSignals = countInteractiveSignals(answer);
        int interactivity = (int) Math.round(30 + matchRatio * 35 + interactiveSignals * 10 + Math.min(len / 30.0, 15));
        dimensions.put("interactivity", clamp(interactivity, 0, 100));
        // confidence 自信度：长度饱满度 + 命中加成 + 覆盖率（连续）
        int confidence = (int) Math.round(35 + Math.min(len / 5.0, 30) + (matched >= 1 ? 15 : 0) + coverage * 20);
        dimensions.put("confidence", clamp(confidence, 0, 100));
        // logic 逻辑清晰：结构词（首先/其次/因为/所以等）计数 + 覆盖率（连续）
        int logic = (int) Math.round(30 + Math.min(structureWordHits, 4) * 12 + coverage * 22);
        dimensions.put("logic", clamp(logic, 0, 100));

        String feedback = buildFeedback(score, matched, keywords.size(), answer.length());
        return new ScoreResult(score, feedback, dimensions);
    }

    /** 数值钳制 [min, max] */
    public int clamp(int v, int min, int max) {
        return Math.max(min, Math.min(max, v));
    }

    /** 逻辑结构词计数（v11.30.2：logic 维度连续信号） */
    private int countStructureWords(String answer) {
        if (StringUtils.isEmpty(answer)) {
            return 0;
        }
        String[] markers = {"首先", "其次", "然后", "最后", "第一", "第二", "第三",
                "因为", "所以", "由于", "导致", "总结", "一方面", "另一方面", "比如", "例如"};
        int count = 0;
        for (String m : markers) {
            int idx = answer.indexOf(m);
            while (idx >= 0) {
                count++;
                idx = answer.indexOf(m, idx + m.length());
            }
        }
        return count;
    }

    /** 互动性信号计数（v11.30.2：interactivity 维度连续信号：举例/对比/承认不确定/反问） */
    private int countInteractiveSignals(String answer) {
        if (StringUtils.isEmpty(answer)) {
            return 0;
        }
        String[] signals = {"举个例子", "例如", "比如说", "我理解", "我认为", "个人认为",
                "相比", "对比", " tradeoff", "权衡", "不太确定", "我的想法是", "我曾经"};
        int count = 0;
        for (String s : signals) {
            if (answer.contains(s.trim())) {
                count++;
            }
        }
        return Math.min(count, 3);
    }

    /** 从 tags/解析文本提取关键词（去停用词、限长、上限 MAX_KEYWORDS） */
    public List<String> extractKeywords(String tags, String solution) {
        Set<String> kw = new LinkedHashSet<>();
        if (StringUtils.isNotEmpty(tags)) {
            for (String t : tags.split("[,，]")) {
                String s = t.trim();
                if (isValidKeyword(s)) kw.add(s);
            }
        }
        if (StringUtils.isNotEmpty(solution) && kw.size() < MAX_KEYWORDS) {
            String[] chunks = solution.split("[\\s,，。.、；;：:！!？?\\n\\r\\t/()（）\\[\\]【】\"'`]+");
            for (String c : chunks) {
                String s = c.trim();
                if (isValidKeyword(s) && kw.size() < MAX_KEYWORDS) kw.add(s);
            }
        }
        return new ArrayList<>(kw);
    }

    private boolean isValidKeyword(String s) {
        if (s == null || s.length() < 2 || s.length() > 10) return false;
        if (STOPWORDS.contains(s)) return false;
        for (int i = 0; i < s.length(); i++) {
            if (!Character.isDigit(s.charAt(i))) return true;
        }
        return false;
    }

    private String buildFeedback(int score, int matched, int total, int len) {
        StringBuilder sb = new StringBuilder();
        if (score >= 80) sb.append("回答全面，覆盖了核心要点");
        else if (score >= 60) sb.append("回答较好，但部分关键点未提及");
        else if (score >= 40) sb.append("回答一般，建议补充更多细节");
        else sb.append("回答不够充分，建议参考标准答案深入理解");
        sb.append("。关键词覆盖 ").append(matched).append("/").append(total);
        sb.append("，答案长度 ").append(len).append(" 字。");
        return sb.toString();
    }
}
