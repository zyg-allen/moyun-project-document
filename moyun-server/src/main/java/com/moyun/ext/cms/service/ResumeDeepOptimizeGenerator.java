package com.moyun.ext.cms.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.moyun.ext.ai2.support.AiSceneJsonClient;
import com.moyun.ext.cms.config.AiProperties;
import com.moyun.ext.cms.domain.vo.ResumeDeepOptimizeVO;
import com.moyun.ext.cms.domain.vo.UserResumeVO;
import com.moyun.portal.domain.entity.PortalResumeJobTarget;
import com.moyun.portal.mapper.PortalResumeJobTargetMapper;
import com.moyun.common.exception.system.ServiceException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 简历深度优化的「生成能力」（v10.19：从 ResumeDeepOptimizeService 抽离，解决循环依赖）
 *
 * <p><strong>抽离原因</strong>：原 {@link ResumeDeepOptimizeService} 同时持有 {@code generate()}
 * 和 {@code submitTask()}（触发异步）两类职责；异步执行方又需要调用 {@code generate()}，
 * 形成 A→B→A 循环依赖。</p>
 *
 * <p><strong>结构改造</strong>：把 generate 及其私有方法（buildResumeContext/fillOriginal/safe）
 * 和它们依赖的 {@code aiProperties/llmClient/objectMapper/jobTargetMapper} 一起迁到本 Bean。
 * v10.23 起异步执行统一走通用 AI 任务基础设施，依赖图无环：</p>
 * <pre>
 *   AiTaskAsyncExecutor ──→ DeepOptimizeTaskHandler ──→ ResumeDeepOptimizeGenerator
 *   ResumeDeepOptimizeService ──→ ResumeDeepOptimizeGenerator（门面委托）
 * </pre>
 *
 * <p>本 Bean 不反向依赖任何 Service/Executor，只持有数据访问与 LLM 客户端，职责单一。</p>
 *
 * @author moyun
 */
@Service
public class ResumeDeepOptimizeGenerator {
    /** v11.39：本服务所属 AI 场景代码（绑定见 ai_scene_config，业务不感知模型选择） */
    private static final String SCENE_RESUME_OPTIMIZE = "resume_optimize";


    private static final Logger log = LoggerFactory.getLogger(ResumeDeepOptimizeGenerator.class);

    @Autowired
    private AiProperties aiProperties;

    @Autowired
    private LlmClient llmClient;

    /** v11.58 P0-3：深度优化生成统一走 AI 网关（task=deep_optimize 子任务） */
    @Autowired
    private AiSceneJsonClient aiSceneJsonClient;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private PortalResumeJobTargetMapper jobTargetMapper;

    /**
     * 生成深度优化建议（逐项前后对比）
     *
     * <p>v11.58 P0-3 业务收口：经统一网关执行 resume_optimize 场景（task=deep_optimize），
     * 提示词收编至 ResumeOptimizeHandler，本方法仅组装 JD+简历上下文与结果映射。</p>
     *
     * @param resume      简历详情
     * @param jobTargetId 岗位目标ID
     */
    public ResumeDeepOptimizeVO generate(UserResumeVO resume, Long jobTargetId) {
        PortalResumeJobTarget target = jobTargetMapper.selectById(jobTargetId);
        if (target == null) {
            throw new ServiceException("岗位目标不存在");
        }
        if (!aiProperties.isEnabled() || !aiProperties.isResumeAdviceEnabled() || !llmClient.isEnabled()) {
            throw new ServiceException("深度优化需要 AI 模型支持，请管理员在后台配置 AI 模型后使用");
        }

        // v10.19：精简 prompt 输入，仅传简历核心内容（去掉 id/version/status/时间戳等无关字段），
        //         降低输入 token 加快响应，避免 60s 超时；异步任务化后 timeout 已调到 180s 双保险。
        // v10.22 阶段3：优先使用 full_text 全文纯文本，上下文更完整；
        //               fullText 为空时降级为 buildResumeContext 拼接结构化字段
        String resumeContent = (resume.getFullText() != null && !resume.getFullText().isBlank())
                ? resume.getFullText()
                : buildResumeContext(resume);
        String context = "【目标岗位】" + target.getPosition()
                + "\n【岗位JD】\n" + target.getJdText()
                + "\n\n【简历核心内容】\n" + resumeContent;

        try {
            Map<String, Object> input = new HashMap<>();
            input.put("task", "deep_optimize");
            input.put("context", context);
            JsonNode node = aiSceneJsonClient.executeForJson(SCENE_RESUME_OPTIMIZE, input, resume.getUserId());
            if (node == null) {
                throw new ServiceException("深度优化生成失败：AI 服务暂不可用，请稍后重试");
            }
            ResumeDeepOptimizeVO vo = new ResumeDeepOptimizeVO();
            vo.setResumeId(resume.getId());
            vo.setJobTargetId(jobTargetId);
            vo.setSummary(node.path("summary").asText(null));
            vo.setAiPowered(true);
            List<ResumeDeepOptimizeVO.OptimizeItem> items = new ArrayList<>();
            JsonNode arr = node.path("items");
            if (arr.isArray()) {
                for (JsonNode itemNode : arr) {
                    ResumeDeepOptimizeVO.OptimizeItem item = new ResumeDeepOptimizeVO.OptimizeItem();
                    item.setSection(itemNode.path("section").asText(null));
                    item.setIndex(itemNode.path("index").asInt(0));
                    item.setField(itemNode.path("field").asText(null));
                    item.setOptimized(itemNode.path("optimized").asText(null));
                    item.setReason(itemNode.path("reason").asText(null));
                    if (item.getSection() != null && item.getOptimized() != null) {
                        fillOriginal(resume, item);
                        items.add(item);
                    }
                }
            }
            if (items.isEmpty()) {
                throw new ServiceException("AI 未生成有效优化建议，请稍后重试");
            }
            vo.setItems(items);
            // v10.20：输出 items 摘要日志，便于排查 section/index 不匹配导致采纳失败的问题
            if (log.isInfoEnabled()) {
                StringBuilder sb = new StringBuilder();
                for (int i = 0; i < items.size(); i++) {
                    ResumeDeepOptimizeVO.OptimizeItem it = items.get(i);
                    if (i > 0) sb.append(", ");
                    sb.append("[").append(i).append("]")
                            .append("section=").append(it.getSection())
                            .append(",index=").append(it.getIndex())
                            .append(",field=").append(it.getField());
                }
                log.info("[DeepOptimize] generate 返回 {} 项：{}", items.size(), sb);
            }
            return vo;
        } catch (ServiceException e) {
            throw e;
        } catch (Exception e) {
            log.error("[DeepOptimize] LLM 生成失败", e);
            throw new ServiceException("深度优化生成失败：" + e.getMessage());
        }
    }

    /**
     * 检查 AI 模型是否可用（供 Service 在 submitTask 前做前置校验，避免重复实现）
     */
    public boolean isAiAvailable() {
        return aiProperties.isEnabled() && aiProperties.isResumeAdviceEnabled() && llmClient.isEnabled();
    }

    /**
     * 校验岗位目标存在（供 Service 在 submitTask 前复用，避免重复查库）
     */
    public PortalResumeJobTarget getJobTarget(Long jobTargetId) {
        return jobTargetMapper.selectById(jobTargetId);
    }

    /**
     * 构建精简的简历上下文文本（v10.19：替代整体 VO 序列化）
     *
     * <p>只保留与优化相关的核心字段：基本信息、求职意向、教育/工作/项目经历、技能、自我评价。
     * 去掉 id/userId/version/status/createTime/updateTime 等无关字段，
     * 降低输入 token 加快 LLM 响应，避免超时。</p>
     */
    private String buildResumeContext(UserResumeVO resume) {
        StringBuilder sb = new StringBuilder();
        sb.append("【姓名】").append(safe(resume.getName())).append('\n');
        if (resume.getJobIntention() != null) {
            UserResumeVO.JobIntention ji = resume.getJobIntention();
            sb.append("【求职意向】")
                    .append("岗位=").append(safe(ji.getPosition()))
                    .append(", 城市=").append(safe(ji.getCity()))
                    .append(", 类型=").append(safe(ji.getJobType()))
                    .append('\n');
        }
        if (resume.getEducations() != null && !resume.getEducations().isEmpty()) {
            sb.append("【教育经历】\n");
            for (int i = 0; i < resume.getEducations().size(); i++) {
                UserResumeVO.EducationItem e = resume.getEducations().get(i);
                sb.append("  ").append(i).append(". ")
                        .append(safe(e.getSchool())).append(" · ")
                        .append(safe(e.getMajor())).append(" · ")
                        .append(safe(e.getDegree())).append(" · ")
                        .append(safe(e.getStartDate())).append("-").append(safe(e.getEndDate()))
                        .append('\n');
                if (e.getDescription() != null && !e.getDescription().isBlank()) {
                    sb.append("    描述：").append(e.getDescription()).append('\n');
                }
            }
        }
        if (resume.getWorks() != null && !resume.getWorks().isEmpty()) {
            sb.append("【工作经历】\n");
            for (int i = 0; i < resume.getWorks().size(); i++) {
                UserResumeVO.WorkItem w = resume.getWorks().get(i);
                sb.append("  ").append(i).append(". ")
                        .append(safe(w.getCompany())).append(" · ")
                        .append(safe(w.getPosition())).append(" · ")
                        .append(safe(w.getStartDate())).append("-").append(safe(w.getEndDate()))
                        .append('\n');
                if (w.getDescription() != null && !w.getDescription().isBlank()) {
                    sb.append("    描述：").append(w.getDescription()).append('\n');
                }
            }
        }
        if (resume.getProjects() != null && !resume.getProjects().isEmpty()) {
            sb.append("【项目经历】\n");
            for (int i = 0; i < resume.getProjects().size(); i++) {
                UserResumeVO.ProjectItem p = resume.getProjects().get(i);
                sb.append("  ").append(i).append(". ")
                        .append(safe(p.getName())).append(" · ").append(safe(p.getRole())).append('\n');
                if (p.getDescription() != null && !p.getDescription().isBlank()) {
                    sb.append("    描述：").append(p.getDescription()).append('\n');
                }
            }
        }
        if (resume.getSkills() != null && !resume.getSkills().isEmpty()) {
            sb.append("【专业技能】");
            for (int i = 0; i < resume.getSkills().size(); i++) {
                UserResumeVO.SkillItem s = resume.getSkills().get(i);
                if (i > 0) sb.append("、");
                sb.append(safe(s.getName())).append("(").append(safe(s.getLevel())).append(")");
            }
            sb.append('\n');
        }
        if (resume.getSelfIntro() != null && !resume.getSelfIntro().isBlank()) {
            sb.append("【自我评价】").append(resume.getSelfIntro()).append('\n');
        }
        return sb.toString();
    }

    private void fillOriginal(UserResumeVO resume, ResumeDeepOptimizeVO.OptimizeItem item) {
        int idx = item.getIndex() == null ? 0 : item.getIndex();
        String original = null;
        // v10.20：section 归一化（与 ResumeDeepOptimizeService.applyItem 保持一致）
        String section = normalizeSection(item.getSection());
        String field = item.getField() == null ? "" : item.getField().trim();
        switch (section) {
            case "selfIntro":
                original = resume.getSelfIntro();
                break;
            case "objective":
                if (resume.getJobIntention() != null && "position".equals(field)) {
                    original = resume.getJobIntention().getPosition();
                }
                break;
            case "education":
                if (resume.getEducations() != null && idx < resume.getEducations().size()
                        && "description".equals(field)) {
                    original = resume.getEducations().get(idx).getDescription();
                }
                break;
            case "work":
                if (resume.getWorks() != null && idx < resume.getWorks().size()
                        && "description".equals(field)) {
                    original = resume.getWorks().get(idx).getDescription();
                }
                break;
            case "project":
                if (resume.getProjects() != null && idx < resume.getProjects().size()
                        && "description".equals(field)) {
                    original = resume.getProjects().get(idx).getDescription();
                }
                break;
            case "skills":
                StringBuilder sb = new StringBuilder();
                if (resume.getSkills() != null) {
                    for (int i = 0; i < resume.getSkills().size(); i++) {
                        UserResumeVO.SkillItem s = resume.getSkills().get(i);
                        if (i > 0) sb.append("、");
                        sb.append(s.getName());
                        if (s.getLevel() != null && !s.getLevel().isBlank()) {
                            sb.append("(").append(s.getLevel()).append(")");
                        }
                    }
                }
                original = sb.toString();
                break;
            default:
                // 未知 section：original 留空（防御 LLM 幻觉，不影响生成结果展示）
                break;
        }
        item.setOriginal(original);
    }

    /**
     * v10.20：section 归一化（与 ResumeDeepOptimizeService.normalizeSection 保持一致）
     * 兼容 LLM 返回 works/projects/experience/self_intro 等变体
     */
    private String normalizeSection(String raw) {
        if (raw == null) return "";
        String s = raw.trim().toLowerCase();
        switch (s) {
            case "works":
            case "experience":
            case "experiences":
            case "working":
                return "work";
            case "projects":
            case "project_experience":
                return "project";
            case "educations":
            case "education_experience":
                return "education";
            case "self_intro":
            case "selfintro":
            case "selfintroduction":
            case "introduction":
            case "intro":
            case "summary":
                return "selfIntro";
            case "job_intention":
            case "jobintention":
            case "intention":
                return "objective";
            case "skill":
            case "skill_list":
            case "skilllist":
                return "skills";
            default:
                return s;
        }
    }

    /** 空值安全处理：null/空字符串统一显示为"未填写" */
    private String safe(String s) {
        return (s == null || s.isBlank()) ? "未填写" : s.trim();
    }
}
