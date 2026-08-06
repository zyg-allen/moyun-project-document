package com.moyun.ext.ai.engine.workflow.node;

import com.fasterxml.jackson.core.type.TypeReference;
import com.moyun.ext.ai.util.JsonUtils;
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
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 参数提取节点执行器
 *
 * <p>使用LLM从文本中提取结构化参数</p>
 *
 * @author laomao
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ExtractorNodeExecutor implements NodeExecutor {

    private final ModelConfigService modelConfigService;
    private final TokenUsageService tokenUsageService;
    private static final Pattern JSON_PATTERN = Pattern.compile("\\{[^{}]*\\}|\\[[^\\[\\]]*\\]", Pattern.DOTALL);

    @Override
    public String getType() {
        return "extractor";
    }

    @Override
    public NodeResult execute(WorkflowNode node, WorkflowContext context) {
        Map<String, Object> config = node.getConfig();
        if (config == null) {
            return NodeResult.fail("提取节点配置为空");
        }

        try {
            String inputVar = (String) config.getOrDefault("inputVariable", "input");
            @SuppressWarnings("unchecked")
            List<Map<String, Object>> fields = (List<Map<String, Object>>) config.get("fields");
            String outputVariable = (String) config.getOrDefault("outputVariable", "extracted");
            String instruction = (String) config.getOrDefault("instruction", "");

            if (fields == null || fields.isEmpty()) {
                return NodeResult.fail("未定义提取字段");
            }

            Object inputValue = context.getVariable(inputVar);
            String input = inputValue != null ? inputValue.toString() : "";

            log.info("🔍 参数提取: input length={}, fields={}", input.length(), fields.size());

            // 构建字段描述
            StringBuilder fieldDesc = new StringBuilder();
            fieldDesc.append("{\n");
            for (int i = 0; i < fields.size(); i++) {
                Map<String, Object> field = fields.get(i);
                String name = (String) field.get("name");
                String type = (String) field.getOrDefault("type", "string");
                String desc = (String) field.getOrDefault("description", "");
                boolean required = Boolean.TRUE.equals(field.get("required"));

                fieldDesc.append("  \"").append(name).append("\": ");
                fieldDesc.append("<").append(type);
                if (required) fieldDesc.append(", 必填");
                if (!desc.isEmpty()) fieldDesc.append(", ").append(desc);
                fieldDesc.append(">");
                if (i < fields.size() - 1) fieldDesc.append(",");
                fieldDesc.append("\n");
            }
            fieldDesc.append("}");

            String systemPrompt = "你是一个信息提取助手。请从用户提供的文本中提取以下信息，并以JSON格式返回。\n\n" +
                    "需要提取的字段:\n" + fieldDesc + "\n\n" +
                    "注意事项:\n" +
                    "- 如果某个字段无法从文本中提取，设为null\n" +
                    "- 严格按照指定的字段名返回\n" +
                    "- 只返回JSON，不要返回其他内容\n";

            if (!instruction.isEmpty()) {
                systemPrompt += "\n额外说明: " + instruction;
            }

            // 获取模型
            ModelConfig defaultConfig = modelConfigService.getDefaultChatConfig();
            if (defaultConfig == null) {
                return NodeResult.fail("无法获取默认LLM模型配置");
            }

            ChatLanguageModel chatModel = modelConfigService.createChatModel(defaultConfig.getId());
            if (chatModel == null) {
                return NodeResult.fail("无法创建LLM模型");
            }

            // 调用LLM提取
            List<ChatMessage> messages = new ArrayList<>();
            messages.add(new SystemMessage(systemPrompt));
            messages.add(new UserMessage("请从以下文本中提取信息:\n\n" + input));

            ChatResponse chatResponse = chatModel.chat(messages);
            String responseText = chatResponse.aiMessage().text().trim();

            // 统计Token使用
            int inputTokens = 0;
            int outputTokens = 0;
            if (chatResponse.metadata() != null && chatResponse.metadata().tokenUsage() != null) {
                inputTokens = chatResponse.metadata().tokenUsage().inputTokenCount();
                outputTokens = chatResponse.metadata().tokenUsage().outputTokenCount();
            } else {
                inputTokens = tokenUsageService.estimateTokens(systemPrompt + input);
                outputTokens = tokenUsageService.estimateTokens(responseText);
            }

            tokenUsageService.recordWorkflowUsageAsync(
                    context.getWorkflowId(),
                    context.getExecutionId(),
                    node.getId(),
                    defaultConfig.getModelName(),
                    defaultConfig.getProvider(),
                    inputTokens,
                    outputTokens,
                    "workflow_extractor"
            );

            // 解析JSON
            Map<String, Object> extracted = parseJsonResponse(responseText);

            log.info("🔍 提取结果: {}", extracted);

            // 保存提取结果
            context.setVariable(outputVariable, extracted);

            // 将每个字段也单独保存为变量
            for (Map.Entry<String, Object> entry : extracted.entrySet()) {
                context.setVariable(outputVariable + "_" + entry.getKey(), entry.getValue());
            }

            return NodeResult.success(extracted);

        } catch (Exception e) {
            log.error("参数提取失败", e);
            return NodeResult.fail("参数提取失败: " + e.getMessage());
        }
    }

    /**
     * 解析JSON响应
     */
    private Map<String, Object> parseJsonResponse(String response) {
        // 尝试直接解析
        Map<String, Object> result = JsonUtils.fromJson(response, new TypeReference<Map<String, Object>>() {});
        if (result != null) {
            return result;
        }
        
        // 尝试提取JSON部分
        Matcher matcher = JSON_PATTERN.matcher(response);
        while (matcher.find()) {
            String json = matcher.group();
            if (json.startsWith("{")) {
                result = JsonUtils.fromJson(json, new TypeReference<Map<String, Object>>() {});
                if (result != null) {
                    return result;
                }
            }
        }
        
        log.warn("无法解析JSON响应: {}", response);
        return new HashMap<>();
    }
}
