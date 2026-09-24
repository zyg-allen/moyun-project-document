package com.moyun.ext.aigateway.service;

import com.moyun.ext.ai.entity.AiSceneConfig;
import com.moyun.ext.ai.exception.BusinessException;
import com.moyun.ext.ai.exception.ErrorCode;
import com.moyun.ext.aigateway.constant.AiErrorCodes;
import com.moyun.ext.aigateway.handler.AbstractAiSceneHandler;
import com.moyun.ext.aigateway.model.AiExecuteRequest;
import com.moyun.ext.aigateway.model.AiExecuteResponse;
import com.moyun.ext.aigateway.model.AiSceneMetadata;
import com.moyun.ext.aigateway.model.ChatOutcome;
import com.moyun.ext.aigateway.model.data.GenericSceneData;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * 默认场景执行器（AI统一网关整改 2A.3）
 *
 * <p>配置驱动执行：模板渲染 → LLM → output_parser 解析 → 兜底。全链路复用
 * {@link AbstractAiSceneHandler} 已验证的骨架方法（chatJson 解析失败重试 / mergePersona /
 * renderTemplate / parseOutput / buildMetadata）——迁移而非重写。</p>
 *
 * <p>路由规则（AiSceneRegistry）：场景无 SPI Handler 时回落本执行器。场景差异全部
 * 由 {@code ai_scene_config} 声明（user_prompt_template / output_parser / output_schema），
 * 业务上下文由各业务 Service 组装进 input——新增场景 = 枚举 + 配置行 + Service 组装，
 * 不再写 Handler。{@code AiSceneHandler} SPI 接口保留作为逃生舱（output_schema 表达力
 * 不足的复杂场景仍可实现 Handler 覆盖）。</p>
 *
 * <p>提示词约定（与 systemPromptTemplate 废弃决策一致）：人设由 Agent 表承载
 * （网关注入 input.agentPersona，经 mergePersona 前置 + 任务边界收口）；任务指令与
 * 数据全部进 user_prompt_template。</p>
 *
 * @author laomao
 * @since 2026-09-24
 */
@Slf4j
@Component
public class DefaultSceneExecutor extends AbstractAiSceneHandler {

    /** 兜底场景码（不与真实场景冲突；Registry 按此识别并在路由表中跳过注册） */
    public static final String SCENE_CODE = "default";

    /** 无 Agent 人设时的最小系统提示词（保证模型收到明确任务视角） */
    private static final String DEFAULT_SYSTEM_PROMPT = "你是一个严谨的任务执行助手，严格按用户消息中的指令与数据完成任务。";

    /** output_schema 配置存在时追加的输出格式约束段 */
    private static final String SCHEMA_CONSTRAINT =
            "\n\n【输出格式】输出必须是符合以下 JSON Schema 的合法 JSON，禁止输出 JSON 以外的任何内容：\n";

    @Override
    public String getSceneCode() {
        return SCENE_CODE;
    }

    @Override
    public String getSupportedOutputMode() {
        return "both";
    }

    @Override
    public void validate(AiExecuteRequest request) {
        // 配置驱动场景：input（结构化参数/模板变量）与 userInput（自由文本）至少其一非空
        boolean emptyInput = request.getInput() == null || request.getInput().isEmpty();
        boolean emptyUserInput = request.getUserInput() == null || request.getUserInput().isBlank();
        if (emptyInput && emptyUserInput) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "输入参数不能为空");
        }
    }

    @Override
    public AiExecuteResponse<?> execute(AiExecuteRequest request, AiSceneConfig config) {
        AiSceneMetadata meta = AiSceneMetadata.from(config);

        // 1. 系统提示词：Agent 人设（网关注入 input.agentPersona）mergePersona 收口；
        //    无 Agent 时最小默认提示词；output_schema 存在时追加输出格式约束
        String systemPrompt = mergePersona(request, DEFAULT_SYSTEM_PROMPT)
                + schemaConstraint(meta);

        // 2. 用户提示词：配置模板渲染（{{variable}} ← input）；无模板回落 userInput 契约文本
        String userPrompt = resolveUserPrompt(request, meta);

        // 3. LLM 调用（结构化输出 + 解析失败降级重试；文本类场景走纯对话）
        ChatOutcome outcome = meta.isTextParser()
                ? chatDetailed(meta.getEffectiveSceneCode(), systemPrompt, userPrompt)
                : chatJsonOutcome(meta.getEffectiveSceneCode(), systemPrompt, userPrompt);
        String raw = outcome.getText();
        if (raw == null || raw.isBlank()) {
            log.warn("[aigateway:{}] 配置驱动执行 LLM 无输出", meta.getEffectiveSceneCode());
            return AiExecuteResponse.failure(AiErrorCodes.AI_CALL_FAILED, "AI服务暂不可用");
        }

        // 4. 输出解析（output_parser 配置接线：json 容错提取 / markdown·text 包装 content）
        Map<String, Object> parsed = parseOutput(raw, config);
        if (parsed == null) {
            log.warn("[aigateway:{}] 配置驱动执行输出解析失败", meta.getEffectiveSceneCode());
            return AiExecuteResponse.failure(AiErrorCodes.AI_PARSE_ERROR, "场景执行结果解析失败");
        }

        // 5. 通用数据载体（消费方经 AiSceneJsonClient.unwrapStructured 透明读取）
        GenericSceneData data = new GenericSceneData();
        if (meta.isTextParser()) {
            data.setContent(cleanLlmText(raw));
        } else {
            data.setStructured(parsed);
        }

        AiExecuteResponse<GenericSceneData> response = AiExecuteResponse.success(data);
        response.setMetadata(buildMetadata(outcome));
        return response;
    }

    /** output_schema 配置存在时构建输出格式约束段（schema 原文进提示词，管理端可编辑） */
    private String schemaConstraint(AiSceneMetadata meta) {
        if (meta.getOutputSchema() == null || meta.getOutputSchema().isBlank() || meta.isTextParser()) {
            return "";
        }
        return SCHEMA_CONSTRAINT + meta.getOutputSchema().trim();
    }

    /**
     * 用户提示词解析：配置模板优先（{{variable}} 占位符 ← input，input 缺 key 保留原样
     * 便于发现配置错误）；无模板回落 userInput（人打的原始输入契约）。
     */
    private String resolveUserPrompt(AiExecuteRequest request, AiSceneMetadata meta) {
        if (meta.getUserPromptTemplate() != null && !meta.getUserPromptTemplate().isBlank()) {
            return renderTemplate(meta.getUserPromptTemplate(), request);
        }
        if (request.getUserInput() != null && !request.getUserInput().isBlank()) {
            return request.getUserInput();
        }
        throw new BusinessException(ErrorCode.PARAM_ERROR,
                "场景缺少用户提示词模板（user_prompt_template）且未传 userInput");
    }
}
