package com.moyun.agent.controller;

import com.moyun.agent.common.ApiResponse;
import com.moyun.agent.dto.PromptGenerateRequest;
import com.moyun.agent.dto.PromptGenerateResponse;
import com.moyun.agent.entity.ModelConfig;
import com.moyun.agent.service.ModelConfigService;
import dev.langchain4j.model.chat.StreamingChatLanguageModel;
import dev.langchain4j.model.chat.response.ChatResponse;
import dev.langchain4j.model.chat.response.StreamingChatResponseHandler;
import dev.langchain4j.data.message.ChatMessage;
import dev.langchain4j.data.message.UserMessage;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;

/**
 * 提示词生成控制器
 *
 * <p>提供基于AI的智能体系统提示词生成功能</p>
 *
 * @author laomao
 * @time 2025/11/23
 */
@Slf4j
@Tag(name = "提示词生成")
@RestController
@RequestMapping("/api/prompt")
public class PromptGeneratorController {

    @Autowired
    private ModelConfigService modelConfigService;

    /**
     * 生成系统提示词
     *
     * <p>根据用户输入的智能体描述和名称，使用AI生成专业的系统提示词</p>
     *
     * @param request 提示词生成请求，包含智能体描述
     * @return 生成的系统提示词
     */
    @Operation(summary = "生成系统提示词", description = "基于AI生成专业的智能体系统提示词")
    @PostMapping("/generate")
    public ApiResponse<PromptGenerateResponse> generateSystemPrompt(
            @RequestBody PromptGenerateRequest request) {

        try {
            // 验证输入参数
            String userInput = request.getDescription();
            if (userInput == null || userInput.trim().isEmpty()) {
                return ApiResponse.error("请输入智能体描述");
            }

            log.info("收到生成提示词请求 - 描述: {}", userInput);

            // 构建生成提示词的prompt
            String generationPrompt = buildGenerationPrompt(userInput);

            // 获取要使用的模型配置
            ModelConfig modelConfig = null;

            if (request.getModelConfigId() != null) {
                // 用户指定了模型ID
                modelConfig = modelConfigService.getById(request.getModelConfigId());
                if (modelConfig == null) {
                    log.error("指定的模型配置不存在: ID={}", request.getModelConfigId());
                    return ApiResponse.error("指定的模型不存在");
                }
                if (!modelConfig.getEnabled()) {
                    log.error("指定的模型未启用: ID={}", request.getModelConfigId());
                    return ApiResponse.error("指定的模型未启用");
                }
                if (!"chat".equals(modelConfig.getModelType())) {
                    log.error("指定的模型类型不是对话模型: ID={}, type={}",
                            request.getModelConfigId(), modelConfig.getModelType());
                    return ApiResponse.error("请选择对话类型的模型");
                }
                log.info("使用指定的对话模型: {} ({})", modelConfig.getName(), modelConfig.getModelName());
            } else {
                // 使用默认模型
                modelConfig = modelConfigService.getDefaultChatConfig();
                if (modelConfig == null) {
                    log.error("未找到默认的聊天模型配置");
                    return ApiResponse.error("系统未配置AI模型，请先在模型管理中添加并设为默认模型");
                }
                log.info("使用默认聊天模型: {} ({})", modelConfig.getName(), modelConfig.getModelName());
            }

            StreamingChatLanguageModel streamingModel = modelConfigService.createStreamingChatModel(modelConfig.getId());

            // 检查模型创建
            if (streamingModel == null) {
                log.error("创建流式聊天模型失败: 模型ID={}", modelConfig.getId());
                return ApiResponse.error("AI模型初始化失败");
            }

            log.info("流式模型创建成功，开始生成提示词...");

            // 构建消息列表
            List<ChatMessage> messages = new ArrayList<>();
            messages.add(new UserMessage(generationPrompt));

            // 使用CompletableFuture收集流式响应
            CompletableFuture<String> futureResponse = new CompletableFuture<>();
            StringBuilder responseBuilder = new StringBuilder();

            log.info("开始调用模型流式接口...");

            streamingModel.chat(messages, new StreamingChatResponseHandler() {
                @Override
                public void onPartialResponse(String token) {
                    responseBuilder.append(token);
                    log.debug("收到部分响应: {} 字符", token.length());
                }

                @Override
                public void onCompleteResponse(ChatResponse response) {
                    log.info("流式响应完成，总长度: {} 字符", responseBuilder.length());
                    futureResponse.complete(responseBuilder.toString());
                }

                @Override
                public void onError(Throwable error) {
                    log.error("流式响应出错", error);
                    futureResponse.completeExceptionally(error);
                }
            });

            log.info("等待流式响应完成（最多等待120秒）...");

            // 等待流式响应完成，设置超时时间
            String generatedPrompt = futureResponse.get(120, java.util.concurrent.TimeUnit.SECONDS);

            log.info("生成的系统提示词长度: {}", generatedPrompt.length());

            // 构建响应对象
            PromptGenerateResponse response = new PromptGenerateResponse(generatedPrompt);
            return ApiResponse.success("生成成功", response);

        } catch (Exception e) {
            log.error("生成系统提示词失败", e);
            return ApiResponse.error("生成失败: " + e.getMessage());
        }
    }

    /**
     * 构建生成系统提示词的prompt
     *
     * <p>根据用户描述构建AI生成提示词所需的prompt模板</p>
     *
     * @param userDescription 用户输入的智能体描述
     * @return 完整的生成prompt
     */
    private String buildGenerationPrompt(String userDescription) {
        StringBuilder prompt = new StringBuilder();

        prompt.append("你是一个专业的AI提示词工程师。用户想要创建一个智能体，需要你帮助生成专业的系统提示词。\n\n");

        prompt.append("【用户输入】\n");
        prompt.append("简单描述: ").append(userDescription).append("\n\n");

        prompt.append("【生成要求】\n");
        prompt.append("1. 根据用户描述，生成清晰、专业、详细的系统提示词\n");
        prompt.append("2. 明确说明智能体的身份、职责、专业领域\n");
        prompt.append("3. 说明擅长回答什么类型的问题\n");
        prompt.append("4. 说明回答风格（专业、严谨、友好等）\n");
        prompt.append("5. 如果是基于知识库的智能体，强调必须基于知识库内容回答\n");
        prompt.append("6. 字数控制在150-300字，简洁有力\n");
        prompt.append("7. 使用第一人称（我是...）\n");
        prompt.append("8. 直接输出系统提示词内容，不要有任何前言或解释\n\n");

        prompt.append("【示例参考】\n");
        prompt.append("输入: 这是一个针对语文考试的智能体\n");
        prompt.append("输出: 我是语文考试智能助手，专门帮助学生准备语文考试。我的专长包括：\n");
        prompt.append("1. 古诗词鉴赏与理解\n");
        prompt.append("2. 阅读理解答题技巧\n");
        prompt.append("3. 作文写作指导\n");
        prompt.append("4. 文言文翻译与解析\n");
        prompt.append("5. 语文基础知识讲解\n");
        prompt.append("我会基于知识库中的考试资料为你提供准确、专业的指导。请注意，我只能回答语文考试相关的问题，超出范围的问题无法解答。\n\n");

        prompt.append("现在请根据上述用户输入，生成系统提示词：");

        return prompt.toString();
    }
}
