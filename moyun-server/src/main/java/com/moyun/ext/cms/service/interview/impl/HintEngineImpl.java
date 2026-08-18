package com.moyun.ext.cms.service.interview.impl;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.moyun.ext.cms.domain.vo.HintVO;
import com.moyun.ext.cms.service.interview.HintEngine;
import com.moyun.portal.domain.entity.PortalInterviewQuestion;
import com.moyun.util.string.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * 面试提示引擎规则版实现（V10.0）
 *
 * <p>纯规则实现，不依赖 LLM，保证离线可用与低延迟：
 * <ul>
 *   <li>关键词提取：复用 MockInterviewServiceImpl 的 tags + solution 切分逻辑</li>
 *   <li>分级提示：
 *     <ul>
 *       <li>level=1 切入点：1~2 个关键词，引导思考方向</li>
 *       <li>level=2 结构：STAR 框架 + 答题大纲</li>
 *       <li>level=3 全量：全部关键词 + 考察点 + 结构，不给完整答案</li>
 *     </ul>
 *   </li>
 *   <li>speakText：可直接 TTS 播报的引导语，level 越高引导越具体</li>
 * </ul>
 *
 * <p>注意：本类为 V10.0 规则版，后续 V10.2 可替换为 LLM 增强实现，接口不变。
 *
 * @author moyun
 */
@Service
public class HintEngineImpl implements HintEngine {

    private static final Logger log = LoggerFactory.getLogger(HintEngineImpl.class);

    /** 关键词提取上限 */
    private static final int MAX_KEYWORDS = 12;

    /** level1 切入点关键词数量 */
    private static final int LEVEL1_KEYWORD_COUNT = 2;

    /** 中文/英文停用词，关键词提取时过滤 */
    private static final Set<String> STOPWORDS = new HashSet<>(Arrays.asList(
            "的", "了", "是", "在", "和", "与", "或", "等", "为", "对", "由", "及",
            "一个", "一种", "可以", "通过", "使用", "进行", "实现", "the", "a", "an",
            "is", "are", "to", "of", "in", "on", "for", "and", "or", "with", "by",
            "this", "that", "it", "be", "as", "at", "so", "we", "he", "she", "you"
    ));

    @Autowired
    private ObjectMapper objectMapper;

    @Override
    public List<String> generateKeywords(PortalInterviewQuestion question) {
        if (question == null) {
            return new ArrayList<>();
        }
        Set<String> kw = new LinkedHashSet<>();
        // tags：逗号分隔，质量较高
        if (StringUtils.isNotEmpty(question.getTags())) {
            for (String t : question.getTags().split("[,，]")) {
                String s = t.trim();
                if (isValidKeyword(s)) {
                    kw.add(s);
                }
            }
        }
        // solution：按标点/空白切分
        if (StringUtils.isNotEmpty(question.getSolution()) && kw.size() < MAX_KEYWORDS) {
            kw.addAll(splitKeywords(question.getSolution(), kw));
        }
        // referenceAnswer：补充关键词（v6.3 结构化字段）
        if (StringUtils.isNotEmpty(question.getReferenceAnswer()) && kw.size() < MAX_KEYWORDS) {
            kw.addAll(splitKeywords(question.getReferenceAnswer(), kw));
        }
        // answerOutline：补充关键词
        if (StringUtils.isNotEmpty(question.getAnswerOutline()) && kw.size() < MAX_KEYWORDS) {
            kw.addAll(splitKeywords(question.getAnswerOutline(), kw));
        }
        return new ArrayList<>(kw);
    }

    @Override
    public HintVO generateHint(PortalInterviewQuestion question, int level) {
        int safeLevel = Math.max(1, Math.min(3, level));
        List<String> keywords = generateKeywords(question);

        HintVO vo;
        switch (safeLevel) {
            case 1:
                vo = buildLevel1(question, keywords);
                break;
            case 2:
                vo = buildLevel2(question, keywords);
                break;
            case 3:
            default:
                vo = buildLevel3(question, keywords);
                break;
        }
        return vo;
    }

    // ========================================================================
    // 分级提示构建
    // ========================================================================

    /** level=1 切入点提示：1~2 个关键词，引导思考方向 */
    private HintVO buildLevel1(PortalInterviewQuestion question, List<String> keywords) {
        HintVO vo = HintVO.of(1, "切入点提示");
        List<String> picks = keywords.subList(0, Math.min(LEVEL1_KEYWORD_COUNT, keywords.size()));
        vo.setKeywords(new ArrayList<>(picks));
        vo.setStructureHint(null);

        String title = question == null ? "本题" : question.getTitle();
        String kwText = picks.isEmpty() ? "暂无关键词" : String.join("、", picks);
        vo.setSpeakText("这道题" + title + "，建议从" + kwText + "切入思考。先理清概念，再举例说明。");
        return vo;
    }

    /** level=2 结构提示：STAR 框架 + 答题大纲 */
    private HintVO buildLevel2(PortalInterviewQuestion question, List<String> keywords) {
        HintVO vo = HintVO.of(2, "结构提示");
        vo.setKeywords(new ArrayList<>(keywords));

        String outline = question == null ? null : question.getAnswerOutline();
        String structure = buildStarStructure(question, outline);
        vo.setStructureHint(structure);

        vo.setSpeakText("建议按 STAR 框架组织回答：先说场景，再说任务，接着讲行动，最后总结结果。"
                + (StringUtils.isNotEmpty(outline) ? "可参考答题大纲展开。" : ""));
        return vo;
    }

    /** level=3 全量提示：全部关键词 + 考察点 + 结构，不给完整答案 */
    private HintVO buildLevel3(PortalInterviewQuestion question, List<String> keywords) {
        HintVO vo = HintVO.of(3, "全量提示");
        vo.setKeywords(new ArrayList<>(keywords));
        vo.setExaminePoints(parseExaminePoints(question));

        String outline = question == null ? null : question.getAnswerOutline();
        vo.setStructureHint(buildStarStructure(question, outline));

        String title = question == null ? "本题" : question.getTitle();
        StringBuilder speak = new StringBuilder();
        speak.append("全量提示：").append(title).append("。");
        if (!keywords.isEmpty()) {
            speak.append("关键词：").append(String.join("、", keywords)).append("。");
        }
        if (vo.getExaminePoints() != null && !vo.getExaminePoints().isEmpty()) {
            speak.append("考察点：").append(String.join("、", vo.getExaminePoints())).append("。");
        }
        speak.append("请按 STAR 框架组织答案，注意覆盖所有考察点。");
        vo.setSpeakText(speak.toString());
        return vo;
    }

    // ========================================================================
    // 辅助方法
    // ========================================================================

    /** STAR 框架结构化文本，若题目有 answerOutline 则拼接到末尾 */
    private String buildStarStructure(PortalInterviewQuestion question, String outline) {
        String qt = question == null ? "" : question.getQuestionType();
        StringBuilder sb = new StringBuilder();
        if ("hr".equals(qt) || "project".equals(qt)) {
            // HR / 项目题用 STAR
            sb.append("【STAR 框架】\n");
            sb.append("- Situation 场景：描述问题背景\n");
            sb.append("- Task 任务：说明你的职责目标\n");
            sb.append("- Action 行动：详述你的具体做法\n");
            sb.append("- Result 结果：量化成果与反思\n");
        } else if ("system_design".equals(qt)) {
            // 系统设计题用通用四步
            sb.append("【系统设计框架】\n");
            sb.append("- 需求澄清：功能/非功能需求、QPS、容量估算\n");
            sb.append("- 总体架构：分层与核心组件\n");
            sb.append("- 关键模块：存储选型、缓存、一致性、扩展性\n");
            sb.append("- 权衡取舍：瓶颈、容灾、演进方向\n");
        } else if ("algorithm".equals(qt)) {
            sb.append("【算法题框架】\n");
            sb.append("- 暴力解：说明朴素思路与复杂度\n");
            sb.append("- 优化思路：识别冗余/单调性/数据结构\n");
            sb.append("- 编码实现：边界处理、复杂度分析\n");
            sb.append("- 测试验证：用例覆盖与优化空间\n");
        } else {
            // 八股题默认总分总
            sb.append("【答题框架】\n");
            sb.append("- 是什么：核心概念定义\n");
            sb.append("- 为什么：原理与设计动机\n");
            sb.append("- 怎么做：典型用法与注意事项\n");
            sb.append("- 对比/扩展：与同类方案对比、应用场景\n");
        }
        if (StringUtils.isNotEmpty(outline)) {
            sb.append("\n【答题大纲】\n").append(outline);
        }
        return sb.toString();
    }

    /** 解析 examinePoints JSON 数组字符串 */
    private List<String> parseExaminePoints(PortalInterviewQuestion question) {
        if (question == null || StringUtils.isEmpty(question.getExaminePoints())) {
            return new ArrayList<>();
        }
        try {
            List<String> list = objectMapper.readValue(
                    question.getExaminePoints(), new TypeReference<List<String>>() {});
            return list == null ? new ArrayList<>() : list;
        } catch (Exception e) {
            // 非 JSON 数组时按逗号分隔降级处理
            log.warn("examinePoints 解析失败，降级按逗号分隔: {}", e.getMessage());
            List<String> fallback = new ArrayList<>();
            for (String p : question.getExaminePoints().split("[,，]")) {
                String s = p.trim();
                if (StringUtils.isNotEmpty(s)) {
                    fallback.add(s);
                }
            }
            return fallback;
        }
    }

    /** 从文本按标点/空白切分关键词 */
    private Set<String> splitKeywords(String text, Set<String> existing) {
        Set<String> kw = new LinkedHashSet<>();
        String[] chunks = text.split("[\\s,，。.、；;：:！!？?\\n\\r\\t/()（）\\[\\]【】\"'`]+");
        for (String c : chunks) {
            String s = c.trim();
            if (isValidKeyword(s) && !existing.contains(s) && kw.size() + existing.size() < MAX_KEYWORDS) {
                kw.add(s);
            }
        }
        return kw;
    }

    private boolean isValidKeyword(String s) {
        if (s == null || s.length() < 2 || s.length() > 10) {
            return false;
        }
        if (STOPWORDS.contains(s)) {
            return false;
        }
        // 纯数字不计
        for (int i = 0; i < s.length(); i++) {
            if (!Character.isDigit(s.charAt(i))) {
                return true;
            }
        }
        return false;
    }
}
