package com.moyun.ext.cms.service.interview;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.moyun.portal.domain.entity.PortalUserResume;
import com.moyun.util.string.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * 简历上下文（v11.x 智能面试）
 *
 * <p>对 {@link PortalUserResume} 的一次性解析封装：JSON 字段只解析一次，
 * digest（项目摘要）与 keywords（出题关键词）懒加载构建。</p>
 *
 * <p>迁移自 VoiceInterviewServiceImpl.buildResumeDigest，外部行为不变；
 * 新增 keywords 能力供 QuestionPicker 简历深挖题源匹配使用。</p>
 *
 * @author moyun
 */
public final class ResumeContext {

    private static final Logger log = LoggerFactory.getLogger(ResumeContext.class);

    private static final ObjectMapper MAPPER = new ObjectMapper();

    /** 关键词提取上限（对齐 VoiceInterviewServiceImpl.MAX_KEYWORDS） */
    private static final int MAX_KEYWORDS = 12;

    /** 简历摘要：项目数量上限 */
    private static final int MAX_DIGEST_PROJECTS = 3;

    private final PortalUserResume resume;

    /** 解析一次的项目节点（锚定题/摘要共用） */
    private final List<JsonNode> projectNodes;

    /** 解析一次的技能名称列表 */
    private final List<String> skillNames;

    /** 懒加载：项目摘要（追问上下文） */
    private volatile String digest;

    /** 懒加载：出题关键词（stack + skills，去重，上限 12） */
    private volatile List<String> keywords;

    private ResumeContext(PortalUserResume resume) {
        this.resume = resume;
        this.projectNodes = parseProjects(resume);
        this.skillNames = parseSkillNames(resume);
    }

    /**
     * 从简历实体构建上下文（JSON 字段在此解析一次）
     */
    public static ResumeContext of(PortalUserResume resume) {
        return new ResumeContext(resume);
    }

    /** 空上下文（无简历） */
    public static ResumeContext empty() {
        return new ResumeContext(null);
    }

    public boolean hasResume() {
        return resume != null;
    }

    public Long getResumeId() {
        return resume == null ? null : resume.getId();
    }

    /** 解析置信度（LLM=85/规则=60/null=未解析），供追问与出题策略参考 */
    public Integer getParseConfidence() {
        return resume == null ? null : resume.getParseConfidence();
    }

    /** 解析后的项目节点（供锚定题生成使用） */
    public List<JsonNode> getProjectNodes() {
        return projectNodes;
    }

    /**
     * 简历项目摘要：项目名（技术栈）——亮点，最多 3 个项目。
     * 迁移自 VoiceInterviewServiceImpl.buildResumeDigest，输出格式不变。
     */
    public String digest() {
        if (digest != null) {
            return digest;
        }
        synchronized (this) {
            if (digest != null) {
                return digest;
            }
            if (projectNodes.isEmpty()) {
                digest = "";
                return digest;
            }
            StringBuilder sb = new StringBuilder();
            int n = Math.min(projectNodes.size(), MAX_DIGEST_PROJECTS);
            for (int i = 0; i < n; i++) {
                JsonNode proj = projectNodes.get(i);
                String name = proj.path("name").asText("");
                String stack = proj.path("stack").asText("");
                String highlight = proj.path("highlight").asText("");
                if (StringUtils.isEmpty(name)) {
                    continue;
                }
                sb.append("项目").append(i + 1).append("：").append(name);
                if (StringUtils.isNotEmpty(stack)) {
                    sb.append("（").append(stack).append("）");
                }
                if (StringUtils.isNotEmpty(highlight)) {
                    sb.append("——").append(highlight);
                }
                sb.append("\n");
            }
            digest = sb.toString();
            return digest;
        }
    }

    /**
     * 出题关键词：项目技术栈 + 技能列表，去重后上限 12 个。
     * 供 QuestionPicker 简历深挖题源按 tags 匹配题目。
     */
    public List<String> keywords() {
        if (keywords != null) {
            return keywords;
        }
        synchronized (this) {
            if (keywords != null) {
                return keywords;
            }
            Set<String> set = new LinkedHashSet<>();
            // 1. 项目技术栈（stack 可能是 "Spring Boot, Redis, MySQL" 形式）
            for (JsonNode proj : projectNodes) {
                String stack = proj.path("stack").asText("");
                for (String token : splitTokens(stack)) {
                    if (set.size() >= MAX_KEYWORDS) {
                        break;
                    }
                    set.add(token);
                }
            }
            // 2. 技能名称
            for (String skill : skillNames) {
                if (set.size() >= MAX_KEYWORDS) {
                    break;
                }
                set.add(skill);
            }
            keywords = List.copyOf(set);
            return keywords;
        }
    }

    // ==================== 内部解析 ====================

    private static List<JsonNode> parseProjects(PortalUserResume resume) {
        if (resume == null || StringUtils.isEmpty(resume.getProjects())) {
            return List.of();
        }
        try {
            JsonNode arr = MAPPER.readTree(resume.getProjects());
            List<JsonNode> nodes = new ArrayList<>();
            if (arr != null && arr.isArray()) {
                arr.forEach(nodes::add);
            }
            return nodes;
        } catch (Exception e) {
            log.warn("[ResumeContext] projects 解析失败 resumeId={}：{}", resume.getId(), e.getMessage());
            return List.of();
        }
    }

    private static List<String> parseSkillNames(PortalUserResume resume) {
        if (resume == null || StringUtils.isEmpty(resume.getSkills())) {
            return List.of();
        }
        try {
            JsonNode arr = MAPPER.readTree(resume.getSkills());
            List<String> names = new ArrayList<>();
            if (arr != null && arr.isArray()) {
                arr.forEach(node -> {
                    String name = node.path("name").asText("");
                    if (StringUtils.isNotEmpty(name)) {
                        names.add(name.trim());
                    }
                });
            }
            return names;
        } catch (Exception e) {
            log.warn("[ResumeContext] skills 解析失败 resumeId={}：{}", resume.getId(), e.getMessage());
            return List.of();
        }
    }

    private static List<String> splitTokens(String raw) {
        List<String> tokens = new ArrayList<>();
        if (StringUtils.isEmpty(raw)) {
            return tokens;
        }
        for (String token : raw.split("[,，、/;；\\s]+")) {
            String t = token.trim();
            if (StringUtils.isNotEmpty(t)) {
                tokens.add(t);
            }
        }
        return tokens;
    }
}