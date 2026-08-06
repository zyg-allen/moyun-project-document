package com.moyun.ext.ai.engine.workflow.node;

import com.moyun.ext.ai.entity.ModelConfig;
import com.moyun.ext.ai.service.ModelConfigService;
import com.moyun.ext.ai.service.TokenUsageService;
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

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * LLM 节点执行器
 *
 * <p>调用大语言模型处理输入，支持变量替换和多轮Token统计</p>
 *
 * <p>线程安全：无状态，Spring单例安全</p>
 *
 * @author laomao
 * @since 2025-11-30
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class LLMNodeExecutor extends BaseNodeExecutor {

    private final ModelConfigService modelConfigService;
    private final TokenUsageService tokenUsageService;

    @Override
    public String getType() {
        return "llm";
    }

    @Override
    public NodeResult execute(WorkflowNode node, WorkflowContext context) {
        Map<String, Object> config = node.getConfig();
        if (config == null) {
            return NodeResult.fail("LLM节点配置为空");
        }

        try {
            // 获取配置（使用基类的安全获取方法）
            // 注意：前端默认 temperature=0.7、maxTokens=2000，这里作为节点级覆盖参数传入模型创建方法
            // 若前端未配置或使用旧数据，则使用模型默认配置
            String modelId = getStringConfig(config, "modelId", "default");
            String systemPrompt = getStringConfig(config, "systemPrompt", "");
            String userPrompt = getStringConfig(config, "userPrompt", "{{input}}");
            String outputVariable = getStringConfig(config, "outputVariable", "llm_output");
            Double temperature = getDoubleConfig(config, "temperature", null);
            Integer maxTokens = getIntConfig(config, "maxTokens", null);

            // 替换变量
            systemPrompt = replaceVariables(systemPrompt, context);
            userPrompt = replaceVariables(userPrompt, context);

            log.info("🤖 LLM节点执行: model={}, temperature={}, maxTokens={}, userPrompt={}", modelId,
                    temperature, maxTokens,
                    userPrompt.length() > 100 ? userPrompt.substring(0, 100) + "..." : userPrompt);

            // 获取模型配置ID
            Long configId = null;
            if (modelId != null && !modelId.isEmpty() && !"default".equals(modelId)) {
                try {
                    configId = Long.parseLong(modelId);
                } catch (NumberFormatException e) {
                    log.warn("无效的模型ID: {}, 使用默认模型", modelId);
                }
            }
            if (configId == null) {
                // 使用默认模型
                ModelConfig defaultConfig = modelConfigService.getDefaultChatConfig();
                if (defaultConfig != null) {
                    configId = defaultConfig.getId();
                }
            }

            if (configId == null) {
                return NodeResult.fail("无法获取LLM模型配置");
            }

            // 创建模型（传入节点级参数覆盖：temperature/maxTokens 优先于模型默认配置）
            ChatLanguageModel chatModel = modelConfigService.createChatModel(configId, temperature, maxTokens);
            if (chatModel == null) {
                return NodeResult.fail("无法创建LLM模型");
            }

            // 构建消息
            List<ChatMessage> messages = new ArrayList<>();
            if (systemPrompt != null && !systemPrompt.isEmpty()) {
                messages.add(new SystemMessage(systemPrompt));
            }
            messages.add(new UserMessage(userPrompt));

            // 调用LLM
            ChatResponse chatResponse = chatModel.chat(messages);
            String response = chatResponse.aiMessage().text();

            // 统计Token使用
            int inputTokens = 0;
            int outputTokens = 0;
            if (chatResponse.metadata() != null && chatResponse.metadata().tokenUsage() != null) {
                inputTokens = chatResponse.metadata().tokenUsage().inputTokenCount();
                outputTokens = chatResponse.metadata().tokenUsage().outputTokenCount();
            } else {
                // 估算token数
                inputTokens = tokenUsageService.estimateTokens(systemPrompt + userPrompt);
                outputTokens = tokenUsageService.estimateTokens(response);
            }

            // 获取模型信息并记录Token使用
            ModelConfig modelConfig = modelConfigService.getById(configId);
            String modelName = modelConfig != null ? modelConfig.getModelName() : "unknown";
            String modelProvider = modelConfig != null ? modelConfig.getProvider() : "unknown";

            tokenUsageService.recordWorkflowUsageAsync(
                    context.getWorkflowId(),
                    context.getExecutionId(),
                    node.getId(),
                    modelName,
                    modelProvider,
                    inputTokens,
                    outputTokens,
                    "workflow_llm"
            );

            log.info("🤖 LLM响应: tokens(in={}, out={}), {}", inputTokens, outputTokens,
                    response.length() > 200 ? response.substring(0, 200) + "..." : response);

            // 保存到变量
            context.setVariable(outputVariable, response);

            return NodeResult.success(response);

        } catch (Exception e) {
            log.error("LLM节点执行失败", e);
            return NodeResult.fail("LLM调用失败: " + e.getMessage());
        }
    }

    // replaceVariables方法已移至BaseNodeExecutor，使用TemplateUtils统一处理
}
