package com.moyun.ext.ai2.handler;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.moyun.ext.ai.entity.AiSceneConfig;
import com.moyun.ext.ai.service.AiSceneResolver;
import com.moyun.ext.ai.service.LLMService;
import com.moyun.ext.ai.service.ModelConfigService;
import com.moyun.ext.ai2.model.AiExecuteRequest;
import dev.langchain4j.data.message.SystemMessage;
import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.model.chat.ChatLanguageModel;
import dev.langchain4j.model.chat.response.ChatResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.List;
import java.util.Map;

/**
 * AI场景处理器基类
 *
 * <p>封装所有场景Handler的公共能力，子类只需实现提示词构建与结果解析：</p>
 * <ul>
 *   <li>模型解析：复用 AI 底座 {@link AiSceneResolver#resolveChatModel}（Agent绑定 → 直绑模型 → 默认模型），
 *       与现有 LlmClient 场景感知链路完全一致，不新增模型选择逻辑</li>
 *   <li>模板渲染：{{variable}} 占位符替换（从 request.input 取值）</li>
 *   <li>JSON 解析：容错提取（自动剥离 ```json 围栏 / 前后杂文本）</li>
 *   <li>流式输出：基于底座 LLMService.generateStream 的 SSE 适配</li>
 * </ul>
 *
 * @author laomao
 * @since 2026-09-09
 */
@Slf4j
public abstract class AbstractAiSceneHandler implements AiSceneHandler {

    protected static final ObjectMapper MAPPER = new ObjectMapper();

    /** 场景解析器（AI底座：scene_code → 绑定模型） */
    @Autowired(required = false)
    protected AiSceneResolver sceneResolver;

    /** AI底座统一 LLM 服务（默认模型回落通道） */
    @Autowired(required = false)
    protected LLMService llmService;

    @Autowired(required = false)
    protected ModelConfigService modelConfigService;

    /**
     * 场景感知同步对话：绑定模型优先，无绑定回落底座默认模型
     *
     * @return AI 回复文本；AI 不可用或调用失败返回 null（由网关降级策略兜底）
     */
    protected String chat(String sceneCode, String systemPrompt, String userPrompt) {
        return chatDetailed(sceneCode, systemPrompt, userPrompt).getText();
    }

    /**
     * 场景感知同步对话（结构化结果，v11.51）：除文本外返回实际使用的模型与 token 消耗。
     * 需要 metadata 可观测的场景 Handler 用本方法，并通过 {@link #buildMetadata} 填充响应。
     */
    protected com.moyun.ext.ai2.model.ChatOutcome chatDetailed(String sceneCode, String systemPrompt, String userPrompt) {
        com.moyun.ext.ai2.model.ChatOutcome outcome = new com.moyun.ext.ai2.model.ChatOutcome();
        // 1. 场景绑定模型（责任链：Agent绑定 → 直绑模型 → null）
        if (sceneResolver != null) {
            try {
                ChatLanguageModel boundModel = sceneResolver.resolveChatModel(sceneCode);
                if (boundModel != null) {
                    ChatResponse resp = boundModel.chat(List.of(
                            new SystemMessage(systemPrompt),
                            new UserMessage(userPrompt)));
                    if (resp != null && resp.aiMessage() != null) {
                        outcome.setText(resp.aiMessage().text());
                        fillUsage(outcome, resp);
                        return outcome;
                    }
                }
            } catch (Exception e) {
                log.warn("[ai2:{}] 场景绑定模型调用失败，回落默认模型: {}", sceneCode, e.getMessage());
            }
        }

        // 2. 回落底座默认模型（无 token 统计，标记 default）
        if (llmService != null) {
            try {
                outcome.setText(llmService.generate(systemPrompt + "\n\n" + userPrompt));
                outcome.setModelUsed("default");
            } catch (Exception e) {
                log.warn("[ai2:{}] 默认模型调用失败: {}", sceneCode, e.getMessage());
            }
        }
        return outcome;
    }

    /** 从 ChatResponse 提取模型名与 token 消耗（字段缺失时静默留空） */
    private void fillUsage(com.moyun.ext.ai2.model.ChatOutcome outcome, ChatResponse resp) {
        try {
            if (resp.tokenUsage() != null && resp.tokenUsage().totalTokenCount() != null) {
                outcome.setTokenUsed(resp.tokenUsage().totalTokenCount());
            }
        } catch (Exception ignored) {
        }
        try {
            if (resp.metadata() != null && resp.metadata().modelName() != null) {
                outcome.setModelUsed(resp.metadata().modelName());
            }
        } catch (Exception ignored) {
        }
    }

    /**
     * LLM 调用结果 → 响应元数据（v11.51）。Handler 调 chatDetailed 后构建：
     * modelUsed/tokenUsed/modelProvider 来自实际调用；agentUsed 由网关统一补充。
     */
    protected com.moyun.ext.ai2.model.AiMetadata buildMetadata(com.moyun.ext.ai2.model.ChatOutcome outcome) {
        com.moyun.ext.ai2.model.AiMetadata metadata = new com.moyun.ext.ai2.model.AiMetadata();
        if (outcome != null) {
            metadata.setModelUsed(outcome.getModelUsed());
            metadata.setModelProvider(outcome.getModelProvider());
            metadata.setTokenUsed(outcome.getTokenUsed());
        }
        return metadata;
    }

    /**
     * 流式对话：底座 LLMService.generateStream（SSE逐token推送）
     */
    protected void chatStream(String sceneCode, String systemPrompt, String userPrompt,
                              SseEmitter emitter, StringBuilder collected) {
        if (llmService == null) {
            throw new IllegalStateException("AI 服务未启用");
        }
        llmService.generateStream(
                systemPrompt + "\n\n" + userPrompt,
                token -> {
                    if (collected != null) {
                        collected.append(token);
                    }
                    try {
                        emitter.send(SseEmitter.event().name("chunk").data(token));
                    } catch (Exception ignored) {
                        // 客户端断开，忽略
                    }
                },
                () -> {
                    try {
                        emitter.send(SseEmitter.event().name("done").data("completed"));
                        emitter.complete();
                    } catch (Exception ignored) {
                    }
                },
                error -> {
                    try {
                        emitter.send(SseEmitter.event().name("error").data(
                                String.valueOf(error.getMessage())));
                        emitter.completeWithError(error);
                    } catch (Exception ignored) {
                    }
                });
    }

    /**
     * 渲染提示词模板：将 {{key}} 替换为 input 中对应值；input 无该 key 时保留原样（便于发现配置错误）
     */
    protected String renderTemplate(String template, AiExecuteRequest request) {
        if (template == null || request == null || request.getInput() == null) {
            return template;
        }
        String result = template;
        for (Map.Entry<String, Object> entry : request.getInput().entrySet()) {
            String placeholder = "{{" + entry.getKey() + "}}";
            if (result.contains(placeholder)) {
                result = result.replace(placeholder,
                        entry.getValue() != null ? String.valueOf(entry.getValue()) : "");
            }
        }
        return result;
    }

    /**
     * 容错解析 JSON 为 Map（泛型集合场景）
     */
    protected Map<String, Object> parseJsonMap(String raw) {
        if (raw == null || raw.isBlank()) {
            return null;
        }
        try {
            return MAPPER.readValue(extractJson(raw), new TypeReference<>() {
            });
        } catch (Exception e) {
            log.warn("[ai2] JSON(Map)解析失败: {}", e.getMessage());
            return null;
        }
    }

    /**
     * 从模型输出中提取 JSON 主体：优先 ```json 围栏，其次首个 { 到最后一个 } / [ 到最后一个 ]
     */
    private String extractJson(String raw) {
        String text = raw.trim();
        // 剥离 markdown 围栏
        if (text.contains("```")) {
            int start = text.indexOf("```");
            int contentStart = text.indexOf('\n', start);
            int end = text.lastIndexOf("```");
            if (contentStart > 0 && end > contentStart) {
                text = text.substring(contentStart + 1, end).trim();
            }
        }
        // 提取首个 {..} 或 [..]
        int objStart = text.indexOf('{');
        int arrStart = text.indexOf('[');
        int begin;
        char open, close;
        if (objStart >= 0 && (arrStart < 0 || objStart < arrStart)) {
            begin = objStart;
            open = '{';
            close = '}';
        } else if (arrStart >= 0) {
            begin = arrStart;
            open = '[';
            close = ']';
        } else {
            return text;
        }
        int end = text.lastIndexOf(close);
        if (end > begin) {
            return text.substring(begin, end + 1);
        }
        return text;
    }

    /**
     * 场景配置模板渲染默认实现：系统提示词优先取子类覆写，其次取注册表模板
     */
    @Override
    public String buildSystemPrompt(AiExecuteRequest request, AiSceneConfig config) {
        if (config != null && config.getSystemPromptTemplate() != null) {
            return renderTemplate(config.getSystemPromptTemplate(), request);
        }
        return null;
    }

    @Override
    public String buildUserPrompt(AiExecuteRequest request, AiSceneConfig config) {
        if (config != null && config.getUserPromptTemplate() != null) {
            return renderTemplate(config.getUserPromptTemplate(), request);
        }
        return null;
    }

    /**
     * Agent 人设前置合并（v11.53 加任务边界声明）
     *
     * <p>背景：用户在管理页绑定的 Agent 人设可能是对话式（如"与用户交流了解需求"），
     * 后台批处理任务中会诱导 LLM 输出问候/反问文本（"您好，请提供数据"）而非按
     * output_schema 输出 JSON，导致解析失败降级。</p>
     *
     * <p>策略：人设仅定义"口吻/专业视角"，注入后紧跟任务边界声明收口——
     * 无论绑什么风格的 Agent，都不会把后台任务带偏成聊天。</p>
     */
    protected String mergePersona(AiExecuteRequest request, String systemPrompt) {
        String persona = getInputString(request, "agentPersona");
        if (persona != null && !persona.isBlank()) {
            return persona + "\n\n【任务边界】本任务为系统自动执行的后台分析任务，所需数据已在用户消息中完整提供："
                    + "不要问候、不要反问、不要向用户索要任何信息，直接基于给定数据完成任务，"
                    + "并严格遵守下方的角色设定与输出格式要求。\n\n" + systemPrompt;
        }
        return systemPrompt;
    }

    /**
     * 从 input 取字符串参数
     */
    protected String getInputString(AiExecuteRequest request, String key) {
        Object value = request.getInput() != null ? request.getInput().get(key) : null;
        return value != null ? String.valueOf(value) : null;
    }

    /**
     * 清洗 LLM 原始文本用于展示兜底（v11.53）：剥离 markdown 围栏与首尾空白。
     * JSON 残骸（以 { 或 [ 开头——通常为截断的结构化输出）不适合人读，返回空串由调用方走模板降级。
     */
    protected String cleanLlmText(String raw) {
        if (raw == null || raw.isBlank()) {
            return "";
        }
        String text = raw.trim();
        if (text.startsWith("```")) {
            int firstLineEnd = text.indexOf('\n');
            int lastFence = text.lastIndexOf("```");
            if (firstLineEnd > 0 && lastFence > firstLineEnd) {
                text = text.substring(firstLineEnd + 1, lastFence).trim();
            } else if (firstLineEnd > 0) {
                text = text.substring(firstLineEnd + 1).trim();
            }
        }
        if (text.startsWith("{") || text.startsWith("[")) {
            return "";
        }
        return text;
    }

    /**
     * 从 input 取整数参数（带默认值）
     */
    protected Integer getInputInteger(AiExecuteRequest request, String key, Integer defaultValue) {
        Object value = request.getInput() != null ? request.getInput().get(key) : null;
        if (value instanceof Number number) {
            return number.intValue();
        }
        if (value instanceof String s && !s.isBlank()) {
            try {
                return Integer.parseInt(s.trim());
            } catch (NumberFormatException ignored) {
            }
        }
        return defaultValue;
    }

    /**
     * 从 input 取必填字符串参数，缺失时抛业务异常
     */
    protected String requireInputString(AiExecuteRequest request, String key) {
        String value = getInputString(request, key);
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("缺少必填参数: " + key);
        }
        return value;
    }

    /**
     * 取必填的用户自由文本（顶层 userInput 字段，v11.52 契约）。
     * 对话/检测/生成类场景的"人打的原始输入"统一走此参数，业务结构化参数仍走 input。
     */
    protected String requireUserInput(AiExecuteRequest request) {
        String value = request.getUserInput();
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("缺少必填参数: userInput（用户输入文本）");
        }
        return value;
    }
}
