package com.moyun.ext.ai.service.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.moyun.ext.ai.config.RagConfig;
import com.moyun.ext.ai.exception.BusinessException;
import com.moyun.ext.ai.exception.ErrorCode;
import com.moyun.ext.ai.service.ToolCallingService;
import com.moyun.ext.ai.engine.tool.ToolContext;
import com.moyun.ext.ai.entity.Agent;
import com.moyun.ext.ai.entity.ModelConfig;
import com.moyun.ext.ai.service.*;
import com.moyun.ext.ai.service.chat.ChatContextBuilderService;
import com.moyun.ext.ai.service.chat.ChatMessagePersistenceService;
import com.moyun.ext.ai.service.chat.ChatResponseBuilderService;
import com.moyun.ext.ai.service.chat.RagRetrievalService;
import com.moyun.ext.ai.service.chat.ReferenceSourceFilterService;
import com.moyun.ext.ai.service.chat.StreamingTokenProcessor;
import com.moyun.ext.ai.service.chat.StreamingSessionManager;
import com.moyun.ext.ai.service.chat.ConversationSummaryService;
import com.moyun.ext.ai.service.chat.IntentRecognitionService;
import com.moyun.ext.ai.service.chat.SelfRagService;
import com.moyun.ext.ai.store.RedisChatMemoryStore;

import dev.langchain4j.data.message.*;
import dev.langchain4j.memory.chat.MessageWindowChatMemory;
import dev.langchain4j.model.chat.StreamingChatLanguageModel;
import dev.langchain4j.model.chat.response.ChatResponse;
import dev.langchain4j.model.chat.response.StreamingChatResponseHandler;
import dev.langchain4j.rag.content.Content;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Sinks;

import java.util.*;

/**
 * 动态对话服务实现类
 *
 * <p>
 * 核心对话服务，提供基于RAG的智能对话功能，支持：
 * <ul>
 * <li>流式对话响应</li>
 * <li>知识库检索增强</li>
 * <li>对话历史管理</li>
 * <li>引用来源追踪</li>
 * </ul>
 * </p>
 *
 * @author laomao
 */
@Slf4j
@Service
public class DynamicChatServiceImpl implements DynamicChatService {

    @Autowired
    private RedisChatMemoryStore redisChatMemoryStore;

    @Autowired
    private AgentService agentService;

    @Autowired
    private RagConfig ragConfig;

    @Autowired
    private ModelConfigService modelConfigService;

    @Autowired
    private ConversationService conversationService;

    @Autowired
    private ToolCallingService toolCallingService;

    @Autowired
    private TokenUsageService tokenUsageService;

    @Autowired
    private ChatHistoryService chatHistoryService;

    @Autowired
    private WorkflowService workflowService;

    // ========== 拆分后的新服务 ==========
    
    @Autowired
    private ChatResponseBuilderService chatResponseBuilderService;

    @Autowired
    private RagRetrievalService ragRetrievalService;

    @Autowired
    private ReferenceSourceFilterService referenceSourceFilterService;

    @Autowired
    private ChatContextBuilderService chatContextBuilderService;

    @Autowired
    private ChatMessagePersistenceService chatMessagePersistenceService;

    @Autowired
    private StreamingTokenProcessor streamingTokenProcessor;

    @Autowired
    private StreamingSessionManager streamingSessionManager;

    @Autowired
    private ConversationSummaryService conversationSummaryService;

    @Autowired
    private IntentRecognitionService intentRecognitionService;

    @Autowired
    private SelfRagService selfRagService;

    /**
     * {@inheritDoc}
     *
     * <p>
     * 简化版本，默认isGreeting为false
     * </p>
     */
    @Override
    public Flux<String> chat(Long conversationId, String userMessage, Long agentId) {
        return chat(conversationId, userMessage, agentId, false, null);
    }

    @Override
    public Flux<String> chat(Long conversationId, String userMessage, Long agentId, boolean isGreeting) {
        return chat(conversationId, userMessage, agentId, isGreeting, null);
    }

    /**
     * {@inheritDoc}
     *
     * <p>
     * 完整对话流程：
     * <ol>
     * <li>获取智能体配置</li>
     * <li>创建流式语言模型</li>
     * <li>构建对话记忆</li>
     * <li>知识库检索（非问候模式）</li>
     * <li>构建系统提示词</li>
     * <li>流式生成回复</li>
     * <li>保存对话历史</li>
     * </ol>
     * </p>
     */
    @Override
    public Flux<String> chat(Long conversationId, String userMessage, Long agentId, boolean isGreeting, List<String> images) {

        Sinks.Many<String> sink = Sinks.many().unicast().onBackpressureBuffer();

        // 🔧 修复：如果conversationId为null，先创建新会话
        Long effectiveConversationId = conversationId;
        if (effectiveConversationId == null) {
            var newConversation = conversationService.createConversation(agentId, null);
            effectiveConversationId = newConversation.getId();
            log.info("📝 创建新会话: conversationId={}", effectiveConversationId);
        }
        final Long finalConversationId = effectiveConversationId;

        // 生成请求唯一ID，注册流式会话（支持中断）
        final String requestId = java.util.UUID.randomUUID().toString();
        final java.util.concurrent.atomic.AtomicBoolean abortFlag = 
            streamingSessionManager.registerSession(finalConversationId, requestId);

        // 清空RagRetrievalService中的重排分数映射（拆分后的新逻辑）
        ragRetrievalService.clearContentRerankScores();

        int imageCount = (images != null) ? images.size() : 0;
        log.info("收到对话请求 - conversationId: {}, agentId: {}, isGreeting: {}, 图片数: {}, requestId: {}", 
            finalConversationId, agentId, isGreeting, imageCount, requestId);

        try {
            Agent agent = agentService.getById(agentId);
            if (agent == null) {
                log.error("智能体不存在! agentId: {}", agentId);
                sink.tryEmitNext("智能体不存在");
                sink.tryEmitComplete();
                return sink.asFlux();
            }

            log.info("\n" + "=".repeat(80));
            log.info("=== 开始对话处理 ===");
            log.info("智能体ID: {}", agent.getId());
            log.info("智能体名称: {}", agent.getName());
            log.info("系统提示词: {}", agent.getSystemPrompt());
            log.info("关联知识库IDs字符串: {}", agent.getKnowledgeBaseIds());
            log.info("会话ID: {}", finalConversationId);
            log.info("用户消息: {}", userMessage);
            log.info("=".repeat(80));

            // ========== 意图识别（用于日志和后续智能路由） ==========
            if (!isGreeting && ragConfig.isEnableIntentRecognition()) {
                try {
                    var intentResult = intentRecognitionService.recognize(userMessage, agent);
                    log.info("🎯 意图识别: {} (置信度: {})", intentResult.getIntent(), 
                            String.format("%.2f", intentResult.getConfidence()));
                } catch (Exception e) {
                    log.debug("意图识别跳过: {}", e.getMessage());
                }
            }

            // ========== 工作流触发检查 ==========
            // 工作流上下文（auto模式会将结果作为上下文，而不是直接返回）
            String workflowContext = null;

            if (!isGreeting && agent.getWorkflowId() != null && shouldTriggerWorkflow(agent, userMessage)) {
                String triggerMode = agent.getWorkflowTriggerMode() != null ? agent.getWorkflowTriggerMode() : "manual";
                log.info("🔄 触发工作流执行 - workflowId: {}, triggerMode: {}", agent.getWorkflowId(), triggerMode);

                try {
                    java.util.Map<String, Object> workflowInput = new java.util.HashMap<>();
                    workflowInput.put("input", userMessage);
                    workflowInput.put("query", userMessage);
                    workflowInput.put("user_message", userMessage);

                    var workflowResult = workflowService.execute(agent.getWorkflowId(), workflowInput);

                    if (workflowResult.isSuccess()) {
                        String output = workflowResult.getOutput() != null ? workflowResult.getOutput().toString() : "";
                        log.info("✅ 工作流执行成功，输出: {}", output.length() > 100 ? output.substring(0, 100) + "..." : output);

                        if (output != null && !output.isEmpty()) {
                            // manual和keyword模式：直接返回工作流结果（替代模式）
                            // auto模式：将结果作为上下文，继续正常对话（增强模式）
                            if ("manual".equals(triggerMode) || "keyword".equals(triggerMode)) {
                                log.info("📤 {}模式：直接返回工作流结果", triggerMode);
                                sink.tryEmitNext(output);
                                sink.tryEmitComplete();
                                return sink.asFlux();
                            } else {
                                // auto模式：工作流结果作为上下文
                                log.info("🔄 auto模式：工作流结果作为对话上下文");
                                workflowContext = output;
                            }
                        }
                    } else {
                        log.warn("⚠️ 工作流执行失败: {}", workflowResult.getErrorMessage());
                        // 工作流失败，继续正常对话流程
                    }
                } catch (Exception e) {
                    log.error("❌ 工作流执行异常: {}", e.getMessage(), e);
                    // 工作流异常，继续正常对话流程
                }
            }

            // 使用智能体配置的历史轮数，如果没有配置则使用全局默认值
            int maxMessages = agent.getMaxHistoryTurns() != null ? agent.getMaxHistoryTurns() * 2
                    : ragConfig.getMaxMessages();

            MessageWindowChatMemory chatMemory = MessageWindowChatMemory.builder()
                    .id(finalConversationId)
                    .maxMessages(maxMessages)
                    .chatMemoryStore(redisChatMemoryStore)
                    .build();

            log.info("对话历史配置: maxMessages={} (智能体配置轮数: {})", maxMessages, agent.getMaxHistoryTurns());

            List<ChatMessage> messages = new ArrayList<>(chatMemory.messages());

            log.info("历史消息数量: {}", messages.size());

            // 对话摘要：如果历史消息过多，压缩为摘要以节省Token
            if (ragConfig.isEnableConversationSummary()) {
                int summaryThreshold = ragConfig.getSummaryThreshold();
                if (conversationSummaryService.needsSummary(messages, summaryThreshold)) {
                    try {
                        int keepRecent = ragConfig.getSummaryKeepRecent();
                        messages = conversationSummaryService.compressHistory(messages, keepRecent);
                        log.info("📦 对话历史已压缩: 保留最近 {} 条消息 + 摘要", keepRecent);
                    } catch (Exception e) {
                        log.warn("⚠️ 对话摘要失败，使用原始历史: {}", e.getMessage());
                    }
                }
            }

            // 构建增强的系统提示词（使用拆分后的ChatContextBuilderService）
            boolean hasTools = toolCallingService.hasTools(agentId);
            String toolPrompt = hasTools ? toolCallingService.buildToolPrompt(agentId) : null;
            String systemPrompt = chatContextBuilderService.buildSystemPrompt(agent, hasTools, toolPrompt);
            messages.add(0, new SystemMessage(systemPrompt));
            log.info("已添加增强的系统提示词到消息列表");

            // 检查是否配置了知识库
            boolean hasKnowledge = (agent.getKnowledgeLibraryIds() != null && !agent.getKnowledgeLibraryIds().isEmpty())
                    || (agent.getKnowledgeBaseIds() != null && !agent.getKnowledgeBaseIds().isEmpty());

            String processedUserMessage = userMessage;
            final List<Content> retrievedContents = new ArrayList<>();

            // RAG - 知识库检索
            log.info("\n========== RAG 检索流程检查 ==========");
            log.info("agent.getKnowledgeLibraryIds() = {}", agent.getKnowledgeLibraryIds());
            log.info("agent.getKnowledgeBaseIds() = {}", agent.getKnowledgeBaseIds());
            log.info("hasKnowledge = {}", hasKnowledge);

            if (hasKnowledge) {
                log.info("\n>>> 智能体配置了知识库,开始RAG检索流程");
                List<Long> documentIds = agentService.getKnowledgeBaseIds(agentId);
                log.info("✅ 解析后的文档ID列表: {}", documentIds);
                log.info("✅ 文档数量: {}", documentIds.size());

                if (documentIds.isEmpty()) {
                    log.error("❌ 文档ID列表为空！");
                    log.error("💡 请检查智能体配置页面，确保正确关联了知识库，且知识库中有已处理完成的文档");
                } else {
                    // 使用拆分后的RagRetrievalService进行检索
                    List<Content> contents = ragRetrievalService.retrieveContents(userMessage, documentIds, agent);
                    
                    // Self-RAG验证：过滤不相关的检索结果
                    if (ragConfig.isEnableSelfRag() && contents != null && !contents.isEmpty()) {
                        try {
                            double minRelevance = agent.getRagMinScore() != null 
                                    ? agent.getRagMinScore() : ragConfig.getSelfRagMinRelevance();
                            contents = selfRagService.filterByRelevance(contents, userMessage, minRelevance);
                            log.info("🔍 Self-RAG验证后保留 {} 个相关内容", contents.size());
                        } catch (Exception e) {
                            log.warn("⚠️ Self-RAG验证失败，使用原始检索结果: {}", e.getMessage());
                        }
                    }
                    
                    if (contents != null && !contents.isEmpty()) {
                        retrievedContents.addAll(contents);
                        log.info("✅✅✅ 成功检索到知识库内容,数量: {}", retrievedContents.size());

                        // 使用ChatContextBuilderService构建RAG上下文
                        ChatContextBuilderService.RagContextResult ragResult = 
                                chatContextBuilderService.buildRagContext(retrievedContents);
                        
                        // 构建处理后的用户消息
                        processedUserMessage = chatContextBuilderService.buildProcessedUserMessage(
                                ragResult.getContext(), userMessage, null, true);
                        
                        // 如果有图片，添加图片引用指令
                        if (ragResult.getImageCount() > 0) {
                            processedUserMessage += "4. 图片引用：如有相关图片写 [[IMAGE_1]]、[[IMAGE_2]] 等";
                            log.info("✅ 已发送 {} 张图片给AI，添加图片引用指令", ragResult.getImageCount());
                        }
                    } else if (contents != null && contents.isEmpty()) {
                        // 使用ChatContextBuilderService构建无RAG内容的提示
                        processedUserMessage = chatContextBuilderService.buildNoRagContentMessage(userMessage);
                    } else {
                        log.error("❌❌❌ 检索失败或未返回结果！");
                        log.error("❌ 文档ID: {}", documentIds);
                        log.error("❌ 用户查询: {}", userMessage);
                        log.error("💡 可能原因：");
                        log.error("   1. 文档未完成向量化处理");
                        log.error("   2. 向量库连接失败");
                        log.error("   3. 向量库中没有对应的knowledgeBaseId数据");
                    }
                }
            } else {
                log.warn("❌ 智能体未配置知识库");
                log.warn("💡 请在智能体管理页面关联知识库");
                log.info(">>> 跳过RAG检索，直接使用用户问题");
            }

            // 如果有工作流上下文（auto模式），将其添加到用户消息中
            if (workflowContext != null && !workflowContext.isEmpty()) {
                processedUserMessage = "【工作流预处理结果】\n" + workflowContext + "\n\n【用户原始问题】\n" + processedUserMessage;
                log.info("✅ 已将工作流结果作为上下文添加到用户消息");
            }

            // 构建用户消息（支持多模态图片）
            if (images != null && !images.isEmpty()) {
                // 多模态消息：文本 + 图片
                List<dev.langchain4j.data.message.Content> contents = new ArrayList<>();
                contents.add(TextContent.from(processedUserMessage));
                
                for (String imageBase64 : images) {
                    // 解析 Base64 图片（格式：data:image/png;base64,xxxxx）
                    if (imageBase64.startsWith("data:image/")) {
                        int commaIndex = imageBase64.indexOf(",");
                        if (commaIndex > 0) {
                            String mimeTypePart = imageBase64.substring(5, imageBase64.indexOf(";"));
                            String base64Data = imageBase64.substring(commaIndex + 1);
                            contents.add(ImageContent.from(base64Data, mimeTypePart));
                            log.info("📷 添加多模态图片，类型: {}", mimeTypePart);
                        }
                    }
                }
                
                messages.add(UserMessage.from(contents));
                log.info("🖼️ 多模态消息已构建，包含 {} 张图片", images.size());
            } else {
                // 纯文本消息
                messages.add(new UserMessage(processedUserMessage));
            }

            // 获取模型配置：优先使用Agent指定的模型，否则使用默认模型
            ModelConfig tempModelConfig = null;
            log.info("🔍 Agent模型配置ID: {}", agent.getModelConfigId());
            if (agent.getModelConfigId() != null) {
                tempModelConfig = modelConfigService.getById(agent.getModelConfigId());
                if (tempModelConfig == null) {
                    log.warn("⚠️ Agent指定的模型配置不存在: modelConfigId={}, 使用默认模型", agent.getModelConfigId());
                } else {
                    log.info("✅ 使用Agent指定的模型配置: name={}, modelName={}, modelType={}, provider={}", 
                        tempModelConfig.getName(), tempModelConfig.getModelName(), 
                        tempModelConfig.getModelType(), tempModelConfig.getProvider());
                }
            }

            // 如果Agent未指定模型或指定的模型不存在，使用默认模型
            if (tempModelConfig == null) {
                tempModelConfig = modelConfigService.getDefaultChatConfig();
                if (tempModelConfig == null) {
                    throw new BusinessException(ErrorCode.CHAT_MODEL_NOT_CONFIGURED, "未找到默认对话模型配置，请在模型配置管理中添加并设置默认模型");
                }
                log.info("📌 使用系统默认模型配置: {}", tempModelConfig.getModelName());
            }

            // 声明为final以便在lambda中使用
            final ModelConfig chatModelConfig = tempModelConfig;

            // Agent配置覆盖ModelConfig的默认值
            Double temperature = agent.getTemperature() != null ? agent.getTemperature()
                    : chatModelConfig.getTemperature();
            Integer maxTokens = agent.getMaxTokens() != null ? agent.getMaxTokens() : chatModelConfig.getMaxTokens();

            StreamingChatLanguageModel chatModel = modelConfigService.createStreamingChatModel(
                    chatModelConfig.getId(), temperature, maxTokens);

            log.info("🤖 使用对话模型: {} ({}), temperature: {}, maxTokens: {}",
                    chatModelConfig.getName(), chatModelConfig.getModelName(), temperature, maxTokens);
            log.info("⚙️ 配置来源: modelConfig={}, temperature={}, maxTokens={}",
                    agent.getModelConfigId() != null ? "Agent指定" : "系统默认",
                    agent.getTemperature() != null ? "Agent" : "ModelConfig",
                    agent.getMaxTokens() != null ? "Agent" : "ModelConfig");
            
            // 多模态诊断（通过模型名判断是否支持图片）
            boolean isVlModel = isVisionModel(chatModelConfig.getModelName());
            boolean hasImages = images != null && !images.isEmpty();
            log.info("🖼️ 多模态诊断: modelName={}, isVlModel={}, hasImages={}, imageCount={}", 
                chatModelConfig.getModelName(), isVlModel, hasImages, hasImages ? images.size() : 0);
            
            // 如果用户上传了图片但模型不支持，忽略图片继续对话
            if (hasImages && !isVlModel) {
                log.info("📷 模型 {} 不支持图片，将忽略图片只处理文本", chatModelConfig.getModelName());
                images = null; // 清空图片，后续按纯文本处理
            }

            // 用于流式处理图片占位符的缓冲区
            final StringBuilder streamBuffer = new StringBuilder();
            final Set<Integer> replacedImageIndexes = new java.util.HashSet<>();
            // 使用拆分后的ChatResponseBuilder构建图片HTML映射
            final Map<Integer, String> imageHtmlMap = chatResponseBuilderService.buildImageHtmlMap(retrievedContents);

            // 📤 在流式响应开始前，先发送图片HTML映射（让前端提前准备）
            if (!imageHtmlMap.isEmpty()) {
                try {
                    String imageHtmlJson = new ObjectMapper().writeValueAsString(imageHtmlMap);
                    sink.tryEmitNext("[IMAGE_HTML_MAP]" + imageHtmlJson + "[/IMAGE_HTML_MAP]");
                    log.info("📤 提前发送图片HTML映射给前端，共 {} 张图片", imageHtmlMap.size());
                } catch (Exception e) {
                    log.warn("⚠️ 序列化图片HTML映射失败: {}", e.getMessage());
                }
            }

            // ⚠️ 关键：在主线程提取 ThreadLocal 数据，传递给异步回调
            // 必须在 chatModel.chat() 调用前提取，否则异步线程拿不到
            // 从RagRetrievalService获取重排分数（拆分后的新逻辑）
            final Map<Content, Double> rerankScoresForAsync = ragRetrievalService.getContentRerankScores();
            log.info("🔧 主线程提取重排分数映射，共 {} 个内容有分数", 
                    rerankScoresForAsync != null ? rerankScoresForAsync.size() : 0);

            // STREAMING
            chatModel.chat(messages, new StreamingChatResponseHandler() {

                @Override
                public void onPartialResponse(String token) {
                    // ⏹️ 检查是否应该中断
                    if (abortFlag.get()) {
                        log.info("⏹️ 检测到中断信号，停止流式输出");
                        sink.tryEmitNext("\n\n[已停止生成]");
                        sink.tryEmitComplete();
                        if (finalConversationId != null) {
                            streamingSessionManager.unregisterSession(finalConversationId);
                        }
                        return;
                    }
                    
                    // 将token添加到缓冲区
                    streamBuffer.append(token);
                    String bufferContent = streamBuffer.toString();

                    // 🔧 过滤工具调用标记（不显示给用户）
                    // 检查是否包含工具调用的任何部分（包括不完整的开始标记）
                    // 检测可能的工具调用开始
                    int possibleStart = bufferContent.indexOf("[");
                    if (possibleStart >= 0 && bufferContent.substring(possibleStart).startsWith("[TOOL_CALL]")) {
                        // 找到完整的开始标记
                        int toolCallStart = possibleStart;
                        int toolCallEnd = bufferContent.indexOf("[/TOOL_CALL]", toolCallStart);

                        if (toolCallEnd < 0) {
                            // 工具调用标记未完成，只输出标记之前的内容
                            if (toolCallStart > 0) {
                                String beforeToolCall = bufferContent.substring(0, toolCallStart);
                                if (!beforeToolCall.trim().isEmpty()) {
                                    sink.tryEmitNext(beforeToolCall);
                                }
                            }
                            // 保留工具调用部分在缓冲区，不输出
                            streamBuffer.setLength(0);
                            streamBuffer.append(bufferContent.substring(toolCallStart));
                            return;
                        } else {
                            // 工具调用标记完整，移除整个工具调用标记（包括结束标记后的内容也要处理）
                            String afterToolCall = bufferContent.substring(toolCallEnd + "[/TOOL_CALL]".length());
                            String beforeToolCall = toolCallStart > 0 ? bufferContent.substring(0, toolCallStart) : "";

                            // 输出工具调用之前的内容
                            if (!beforeToolCall.trim().isEmpty()) {
                                sink.tryEmitNext(beforeToolCall);
                            }

                            // 清空缓冲区，保留工具调用之后的内容
                            streamBuffer.setLength(0);
                            if (!afterToolCall.trim().isEmpty()) {
                                streamBuffer.append(afterToolCall);
                            }
                            // 不输出任何工具调用相关内容，直接返回
                            return;
                        }
                    }

                    // 检查是否有不完整的工具调用标记或图片占位符
                    // ⚠️ 注意：不要误判图片占位符 [[IMAGE_X]] 为工具调用
                    // 工具调用格式：[TOOL_CALL]...[/TOOL_CALL]
                    // 图片占位符格式：[[IMAGE_N]]
                    if (bufferContent.contains("[") && !bufferContent.contains("[TOOL_CALL]")) {
                        // 🔧 修复：先检测是否是不完整的图片占位符开始
                        // 图片占位符可能被分成多个token，如 "[[" 或 "[[IMAGE_" 等
                        String[] possibleImageStarts = {"[[IMAGE_", "[[IMAGE", "[[IMAG", "[[IMA", "[[IM", "[[I", "[["};
                        for (String start : possibleImageStarts) {
                            if (bufferContent.endsWith(start)) {
                                // 可能是图片占位符开始，保留在缓冲区
                                int safeEnd = bufferContent.length() - start.length();
                                if (safeEnd > 0) {
                                    String safeContent = bufferContent.substring(0, safeEnd);
                                    streamBuffer.setLength(0);
                                    streamBuffer.append(start);
                                    if (!safeContent.isEmpty()) {
                                        sink.tryEmitNext(safeContent);
                                    }
                                    return; // 等待下一个token完成图片占位符
                                } else {
                                    // 整个缓冲区都是图片占位符开始，继续等待
                                    return;
                                }
                            }
                        }
                        
                        // 排除已完整的图片占位符
                        boolean isImagePlaceholder = bufferContent.contains("[[IMAGE_") 
                                || java.util.regex.Pattern.compile("\\[\\[IMAGE_\\d*\\]?\\]?$").matcher(bufferContent).find();
                        
                        if (!isImagePlaceholder) {
                            // 检查是否可能是工具调用开始的一部分（只有单个[才可能是）
                            if (bufferContent.endsWith("[") && !bufferContent.endsWith("[[")) {
                                // 可能是工具调用开始，暂不输出末尾的[
                                int safeEnd = bufferContent.length() - 1;
                                if (safeEnd > 0) {
                                    String safeContent = bufferContent.substring(0, safeEnd);
                                    streamBuffer.setLength(0);
                                    streamBuffer.append("[");
                                    if (!safeContent.isEmpty()) {
                                        sink.tryEmitNext(safeContent);
                                    }
                                    return; // 等待下一个token判断是工具调用还是其他
                                } else {
                                    return; // 等待更多内容
                                }
                            }
                        }
                    }

                    // 检查是否有完整的图片占位符 [[IMAGE_X]]
                    java.util.regex.Pattern pattern = java.util.regex.Pattern.compile("\\[\\[IMAGE_(\\d+)\\]\\]");
                    java.util.regex.Matcher matcher = pattern.matcher(bufferContent);

                    int lastEnd = 0;
                    StringBuilder outputBuffer = new StringBuilder();

                    while (matcher.find()) {
                        int imageIndex = Integer.parseInt(matcher.group(1));

                        // 输出占位符之前的内容
                        if (matcher.start() > lastEnd) {
                            outputBuffer.append(bufferContent.substring(lastEnd, matcher.start()));
                        }

                        // 替换占位符
                        if (!replacedImageIndexes.contains(imageIndex) && imageHtmlMap.containsKey(imageIndex)) {
                            // 第一次出现，替换为图片HTML
                            outputBuffer.append(imageHtmlMap.get(imageIndex));
                            replacedImageIndexes.add(imageIndex);
                            log.info("✅ 流式替换图片占位符: [[IMAGE_{}]] -> 图片HTML", imageIndex);
                        } else if (replacedImageIndexes.contains(imageIndex)) {
                            // 重复出现，替换为文字引用
                            outputBuffer.append("（见上图 ").append(imageIndex).append("）");
                        } else {
                            // 没有对应的图片，保留原占位符
                            outputBuffer.append(matcher.group());
                        }

                        lastEnd = matcher.end();
                    }

                    // 检查是否有未完成的占位符（如 "[[IMAGE_" 但还没有 "]]"）
                    int incompleteStart = bufferContent.lastIndexOf("[[IMAGE_");
                    if (incompleteStart >= lastEnd && !bufferContent.substring(incompleteStart).contains("]]")) {
                        // 有未完成的占位符，保留在缓冲区
                        if (incompleteStart > lastEnd) {
                            outputBuffer.append(bufferContent.substring(lastEnd, incompleteStart));
                        }
                        streamBuffer.setLength(0);
                        streamBuffer.append(bufferContent.substring(incompleteStart));
                    } else {
                        // 没有未完成的占位符，输出剩余内容
                        if (lastEnd < bufferContent.length()) {
                            outputBuffer.append(bufferContent.substring(lastEnd));
                        }
                        streamBuffer.setLength(0);
                    }

                    // 发送处理后的内容
                    if (outputBuffer.length() > 0) {
                        sink.tryEmitNext(outputBuffer.toString());
                    }
                }

                /** 新版本要求的完整响应方法 */
                @Override
                public void onCompleteResponse(ChatResponse response) {
                    // 安全获取AI响应文本，防止空指针
                    String complete = "";
                    if (response.aiMessage() != null && response.aiMessage().text() != null) {
                        complete = response.aiMessage().text();
                    }

                    chatMemory.add(new UserMessage(userMessage));
                    chatMemory.add(new AiMessage(complete));

                    // 📊 记录Token使用情况
                    try {
                        var tokenUsage = response.tokenUsage();
                        if (tokenUsage != null) {
                            int inputTokens = tokenUsage.inputTokenCount() != null ? tokenUsage.inputTokenCount() : 0;
                            int outputTokens = tokenUsage.outputTokenCount() != null ? tokenUsage.outputTokenCount()
                                    : 0;
                            tokenUsageService.recordUsageAsync(
                                    finalConversationId, agentId,
                                    chatModelConfig.getModelName(),
                                    chatModelConfig.getProvider(),
                                    inputTokens, outputTokens, "chat");
                        }
                    } catch (Exception e) {
                        log.warn("记录Token使用失败: {}", e.getMessage());
                    }

                    // 输出缓冲区中剩余的内容（过滤工具调用标记）
                    if (streamBuffer.length() > 0) {
                        String remaining = streamBuffer.toString();
                        // 过滤掉工具调用标记
                        remaining = java.util.regex.Pattern
                                .compile("\\[TOOL_CALL\\].*?\\[/TOOL_CALL\\]", java.util.regex.Pattern.DOTALL)
                                .matcher(remaining).replaceAll("");
                        if (!remaining.isEmpty()) {
                            sink.tryEmitNext(remaining);
                        }
                        streamBuffer.setLength(0);
                    }

                    // 🔧 检测并执行工具调用
                    ToolCallingService.ToolCallResult toolCallResult = null;
                    if (toolCallingService.hasTools(agentId)) {
                        var toolContext = ToolContext.builder()
                                .agentId(agentId)
                                .conversationId(finalConversationId)
                                .userQuery(userMessage)
                                .build();

                        toolCallResult = toolCallingService.detectAndExecute(complete, toolContext);
                        if (toolCallResult != null && toolCallResult.isHasToolCall()) {
                            log.info("🔧 检测到工具调用: {}", toolCallResult.getToolName());

                            if (toolCallResult.isSuccess()) {
                                // 发送工具执行结果（使用拆分后的ChatResponseBuilder）
                                String toolResultHtml = chatResponseBuilderService.buildToolResultHtml(
                                        toolCallResult.getToolName(),
                                        toolCallResult.getToolResult());
                                sink.tryEmitNext(toolResultHtml);
                                log.info("✅ 工具执行成功: {}", toolCallResult.getToolName());
                            } else {
                                // 工具执行失败
                                String errorHtml = "<div style='color: #f56c6c; margin-top: 10px;'>" +
                                        "<i class='fa-solid fa-exclamation-triangle'></i> " +
                                        "工具执行失败: " + toolCallResult.getErrorMessage() + "</div>";
                                sink.tryEmitNext(errorHtml);
                                log.warn("❌ 工具执行失败: {}", toolCallResult.getErrorMessage());
                            }
                        }
                    }

                    // 保存工具调用结果的引用，供后续保存消息使用
                    final ToolCallingService.ToolCallResult finalToolCallResult = toolCallResult;

                    // 过滤有效的参考来源（根据AI提到的页码匹配）
                    // 过滤有效的参考来源（使用拆分后的ReferenceSourceFilter）
                    // 使用主线程提取的重排分数（rerankScoresForAsync）
                    List<Content> validReferenceSources = null;
                    if (retrievedContents != null && !retrievedContents.isEmpty()) {
                        validReferenceSources = referenceSourceFilterService.filterValidReferenceSources(agent, retrievedContents, complete, rerankScoresForAsync);
                        log.info("📊 最终参考来源数量: {}", validReferenceSources.size());
                    }

                    // 在回答结束后只添加参考来源按钮（图片已在流式过程中替换）
                    // 使用拆分后的ChatResponseBuilder构建参考来源按钮
                    if (validReferenceSources != null && !validReferenceSources.isEmpty()) {
                        // 附加参考资料按钮（简洁样式）
                        String referencesHtml = chatResponseBuilderService.buildReferencesButtons(validReferenceSources, rerankScoresForAsync);
                        if (!referencesHtml.isEmpty()) {
                            sink.tryEmitNext(referencesHtml);
                            log.info("✅ 实时显示 {} 个参考来源按钮", validReferenceSources.size());
                        }
                    }

                    // 保存消息到数据库（使用拆分后的ChatMessagePersistenceService）
                    // 只有非系统打招呼时，才保存用户消息
                    if (!isGreeting) {
                        chatMessagePersistenceService.saveUserMessage(finalConversationId, userMessage);
                    } else {
                        log.info("ℹ️ 系统打招呼，跳过保存用户消息");
                    }

                    // 构建工具结果HTML（如果有）
                    String toolResultHtml = null;
                    if (finalToolCallResult != null && finalToolCallResult.isHasToolCall()
                            && finalToolCallResult.isSuccess()) {
                        toolResultHtml = chatResponseBuilderService.buildToolResultHtml(
                                finalToolCallResult.getToolName(),
                                finalToolCallResult.getToolResult());
                    }

                    // 保存AI响应（自动清理工具调用标记和图片占位符）
                    String cleanedResponse = chatMessagePersistenceService.saveAssistantMessage(
                            finalConversationId, complete, validReferenceSources, toolResultHtml);

                    // 保存对话历史（用于统计）
                    var tokenUsage = response.tokenUsage();
                    int tokensUsed = 0;
                    if (tokenUsage != null) {
                        tokensUsed = (tokenUsage.inputTokenCount() != null ? tokenUsage.inputTokenCount() : 0)
                                + (tokenUsage.outputTokenCount() != null ? tokenUsage.outputTokenCount() : 0);
                    }
                    String referenceSources = validReferenceSources != null && !validReferenceSources.isEmpty()
                            ? chatResponseBuilderService.buildReferenceSourcesJson(validReferenceSources)
                            : null;
                    int retrievalCount = validReferenceSources != null ? validReferenceSources.size() : 0;
                    chatMessagePersistenceService.saveChatHistory(agentId, finalConversationId, userMessage,
                            cleanedResponse, tokensUsed, referenceSources, retrievalCount);

                    // 注销流式会话
                    if (finalConversationId != null) {
                        streamingSessionManager.unregisterSession(finalConversationId);
                    }
                    sink.tryEmitComplete();
                }

                @Override
                public void onError(Throwable error) {
                    log.error("AI 响应错误", error);
                    // 注销流式会话
                    if (finalConversationId != null) {
                        streamingSessionManager.unregisterSession(finalConversationId);
                    }
                    sink.tryEmitError(error);
                }
            });

        } catch (Exception e) {
            log.error("对话处理失败", e);
            sink.tryEmitNext("对话处理失败：" + e.getMessage());
            // 注销流式会话
            if (finalConversationId != null) {
                streamingSessionManager.unregisterSession(finalConversationId);
            }
            sink.tryEmitComplete();
        }

        return sink.asFlux();
    }

    // ========== 以下方法已移至拆分后的服务类，保留shouldTriggerWorkflow ==========

    /**
     * 判断是否应该触发工作流
     *
     * <p>
     * 根据智能体配置的触发模式判断：
     * <ul>
     * <li>auto模式：每次对话都触发</li>
     * <li>keyword模式：消息包含特定关键词时触发</li>
     * <li>manual模式：用户发送 /run 或 /workflow 命令时触发</li>
     * </ul>
     * </p>
     *
     * @param agent       智能体配置
     * @param userMessage 用户消息
     * @return 是否应该触发工作流
     */
    private boolean shouldTriggerWorkflow(Agent agent, String userMessage) {
        String triggerMode = agent.getWorkflowTriggerMode();
        if (triggerMode == null) {
            triggerMode = "manual";
        }

        switch (triggerMode) {
            case "auto":
                // 自动模式：每次对话都触发
                return true;

            case "keyword":
                // 关键词模式：消息包含特定关键词时触发
                String keywordsJson = agent.getWorkflowTriggerKeywords();
                if (keywordsJson != null && !keywordsJson.isEmpty()) {
                    try {
                        ObjectMapper mapper = new ObjectMapper();
                        List<String> keywords = mapper.readValue(keywordsJson,
                                mapper.getTypeFactory().constructCollectionType(List.class, String.class));
                        String lowerMessage = userMessage.toLowerCase();
                        for (String keyword : keywords) {
                            if (lowerMessage.contains(keyword.toLowerCase())) {
                                log.info("🔑 关键词触发工作流: {}", keyword);
                                return true;
                            }
                        }
                    } catch (Exception e) {
                        log.error("解析工作流触发关键词失败: {}", e.getMessage());
                    }
                }
                return false;

            case "manual":
            default:
                // 手动模式：用户发送 /run 或 /workflow 命令时触发
                return userMessage.trim().startsWith("/run") || userMessage.trim().startsWith("/workflow");
        }
    }

    /**
     * 判断模型是否支持图片（视觉模型）
     *
     * @param modelName 模型名称
     * @return 是否支持图片
     */
    private boolean isVisionModel(String modelName) {
        if (modelName == null) return false;
        String lower = modelName.toLowerCase();
        return lower.contains("-vl") || lower.contains("vl-") || lower.contains("vision") || lower.contains("gpt-4o");
    }
}
