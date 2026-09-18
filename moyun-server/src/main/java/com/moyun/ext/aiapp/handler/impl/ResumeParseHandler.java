package com.moyun.ext.aiapp.handler.impl;

import com.moyun.ext.ai.entity.AiSceneConfig;
import com.moyun.ext.aiapp.constant.AiErrorCodes;
import com.moyun.ext.aiapp.handler.AbstractAiSceneHandler;
import com.moyun.ext.aiapp.model.AiExecuteRequest;
import com.moyun.ext.aiapp.model.AiExecuteResponse;
import com.moyun.ext.aiapp.model.data.ResumeSceneData;
import com.moyun.ext.aiapp.support.PromptInjectionGuard;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Map;
import com.moyun.ext.ai.enums.AiSceneEnum;

/**
 * 简历解析场景Handler（scene = resume_parse）
 *
 * <p>职责：简历文本 → 结构化 JSON（字段语义对齐在线简历表单的完整 Schema）。</p>
 *
 * <p>业务收口：提示词与 {@code ResumeParseService.parseByLlm} 原文完全一致
 * （name/gender/birthDate/jobIntention/educations/works/projects/skills/selfIntro 全字段），
 * 业务调用方从 {@link ResumeSceneData#getStructured()} 反序列化为 ResumeParseVO，
 * 保证切换网关前后解析行为零变化。</p>
 *
 * <p>输入参数：text(简历原文)</p>
 *
 * @author laomao
 * @since 2026-09-09
 */
@Slf4j
@Component("resumeParseHandler")
public class ResumeParseHandler extends AbstractAiSceneHandler {

    @Override
    public String getSceneCode() {
        return AiSceneEnum.RESUME_PARSE.getCode();
    }

    @Override
    public void validate(AiExecuteRequest request) {
        // 快速失败：简历原文缺失直接拒绝（解析场景唯一必填参数）
        requireInputString(request, "text");
    }

    @Override
    public AiExecuteResponse<?> execute(AiExecuteRequest request, AiSceneConfig config) {
        String text = requireInputString(request, "text");

        // 与 ResumeParseService.parseByLlm 原提示词逐字一致（全字段 Schema）
        String systemPrompt = "从简历原文抽取结构化JSON。字段：name,gender(男/女),birthDate(yyyy-MM-dd),"
                + "phone,email,title,jobIntention{position,city,salaryMin,salaryMax,jobType,availableTime},"
                + "educations[{school,major,degree,startDate(yyyy-MM),endDate(yyyy-MM),description}],"
                + "works[{company,position,startDate,endDate,description}],"
                + "projects[{name,role,startDate,endDate,description,url}],"
                + "skills[{name,level(精通/熟练/了解)}],selfIntro。"
                + "规则：只抽取原文存在的信息，缺失返回null或空数组，禁止编造。"
                + "只输出JSON本体，禁止markdown代码块。";

        // 数据通道隔离——简历原文为不可信数据，分隔符包裹防注入（数据内指令性文字不构成指令）
        String raw = chatJson(getSceneCode(), systemPrompt,
                PromptInjectionGuard.wrapData("简历原文", text));
        if (raw == null || raw.isBlank()) {
            return AiExecuteResponse.failure(AiErrorCodes.AI_CALL_FAILED, "AI服务暂不可用");
        }

        Map<String, Object> parsed = parseOutput(raw, config);
        if (parsed == null) {
            return AiExecuteResponse.failure(AiErrorCodes.AI_PARSE_ERROR, "简历解析结果格式错误");
        }

        ResumeSceneData data = new ResumeSceneData();
        // 完整解析结果置于 structured（业务侧 convertValue → ResumeParseVO）
        data.setStructured(parsed);
        return AiExecuteResponse.success(data);
    }
}
