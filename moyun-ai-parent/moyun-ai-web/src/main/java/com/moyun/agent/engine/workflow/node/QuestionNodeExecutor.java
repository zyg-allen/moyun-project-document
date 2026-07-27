package com.moyun.agent.engine.workflow.node;

import com.moyun.agent.entity.ModelConfig;
import com.moyun.agent.service.ModelConfigService;
import com.moyun.agent.service.TokenUsageService;
import com.moyun.agent.engine.workflow.NodeExecutor;
import com.moyun.agent.engine.workflow.WorkflowContext;
import com.moyun.agent.engine.workflow.WorkflowNode;
import dev.langchain4j.data.message.ChatMessage;
import dev.langchain4j.data.message.SystemMessage;
import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.model.chat.ChatLanguageModel;
import dev.langchain4j.model.chat.response.ChatResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

/**
 * 问答生成节点执行器
 *
 * <p>基于上下文自动生成问答对</p>
 *
 * @author laomao
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class QuestionNodeExecutor implements NodeExecutor {

    private final ModelConfigService modelConfigService;
    private final TokenUsageService tokenUsageService;
    private static final Pattern VARIABLE_PATTERN = Pattern.compile("\\{\\{\\s*(\\w+)\\s*\\}\\}");

    @Override
    public String getType() {
        return "question";
    }

    @Override
    public NodeResult execute(WorkflowNode node, WorkflowContext context) {
        Map<String, Object> config = node.getConfig();
        if (config == null) {
            return NodeResult.fail("问答节点配置为空");
        }

        try {
            String mode = (String) config.getOrDefault("mode", "answer"); // answer, generate
            String contextVar = (String) config.getOrDefault("contextVariable", "knowledge_result");
            String questionVar = (String) config.getOrDefault("questionVariable", "input");
            String outputVariable = (String) config.getOrDefault("outputVariable", "answer");
            int questionCount = config.containsKey("questionCount") ? ((Number) config.get("questionCount")).intValue() : 5;

            String knowledgeContext = getVariableAsString(context, contextVar);
            String question = getVariableAsString(context, questionVar);

            log.info("❓ 问答节点: mode={}, context length={}", mode, knowledgeContext.length());

            // 获取模型
            ModelConfig defaultConfig = modelConfigService.getDefaultChatConfig();
            if (defaultConfig == null) {
                return NodeResult.fail("无法获取默认LLM模型配置");
            }

            ChatLanguageModel chatModel = modelConfigService.createChatModel(defaultConfig.getId());
            if (chatModel == null) {
                return NodeResult.fail("无法创建LLM模型");
            }

            String result;

            int inputTokens = 0;
            int outputTokens = 0;

            if ("generate".equals(mode)) {
                // 生成问题模式
                String systemPrompt = "你是一个问题生成助手。请根据提供的文本内容，生成 " + questionCount + " 个有价值的问题。";
                ChatResponse response = generateQuestionsWithResponse(chatModel, knowledgeContext, questionCount);
                result = response.aiMessage().text();

                if (response.metadata() != null && response.metadata().tokenUsage() != null) {
                    inputTokens = response.metadata().tokenUsage().inputTokenCount();
                    outputTokens = response.metadata().tokenUsage().outputTokenCount();
                } else {
                    inputTokens = tokenUsageService.estimateTokens(systemPrompt + knowledgeContext);
                    outputTokens = tokenUsageService.estimateTokens(result);
                }
                context.setVariable(outputVariable, result);
            } else {
                // 回答问题模式（默认）
                String systemPrompt = "你是一个智能问答助手。请根据提供的上下文信息回答用户问题。";
                ChatResponse response = answerQuestionWithResponse(chatModel, knowledgeContext, question);
                result = response.aiMessage().text();

                if (response.metadata() != null && response.metadata().tokenUsage() != null) {
                    inputTokens = response.metadata().tokenUsage().inputTokenCount();
                    outputTokens = response.metadata().tokenUsage().outputTokenCount();
                } else {
                    inputTokens = tokenUsageService.estimateTokens(systemPrompt + knowledgeContext + question);
                    outputTokens = tokenUsageService.estimateTokens(result);
                }
                context.setVariable(outputVariable, result);
            }

            // 记录Token使用
            tokenUsageService.recordWorkflowUsageAsync(
                    context.getWorkflowId(),
                    context.getExecutionId(),
                    node.getId(),
                    defaultConfig.getModelName(),
                    defaultConfig.getProvider(),
                    inputTokens,
                    outputTokens,
                    "workflow_question"
            );

            return NodeResult.success(result);

        } catch (Exception e) {
            log.error("问答节点执行失败", e);
            return NodeResult.fail("问答节点执行失败: " + e.getMessage());
        }
    }

    /**
     * 回答问题（返回ChatResponse以获取token统计）
     */
    private ChatResponse answerQuestionWithResponse(ChatLanguageModel chatModel, String context, String question) {
        String systemPrompt = "你是一个智能问答助手。请根据提供的上下文信息回答用户问题。\n\n" +
                "注意事项:\n" +
                "- 只使用上下文中的信息来回答\n" +
                "- 如果上下文中没有相关信息，请明确说明\n" +
                "- 回答要准确、简洁、有条理\n\n" +
                "上下文信息:\n" + context;

        List<ChatMessage> messages = new ArrayList<>();
        messages.add(new SystemMessage(systemPrompt));
        messages.add(new UserMessage(question));

        return chatModel.chat(messages);
    }

    /**
     * 生成问题（返回ChatResponse以获取token统计）
     */
    private ChatResponse generateQuestionsWithResponse(ChatLanguageModel chatModel, String context, int count) {
        String systemPrompt = "你是一个问题生成助手。请根据提供的文本内容，生成 " + count + " 个有价值的问题。\n\n" +
                "要求:\n" +
                "- 问题应该能够从文本中找到答案\n" +
                "- 问题类型要多样化（是什么、为什么、如何等）\n" +
                "- 每个问题单独一行\n" +
                "- 只输出问题，不要输出答案\n";

        List<ChatMessage> messages = new ArrayList<>();
        messages.add(new SystemMessage(systemPrompt));
        messages.add(new UserMessage("请根据以下文本生成问题:\n\n" + context));

        return chatModel.chat(messages);
    }

    private String getVariableAsString(WorkflowContext context, String varName) {
        Object value = context.getVariable(varName);
        return value != null ? value.toString() : "";
    }
}
