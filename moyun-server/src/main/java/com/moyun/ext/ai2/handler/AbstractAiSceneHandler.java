package com.moyun.ext.ai2.handler;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.moyun.ext.ai.service.AiSceneResolver;
import com.moyun.ext.ai.service.LLMService;
import com.moyun.ext.ai.service.ModelConfigService;
import com.moyun.ext.ai2.entity.AiSceneRegistryConfig;
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
        // 1. 场景绑定模型（责任链：Agent绑定 → 直绑模型 → null）
        if (sceneResolver != null) {
            try {
                ChatLanguageModel boundModel = sceneResolver.resolveChatModel(sceneCode);
                if (boundModel != null) {
                    ChatResponse resp = boundModel.chat(List.of(
                            new SystemMessage(systemPrompt),
                            new UserMessage(userPrompt)));
                    if (resp != null && resp.aiMessage() != null) {
                        return resp.aiMessage().text();
                    }
                }
            } catch (Exception e) {
                log.warn("[ai2:{}] 场景绑定模型调用失败，回落默认模型: {}", sceneCode, e.getMessage());
            }
        }

        // 2. 回落底座默认模型
        if (llmService != null) {
            try {
                return llmService.generate(systemPrompt + "\n\n" + userPrompt);
            } catch (Exception e) {
                log.warn("[ai2:{}] 默认模型调用失败: {}", sceneCode, e.getMessage());
            }
        }
        return null;
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
     * 容错解析模型返回的 JSON：剥离 markdown 围栏与前后杂文本
     *
     * @return 解析后的对象；解析失败返回 null
     */
    protected <T> T parseJson(String raw, Class<T> type) {
        if (raw == null || raw.isBlank()) {
            return null;
        }
        String json = extractJson(raw);
        try {
            return MAPPER.readValue(json, type);
        } catch (Exception e) {
            log.warn("[ai2] JSON解析失败: {}, 原文前200字符: {}", e.getMessage(),
                    raw.substring(0, Math.min(200, raw.length())));
            return null;
        }
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
    public String buildSystemPrompt(AiExecuteRequest request, AiSceneRegistryConfig config) {
        if (config != null && config.getSystemPromptTemplate() != null) {
            return renderTemplate(config.getSystemPromptTemplate(), request);
        }
        return null;
    }

    @Override
    public String buildUserPrompt(AiExecuteRequest request, AiSceneRegistryConfig config) {
        if (config != null && config.getUserPromptTemplate() != null) {
            return renderTemplate(config.getUserPromptTemplate(), request);
        }
        return null;
    }

    /**
     * 从 input 取字符串参数
     */
    protected String getInputString(AiExecuteRequest request, String key) {
        Object value = request.getInput() != null ? request.getInput().get(key) : null;
        return value != null ? String.valueOf(value) : null;
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
}
