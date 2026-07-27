package com.moyun.ext.cms.service;

import com.fasterxml.jackson.databind.ObjectMapper;
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
import java.util.List;

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

    private static final Logger log = LoggerFactory.getLogger(ResumeAiAdviceService.class);

    @Autowired
    private AiProperties aiProperties;

    @Autowired
    private LlmClient llmClient;

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
     * 通过 LLM 生成建议（v5.9 阶段3：AI 模型接入）
     * <p>
     * 构造 system prompt 定义 AI 角色，将简历摘要 + 评分明细 + 目标岗位作为 user message 输入。
     * 当前为框架预留：解析 LLM 返回的 JSON 为 ResumeAiAdviceVO；解析失败回退 null。
     */
    private ResumeAiAdviceVO generateAdviceWithLlm(UserResumeVO vo, List<ScoreItem> scoreItems, String targetPosition) {
        String systemPrompt = "你是一名资深 HR 与简历顾问，擅长基于评分明细给出可执行的改进建议。"
                + "请返回 JSON 格式，字段：summary(整体总结), advices(数组，每项含 dimension/priority(high/medium/low)/content/type(fill/refine/match)), missingSkills(字符串数组)。"
                + "建议要具体、可执行，优先关注得分率低于60%的维度与岗位匹配度缺失技能。";

        StringBuilder userMessage = new StringBuilder();
        userMessage.append("目标岗位：").append(StringUtils.isNotEmpty(targetPosition) ? targetPosition : "未设置").append("\n");
        userMessage.append("当前评分：").append(scoringServiceTotal(scoreItems)).append(" 分\n");
        userMessage.append("评分明细：\n");
        for (ScoreItem item : scoreItems) {
            userMessage.append("- ").append(item.getItem())
                    .append("：").append(item.getScore()).append("/").append(item.getMaxScore())
                    .append("（").append(item.getMessage()).append("）\n");
            if (item.getSubItems() != null) {
                for (SubScoreItem sub : item.getSubItems()) {
                    userMessage.append("  · ").append(sub.getName())
                            .append(sub.getHit() ? "（已掌握）" : "（缺失）").append("\n");
                }
            }
        }

        String llmResponse = llmClient.chat(systemPrompt, userMessage.toString());
        if (StringUtils.isEmpty(llmResponse)) {
            return null;
        }

        try {
            // 解析 LLM 返回的 JSON 为 VO
            ResumeAiAdviceVO result = objectMapper.readValue(llmResponse, ResumeAiAdviceVO.class);
            result.setResumeId(vo.getId());
            result.setGeneratedTime(LocalDateTime.now());
            result.setAiPowered(true);
            // 确保 score 与 grade 来自评分明细（避免 LLM 幻觉）
            int total = scoringServiceTotal(scoreItems);
            result.setScore(total);
            result.setGrade(calcGrade(total, sumMax(scoreItems)));
            return result;
        } catch (Exception e) {
            log.warn("[ResumeAiAdvice] LLM 返回 JSON 解析失败：{}", e.getMessage());
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
