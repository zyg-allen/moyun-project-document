package com.moyun.ext.cms.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.moyun.ext.cms.config.AiProperties;
import com.moyun.ext.cms.domain.vo.ResumeDeepOptimizeVO;
import com.moyun.ext.cms.domain.vo.ResumeOptimizeTaskVO;
import com.moyun.ext.cms.domain.vo.UserResumeVO;
import com.moyun.portal.domain.entity.PortalResumeJobMatch;
import com.moyun.portal.domain.entity.PortalResumeJobTarget;
import com.moyun.portal.domain.entity.PortalResumeOptimizeHistory;
import com.moyun.portal.domain.entity.PortalResumeOptimizeTask;
import com.moyun.portal.domain.entity.PortalUserResume;
import com.moyun.portal.mapper.PortalResumeJobTargetMapper;
import com.moyun.portal.mapper.PortalResumeJobMatchMapper;
import com.moyun.portal.mapper.PortalResumeOptimizeHistoryMapper;
import com.moyun.portal.mapper.PortalResumeOptimizeTaskMapper;
import com.moyun.portal.mapper.PortalUserResumeMapper;
import com.moyun.common.exception.system.ServiceException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

/**
 * 简历深度优化服务（v10.13 简历优化重构 / v10.19：抽出 Generator 解决循环依赖）
 * <p>
 * 链路：LLM 基于 JD 对简历逐项生成优化建议（前后对比）→ 用户逐项/批量采纳 →
 * 应用到简历表单 → 保存为新版本（版本号+1）→ 记录优化历史（评分/匹配度前后对比）。
 * </p>
 * <p>依赖 AI 模型，未启用/调用失败时抛出明确提示（深度优化无规则兜底——规则无法改写文本）。</p>
 *
 * <p><strong>v10.19 结构调整</strong>：原 {@code generate()} 及其私有方法（buildResumeContext/
 * fillOriginal/safe）和 LLM 相关依赖已迁到 {@link ResumeDeepOptimizeGenerator}，本类仅保留
 * 对外门面方法（generate/submitTask/getTaskStatus/applyAndSave），通过 generator 委托调用。
 * 这样异步执行器 {@link ResumeOptimizeAsyncExecutor} 依赖 generator 而非本类，
 * 依赖图变成 Service → Executor → Generator（单向无环），消除 A→B→A 循环依赖。</p>
 *
 * @author moyun
 */
@Service
public class ResumeDeepOptimizeService {

    private static final Logger log = LoggerFactory.getLogger(ResumeDeepOptimizeService.class);

    @Autowired
    private AiProperties aiProperties;

    @Autowired
    private LlmClient llmClient;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private PortalResumeJobTargetMapper jobTargetMapper;

    @Autowired
    private PortalResumeOptimizeHistoryMapper optimizeHistoryMapper;

    @Autowired
    private ResumeScoringService resumeScoringService;

    @Autowired
    private IUserResumeService userResumeService;

    @Autowired
    private PortalResumeJobMatchMapper jobMatchMapper;

    /** v10.19：异步任务 Mapper */
    @Autowired
    private PortalResumeOptimizeTaskMapper optimizeTaskMapper;

    /** v10.19：异步执行器（独立 Bean，保证 @Async 通过 Spring 代理生效） */
    @Autowired
    private ResumeOptimizeAsyncExecutor asyncExecutor;

    /** v10.19：生成器（抽出 generate 能力，打破循环依赖） */
    @Autowired
    private ResumeDeepOptimizeGenerator generator;

    /**
     * AI 实时辅助编辑（v10.14 设计文档 P0 需求#2）：字段级多版本优化建议
     *
     * @param field        字段类型：work_description/project_description/self_intro/skills
     * @param originalText 用户当前输入的原文
     * @param position     目标岗位（可空，来自简历求职意向）
     * @param skillNames   技能名列表（可空，作为上下文）
     * @return 建议列表（text=优化后文本，reason=理由）
     */
    public List<Map<String, String>> fieldAssist(String field, String originalText, String position, List<String> skillNames) {
        if (!aiProperties.isEnabled() || !aiProperties.isResumeAdviceEnabled() || !llmClient.isEnabled()) {
            throw new ServiceException("AI 辅助需要 AI 模型支持，请管理员在后台配置 AI 模型后使用");
        }
        if (originalText == null || originalText.trim().isEmpty()) {
            throw new ServiceException("请先输入内容，AI 才能帮你优化");
        }
        String fieldLabel;
        switch (field == null ? "" : field) {
            case "work_description": fieldLabel = "工作经历描述"; break;
            case "project_description": fieldLabel = "项目经历描述"; break;
            case "self_intro": fieldLabel = "自我评价"; break;
            case "skills": fieldLabel = "专业技能清单"; break;
            default: throw new ServiceException("不支持的字段类型：" + field);
        }

        String systemPrompt = "你是一名资深简历优化专家，对简历中的「" + fieldLabel + "」给出3个不同风格的优化版本。"
                + "优化原则：STAR法则（情境-任务-行动-结果）、量化数据（无依据数据用[X%][X万]占位符供用户填写）、"
                + "突出与目标岗位相关的能力、专业商务表达避免口语化、每版50-150字。"
                + "三个版本风格差异化：版本1侧重成果量化（推荐），版本2侧重技术深度，版本3侧重业务价值。"
                + "保持与原文语义一致，禁止编造经历。"
                + "返回 JSON：{\"suggestions\":[{\"text\":\"优化后完整文本\",\"reason\":\"一句话优化理由\"}]}，恰好3条。"
                + "只输出 JSON 本体，禁止 markdown 代码块包裹，禁止前后说明文字。";
        StringBuilder userPrompt = new StringBuilder();
        userPrompt.append("【目标岗位】").append(position == null || position.isBlank() ? "未指定" : position).append('\n');
        if (skillNames != null && !skillNames.isEmpty()) {
            userPrompt.append("【用户技能】").append(String.join("、", skillNames)).append('\n');
        }
        userPrompt.append("【原始内容】\n").append(originalText);

        try {
            String response = llmClient.chat(systemPrompt, userPrompt.toString());
            JsonNode node = objectMapper.readTree(LlmJsonExtractor.extract(response));
            List<Map<String, String>> list = new ArrayList<>();
            JsonNode arr = node.path("suggestions");
            if (arr.isArray()) {
                for (JsonNode n : arr) {
                    String text = n.path("text").asText(null);
                    if (text != null && !text.isBlank()) {
                        Map<String, String> m = new HashMap<>();
                        m.put("text", text.trim());
                        m.put("reason", n.path("reason").asText(""));
                        list.add(m);
                    }
                }
            }
            if (list.isEmpty()) {
                throw new ServiceException("AI 未生成有效建议，请稍后重试");
            }
            return list;
        } catch (ServiceException e) {
            throw e;
        } catch (Exception e) {
            log.error("[FieldAssist] LLM 生成失败 field={}", field, e);
            throw new ServiceException("AI 辅助生成失败：" + e.getMessage());
        }
    }

    /**
     * AI 填充空字段（v10.22）：为空的工作经历/项目经历/自我介绍生成初始草稿
     *
     * <p>与 {@link #fieldAssist} 的区别：fieldAssist 是对已有内容生成 3 个优化版本；
     * 本方法是为空字段生成初始内容，供用户采纳填充到表单。</p>
     *
     * <p>流程：</p>
     * <ol>
     *   <li>查询简历详情，识别 works/projects/selfIntro 中为空的字段</li>
     *   <li>若无空字段，返回"无可填充字段"</li>
     *   <li>LLM 未启用/失败时返回空结果 + 提示信息（不抛异常，前端可正常渲染）</li>
     *   <li>构造 prompt：已有信息（name/skills/jobIntention/educations）+ 可选 JD（jobTargetId 查询），
     *       让 LLM 为空字段生成初始草稿</li>
     *   <li>返回 Map：works(List&lt;WorkItem&gt;)/projects(List&lt;ProjectItem&gt;)/selfIntro(String)/message</li>
     * </ol>
     *
     * @param resumeId    简历ID
     * @param userId      用户ID（权限校验）
     * @param jobTargetId 岗位目标ID（可选，提供时带入 JD 上下文）
     * @return 草稿结果 Map
     */
    public Map<String, Object> aiDraftEmptyFields(Long resumeId, Long userId, Long jobTargetId) {
        Map<String, Object> result = new HashMap<>();
        // 空字段占位，前端按 key 渲染
        result.put("works", new ArrayList<UserResumeVO.WorkItem>());
        result.put("projects", new ArrayList<UserResumeVO.ProjectItem>());
        result.put("selfIntro", "");

        // 1. 查询简历详情
        UserResumeVO resume = userResumeService.selectResumeDetail(resumeId, userId);
        if (resume == null) {
            result.put("message", "简历不存在或无权访问");
            return result;
        }

        // 2. 识别空字段
        boolean worksEmpty = resume.getWorks() == null || resume.getWorks().isEmpty();
        boolean projectsEmpty = resume.getProjects() == null || resume.getProjects().isEmpty();
        boolean selfIntroEmpty = resume.getSelfIntro() == null || resume.getSelfIntro().isBlank();
        if (!worksEmpty && !projectsEmpty && !selfIntroEmpty) {
            result.put("message", "无可填充字段：工作经历、项目经历、自我介绍均已填写");
            return result;
        }

        // 3. LLM 未启用/不可用时返回空结果 + 提示
        if (!aiProperties.isEnabled() || !aiProperties.isResumeAdviceEnabled() || !llmClient.isEnabled()) {
            result.put("message", "AI 填充需要 AI 模型支持，请管理员在后台配置 AI 模型后使用");
            return result;
        }

        // 4. 查询可选 JD（jobTargetId 非空时）
        String jdText = "";
        String jdPosition = "";
        if (jobTargetId != null) {
            PortalResumeJobTarget target = jobTargetMapper.selectById(jobTargetId);
            if (target != null && target.getUserId() != null && target.getUserId().equals(userId)) {
                jdText = target.getJdText() == null ? "" : target.getJdText();
                jdPosition = target.getPosition() == null ? "" : target.getPosition();
            }
        }

        // 需要生成的字段清单（仅空字段）
        List<String> needFields = new ArrayList<>();
        if (worksEmpty) needFields.add("工作经历");
        if (projectsEmpty) needFields.add("项目经历");
        if (selfIntroEmpty) needFields.add("自我介绍");

        String systemPrompt = "你是一名简历撰写专家。根据用户已有信息，为空缺的字段生成初始草稿。"
                + "生成原则：STAR 法则（情境-任务-行动-结果）、量化数据（无依据数据用 [X%][X万] 占位符供用户填写）、"
                + "突出与目标岗位相关的能力、专业商务表达避免口语化。"
                + "工作经历 2-3 条，每条描述 50-150 字；项目经历 2-3 条，每条描述 50-150 字；"
                + "自我介绍 100-200 字，突出技能和经验。"
                + "保持语义合理，禁止编造具体公司名（用 [公司名] 占位符）。"
                + "返回 JSON：{works:[{company,position,startDate,endDate,description}],"
                + "projects:[{name,role,startDate,endDate,description}],selfIntro:\"\"}。"
                + "仅生成空缺字段，已有字段不输出。"
                + "只输出 JSON 本体，禁止 markdown 代码块包裹，禁止前后说明文字。";

        StringBuilder userPrompt = new StringBuilder();
        userPrompt.append("【已有信息】\n");
        userPrompt.append("- 姓名：").append(safeDraft(resume.getName())).append('\n');
        // 技能
        if (resume.getSkills() != null && !resume.getSkills().isEmpty()) {
            StringBuilder skills = new StringBuilder();
            for (int i = 0; i < resume.getSkills().size(); i++) {
                UserResumeVO.SkillItem s = resume.getSkills().get(i);
                if (i > 0) skills.append("、");
                skills.append(safeDraft(s.getName()));
                if (s.getLevel() != null && !s.getLevel().isBlank()) {
                    skills.append("(").append(s.getLevel()).append(")");
                }
            }
            userPrompt.append("- 技能：").append(skills).append('\n');
        }
        // 求职意向
        if (resume.getJobIntention() != null) {
            UserResumeVO.JobIntention ji = resume.getJobIntention();
            userPrompt.append("- 求职意向：岗位=").append(safeDraft(ji.getPosition()))
                    .append(", 城市=").append(safeDraft(ji.getCity()))
                    .append(", 类型=").append(safeDraft(ji.getJobType()))
                    .append('\n');
        }
        // 教育经历
        if (resume.getEducations() != null && !resume.getEducations().isEmpty()) {
            userPrompt.append("- 教育经历：");
            for (int i = 0; i < resume.getEducations().size(); i++) {
                UserResumeVO.EducationItem e = resume.getEducations().get(i);
                if (i > 0) userPrompt.append("；");
                userPrompt.append(safeDraft(e.getSchool())).append("·")
                        .append(safeDraft(e.getMajor())).append("·")
                        .append(safeDraft(e.getDegree()));
            }
            userPrompt.append('\n');
        }
        // 目标岗位 JD（如有）
        if (!jdText.isBlank()) {
            userPrompt.append("\n【目标岗位】").append(jdPosition).append('\n');
            userPrompt.append("【岗位JD】\n").append(jdText).append('\n');
        }
        userPrompt.append("\n【需要生成的字段（仅生成空缺的）】\n");
        for (int i = 0; i < needFields.size(); i++) {
            userPrompt.append(i + 1).append(". ").append(needFields.get(i)).append('\n');
        }

        try {
            String response = llmClient.chat(systemPrompt, userPrompt.toString());
            if (response == null || response.isBlank()) {
                result.put("message", "AI 未生成有效草稿，请稍后重试");
                return result;
            }
            JsonNode node = objectMapper.readTree(LlmJsonExtractor.extract(response));

            int draftCount = 0;
            // 解析工作经历
            if (worksEmpty) {
                JsonNode worksArr = node.path("works");
                if (worksArr.isArray()) {
                    List<UserResumeVO.WorkItem> works = new ArrayList<>();
                    for (JsonNode wNode : worksArr) {
                        UserResumeVO.WorkItem w = new UserResumeVO.WorkItem();
                        w.setCompany(wNode.path("company").asText(""));
                        w.setPosition(wNode.path("position").asText(""));
                        w.setStartDate(wNode.path("startDate").asText(""));
                        w.setEndDate(wNode.path("endDate").asText(""));
                        w.setDescription(wNode.path("description").asText(""));
                        if (!w.getDescription().isBlank()) {
                            works.add(w);
                        }
                    }
                    result.put("works", works);
                    draftCount += works.size();
                }
            }
            // 解析项目经历
            if (projectsEmpty) {
                JsonNode projectsArr = node.path("projects");
                if (projectsArr.isArray()) {
                    List<UserResumeVO.ProjectItem> projects = new ArrayList<>();
                    for (JsonNode pNode : projectsArr) {
                        UserResumeVO.ProjectItem p = new UserResumeVO.ProjectItem();
                        p.setName(pNode.path("name").asText(""));
                        p.setRole(pNode.path("role").asText(""));
                        p.setStartDate(pNode.path("startDate").asText(""));
                        p.setEndDate(pNode.path("endDate").asText(""));
                        p.setDescription(pNode.path("description").asText(""));
                        if (!p.getDescription().isBlank()) {
                            projects.add(p);
                        }
                    }
                    result.put("projects", projects);
                    draftCount += projects.size();
                }
            }
            // 解析自我介绍
            if (selfIntroEmpty) {
                String selfIntro = node.path("selfIntro").asText("");
                if (!selfIntro.isBlank()) {
                    result.put("selfIntro", selfIntro.trim());
                    draftCount += 1;
                }
            }

            if (draftCount == 0) {
                result.put("message", "AI 未生成有效草稿，请稍后重试");
            } else {
                result.put("message", "已生成 " + draftCount + " 项草稿，点击采纳后填充到表单");
            }
            return result;
        } catch (Exception e) {
            log.error("[AiDraftEmptyFields] LLM 生成失败 resumeId={}", resumeId, e);
            result.put("message", "AI 草稿生成失败：" + e.getMessage());
            return result;
        }
    }

    /** 空值安全处理：null/空字符串统一显示为"未填写"（aiDraftEmptyFields 专用，避免与 Generator 同名方法混淆） */
    private String safeDraft(String s) {
        return (s == null || s.isBlank()) ? "未填写" : s.trim();
    }

    /**
     * 生成深度优化建议（逐项前后对比）
     *
     * <p>v10.19：实现已迁到 {@link ResumeDeepOptimizeGenerator}，本方法为门面委托。
     * 保留公共 API 不变，避免影响 {@code PortalResumeOptimizeController} 等调用方。</p>
     *
     * @param resume      简历详情
     * @param jobTargetId 岗位目标ID
     */
    public ResumeDeepOptimizeVO generate(UserResumeVO resume, Long jobTargetId) {
        return generator.generate(resume, jobTargetId);
    }

    // ==================== v10.19：异步任务化（解决大模型调用超时） ====================

    /**
     * 提交深度优化异步任务：同步入库返回任务ID，调用方立即响应前端。
     *
     * <p>不阻塞等待 LLM 结果，前端通过 {@link #getTaskStatus(Long, Long)} 轮询任务进度。
     * 异步执行由独立 Bean {@link ResumeOptimizeAsyncExecutor} 承担，保证 @Async 通过 Spring 代理生效。</p>
     *
     * @param userId     用户ID
     * @param resume     简历详情
     * @param jobTargetId 岗位目标ID
     * @return 任务ID
     */
    public Long submitTask(Long userId, UserResumeVO resume, Long jobTargetId) {
        // v10.19：前置校验委托给 generator（避免重复实现 AI 可用性与岗位校验逻辑）
        if (generator.getJobTarget(jobTargetId) == null) {
            throw new ServiceException("岗位目标不存在");
        }
        if (!generator.isAiAvailable()) {
            throw new ServiceException("深度优化需要 AI 模型支持，请管理员在后台配置 AI 模型后使用");
        }

        // 同步入库任务记录（pending），调用方立即返回
        PortalResumeOptimizeTask task = new PortalResumeOptimizeTask();
        task.setUserId(userId);
        task.setResumeId(resume.getId());
        task.setJobTargetId(jobTargetId);
        task.setStatus("pending");
        task.setAiPowered(1);
        task.setCreateTime(LocalDateTime.now());
        optimizeTaskMapper.insert(task);

        // 触发异步执行（独立 Bean 调用，确保 @Async 生效）
        try {
            asyncExecutor.executeTask(task.getId(), resume, jobTargetId);
        } catch (Exception e) {
            // 异步触发失败（如线程池满）回写失败状态，不阻塞调用方
            log.error("[DeepOptimizeTask] 异步任务触发失败 taskId={}", task.getId(), e);
            PortalResumeOptimizeTask fail = new PortalResumeOptimizeTask();
            fail.setId(task.getId());
            fail.setStatus("failed");
            fail.setErrorMsg("任务触发失败：" + e.getMessage());
            fail.setFinishTime(LocalDateTime.now());
            optimizeTaskMapper.updateById(fail);
        }
        return task.getId();
    }

    /**
     * 查询任务状态（前端轮询调用）
     *
     * @param taskId 任务ID
     * @param userId 用户ID（权限校验，防止越权查询他人任务）
     * @return 任务状态 VO（含进度、结果、错误信息）
     */
    public ResumeOptimizeTaskVO getTaskStatus(Long taskId, Long userId) {
        PortalResumeOptimizeTask task = optimizeTaskMapper.selectById(taskId);
        if (task == null || !task.getUserId().equals(userId)) {
            throw new ServiceException("任务不存在或无权访问");
        }
        ResumeOptimizeTaskVO vo = new ResumeOptimizeTaskVO();
        vo.setTaskId(task.getId());
        vo.setStatus(task.getStatus());
        vo.setResult(task.getResultJson());
        vo.setErrorMsg(task.getErrorMsg());
        // 粗粒度进度估算
        switch (task.getStatus() == null ? "" : task.getStatus()) {
            case "pending":  vo.setProgress(10); break;
            case "running":   vo.setProgress(50); break;
            case "success":   vo.setProgress(100); break;
            case "failed":    vo.setProgress(0); break;
            default:          vo.setProgress(0);
        }
        return vo;
    }

    /**
     * 应用选中的优化建议到简历并直接保存更新（无版本概念，幂等），记录优化历史
     *
     * @param userId   用户ID
     * @param resume   优化前简历详情
     * @param optimize 完整优化结果
     * @param adopted  用户采纳的建议索引列表（对应 optimize.items 下标）
     * @param saver    保存回调（由 Controller 注入，复用现有保存逻辑）
     * @return 简历ID
     */
    public Long applyAndSave(Long userId, UserResumeVO resume, ResumeDeepOptimizeVO optimize,
                             List<Integer> adopted, java.util.function.BiFunction<UserResumeVO, Long, Long> saver) {
        if (adopted == null || adopted.isEmpty()) {
            throw new ServiceException("请至少选择一项优化建议");
        }
        List<ResumeDeepOptimizeVO.OptimizeItem> items = optimize.getItems();
        log.info("[DeepOptimize] applyAndSave 开始：resumeId={} 简历含 works={}/projects={}/educations={}，采纳索引={}",
                resume.getId(),
                resume.getWorks() == null ? 0 : resume.getWorks().size(),
                resume.getProjects() == null ? 0 : resume.getProjects().size(),
                resume.getEducations() == null ? 0 : resume.getEducations().size(),
                adopted);
        for (Integer idx : adopted) {
            if (idx == null || idx < 0 || idx >= items.size()) {
                throw new ServiceException("优化建议索引非法：" + idx);
            }
            ResumeDeepOptimizeVO.OptimizeItem item = items.get(idx);
            log.info("[DeepOptimize] 应用建议[{}]：section={}, index={}, field={}",
                    idx, item.getSection(), item.getIndex(), item.getField());
            applyItem(resume, item);
        }

        // 无版本概念：直接更新原简历（幂等，saveResume 更新路径会自动清除陈旧评分）
        Integer oldScore = resume.getScore();
        resume.setScore(null);
        resume.setScoreDetail(null);
        // 优化前最近一次 JD 匹配分（若存在），用于历史对比展示
        Integer matchScoreBefore = null;
        try {
            PortalResumeJobMatch lastMatch = jobMatchMapper.selectOne(
                    new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<PortalResumeJobMatch>()
                            .eq(PortalResumeJobMatch::getUserId, userId)
                            .eq(PortalResumeJobMatch::getResumeId, optimize.getResumeId())
                            .orderByDesc(PortalResumeJobMatch::getId)
                            .last("LIMIT 1"));
            if (lastMatch != null) {
                matchScoreBefore = lastMatch.getMatchScore();
            }
        } catch (Exception e) {
            log.warn("[DeepOptimize] 查询优化前匹配分失败（忽略）：{}", e.getMessage());
        }
        Long newResumeId = saver.apply(resume, userId);

        // 优化保存成功后立即重评：规则评分零 LLM 成本，同步完成评分闭环
        Integer newScore = null;
        try {
            UserResumeVO rescored = userResumeService.scoreResume(newResumeId, userId);
            if (rescored != null) {
                newScore = rescored.getScore();
            }
        } catch (Exception e) {
            log.warn("[DeepOptimize] 优化后重评失败（不影响保存结果）：{}", e.getMessage());
        }

        // 记录优化历史（含全部建议快照与采纳状态）
        try {
            PortalResumeOptimizeHistory history = new PortalResumeOptimizeHistory();
            history.setUserId(userId);
            history.setResumeId(newResumeId);
            history.setFromResumeId(optimize.getResumeId());
            history.setJobTargetId(optimize.getJobTargetId());
            history.setScoreBefore(oldScore);
            history.setScoreAfter(newScore);
            history.setMatchScoreBefore(matchScoreBefore);
            // matchScoreAfter 不自动填充：JD 匹配依赖 LLM，成本高，留待用户主动重新匹配
            history.setAdoptedCount(adopted.size());
            history.setTotalCount(items.size());
            history.setOptimizeData(buildHistoryData(optimize, adopted));
            optimizeHistoryMapper.insert(history);
        } catch (Exception e) {
            log.warn("[DeepOptimize] 优化历史记录失败（不影响保存结果）：{}", e.getMessage());
        }
        return newResumeId;
    }

    /** 将单条建议应用到简历字段 */
    private void applyItem(UserResumeVO resume, ResumeDeepOptimizeVO.OptimizeItem item) {
        String text = item.getOptimized().trim();
        int idx = item.getIndex() == null ? 0 : item.getIndex();
        // v10.20：section 归一化（trim+lowercase，兼容 LLM 返回 works/projects/experience 等变体）
        String section = normalizeSection(item.getSection());
        String field = item.getField() == null ? "" : item.getField().trim();
        switch (section) {
            case "selfIntro":
                resume.setSelfIntro(text);
                break;
            case "objective":
                if (resume.getJobIntention() != null && "position".equals(field)) {
                    resume.getJobIntention().setPosition(text);
                }
                break;
            case "education":
                if (resume.getEducations() == null || idx >= resume.getEducations().size()) {
                    throw new ServiceException("教育经历索引越界：" + idx + "（简历只有 "
                            + (resume.getEducations() == null ? 0 : resume.getEducations().size()) + " 条）");
                }
                UserResumeVO.EducationItem e = resume.getEducations().get(idx);
                if ("description".equals(field)) {
                    e.setDescription(text);
                }
                break;
            case "work":
                if (resume.getWorks() == null || idx >= resume.getWorks().size()) {
                    throw new ServiceException("工作经历索引越界：" + idx + "（简历只有 "
                            + (resume.getWorks() == null ? 0 : resume.getWorks().size()) + " 条）");
                }
                UserResumeVO.WorkItem w = resume.getWorks().get(idx);
                if ("description".equals(field)) {
                    w.setDescription(text);
                }
                break;
            case "project":
                if (resume.getProjects() == null || idx >= resume.getProjects().size()) {
                    throw new ServiceException("项目经历索引越界：" + idx + "（简历只有 "
                            + (resume.getProjects() == null ? 0 : resume.getProjects().size()) + " 条）");
                }
                UserResumeVO.ProjectItem p = resume.getProjects().get(idx);
                if ("description".equals(field)) {
                    p.setDescription(text);
                }
                break;
            case "skills":
                applySkills(resume, text);
                break;
            default:
                // 未知 section 抛错（而非静默忽略），让用户知道 LLM 返回异常可重试
                throw new ServiceException("无法识别的优化字段类型：" + item.getSection()
                        + "（建议重新生成，AI 返回了未预期的字段标识）");
        }
    }

    /**
     * v10.20：section 归一化，兼容 LLM 返回的常见变体
     * <ul>
     *   <li>去前后空白 + 转小写</li>
     *   <li>works→work、projects→project、educations→education、experiences→experience→work</li>
     *   <li>self_intro→selfIntro、selfIntroduction→selfIntro、intro→selfIntro</li>
     *   <li>objective→objective、jobIntention→objective</li>
     * </ul>
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

    /** skills 建议为"精通：A、B\n熟练：C"分级文本：解析合并（去重） */
    private void applySkills(UserResumeVO resume, String text) {
        if (resume.getSkills() == null) {
            resume.setSkills(new ArrayList<>());
        }
        List<UserResumeVO.SkillItem> existing = resume.getSkills();
        for (String rawLine : text.split("\n")) {
            String line = rawLine.trim();
            if (line.isEmpty()) {
                continue;
            }
            String level = "熟练";
            String namesPart = line;
            java.util.regex.Matcher m = java.util.regex.Pattern
                    .compile("^(精通|熟练|了解|一般)[:：]\\s*(.+)$").matcher(line);
            if (m.find()) {
                level = m.group(1);
                namesPart = m.group(2);
            }
            for (String raw : namesPart.split("[、,，;；]")) {
                String name = raw.trim().replaceAll("\\(.*?\\)|（.*?）", "").trim();
                if (name.isEmpty() || name.length() > 30 || name.startsWith("[")) {
                    continue;
                }
                boolean exists = existing.stream()
                        .anyMatch(s -> name.equals(s.getName()));
                if (!exists) {
                    UserResumeVO.SkillItem s = new UserResumeVO.SkillItem();
                    s.setName(name);
                    s.setLevel(level);
                    existing.add(s);
                }
            }
        }
    }

    /** 构建优化历史快照：全部建议 + adopted 标记 */
    private JsonNode buildHistoryData(ResumeDeepOptimizeVO optimize, List<Integer> adopted) {
        ObjectNode root = objectMapper.createObjectNode();
        root.put("summary", optimize.getSummary());
        ArrayNode arr = objectMapper.createArrayNode();
        for (int i = 0; i < optimize.getItems().size(); i++) {
            ResumeDeepOptimizeVO.OptimizeItem item = optimize.getItems().get(i);
            ObjectNode n = arr.addObject();
            n.put("section", item.getSection());
            n.put("index", item.getIndex());
            n.put("field", item.getField());
            n.put("original", item.getOriginal());
            n.put("optimized", item.getOptimized());
            n.put("reason", item.getReason());
            n.put("status", adopted.contains(i) ? "adopted" : "skipped");
        }
        root.set("items", arr);
        return root;
    }

    /** 查询简历的优化历史列表 */
    public List<PortalResumeOptimizeHistory> listHistory(Long userId, Long resumeId) {
        return optimizeHistoryMapper.selectList(
                new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<PortalResumeOptimizeHistory>()
                        .eq(PortalResumeOptimizeHistory::getUserId, userId)
                        .eq(PortalResumeOptimizeHistory::getResumeId, resumeId)
                        .orderByDesc(PortalResumeOptimizeHistory::getId));
    }
}
