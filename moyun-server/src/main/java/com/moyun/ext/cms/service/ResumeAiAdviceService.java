package com.moyun.ext.cms.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.moyun.ext.ai2.support.AiSceneJsonClient;
import com.moyun.ext.cms.config.AiProperties;
import com.moyun.ext.cms.domain.vo.ResumeAiAdviceVO;
import com.moyun.ext.cms.domain.vo.UserResumeVO;
import com.moyun.ext.cms.domain.vo.UserResumeVO.ScoreItem;
import com.moyun.ext.cms.domain.vo.UserResumeVO.SubScoreItem;
import com.moyun.util.string.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 简历 AI 改进建议服务（v5.9 阶段2/3）
 * <p>
 * 双模式生成：
 * - 规则化（默认）：基于评分明细 + 岗位匹配度子项生成建议，不依赖外部模型
 * - AI 模型（可选）：当 moyun.ai.enabled=true 且 moyun.ai.resume-advice-enabled=true 时，
 *   通过 {@link LlmClient} 调用真实 LLM 生成建议；LLM 调用失败时自动回退到规则化
 * <p>
 * 后期接入 AI 时，仅需在 application.yaml 开启配置，无需修改业务代码（VO 结构不变）。
 *
 * @author moyun
 */
@Service
public class ResumeAiAdviceService {
    /** v11.39：本服务所属 AI 场景代码（绑定见 ai_scene_config，业务不感知模型选择） */
    private static final String SCENE_RESUME_OPTIMIZE = "resume_optimize";


    private static final Logger log = LoggerFactory.getLogger(ResumeAiAdviceService.class);

    @Autowired
    private AiProperties aiProperties;

    @Autowired
    private LlmClient llmClient;

    /** v11.58 P0-3：AI 建议生成统一走 AI 网关（task=advice 子任务） */
    @Autowired
    private AiSceneJsonClient aiSceneJsonClient;

    @Autowired
    private ObjectMapper objectMapper;

    /**
     * 生成简历改进建议（统一入口，自动选择规则化或 AI 模型）
     *
     * @param vo             简历 VO
     * @param scoreItems     评分明细（含岗位匹配度子项）
     * @param targetPosition 目标岗位名称（可为空）
     * @return 改进建议 VO
     */
    public ResumeAiAdviceVO generateAdvice(UserResumeVO vo, List<ScoreItem> scoreItems, String targetPosition) {
        // 1. 优先尝试 AI 模型生成（仅在配置启用时）
        if (aiProperties.isEnabled() && aiProperties.isResumeAdviceEnabled() && llmClient.isEnabled()) {
            try {
                ResumeAiAdviceVO aiResult = generateAdviceWithLlm(vo, scoreItems, targetPosition);
                if (aiResult != null) {
                    return aiResult;
                }
                log.info("[ResumeAiAdvice] LLM 返回空，回退规则化");
            } catch (Exception e) {
                log.warn("[ResumeAiAdvice] LLM 调用失败，回退规则化：{}", e.getMessage());
            }
        }

        // 2. 规则化兜底
        return generateAdviceRuleBased(vo, scoreItems, targetPosition);
    }

    /**
     * 通过 AI 网关生成建议（v11.58 P0-3 收口：task=advice 子任务，提示词收编至
     * ResumeOptimizeHandler，本方法仅组装业务上下文与结果映射）
     * <p>
     * 构造评分明细上下文，经网关调用 LLM；解析返回的 JSON 为 ResumeAiAdviceVO；
     * 失败返回 null 由上层回退规则化。
     */
    private ResumeAiAdviceVO generateAdviceWithLlm(UserResumeVO vo, List<ScoreItem> scoreItems, String targetPosition) {
        StringBuilder context = new StringBuilder();
        context.append("目标岗位：").append(StringUtils.isNotEmpty(targetPosition) ? targetPosition : "未设置").append("\n");
        context.append("当前评分：").append(scoringServiceTotal(scoreItems)).append(" 分\n");
        context.append("评分明细：\n");
        for (ScoreItem item : scoreItems) {
            context.append("- ").append(item.getItem())
                    .append("：").append(item.getScore()).append("/").append(item.getMaxScore())
                    .append("（").append(item.getMessage()).append("）\n");
            if (item.getSubItems() != null) {
                for (SubScoreItem sub : item.getSubItems()) {
                    context.append("  · ").append(sub.getName())
                            .append(sub.getHit() ? "（已掌握）" : "（缺失）").append("\n");
                }
            }
        }

        Map<String, Object> input = new HashMap<>();
        input.put("task", "advice");
        input.put("context", context.toString());
        JsonNode node = aiSceneJsonClient.executeForJson(SCENE_RESUME_OPTIMIZE, input, vo.getUserId());
        if (node == null) {
            return null;
        }

        try {
            ResumeAiAdviceVO result = objectMapper.convertValue(node, ResumeAiAdviceVO.class);
            result.setResumeId(vo.getId());
            result.setGeneratedTime(LocalDateTime.now());
            result.setAiPowered(true);
            // 确保 score 与 grade 来自评分明细（避免 LLM 幻觉）
            int total = scoringServiceTotal(scoreItems);
            result.setScore(total);
            result.setGrade(calcGrade(total, sumMax(scoreItems)));
            return result;
        } catch (Exception e) {
            log.warn("[ResumeAiAdvice] 网关返回 JSON 映射失败：{}", e.getMessage());
            return null;
        }
    }

    /** 计算总分 */
    private int scoringServiceTotal(List<ScoreItem> items) {
        int sum = 0;
        for (ScoreItem it : items) {
            if (it.getScore() != null) sum += it.getScore();
        }
        return sum;
    }

    /** 计算满分 */
    private int sumMax(List<ScoreItem> items) {
        int sum = 0;
        for (ScoreItem it : items) {
            if (it.getMaxScore() != null) sum += it.getMaxScore();
        }
        return sum;
    }

    // ========================================================================
    // 规则化生成（默认实现，AI 未启用或失败时兜底）
    // ========================================================================
    private ResumeAiAdviceVO generateAdviceRuleBased(UserResumeVO vo, List<ScoreItem> scoreItems, String targetPosition) {
        ResumeAiAdviceVO result = new ResumeAiAdviceVO();
        result.setResumeId(vo.getId());
        result.setGeneratedTime(LocalDateTime.now());
        result.setAiPowered(false); // 规则化生成

        int totalScore = scoringServiceTotal(scoreItems);
        int totalMax = sumMax(scoreItems);
        result.setScore(totalScore);
        result.setGrade(calcGrade(totalScore, totalMax));

        List<ResumeAiAdviceVO.AdviceItem> advices = new ArrayList<>();
        List<String> missingSkills = new ArrayList<>();

        // 1. 遍历各维度，识别低分维度生成建议
        for (ScoreItem item : scoreItems) {
            if (item == null || item.getMaxScore() == null || item.getMaxScore() == 0) continue;
            double rate = (item.getScore() == null ? 0 : item.getScore()) / (double) item.getMaxScore();

            // 岗位匹配度维度：提取缺失技能
            if ("岗位匹配度".equals(item.getItem())) {
                handlePositionMatchAdvice(item, targetPosition, advices, missingSkills);
                continue;
            }

            // 其他维度：得分率 < 60% 生成改进建议
            if (rate < 0.6) {
                ResumeAiAdviceVO.AdviceItem advice = new ResumeAiAdviceVO.AdviceItem();
                advice.setDimension(item.getItem());
                advice.setType(rate < 0.3 ? "fill" : "refine");
                advice.setPriority(rate < 0.3 ? "high" : "medium");
                advice.setContent(buildDimensionAdvice(item, rate));
                advice.setOptimized(buildDimensionOptimized(item, rate));
                advices.add(advice);
            }
        }

        // 2. 生成整体 summary
        result.setSummary(buildSummary(totalScore, totalMax, targetPosition, missingSkills));
        result.setAdvices(advices);
        result.setMissingSkills(missingSkills);

        log.debug("[ResumeAiAdvice] 简历 {} 生成 {} 条建议，缺失技能 {} 个",
                vo.getId(), advices.size(), missingSkills.size());
        return result;
    }

    /** 处理岗位匹配度维度的建议：提取缺失技能，生成高优先级 match 建议 */
    private void handlePositionMatchAdvice(ScoreItem item, String targetPosition,
                                           List<ResumeAiAdviceVO.AdviceItem> advices,
                                           List<String> missingSkills) {
        if (item.getSubItems() == null || item.getSubItems().isEmpty()) return;

        for (SubScoreItem sub : item.getSubItems()) {
            if (sub.getHit() != null && !sub.getHit() && StringUtils.isNotEmpty(sub.getName())) {
                missingSkills.add(sub.getName());
            }
        }

        if (!missingSkills.isEmpty()) {
            ResumeAiAdviceVO.AdviceItem advice = new ResumeAiAdviceVO.AdviceItem();
            advice.setDimension("岗位匹配度");
            advice.setType("match");
            advice.setPriority("high");
            String posDesc = StringUtils.isNotEmpty(targetPosition) ? "目标岗位「" + targetPosition + "」" : "目标岗位";
            advice.setContent(posDesc + "尚有 " + missingSkills.size() + " 项必备技能缺失（"
                    + String.join("、", missingSkills.size() > 5 ? missingSkills.subList(0, 5) : missingSkills)
                    + (missingSkills.size() > 5 ? "等" : "")
                    + "），建议优先补充相关项目经验或技能证明");
            // optimized：缺失技能整理为按熟练度分级的技能清单模板（可直接采纳到技能列表）
            advice.setOptimized("了解：" + String.join("、", missingSkills));
            advices.add(advice);
        }
    }

    /** 根据维度名生成针对性建议内容 */
    private String buildDimensionAdvice(ScoreItem item, double rate) {
        String name = item.getItem();
        String msg = item.getMessage();
        if (rate < 0.3) {
            switch (name) {
                case "基本信息": return "基本信息严重缺失（" + msg + "），求职联系方式不完整将影响 HR 联系，请优先补全姓名、电话、邮箱";
                case "求职意向": return "求职意向不明确，HR 筛选简历时无法判断匹配度，请补全期望职位、城市、薪资范围";
                case "教育经历": return "教育经历缺失或不完整，建议补全学校、专业、学历与起止时间";
                case "工作经历": return "工作经历缺失或描述粗糙，HR 最看重此维度，建议用 STAR 法则（情境-任务-行动-结果）补充量化成果";
                case "项目经历": return "项目经历缺失或描述简略，建议补充 2-3 个代表性项目，突出技术栈与个人贡献";
                case "技能列表": return "技能列表过少，建议补充与目标岗位相关的核心技术栈";
                case "自我介绍": return "自我介绍缺失或过短，建议用 50-100 字概括核心优势与求职亮点";
                default: return name + "维度得分较低（" + msg + "），建议完善";
            }
        } else {
            switch (name) {
                case "基本信息": return "基本信息可进一步完整，建议补充性别、出生日期等辅助字段";
                case "求职意向": return "求职意向可优化，建议明确工作性质（全职/兼职）与到岗时间";
                case "教育经历": return "教育经历可补充描述，如主修课程、GPA、奖学金等亮点";
                case "工作经历": return "工作经历描述可优化，建议量化产出（如\"性能提升 30%\"、\"承担 X 人团队管理\"）";
                case "项目经历": return "项目经历可优化，建议补充项目角色、技术难点与解决方案";
                case "技能列表": return "技能列表可优化，建议标注熟练度（了解/一般/熟练/精通）与分类";
                case "自我介绍": return "自我介绍可优化，建议结合目标岗位突出差异化优势";
                default: return name + "维度可进一步优化（" + msg + "）";
            }
        }
    }

    /**
     * 根据维度名生成 optimized（优化后可直接采纳的文本模板）
     * <p>规则化兜底无改写能力，生成"结构模板 + 占位符"供用户采纳后微调；
     * [X]/[X%] 等占位符由前端提示用户填写。</p>
     */
    private String buildDimensionOptimized(ScoreItem item, double rate) {
        String name = item.getItem();
        if (rate < 0.3) {
            switch (name) {
                case "基本信息": return null; // 基本信息为结构化字段（姓名/电话等），无文本可替换，前端引导手动完善
                case "求职意向": return null; // 同上：期望职位/城市/薪资为结构化字段
                case "教育经历": return "[学校名称] · [专业] · [学历] · [起止年份]\n主修课程：[课程1]、[课程2]、[课程3]\n荣誉亮点：[GPA/奖学金/竞赛，无则删除本行]";
                case "工作经历": return "1. [负责/主导][业务模块]，通过[技术方案]，实现[量化成果，如效率提升 X%]\n2. [第二项职责成果，突出个人贡献]\n3. [第三项职责成果，突出团队协作或技术深度]";
                case "项目经历": return "[项目名称] · [担任角色]\n项目背景：[一句话说明业务规模与价值]\n1. [技术难点] → [解决方案与选型思路]\n2. [量化成果，如性能提升 X%、覆盖用户 X 万]";
                case "技能列表": return "精通：[核心技术1]、[核心技术2]\n熟练：[技术3]、[技术4]、[技术5]\n了解：[技术6]、[技术7]";
                case "自我介绍": return "[X 年][领域]经验，专注[核心方向]。[主导/参与]过[代表性项目/业务]，实现[量化成果]。熟悉[技术栈/方法论]，具备[软实力亮点]。期望在[目标岗位]方向持续深耕。";
                default: return null;
            }
        } else {
            switch (name) {
                case "基本信息": return null;
                case "求职意向": return null;
                case "教育经历": return "主修课程：[课程1]、[课程2]、[课程3]\n荣誉亮点：[GPA/奖学金/竞赛]";
                case "工作经历": return "1. [现有职责]升级表述：负责[业务]，通过[方案]，使[指标]提升[X%]\n2. [补充第二条量化成果]";
                case "项目经历": return "项目角色：[角色]\n技术难点：[难点] → 解决方案：[方案]\n量化成果：[指标]从[X]提升至[Y]";
                case "技能列表": return "精通：[最高频使用的核心技术]\n熟练：[常用技术]\n了解：[接触过的扩展技术]";
                case "自我介绍": return "在原有自评基础上补充：[X 年]经验 + [核心成果] + [技术深度] + [职业态度]，删除形容词堆砌，每个论点配一个数字。";
                default: return null;
            }
        }
    }

    /** 计算评分等级：A≥90%, B≥75%, C≥60%, D<60%（按得分率） */
    private String calcGrade(int score, int max) {
        if (max == 0) return "D";
        double rate = score / (double) max;
        if (rate >= 0.9) return "A";
        if (rate >= 0.75) return "B";
        if (rate >= 0.6) return "C";
        return "D";
    }

    /** 生成整体总结 */
    private String buildSummary(int score, int max, String targetPosition, List<String> missingSkills) {
        StringBuilder sb = new StringBuilder();
        sb.append("当前评分 ").append(score).append("/").append(max);
        if (StringUtils.isNotEmpty(targetPosition)) {
            sb.append("，目标岗位「").append(targetPosition).append("」");
        }
        double rate = max == 0 ? 0 : score / (double) max;
        if (rate >= 0.9) {
            sb.append("，简历质量优秀，仅需微调即可投递");
        } else if (rate >= 0.75) {
            sb.append("，简历质量良好，针对低分维度优化可显著提升");
        } else if (rate >= 0.6) {
            sb.append("，简历质量合格，建议重点完善核心维度");
        } else {
            sb.append("，简历质量待提升，建议优先补全基础信息与工作经历");
        }
        if (!missingSkills.isEmpty()) {
            sb.append("。另有 ").append(missingSkills.size()).append(" 项岗位必备技能缺失，建议针对性补充");
        }
        return sb.toString();
    }
}
