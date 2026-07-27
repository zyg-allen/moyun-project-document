package com.moyun.agent.engine.workflow.node;

import com.moyun.agent.entity.Agent;
import com.moyun.agent.entity.ModelConfig;
import com.moyun.agent.service.AgentService;
import com.moyun.agent.service.ModelConfigService;
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

/**
 * 智能体节点执行器
 * 
 * <p>调用已配置的智能体处理输入</p>
 * <p>线程安全：无状态，继承BaseNodeExecutor</p>
 *
 * @author laomao
 * @since 2025-11-30
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class AgentNodeExecutor extends BaseNodeExecutor {

    private final AgentService agentService;
    private final ModelConfigService modelConfigService;

    @Override
    public String getType() {
        return "agent";
    }

    @Override
    public NodeResult execute(WorkflowNode node, WorkflowContext context) {
        Map<String, Object> config = node.getConfig();
        if (config == null) {
            return NodeResult.fail("Agent节点配置为空");
        }

        try {
            // 获取配置
            Object agentIdObj = config.get("agentId");
            if (agentIdObj == null) {
                return NodeResult.fail("未选择智能体");
            }

            Long agentId;
            if (agentIdObj instanceof Number) {
                agentId = ((Number) agentIdObj).longValue();
            } else {
                agentId = Long.parseLong(agentIdObj.toString());
            }

            String userPrompt = (String) config.getOrDefault("userPrompt", "{{input}}");
            String outputVariable = (String) config.getOrDefault("outputVariable", "agent_output");

            // 替换变量
            userPrompt = replaceVariables(userPrompt, context);

            log.info("🤖 Agent节点执行: agentId={}, prompt={}", agentId,
                    userPrompt.length() > 100 ? userPrompt.substring(0, 100) + "..." : userPrompt);

            // 获取智能体
            Agent agent = agentService.getById(agentId);
            if (agent == null) {
                return NodeResult.fail("智能体不存在: " + agentId);
            }

            // 获取默认模型配置
            ModelConfig defaultConfig = modelConfigService.getDefaultChatConfig();
            if (defaultConfig == null) {
                return NodeResult.fail("无法获取默认LLM模型配置");
            }

            // 创建模型
            ChatLanguageModel chatModel = modelConfigService.createChatModel(defaultConfig.getId());
            if (chatModel == null) {
                return NodeResult.fail("无法创建LLM模型");
            }

            // 构建消息
            List<ChatMessage> messages = new ArrayList<>();
            if (agent.getSystemPrompt() != null && !agent.getSystemPrompt().isEmpty()) {
                messages.add(new SystemMessage(agent.getSystemPrompt()));
            }
            messages.add(new UserMessage(userPrompt));

            // 调用LLM
            ChatResponse chatResponse = chatModel.chat(messages);
            String response = chatResponse.aiMessage().text();

            log.info("🤖 Agent响应: {}", response.length() > 200 ? response.substring(0, 200) + "..." : response);

            // 保存到变量
            context.setVariable(outputVariable, response);

            return NodeResult.success(response);

        } catch (Exception e) {
            log.error("Agent节点执行失败", e);
            return NodeResult.fail("Agent调用失败: " + e.getMessage());
        }
    }

    // replaceVariables方法已移至BaseNodeExecutor
}
