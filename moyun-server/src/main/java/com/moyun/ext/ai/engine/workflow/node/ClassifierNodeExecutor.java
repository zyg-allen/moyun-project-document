package com.moyun.ext.ai.engine.workflow.node;

import com.moyun.ext.ai.entity.ModelConfig;
import com.moyun.ext.ai.service.ModelConfigService;
import com.moyun.ext.ai.service.TokenUsageService;
import com.moyun.ext.ai.engine.workflow.NodeExecutor;
import com.moyun.ext.ai.engine.workflow.WorkflowContext;
import com.moyun.ext.ai.engine.workflow.WorkflowNode;
import dev.langchain4j.data.message.ChatMessage;
import dev.langchain4j.data.message.SystemMessage;
import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.model.chat.ChatLanguageModel;
import dev.langchain4j.model.chat.response.ChatResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.regex.Pattern;

/**
 * 意图分类节点执行器
 *
 * <p>使用LLM对输入进行意图分类，支持多分类</p>
 *
 * @author laomao
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ClassifierNodeExecutor implements NodeExecutor {

    private final ModelConfigService modelConfigService;
    private final TokenUsageService tokenUsageService;
    private static final Pattern VARIABLE_PATTERN = Pattern.compile("\\{\\{\\s*(\\w+)\\s*\\}\\}");

    @Override
    public String getType() {
        return "classifier";
    }

    @Override
    public NodeResult execute(WorkflowNode node, WorkflowContext context) {
        Map<String, Object> config = node.getConfig();
        if (config == null) {
            return NodeResult.fail("分类节点配置为空");
        }

        try {
            String inputVar = (String) config.getOrDefault("inputVariable", "input");
            @SuppressWarnings("unchecked")
            List<?> categoriesRaw = (List<?>) config.get("categories");
            String outputVariable = (String) config.getOrDefault("outputVariable", "category");

            if (categoriesRaw == null || categoriesRaw.isEmpty()) {
                return NodeResult.fail("未定义分类类别");
            }

            Object inputValue = context.getVariable(inputVar);
            String input = inputValue != null ? inputValue.toString() : "";

            log.info("🏷️ 意图分类: input={}, categories={}",
                    input.length() > 50 ? input.substring(0, 50) + "..." : input,
                    categoriesRaw.size());

            // 构建分类提示词
            StringBuilder categoryList = new StringBuilder();
            for (int i = 0; i < categoriesRaw.size(); i++) {
                Object catObj = categoriesRaw.get(i);
                String catName, catDesc = "";
                
                if (catObj instanceof Map) {
                    @SuppressWarnings("unchecked")
                    Map<String, Object> cat = (Map<String, Object>) catObj;
                    catName = String.valueOf(cat.get("name"));
                    catDesc = String.valueOf(cat.getOrDefault("description", ""));
                } else {
                    // 简单字符串格式
                    catName = catObj.toString();
                }
                
                categoryList.append(i + 1).append(". ").append(catName);
                if (!catDesc.isEmpty() && !"null".equals(catDesc)) {
                    categoryList.append(": ").append(catDesc);
                }
                categoryList.append("\n");
            }

            String systemPrompt = "你是一个意图分类助手。请根据用户输入，从以下类别中选择最匹配的一个。\n\n" +
                    "可选类别:\n" + categoryList + "\n" +
                    "只需要回复类别名称，不要回复其他内容。如果无法分类，回复 'unknown'。";

            // 获取模型
            ModelConfig defaultConfig = modelConfigService.getDefaultChatConfig();
            if (defaultConfig == null) {
                return NodeResult.fail("无法获取默认LLM模型配置");
            }

            ChatLanguageModel chatModel = modelConfigService.createChatModel(defaultConfig.getId());
            if (chatModel == null) {
                return NodeResult.fail("无法创建LLM模型");
            }

            // 调用LLM分类
            List<ChatMessage> messages = new ArrayList<>();
            messages.add(new SystemMessage(systemPrompt));
            messages.add(new UserMessage(input));

            ChatResponse chatResponse = chatModel.chat(messages);
            String category = chatResponse.aiMessage().text().trim();

            // 统计Token使用
            int inputTokens = 0;
            int outputTokens = 0;
            if (chatResponse.metadata() != null && chatResponse.metadata().tokenUsage() != null) {
                inputTokens = chatResponse.metadata().tokenUsage().inputTokenCount();
                outputTokens = chatResponse.metadata().tokenUsage().outputTokenCount();
            } else {
                inputTokens = tokenUsageService.estimateTokens(systemPrompt + input);
                outputTokens = tokenUsageService.estimateTokens(category);
            }

            tokenUsageService.recordWorkflowUsageAsync(
                    context.getWorkflowId(),
                    context.getExecutionId(),
                    node.getId(),
                    defaultConfig.getModelName(),
                    defaultConfig.getProvider(),
                    inputTokens,
                    outputTokens,
                    "workflow_classifier"
            );

            // 验证分类结果
            String matchedCategory = "unknown";
            for (Object catObj : categoriesRaw) {
                String catName;
                if (catObj instanceof Map) {
                    @SuppressWarnings("unchecked")
                    Map<String, Object> cat = (Map<String, Object>) catObj;
                    catName = String.valueOf(cat.get("name"));
                } else {
                    catName = catObj.toString();
                }
                
                if (catName.equalsIgnoreCase(category)) {
                    matchedCategory = catName;
                    break;
                }
            }

            log.info("🏷️ 分类结果: {}", matchedCategory);

            context.setVariable(outputVariable, matchedCategory);

            // 返回对应分支的句柄
            return NodeResult.success(matchedCategory, matchedCategory);

        } catch (Exception e) {
            log.error("意图分类失败", e);
            return NodeResult.fail("意图分类失败: " + e.getMessage());
        }
    }
}
