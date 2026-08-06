package com.moyun.ext.ai.service.impl;

import com.moyun.ext.ai.entity.ModelConfig;
import com.moyun.ext.ai.exception.BusinessException;
import com.moyun.ext.ai.exception.ErrorCode;
import com.moyun.ext.ai.service.LLMService;
import com.moyun.ext.ai.service.ModelConfigService;
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
 * @author laomao
 * @since 2025-11-30
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class LLMServiceImpl implements LLMService {
    
    private final ModelConfigService modelConfigService;
    
    /** Prompt最大长度限制（字符数）避免超过模型token限制 */
    private static final int MAX_PROMPT_LENGTH = 100000;
    
    /**
     * 使用默认聊天模型生成内容
     * 
     * <p>自动选择系统配置的默认聊天模型进行生成</p>
     * 
     * @param prompt 输入提示词
     * @return LLM生成的响应文本
     * @throws IllegalArgumentException 如果prompt为空或超长
     * @throws RuntimeException 如果未配置默认模型或生成失败
     */
    @Override
    public String generate(String prompt) {
        // 1. 获取默认模型配置
        ModelConfig config = modelConfigService.getDefaultChatConfig();
        if (config == null) {
            log.error("❌ 未找到默认聊天模型配置");
            throw new BusinessException(ErrorCode.CHAT_MODEL_NOT_CONFIGURED, "未找到默认聊天模型配置，请在系统设置中配置默认模型");
        }
        
        log.debug("🤖 使用默认模型: {}", config.getModelName());
        return generate(config.getId(), prompt);
    }
    
    /**
     * 使用指定模型生成内容
     * 
     * <p>主要流程：</p>
     * <ol>
     *     <li>校验prompt参数</li>
     *     <li>创建ChatLanguageModel实例</li>
     *     <li>调用模型生成响应</li>
     *     <li>记录性能指标和日志</li>
     * </ol>
     * 
     * @param modelConfigId 模型配置ID
     * @param prompt 输入提示词
     * @return LLM生成的响应文本
     * @throws IllegalArgumentException 如果参数无效
     * @throws RuntimeException 如果生成失败
     */
    @Override
    public String generate(Long modelConfigId, String prompt) {
        StopWatch stopWatch = new StopWatch("LLMGeneration");
        
        try {
            // 1. 参数校验
            validatePrompt(prompt);
            
            log.info("🤖 开始LLM生成: modelConfigId={}, promptLength={}", 
                    modelConfigId, prompt.length());
            
            // 2. 创建模型实例
            stopWatch.start("创建模型");
            ChatLanguageModel model = modelConfigService.createChatModel(modelConfigId);
            stopWatch.stop();
            
            if (model == null) {
                throw new BusinessException(ErrorCode.MODEL_CREATE_FAILED, "创建模型失败，模型配置可能不存在: " + modelConfigId);
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
            
            return response;
            
        } catch (IllegalArgumentException e) {
            log.warn("⚠️  参数校验失败: {}", e.getMessage());
            throw e;
        } catch (Exception e) {
            log.error("❌ LLM生成失败: modelConfigId={}, error={}", 
                    modelConfigId, e.getMessage(), e);
            throw new BusinessException(ErrorCode.CHAT_FAILED, "LLM生成失败: " + e.getMessage(), e);
        } finally {
            if (stopWatch.isRunning()) {
                stopWatch.stop();
            }
        }
    }
    
    /**
     * 流式生成文本
     * 
     * @param prompt 提示词
     * @param onToken 每个token的回调
     * @param onComplete 完成回调
     * @param onError 错误回调
     */
    @Override
    public void generateStream(String prompt, 
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
            
            log.info("🤖 开始流式LLM生成: model={}, promptLength={}", 
                    config.getModelName(), prompt.length());
            
            // 3. 创建流式模型实例（增加 maxTokens 到 8000，确保能生成完整的 XML）
            StreamingChatLanguageModel streamingModel = 
                    modelConfigService.createStreamingChatModel(config.getId(), null, 8000);
            
            if (streamingModel == null) {
                throw new BusinessException(ErrorCode.MODEL_CREATE_FAILED, "创建流式模型失败");
            }
            
            // 4. 流式调用
            log.info("🤖 开始流式调用...");
            final StringBuilder responseBuilder = new StringBuilder();
            
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
