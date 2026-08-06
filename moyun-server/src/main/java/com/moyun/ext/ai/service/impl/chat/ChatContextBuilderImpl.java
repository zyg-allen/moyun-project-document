package com.moyun.ext.ai.service.impl.chat;

import com.moyun.ext.ai.entity.Agent;
import com.moyun.ext.ai.service.chat.ChatContextBuilderService;
import dev.langchain4j.rag.content.Content;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 对话上下文构建服务实现类
 *
 * <p>负责构建对话过程中的各种上下文，包括：
 * <ul>
 *   <li>系统提示词构建</li>
 *   <li>RAG检索上下文构建</li>
 *   <li>用户消息处理</li>
 * </ul>
 * </p>
 *
 * @author laomao
 * @since 2025-12-11
 */
@Slf4j
@Service
public class ChatContextBuilderImpl implements ChatContextBuilderService {

    /**
     * 构建系统提示词
     *
     * @param agent      智能体配置
     * @param hasTools   是否配置了工具
     * @param toolPrompt 工具提示词（如果有）
     * @return 完整的系统提示词
     */
    @Override
    public String buildSystemPrompt(Agent agent, boolean hasTools, String toolPrompt) {
        StringBuilder systemPromptBuilder = new StringBuilder();

        // 添加基础身份介绍
        systemPromptBuilder.append("你是").append(agent.getName()).append("，");
        if (agent.getDescription() != null && !agent.getDescription().isEmpty()) {
            systemPromptBuilder.append(agent.getDescription());
        } else {
            systemPromptBuilder.append("一个专业的AI助手");
        }
        systemPromptBuilder.append("。\n\n");

        // 添加用户自定义的系统提示词
        if (agent.getSystemPrompt() != null && !agent.getSystemPrompt().isEmpty()) {
            systemPromptBuilder.append(agent.getSystemPrompt()).append("\n\n");
        }

        // 添加工具说明（工具优先级最高）
        if (hasTools && toolPrompt != null && !toolPrompt.isEmpty()) {
            systemPromptBuilder.append(toolPrompt);
            log.info("✅ 智能体配置了工具，已添加工具说明到系统提示词");
        }

        // 添加知识库使用规则（如果配置了知识库）
        boolean hasKnowledge = hasKnowledgeConfig(agent);
        if (hasKnowledge) {
            appendKnowledgeRules(systemPromptBuilder, hasTools);
        }

        return systemPromptBuilder.toString();
    }

    /**
     * 检查智能体是否配置了知识库
     */
    private boolean hasKnowledgeConfig(Agent agent) {
        return (agent.getKnowledgeLibraryIds() != null && !agent.getKnowledgeLibraryIds().isEmpty())
                || (agent.getKnowledgeBaseIds() != null && !agent.getKnowledgeBaseIds().isEmpty());
    }

    /**
     * 添加知识库使用规则
     */
    private void appendKnowledgeRules(StringBuilder builder, boolean hasTools) {
        builder.append("\n【知识库使用规则】\n");
        builder.append("1. 📚 回答专业问题时：\n");
        builder.append("   - 优先使用知识库中的参考资料\n");
        builder.append("   - 引用时注明来源（文件名、页码）\n");
        builder.append("   - 不使用知识库外的专业知识\n\n");
        builder.append("2. 💬 日常交流时（如问候、自我介绍）：\n");
        builder.append("   - 可以自然地介绍自己，说明专业领域\n");
        builder.append("   - 友好地引导用户提问专业问题\n\n");

        if (hasTools) {
            builder.append("3. 🔧 当用户需要实时信息（如天气、时间）时：\n");
            builder.append("   - 优先使用可用工具获取信息\n");
            builder.append("   - 工具调用优先于知识库规则\n\n");
            builder.append("4. ⚠️ 当专业问题没有知识库资料支持且无可用工具时：\n");
        } else {
            builder.append("3. ⚠️ 当专业问题没有知识库资料支持时：\n");
        }
        builder.append("   - 明确告知：\"抱歉，这个问题不在我的知识库范围内\"\n");
        builder.append("   - 不编造答案，不使用知识库外的专业信息\n");
    }

    /**
     * 构建RAG检索上下文
     *
     * @param contents 检索到的内容列表
     * @return RAG上下文构建结果
     */
    @Override
    public RagContextResult buildRagContext(List<Content> contents) {
        if (contents == null || contents.isEmpty()) {
            return new RagContextResult("", 0);
        }

        StringBuilder contextBuilder = new StringBuilder();
        int imageIndex = 1;

        log.info("📋 发送给AI的参考资料列表:");

        for (int i = 0; i < contents.size(); i++) {
            Content content = contents.get(i);
            var metadata = content.textSegment().metadata();
            String type = metadata != null ? metadata.getString("type") : "text";

            // 简洁的来源标注
            if (metadata != null) {
                String fileName = metadata.getString("fileName");
                String pageNumber = metadata.getString("pageNumber");
                String segmentIndex = metadata.getString("segmentIndex");

                // 日志记录
                log.info("  📄 参考资料{}: 文件={}, 页码={}, 分片索引={}, 类型={}",
                        i + 1, fileName, pageNumber, segmentIndex, type);

                // 简洁标注：[第X页]
                if (pageNumber != null) {
                    contextBuilder.append("[第").append(pageNumber).append("页]\n");
                }
            }

            // 如果是图片，简洁标注
            if ("image".equals(type)) {
                contextBuilder.append("[图片").append(imageIndex).append("] ");
                contextBuilder.append(content.textSegment().text()).append("\n");
                contextBuilder.append("(引用图片请写: [[IMAGE_").append(imageIndex).append("]])\n");
                imageIndex++;
            } else {
                contextBuilder.append(content.textSegment().text());
            }
            contextBuilder.append("\n\n");
        }

        return new RagContextResult(contextBuilder.toString(), imageIndex - 1);
    }

    /**
     * 构建处理后的用户消息
     *
     * @param ragContext      RAG上下文（可为null）
     * @param userMessage     原始用户消息
     * @param workflowContext 工作流上下文（可为null）
     * @param hasRagContent   是否有RAG内容
     * @return 处理后的用户消息
     */
    @Override
    public String buildProcessedUserMessage(String ragContext, String userMessage,
                                             String workflowContext, boolean hasRagContent) {
        String processedMessage = userMessage;

        // 如果有RAG内容，构建完整的提问格式
        if (hasRagContent && ragContext != null && !ragContext.isEmpty()) {
            processedMessage = buildRagEnhancedMessage(ragContext, userMessage);
        }

        // 如果有工作流上下文，添加到消息中
        if (workflowContext != null && !workflowContext.isEmpty()) {
            processedMessage = "【工作流预处理结果】\n" + workflowContext + 
                              "\n\n【用户原始问题】\n" + processedMessage;
            log.info("✅ 已将工作流结果作为上下文添加到用户消息");
        }

        return processedMessage;
    }

    /**
     * 构建RAG增强的消息
     */
    private String buildRagEnhancedMessage(String ragContext, String userMessage) {
        StringBuilder builder = new StringBuilder();
        builder.append("【背景知识】\n")
                .append(ragContext)
                .append("\n\n【用户问题】\n").append(userMessage)
                .append("\n\n【回答要求】\n")
                .append("1. 专业简洁：用专业术语准确回答，避免口语化\n")
                .append("2. 直接回答：不要提及来源、页码、文档等，就像你本身就知道这些知识一样\n")
                .append("3. 结构清晰：可用分点或分段组织\n");

        log.info("已将知识库内容添加到用户消息中");
        return builder.toString();
    }

    /**
     * 添加图片引用指令到消息（内部辅助方法）
     *
     * @param message    原始消息
     * @param imageCount 图片数量
     * @return 添加图片指令后的消息
     */
    private String appendImageInstruction(String message, int imageCount) {
        if (imageCount > 0) {
            log.info("✅ 已发送 {} 张图片给AI，添加图片引用指令", imageCount);
            return message + "4. 图片引用：如有相关图片写 [[IMAGE_1]]、[[IMAGE_2]] 等";
        }
        return message;
    }

    /**
     * 构建无RAG内容时的提示消息
     *
     * @param userMessage 用户消息
     * @return 提示消息
     */
    @Override
    public String buildNoRagContentMessage(String userMessage) {
        log.info("ℹ️ 重排后未找到相关内容，无知识库资料支持");
        log.info("💡 这通常发生在用户问候语或与知识库完全无关的问题");
        log.info(">>> AI将根据用户意图智能回答");

        return "【提示】本次对话未从知识库中检索到相关内容。\n\n"
                + "请根据用户意图智能回答：\n"
                + "1️⃣ 如果是问候、自我介绍询问（如\"你好\"、\"你是谁\"）：\n"
                + "   → 友好地介绍自己，说明专业领域，引导提问\n\n"
                + "2️⃣ 如果是专业问题但知识库无资料：\n"
                + "   → 明确告知：\"抱歉，这个问题不在我的知识库范围内\"\n"
                + "   → 不编造答案，不使用知识库外的专业知识\n\n"
                + "【用户消息】\n" + userMessage;
    }
}
