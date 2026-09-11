package com.moyun.ext.ai2.handler.impl;

import com.moyun.ext.ai.entity.AiSceneConfig;
import com.moyun.ext.ai2.constant.AiErrorCodes;
import com.moyun.ext.ai2.handler.AbstractAiSceneHandler;
import com.moyun.ext.ai2.model.AiExecuteRequest;
import com.moyun.ext.ai2.model.AiExecuteResponse;
import com.moyun.ext.ai2.model.data.ResumeSceneData;
import com.moyun.ext.ai2.support.PromptInjectionGuard;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

/**
 * 简历优化场景Handler（scene = resume_optimize）
 *
 * <p><strong>v11.58 P0-3 业务收口——双子任务契约：</strong></p>
 * <ul>
 *   <li><b>子任务模式</b>：input 传 {@code task} + {@code context}。系统提示词按子任务路由
 *       （advice/job_match/field_assist/draft_empty/deep_optimize，均从原业务服务逐字收编，
 *       保证切换前后 LLM 行为一致）；业务数据经 {@link PromptInjectionGuard#wrapData} 隔离，
 *       结果原样置于 {@link ResumeSceneData#getStructured()} 由业务映射 VO。</li>
 *   <li><b>通用模式</b>（task 缺省，管理台场景调试/开放入口）：input 传 resumeText/targetPosition，
 *       输出评分/建议/关键词/优化片段。</li>
 * </ul>
 *
 * @author laomao
 * @since 2026-09-09
 */
@Slf4j
@Component("resumeOptimizeHandler")
public class ResumeOptimizeHandler extends AbstractAiSceneHandler {

    @Override
    public String getSceneCode() {
        return "resume_optimize";
    }

    @Override
    public void validate(AiExecuteRequest request) {
        String task = getInputString(request, "task");
        if (task == null || task.isBlank()) {
            // 通用模式：简历内容必填
            requireInputString(request, "resumeText");
            return;
        }
        if (subTaskSystemPrompt(task) == null) {
            throw new IllegalArgumentException("不支持的子任务: " + task);
        }
        requireInputString(request, "context");
    }

    @Override
    public AiExecuteResponse<?> execute(AiExecuteRequest request, AiSceneConfig config) {
        String task = getInputString(request, "task");
        if (task != null && !task.isBlank()) {
            return executeSubTask(request, task);
        }
        return executeGeneric(request, config);
    }

    // ==================== 子任务模式（v11.58 业务收口） ====================

    private AiExecuteResponse<?> executeSubTask(AiExecuteRequest request, String task) {
        String context = requireInputString(request, "context");
        String systemPrompt = subTaskSystemPrompt(task);

        // 数据通道隔离：业务上下文（简历/JD/评分明细）为不可信数据，分隔符包裹防注入
        String raw = chat(getSceneCode(), systemPrompt,
                PromptInjectionGuard.wrapData("业务数据", context));
        if (raw == null || raw.isBlank()) {
            return AiExecuteResponse.failure(AiErrorCodes.AI_CALL_FAILED, "AI服务暂不可用");
        }

        Map<String, Object> parsed = parseJsonMap(raw);
        if (parsed == null) {
            return AiExecuteResponse.failure(AiErrorCodes.AI_PARSE_ERROR, "简历优化结果解析失败");
        }

        ResumeSceneData data = new ResumeSceneData();
        // 子任务结果原样透传（VO 映射留在业务层，各子任务 Schema 不同）
        data.setStructured(parsed);
        return AiExecuteResponse.success(data);
    }

    /**
     * 子任务 → 系统提示词（从业务服务逐字收编，v11.58）
     *
     * @return 未知子任务返回 null（validate 已前置拦截，此处防御性返回）
     */
    private String subTaskSystemPrompt(String task) {
        return switch (task) {
            // ResumeAiAdviceService.generateAdviceWithLlm：评分明细 → 改进建议
            case "advice" -> "你是一名资深 HR 与简历顾问，擅长基于评分明细给出可执行的改进建议。"
                    + "请返回 JSON 格式，字段：summary(整体总结), advices(数组，每项含 dimension/priority(high/medium/low)/content/type(fill/refine/match)/optimized), missingSkills(字符串数组)。"
                    + "content 为该维度的改进思路说明；optimized 为优化后的完整可用文本（可直接替换简历对应模块内容），"
                    + "必须基于用户简历现有信息改写而非凭空编造，量化数据无依据时可使用占位符如 [X%] 供用户填写；"
                    + "dimension 取值限定：基本信息/求职意向/教育经历/工作经历/项目经历/技能列表/自我介绍/岗位匹配度。"
                    + "建议要具体、可执行，优先关注得分率低于60%的维度与岗位匹配度缺失技能。"
                    + "只输出 JSON 本体，禁止使用 markdown 代码块（```）包裹，禁止在 JSON 前后添加任何说明文字。";
            // ResumeJobMatchService.analyzeByLlm：JD × 简历 → 匹配报告
            case "job_match" -> "你是一名资深技术招聘官，负责评估候选人与岗位的匹配度。"
                    + "请基于目标岗位JD和候选人简历，返回 JSON："
                    + "matchScore(0-100综合匹配度), grade(excellent/good/medium/poor), "
                    + "matchedKeywords(数组,简历已覆盖的JD核心要求关键词), missingKeywords(数组,简历缺失的JD核心要求关键词), "
                    + "dimensions(对象,含四个维度，每维 score 0-100 与 suggestions 数组: "
                    + "keywordMatch关键词匹配/experienceMatch经验匹配/skillMatch技能匹配/structureMatch结构完整度), "
                    + "summary(2-3句总体评价与改进方向)。"
                    + "评估要客观，基于简历真实内容，缺失项如实指出；关键词控制在20个以内。"
                    + "只输出 JSON 本体，禁止使用 markdown 代码块（```）包裹，禁止在 JSON 前后添加任何说明文字。";
            // ResumeDeepOptimizeService.fieldAssist：字段级 3 版本优化
            case "field_assist" -> "你是一名资深简历优化专家，对简历中的指定字段给出3个不同风格的优化版本。"
                    + "优化原则：STAR法则（情境-任务-行动-结果）、量化数据（无依据数据用[X%][X万]占位符供用户填写）、"
                    + "突出与目标岗位相关的能力、专业商务表达避免口语化、每版50-150字。"
                    + "三个版本风格差异化：版本1侧重成果量化（推荐），版本2侧重技术深度，版本3侧重业务价值。"
                    + "保持与原文语义一致，禁止编造经历。"
                    + "返回 JSON：{\"suggestions\":[{\"text\":\"优化后完整文本\",\"reason\":\"一句话优化理由\"}]}，恰好3条。"
                    + "只输出 JSON 本体，禁止 markdown 代码块包裹，禁止前后说明文字。";
            // ResumeDeepOptimizeService.aiDraftEmptyFields：空字段初始草稿
            case "draft_empty" -> "你是一名简历撰写专家。根据用户已有信息，为空缺的字段生成初始草稿。"
                    + "生成原则：STAR 法则（情境-任务-行动-结果）、量化数据（无依据数据用 [X%][X万] 占位符供用户填写）、"
                    + "突出与目标岗位相关的能力、专业商务表达避免口语化。"
                    + "工作经历 2-3 条，每条描述 50-150 字；项目经历 2-3 条，每条描述 50-150 字；"
                    + "自我介绍 100-200 字，突出技能和经验。"
                    + "保持语义合理，禁止编造具体公司名（用 [公司名] 占位符）。"
                    + "返回 JSON：{works:[{company,position,startDate,endDate,description}],"
                    + "projects:[{name,role,startDate,endDate,description}],selfIntro:\"\"}。"
                    + "仅生成空缺字段，已有字段不输出。"
                    + "只输出 JSON 本体，禁止 markdown 代码块包裹，禁止前后说明文字。";
            // ResumeDeepOptimizeGenerator.generate：整份简历逐项深度优化
            case "deep_optimize" -> "你是一名资深简历优化专家，基于目标岗位JD对简历进行逐项深度优化。"
                    + "返回 JSON：summary(总体优化说明，50字内), items(优化建议数组，3-6项)。每项含："
                    + "section(必为以下枚举之一：objective/education/work/project/skills/selfIntro；"
                    + "严禁使用复数如works/projects，严禁使用experience/introduction 等同义词，必须完全匹配枚举值), "
                    + "index(列表条目索引，从0开始；skills 填 0), field(position/description/name), "
                    + "optimized(优化后完整文本，可直接替换，50-200字), reason(优化理由，一句话，30字内)。"
                    + "不要输出 original 字段（原文由系统回填）。"
                    + "优化原则：STAR法则+量化数据+[X%]占位符（无依据数据用占位符供用户填写）；"
                    + "skills 的 optimized 用\"精通：A、B\\n熟练：C\"格式；"
                    + "保持语义一致禁止编造经历。只输出 JSON 本体，禁止 markdown 代码块包裹，"
                    + "输出务必完整，禁止中途截断。";
            default -> null;
        };
    }

    // ==================== 通用模式（管理台场景调试/开放入口） ====================

    private AiExecuteResponse<?> executeGeneric(AiExecuteRequest request, AiSceneConfig config) {
        String resumeText = requireInputString(request, "resumeText");
        String targetPosition = getInputString(request, "targetPosition");

        String systemPrompt = """
                你是资深简历优化顾问。结合目标岗位评估简历并给出优化建议，只输出 JSON：
                {"score": 0到100整数（岗位匹配度）,
                 "suggestions": ["具体可执行的优化建议，按重要性排序，3-6条"],
                 "keywords": ["建议补充的关键词"],
                 "optimizedText": "优化后的核心内容片段（可选，重点段落改写）"}
                建议要具体到 STAR 法则、量化成果、技能匹配。禁止输出 JSON 以外内容。""";

        StringBuilder user = new StringBuilder("简历内容：\n").append(resumeText);
        if (targetPosition != null && !targetPosition.isBlank()) {
            user.append("\n\n目标岗位：").append(targetPosition);
        }

        String raw = chat(getSceneCode(), systemPrompt,
                PromptInjectionGuard.wrapData("简历内容", user.toString()));
        if (raw == null || raw.isBlank()) {
            return AiExecuteResponse.failure(AiErrorCodes.AI_CALL_FAILED, "AI服务暂不可用");
        }

        Map<String, Object> parsed = parseOutput(raw, config);
        if (parsed == null) {
            return AiExecuteResponse.failure(AiErrorCodes.AI_PARSE_ERROR, "简历优化结果解析失败");
        }

        ResumeSceneData data = new ResumeSceneData();
        data.setOriginalText(resumeText);
        if (parsed.get("score") instanceof Number number) {
            data.setScore(number.intValue());
        }
        if (parsed.get("suggestions") instanceof List<?> suggestions) {
            data.setSuggestions(suggestions.stream().map(String::valueOf).toList());
        }
        if (parsed.get("keywords") instanceof List<?> keywords) {
            data.setKeywords(keywords.stream().map(String::valueOf).toList());
        }
        if (parsed.get("optimizedText") instanceof String optimized) {
            data.setOptimizedText(optimized);
        }
        return AiExecuteResponse.success(data);
    }
}
