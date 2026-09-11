package com.moyun.ext.cms.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.moyun.ext.ai2.support.AiSceneJsonClient;
import com.moyun.ext.cms.config.AiProperties;
import com.moyun.ext.cms.domain.vo.UserResumeVO;
import com.moyun.portal.domain.entity.PortalResumeJobMatch;
import com.moyun.portal.domain.entity.PortalResumeJobTarget;
import com.moyun.portal.mapper.PortalResumeJobMatchMapper;
import com.moyun.portal.mapper.PortalResumeJobTargetMapper;
import com.moyun.common.exception.system.ServiceException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * 岗位匹配分析服务（v10.13 简历优化重构）
 * <p>
 * 链路：目标岗位 JD + 在线简历 → LLM 四维匹配分析（关键词/经验/技能/结构）→ 匹配报告存档。
 * LLM 未启用/失败时回退规则分析（JD 关键词命中统计）。
 * </p>
 *
 * @author moyun
 */
@Service
public class ResumeJobMatchService {
    /** v11.39：本服务所属 AI 场景代码（绑定见 ai_scene_config，业务不感知模型选择） */
    private static final String SCENE_RESUME_OPTIMIZE = "resume_optimize";


    private static final Logger log = LoggerFactory.getLogger(ResumeJobMatchService.class);

    @Autowired
    private AiProperties aiProperties;

    @Autowired
    private LlmClient llmClient;

    /** v11.58 P0-3：匹配分析统一走 AI 网关（task=job_match 子任务） */
    @Autowired
    private AiSceneJsonClient aiSceneJsonClient;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private PortalResumeJobTargetMapper jobTargetMapper;

    @Autowired
    private PortalResumeJobMatchMapper jobMatchMapper;

    /**
     * 执行岗位匹配分析并保存报告
     *
     * @param userId      用户ID
     * @param resume      简历详情
     * @param jobTargetId 岗位目标ID
     * @return 匹配报告（已含 id，可追溯）
     */
    public PortalResumeJobMatch analyze(Long userId, UserResumeVO resume, Long jobTargetId) {
        PortalResumeJobTarget target = jobTargetMapper.selectById(jobTargetId);
        if (target == null || !target.getUserId().equals(userId)) {
            throw new ServiceException("岗位目标不存在或无权访问");
        }

        PortalResumeJobMatch report;
        boolean llmOk = false;
        if (aiProperties.isEnabled() && aiProperties.isResumeAdviceEnabled() && llmClient.isEnabled()) {
            try {
                report = analyzeByLlm(userId, resume, target);
                llmOk = true;
            } catch (Exception e) {
                log.warn("[JobMatch] LLM 分析失败，回退规则分析：{}", e.getMessage());
                report = analyzeByRule(resume, target);
            }
        } else {
            report = analyzeByRule(resume, target);
        }

        report.setUserId(userId);
        report.setResumeId(resume.getId());
        report.setJobTargetId(jobTargetId);
        report.setAiPowered(llmOk ? 1 : 0);
        jobMatchMapper.insert(report);
        return report;
    }

    /** 查询简历最近一次匹配报告（无则 null） */
    public PortalResumeJobMatch latestReport(Long userId, Long resumeId) {
        return jobMatchMapper.selectOne(
                new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<PortalResumeJobMatch>()
                        .eq(PortalResumeJobMatch::getUserId, userId)
                        .eq(PortalResumeJobMatch::getResumeId, resumeId)
                        .orderByDesc(PortalResumeJobMatch::getId)
                        .last("LIMIT 1"));
    }

    // ==================== LLM 分析 ====================

    /**
     * v11.58 P0-3 业务收口：经统一网关执行 resume_optimize 场景（task=job_match）。
     * 提示词已收编至 ResumeOptimizeHandler（逐字一致），本方法仅组装上下文与结果映射。
     */
    private PortalResumeJobMatch analyzeByLlm(Long userId, UserResumeVO resume, PortalResumeJobTarget target) throws Exception {
        // v10.22 阶段3：AI 分析优先使用 full_text 全文纯文本，上下文更完整；
        // fullText 为空时降级为结构化 JSON（兼容旧简历或未拼接 full_text 的场景）
        String resumeContent = (resume.getFullText() != null && !resume.getFullText().isBlank())
                ? resume.getFullText()
                : objectMapper.writeValueAsString(resume);
        String context = "【目标岗位】" + target.getPosition()
                + (target.getCompany() != null ? " · " + target.getCompany() : "")
                + "\n【岗位JD】\n" + target.getJdText()
                + "\n\n【候选人简历】\n" + resumeContent;

        Map<String, Object> input = new HashMap<>();
        input.put("task", "job_match");
        input.put("context", context);
        JsonNode node = aiSceneJsonClient.executeForJson(SCENE_RESUME_OPTIMIZE, input, userId);
        if (node == null) {
            throw new IllegalStateException("AI网关匹配分析失败");
        }
        if (node.path("matchScore").isMissingNode()) {
            throw new IllegalStateException("LLM 返回缺少 matchScore 字段");
        }

        PortalResumeJobMatch report = new PortalResumeJobMatch();
        report.setMatchScore(node.path("matchScore").asInt());
        report.setGrade(node.path("grade").asText(null));
        report.setMatchedKeywords(joinKeywords(node.path("matchedKeywords")));
        report.setMissingKeywords(joinKeywords(node.path("missingKeywords")));
        report.setDimensions(node.path("dimensions").isObject() ? node.path("dimensions") : null);
        report.setSummary(node.path("summary").asText(null));
        return report;
    }

    // ==================== 规则分析（兜底） ====================

    /** 规则兜底：JD 关键词命中统计 + 结构完整度 */
    private PortalResumeJobMatch analyzeByRule(UserResumeVO resume, PortalResumeJobTarget target) {
        String resumeText = buildResumeText(resume);
        Set<String> jdKeywords = extractJdKeywords(target.getJdText());
        Set<String> matched = new HashSet<>();
        Set<String> missing = new HashSet<>();
        for (String kw : jdKeywords) {
            if (resumeText.toLowerCase().contains(kw.toLowerCase())) {
                matched.add(kw);
            } else {
                missing.add(kw);
            }
        }
        // 关键词匹配度（无关键词时按 60 兜底）
        int kwScore = jdKeywords.isEmpty() ? 60 : (int) Math.round(matched.size() * 100.0 / jdKeywords.size());
        // 结构完整度：七区块覆盖统计
        boolean[] filled = {
                resume.getName() != null && !resume.getName().isBlank(),
                resume.getJobIntention() != null && resume.getJobIntention().getPosition() != null,
                resume.getEducations() != null && !resume.getEducations().isEmpty(),
                resume.getWorks() != null && !resume.getWorks().isEmpty(),
                resume.getProjects() != null && !resume.getProjects().isEmpty(),
                resume.getSkills() != null && !resume.getSkills().isEmpty(),
                resume.getSelfIntro() != null && !resume.getSelfIntro().isBlank(),
        };
        int filledCount = 0;
        for (boolean b : filled) {
            if (b) filledCount++;
        }
        int structScore = (int) Math.round(filledCount * 100.0 / filled.length);
        int matchScore = Math.round(kwScore * 0.6f + structScore * 0.4f);

        PortalResumeJobMatch report = new PortalResumeJobMatch();
        report.setMatchScore(matchScore);
        report.setGrade(matchScore >= 85 ? "excellent" : matchScore >= 70 ? "good" : matchScore >= 50 ? "medium" : "poor");
        report.setMatchedKeywords(String.join("、", matched));
        report.setMissingKeywords(String.join("、", missing));
        report.setDimensions(buildRuleDimensions(kwScore, structScore, missing));
        report.setSummary("基于JD关键词与简历结构的基础分析：关键词覆盖 " + matched.size() + "/" + jdKeywords.size()
                + "，结构完整度 " + structScore + "%。启用 AI 模型后可获得更精准的四维分析。");
        return report;
    }

    private JsonNode buildRuleDimensions(int kwScore, int structScore, Set<String> missing) {
        ObjectNode dims = objectMapper.createObjectNode();
        dims.putObject("keywordMatch").put("score", kwScore)
                .set("suggestions", missing.isEmpty() ? objectMapper.createArrayNode()
                        : arrayOf("补充缺失关键词相关经验：" + String.join("、", limit(missing, 5))));
        dims.putObject("experienceMatch").put("score", Math.min(100, kwScore + 10))
                .set("suggestions", objectMapper.createArrayNode());
        dims.putObject("skillMatch").put("score", kwScore)
                .set("suggestions", objectMapper.createArrayNode());
        dims.putObject("structureMatch").put("score", structScore)
                .set("suggestions", objectMapper.createArrayNode());
        return dims;
    }

    private ArrayNode arrayOf(String text) {
        ArrayNode arr = objectMapper.createArrayNode();
        arr.add(text);
        return arr;
    }

    private List<String> limit(Set<String> set, int n) {
        return set.stream().limit(n).collect(Collectors.toList());
    }

    /** 简历全文（关键词匹配用） */
    private String buildResumeText(UserResumeVO r) {
        StringBuilder sb = new StringBuilder();
        if (r.getJobIntention() != null) {
            sb.append(r.getJobIntention().getPosition()).append(' ');
        }
        if (r.getSkills() != null) {
            r.getSkills().forEach(s -> sb.append(s.getName()).append(' '));
        }
        if (r.getWorks() != null) {
            r.getWorks().forEach(w -> sb.append(w.getCompany()).append(' ').append(w.getPosition()).append(' ')
                    .append(w.getDescription() == null ? "" : w.getDescription()).append(' '));
        }
        if (r.getProjects() != null) {
            r.getProjects().forEach(p -> sb.append(p.getName()).append(' ')
                    .append(p.getDescription() == null ? "" : p.getDescription()).append(' '));
        }
        sb.append(r.getSelfIntro() == null ? "" : r.getSelfIntro());
        return sb.toString();
    }

    /** JD 关键词提取：常见技术/要求词表 + 引号内容 */
    private static final List<String> KNOWN_KEYWORDS = Arrays.asList(
            "Java", "Spring Boot", "Spring Cloud", "微服务", "分布式", "高并发", "MySQL", "Redis",
            "Kafka", "RabbitMQ", "Docker", "Kubernetes", "K8s", "CI/CD", "Linux", "Python", "Go",
            "JavaScript", "TypeScript", "Vue", "React", "Node.js", "性能调优", "架构设计", "团队管理",
            "沟通能力", "MySQL调优", "消息队列", "缓存", "搜索", "ES", "Elasticsearch");

    private Set<String> extractJdKeywords(String jd) {
        Set<String> keywords = new HashSet<>();
        if (jd == null) {
            return keywords;
        }
        for (String kw : KNOWN_KEYWORDS) {
            if (jd.toLowerCase().contains(kw.toLowerCase())) {
                keywords.add(kw);
            }
        }
        // “精通XXX”模式补充
        Matcher m = Pattern.compile("(精通|熟悉|掌握|了解)([A-Za-z0-9+/\\.\\-\\u4e00-\\u9fa5]{2,12})").matcher(jd);
        while (m.find()) {
            keywords.add(m.group(2));
        }
        return keywords;
    }

    private String joinKeywords(JsonNode arr) {
        if (arr == null || !arr.isArray()) {
            return null;
        }
        List<String> list = new ArrayList<>();
        arr.forEach(n -> {
            String v = n.asText(null);
            if (v != null && !v.isBlank()) {
                list.add(v.trim());
            }
        });
        return list.isEmpty() ? null : String.join("、", list);
    }
}
