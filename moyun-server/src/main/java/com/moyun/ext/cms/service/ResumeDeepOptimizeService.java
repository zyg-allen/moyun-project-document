package com.moyun.ext.cms.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.moyun.ext.cms.config.AiProperties;
import com.moyun.ext.cms.domain.vo.ResumeDeepOptimizeVO;
import com.moyun.ext.cms.domain.vo.UserResumeVO;
import com.moyun.portal.domain.entity.PortalResumeJobTarget;
import com.moyun.portal.domain.entity.PortalResumeOptimizeHistory;
import com.moyun.portal.mapper.PortalResumeJobTargetMapper;
import com.moyun.portal.mapper.PortalResumeOptimizeHistoryMapper;
import com.moyun.common.exception.system.ServiceException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

/**
 * 简历深度优化服务（v10.13 简历优化重构）
 * <p>
 * 链路：LLM 基于 JD 对简历逐项生成优化建议（前后对比）→ 用户逐项/批量采纳 →
 * 应用到简历表单 → 保存为新版本（版本号+1）→ 记录优化历史（评分/匹配度前后对比）。
 * </p>
 * <p>依赖 AI 模型，未启用/调用失败时抛出明确提示（深度优化无规则兜底——规则无法改写文本）。</p>
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
     * 生成深度优化建议（逐项前后对比）
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

        String systemPrompt = "你是一名资深简历优化专家，基于目标岗位JD对简历进行逐项深度优化。"
                + "返回 JSON：summary(总体优化说明，50字内), items(优化建议数组，3-6项)。每项含："
                + "section(objective/education/work/project/skills/selfIntro), "
                + "index(列表条目索引，从0开始；skills 填 0), field(position/description/name), "
                + "optimized(优化后完整文本，可直接替换，50-200字), reason(优化理由，一句话，30字内)。"
                + "不要输出 original 字段（原文由系统回填）。"
                + "优化原则：STAR法则+量化数据+[X%]占位符（无依据数据用占位符供用户填写）；"
                + "skills 的 optimized 用\"精通：A、B\\n熟练：C\"格式；"
                + "保持语义一致禁止编造经历。只输出 JSON 本体，禁止 markdown 代码块包裹，"
                + "输出务必完整，禁止中途截断。";
        String userPrompt = "【目标岗位】" + target.getPosition()
                + "\n【岗位JD】\n" + target.getJdText()
                + "\n\n【简历 JSON】\n" + objectMapper.valueToTree(resume).toString();

        try {
            String response = llmClient.chat(systemPrompt, userPrompt);
            JsonNode node = objectMapper.readTree(LlmJsonExtractor.extract(response));
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
            return vo;
        } catch (ServiceException e) {
            throw e;
        } catch (com.fasterxml.jackson.core.JsonProcessingException e) {
            log.error("[DeepOptimize] LLM 返回 JSON 解析失败（疑似输出被截断）", e);
            throw new ServiceException("AI 输出被截断或格式异常，请管理员在后台「AI 模块 → 模型配置」调大最大 Token 数后重试");
        } catch (Exception e) {
            log.error("[DeepOptimize] LLM 生成失败", e);
            throw new ServiceException("深度优化生成失败：" + e.getMessage());
        }
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
        for (Integer idx : adopted) {
            if (idx == null || idx < 0 || idx >= items.size()) {
                throw new ServiceException("优化建议索引非法：" + idx);
            }
            applyItem(resume, items.get(idx));
        }

        // 无版本概念：直接更新原简历（幂等，saveResume 更新路径会自动清除陈旧评分）
        Integer oldScore = resume.getScore();
        resume.setScore(null);
        resume.setScoreDetail(null);
        Long newResumeId = saver.apply(resume, userId);

        // 记录优化历史（含全部建议快照与采纳状态）
        try {
            PortalResumeOptimizeHistory history = new PortalResumeOptimizeHistory();
            history.setUserId(userId);
            history.setResumeId(newResumeId);
            history.setFromResumeId(optimize.getResumeId());
            history.setJobTargetId(optimize.getJobTargetId());
            history.setScoreBefore(oldScore);
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
        switch (item.getSection() == null ? "" : item.getSection()) {
            case "selfIntro":
                resume.setSelfIntro(text);
                break;
            case "objective":
                if (resume.getJobIntention() != null && "position".equals(item.getField())) {
                    resume.getJobIntention().setPosition(text);
                }
                break;
            case "education":
                if (resume.getEducations() != null && idx < resume.getEducations().size()) {
                    UserResumeVO.EducationItem e = resume.getEducations().get(idx);
                    if ("description".equals(item.getField())) {
                        e.setDescription(text);
                    }
                }
                break;
            case "work":
                if (resume.getWorks() != null && idx < resume.getWorks().size()) {
                    UserResumeVO.WorkItem w = resume.getWorks().get(idx);
                    if ("description".equals(item.getField())) {
                        w.setDescription(text);
                    }
                }
                break;
            case "project":
                if (resume.getProjects() != null && idx < resume.getProjects().size()) {
                    UserResumeVO.ProjectItem p = resume.getProjects().get(idx);
                    if ("description".equals(item.getField())) {
                        p.setDescription(text);
                    }
                }
                break;
            case "skills":
                applySkills(resume, text);
                break;
            default:
                // 未知 section 忽略（防御 LLM 幻觉）
        }
    }

    /** 服务端回填原文（v10.14：LLM 不再回显 original，避免输出 token 超限被截断） */
    private void fillOriginal(UserResumeVO resume, ResumeDeepOptimizeVO.OptimizeItem item) {
        int idx = item.getIndex() == null ? 0 : item.getIndex();
        String original = null;
        switch (item.getSection() == null ? "" : item.getSection()) {
            case "selfIntro":
                original = resume.getSelfIntro();
                break;
            case "objective":
                if (resume.getJobIntention() != null && "position".equals(item.getField())) {
                    original = resume.getJobIntention().getPosition();
                }
                break;
            case "education":
                if (resume.getEducations() != null && idx < resume.getEducations().size()
                        && "description".equals(item.getField())) {
                    original = resume.getEducations().get(idx).getDescription();
                }
                break;
            case "work":
                if (resume.getWorks() != null && idx < resume.getWorks().size()
                        && "description".equals(item.getField())) {
                    original = resume.getWorks().get(idx).getDescription();
                }
                break;
            case "project":
                if (resume.getProjects() != null && idx < resume.getProjects().size()
                        && "description".equals(item.getField())) {
                    original = resume.getProjects().get(idx).getDescription();
                }
                break;
            case "skills":
                if (resume.getSkills() != null && !resume.getSkills().isEmpty()) {
                    StringBuilder sb = new StringBuilder();
                    for (UserResumeVO.SkillItem s : resume.getSkills()) {
                        if (sb.length() > 0) {
                            sb.append('、');
                        }
                        sb.append(s.getName());
                    }
                    original = sb.toString();
                }
                break;
            default:
                // 未知 section：original 留空（防御 LLM 幻觉）
        }
        item.setOriginal(original);
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
