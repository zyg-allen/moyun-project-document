package com.moyun.ext.ai.service.impl;

import com.moyun.ext.ai.entity.ModelConfig;
import com.moyun.ext.ai.exception.BusinessException;
import com.moyun.ext.ai.exception.ErrorCode;
import com.moyun.ext.ai.service.LLMService;
import com.moyun.ext.ai.service.ModelConfigService;
import com.moyun.ext.aigateway.model.AiMetadata;
import com.moyun.ext.aigateway.support.AiExecuteLogService;
import com.moyun.ext.aigateway.support.TokenMeter;
import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.model.chat.ChatLanguageModel;
import dev.langchain4j.model.chat.StreamingChatLanguageModel;
import dev.langchain4j.model.chat.response.ChatResponse;
import dev.langchain4j.model.chat.response.StreamingChatResponseHandler;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.StopWatch;
import org.springframework.util.StringUtils;

import java.util.Collections;
import java.util.UUID;
import java.util.function.Consumer;

/**
 * LLM服务实现
 *
 * <p>核心功能：</p>
 * <ul>
 *     <li>封装LangChain4j的ChatLanguageModel调用</li>
 *     <li>提供统一的LLM生成接口</li>
 *     <li>支持默认模型和指定模型两种调用方式</li>
 *     <li>集成性能监控和错误处理</li>
 * </ul>
 *
 * <p><b>v14.72 计量收口</b>：本类是 admin 底座直连 LLM 的公共通道（图表 XML /
 * 提示词生成 / SQL 生成 / 会话摘要等），历史上无任何记账——token 成本盲区。
 * 现在每次调用按 meterScene 场景落 ai_execute_log（真实 usage 优先，缺失由
 * {@link TokenMeter} 本地估算并打标），成本看板与网关场景同口径可见。
 * <b>不加强制限流/熔断</b>（尊重 admin 底座豁免决策），只补计量记账。
 * meterScene=null（网关内部回落调用）跳过记账，由网关按场景统一计量，防双记。</p>
 *
 * @author laomao
 * @since 2025-11-30
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class LLMServiceImpl implements LLMService {

    private final ModelConfigService modelConfigService;
    private final TokenMeter tokenMeter;
    private final AiExecuteLogService executeLogService;

    /** Prompt最大长度限制（字符数）避免超过模型token限制 */
    private static final int MAX_PROMPT_LENGTH = 100000;

    /** 计量日志的输入/输出摘要截断长度 */
    private static final int SUMMARY_MAX_LENGTH = 500;

    @Override
    public String generate(String prompt, String meterScene) {
        // 1. 获取默认模型配置
        ModelConfig config = modelConfigService.getDefaultChatConfig();
        if (config == null) {
            log.error("❌ 未找到默认聊天模型配置");
            throw new BusinessException(ErrorCode.CHAT_MODEL_NOT_CONFIGURED, "未找到默认聊天模型配置，请在系统设置中配置默认模型");
        }

        log.debug("🤖 使用默认模型: {}", config.getModelName());
        return doGenerate(config, prompt, meterScene);
    }

    @Override
    public String generate(Long modelConfigId, String prompt) {
        ModelConfig config = modelConfigService.getById(modelConfigId);
        if (config == null) {
            throw new BusinessException(ErrorCode.MODEL_CREATE_FAILED, "模型配置不存在: " + modelConfigId);
        }
        return doGenerate(config, prompt, DIRECT_SCENE);
    }

    /**
     * 生成 + 计量记账（同步公共实现）
     */
    private String doGenerate(ModelConfig config, String prompt, String meterScene) {
        StopWatch stopWatch = new StopWatch("LLMGeneration");

        try {
            // 1. 参数校验
            validatePrompt(prompt);

            log.info("🤖 开始LLM生成: modelConfigId={}, meterScene={}, promptLength={}",
                    config.getId(), meterScene, prompt.length());

            // 2. 创建模型实例
            stopWatch.start("创建模型");
            ChatLanguageModel model = modelConfigService.createChatModel(config.getId());
            stopWatch.stop();

            if (model == null) {
                throw new BusinessException(ErrorCode.MODEL_CREATE_FAILED, "创建模型失败，模型配置可能不存在: " + config.getId());
            }

            // 3. 调用模型生成
            stopWatch.start("模型调用");
            ChatResponse chatResponse = model.chat(Collections.singletonList(new UserMessage(prompt)));
            String response = chatResponse.aiMessage().text();
            stopWatch.stop();

            // 4. 记录日志
            log.info("✅ LLM生成成功: responseLength={}, 耗时={}ms",
                    response != null ? response.length() : 0,
                    stopWatch.getTotalTimeMillis());
            log.debug("⏱️  详细耗时: {}", stopWatch.prettyPrint());

            // 5. 计量记账（meterScene=null 为网关内部调用，跳过防双记）
            recordMetered(meterScene, config, chatResponse, prompt, response,
                    stopWatch.getTotalTimeMillis(), null);

            return response;

        } catch (IllegalArgumentException e) {
            log.warn("⚠️  参数校验失败: {}", e.getMessage());
            throw e;
        } catch (Exception e) {
            log.error("❌ LLM生成失败: modelConfigId={}, error={}",
                    config.getId(), e.getMessage(), e);
            recordMetered(meterScene, config, null, prompt, null,
                    stopWatch.getTotalTimeMillis(), e.getMessage());
            throw new BusinessException(ErrorCode.CHAT_FAILED, "LLM生成失败: " + e.getMessage(), e);
        } finally {
            if (stopWatch.isRunning()) {
                stopWatch.stop();
            }
        }
    }

    @Override
    public void generateStream(String prompt, String meterScene,
                               Consumer<String> onToken,
                               Runnable onComplete,
                               Consumer<Throwable> onError) {
        try {
            // 1. 参数校验
            validatePrompt(prompt);

            // 2. 获取默认模型配置
            ModelConfig config = modelConfigService.getDefaultChatConfig();
            if (config == null) {
                throw new BusinessException(ErrorCode.CHAT_MODEL_NOT_CONFIGURED);
            }

            log.info("🤖 开始流式LLM生成: model={}, meterScene={}, promptLength={}",
                    config.getModelName(), meterScene, prompt.length());

            // 3. 创建流式模型实例（增加 maxTokens 到 8000，确保能生成完整的 XML）
            StreamingChatLanguageModel streamingModel =
                    modelConfigService.createStreamingChatModel(config.getId(), null, 8000);

            if (streamingModel == null) {
                throw new BusinessException(ErrorCode.MODEL_CREATE_FAILED, "创建流式模型失败");
            }

            // 4. 流式调用
            log.info("🤖 开始流式调用...");
            final StringBuilder responseBuilder = new StringBuilder();
            final long streamStart = System.currentTimeMillis();

            streamingModel.chat(
                    Collections.singletonList(new UserMessage(prompt)),
                    new StreamingChatResponseHandler() {
                        @Override
                        public void onPartialResponse(String partialResponse) {
                            if (partialResponse != null && !partialResponse.isEmpty()) {
                                responseBuilder.append(partialResponse);
                                log.debug("📝 收到token: {} 字符, 累计: {} 字符",
                                        partialResponse.length(), responseBuilder.length());
                                try {
                                    onToken.accept(partialResponse);
                                } catch (Exception e) {
                                    log.error("❌ onToken 回调异常: {}", e.getMessage(), e);
                                }
                            }
                        }

                        @Override
                        public void onCompleteResponse(ChatResponse response) {
                            String fullContent = responseBuilder.toString();
                            log.info("✅ 流式LLM生成完成, 总长度: {} 字符", fullContent.length());
                            log.info("✅ 内容末尾100字符: {}",
                                    fullContent.length() > 100 ? fullContent.substring(fullContent.length() - 100) : fullContent);
                            log.info("✅ 包含 </mxGraphModel>? {}", fullContent.contains("</mxGraphModel>"));
                            // v14.72 计量收口：流式同样记账（真实 usage 优先，缺失估算）
                            recordMetered(meterScene, config, response, prompt, fullContent,
                                    System.currentTimeMillis() - streamStart, null);
                            try {
                                onComplete.run();
                            } catch (Exception e) {
                                log.error("❌ onComplete 回调异常: {}", e.getMessage(), e);
                            }
                        }

                        @Override
                        public void onError(Throwable error) {
                            log.error("❌ 流式LLM生成失败: {}, 已生成: {} 字符",
                                    error.getMessage(), responseBuilder.length(), error);
                            recordMetered(meterScene, config, null, prompt, responseBuilder.toString(),
                                    System.currentTimeMillis() - streamStart, error.getMessage());
                            try {
                                onError.accept(error);
                            } catch (Exception e) {
                                log.error("❌ onError 回调异常: {}", e.getMessage(), e);
                            }
                        }
                    }
            );
            log.info("🤖 流式调用已启动（异步执行中）");

        } catch (Exception e) {
            log.error("❌ 流式生成初始化失败: {}", e.getMessage());
            onError.accept(e);
        }
    }

    /**
     * 计量记账：真实 usage 优先，缺失由 TokenMeter 本地分词估算并打标；
     * meterScene 为 null（网关内部调用）时跳过——由网关按场景统一计量，防双记。
     * 记账失败不影响业务（AiExecuteLogService 内部已吞异常）。
     */
    private void recordMetered(String meterScene, ModelConfig config, ChatResponse resp,
                               String prompt, String output, long elapsedMs, String errorMsg) {
        if (meterScene == null) {
            return;
        }
        try {
            AiMetadata metadata = new AiMetadata();
            metadata.setModelUsed(config != null ? config.getModelName() : null);
            TokenMeter.Metered metered = tokenMeter.meterTexts(
                    prompt != null ? prompt : "",
                    output != null ? output : "");
            boolean estimated = true;
            if (resp != null && resp.tokenUsage() != null && resp.tokenUsage().totalTokenCount() != null) {
                Integer total = resp.tokenUsage().totalTokenCount();
                metadata.setInputTokens(resp.tokenUsage().inputTokenCount());
                metadata.setOutputTokens(resp.tokenUsage().outputTokenCount());
                metadata.setTokenUsed(total);
                estimated = false;
            } else {
                metadata.setInputTokens(metered.inputTokens());
                metadata.setOutputTokens(metered.outputTokens());
                metadata.setTokenUsed(metered.totalTokens());
            }
            metadata.setTokenEstimated(estimated);
            executeLogService.record(
                    UUID.randomUUID().toString().replace("-", ""),
                    null,
                    meterScene,
                    "LLMService",
                    "model",
                    metadata,
                    abbreviate(prompt, SUMMARY_MAX_LENGTH),
                    abbreviate(output, SUMMARY_MAX_LENGTH),
                    errorMsg == null ? "success" : "fail",
                    errorMsg,
                    elapsedMs);
        } catch (Exception e) {
            log.warn("⚠️ LLM直连计量记账失败（不影响业务）: {}", e.getMessage());
        }
    }

    private String abbreviate(String text, int maxLength) {
        if (text == null) {
            return null;
        }
        return text.length() > maxLength ? text.substring(0, maxLength) : text;
    }

    /**
     * 校验Prompt参数
     *
     * @param prompt 提示词
     * @throws IllegalArgumentException 如果prompt无效
     */
    private void validatePrompt(String prompt) {
        if (!StringUtils.hasText(prompt)) {
            throw new IllegalArgumentException("Prompt不能为空");
        }

        if (prompt.length() > MAX_PROMPT_LENGTH) {
            throw new IllegalArgumentException(
                    String.format("Prompt长度超过限制: %d > %d", prompt.length(), MAX_PROMPT_LENGTH));
        }
    }
}
