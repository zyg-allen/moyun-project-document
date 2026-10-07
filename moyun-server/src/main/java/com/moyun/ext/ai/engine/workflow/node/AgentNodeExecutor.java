package com.moyun.ext.ai.engine.workflow.node;

import com.moyun.ext.ai.entity.Agent;
import com.moyun.ext.ai.entity.ModelConfig;
import com.moyun.ext.ai.service.AgentService;
import com.moyun.ext.ai.service.ModelConfigService;
import com.moyun.ext.ai.engine.workflow.WorkflowContext;
import com.moyun.ext.ai.engine.workflow.WorkflowNode;
import com.moyun.ext.aigateway.constant.AiErrorCodes;
import com.moyun.ext.aigateway.model.AiMetadata;
import com.moyun.ext.aigateway.support.AiExecuteLogService;
import com.moyun.ext.aigateway.support.AiSceneJsonClient;
import com.moyun.ext.aigateway.support.TokenCostGuard;
import com.moyun.ext.aigateway.support.TokenMeter;
import dev.langchain4j.data.message.ChatMessage;
import dev.langchain4j.data.message.SystemMessage;
import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.model.chat.ChatLanguageModel;
import dev.langchain4j.model.chat.response.ChatResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * 智能体节点执行器
 *
 * <p>调用已配置的智能体处理输入。线程安全：无状态，继承 BaseNodeExecutor。</p>
 *
 * <p><b>v14.72 统一收口（治理批次3）</b>：Agent 节点的 LLM 调用历史上直连
 * {@code modelConfigService.createChatModel().chat()}，绕过网关全部治理——工作流内
 * N 次 LLM 调用无限流/熔断/计量/日志。现按"配置驱动、渐进双通道"接入：</p>
 * <ul>
 *   <li><b>优先</b>：经统一网关场景 {@code workflow_agent_node} 执行
 *       （限流/Token熔断/计量/执行日志全治理生效）；人设由本节点绑定 Agent 的
 *       system_prompt 经 input.agentPersona 注入（与网关 mergePersona 契约一致）。</li>
 *   <li><b>治理生效</b>：场景已配置时，限流/配额熔断判定对节点生效（节点失败，
 *       不再绕行直连——否则治理形同虚设）；场景配置的 fallback_response 降级内容
 *       原样透传（管理员可兜底）。</li>
 *   <li><b>兜底</b>：场景行未配置/停用（SCENE_NOT_FOUND）时保持原直连行为，
 *       但补计量记账（TokenMeter 估算 + TokenCostGuard 累计 + ai_execute_log），
 *       消除成本盲区。存量工作流零破坏。</li>
 * </ul>
 *
 * @author laomao
 * @since 2025-11-30
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class AgentNodeExecutor extends BaseNodeExecutor {

    /** 网关治理场景码（AiSceneEnum.WORKFLOW_AGENT_NODE，配置行缺失时走兜底直连） */
    public static final String GATEWAY_SCENE = "workflow_agent_node";

    private final AgentService agentService;
    private final ModelConfigService modelConfigService;
    private final AiSceneJsonClient sceneJsonClient;
    private final TokenMeter tokenMeter;
    private final TokenCostGuard tokenCostGuard;
    private final AiExecuteLogService executeLogService;

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

            Long userId = resolveUserId(context);

            // 1. 优先走统一网关治理场景（限流/Token熔断/计量/日志）
            AiSceneJsonClient.TextOutcome outcome = tryGateway(agent, userPrompt, userId);
            if (outcome != null) {
                if (outcome.code() == AiErrorCodes.SUCCESS && outcome.content() != null) {
                    String response = outcome.content();
                    logGatewayHit(node, context, agent, response);
                    context.setVariable(outputVariable, response);
                    return NodeResult.success(response);
                }
                if (!outcome.sceneMissing()) {
                    // 场景已配置：治理判定（限流/配额/模型失败且无兜底内容）对节点生效——
                    // 直连绕行会让治理形同虚设
                    return NodeResult.fail("Agent节点被网关治理拒绝: " + outcome.msg());
                }
                // 场景未配置/停用 → 落入下方兜底直连（补计量）
                log.info("🤖 网关场景 {} 未配置/停用，Agent节点走兜底直连（补计量）", GATEWAY_SCENE);
            }

            // 2. 兜底直连（存量行为）+ 计量记账
            String response = directCallMetered(node, context, agent, userPrompt, userId);

            log.info("🤖 Agent响应: {}", response.length() > 200 ? response.substring(0, 200) + "..." : response);
            context.setVariable(outputVariable, response);
            return NodeResult.success(response);

        } catch (Exception e) {
            log.error("Agent节点执行失败", e);
            return NodeResult.fail("Agent调用失败: " + e.getMessage());
        }
    }

    /**
     * 网关通道：Agent 人设经 input.agentPersona 注入（网关 sanitizeInputChannel 仅做
     * 字符级清洗，人设完整保留；DefaultSceneExecutor.mergePersona 前置生效）。
     * 网关通道自身异常不阻断节点（返回 null 走兜底）。
     */
    private AiSceneJsonClient.TextOutcome tryGateway(Agent agent, String userPrompt, Long userId) {
        try {
            Map<String, Object> input = new LinkedHashMap<>();
            if (agent.getSystemPrompt() != null && !agent.getSystemPrompt().isEmpty()) {
                input.put("agentPersona", agent.getSystemPrompt());
            }
            input.put("agentName", agent.getName());
            return sceneJsonClient.executeForText(GATEWAY_SCENE, input, userPrompt, userId);
        } catch (Exception e) {
            log.warn("🤖 网关通道异常（走兜底直连）: {}", e.getMessage());
            return null;
        }
    }

    /** 网关命中留痕（治理链已在网关侧完成计量与执行日志） */
    private void logGatewayHit(WorkflowNode node, WorkflowContext context, Agent agent, String response) {
        log.info("🤖 Agent节点经网关场景 {} 执行成功: workflowId={}, node={}, agent={}, respLen={}",
                GATEWAY_SCENE, context.getWorkflowId(), node.getId(), agent.getName(), response.length());
    }

    /**
     * 兜底直连 + 计量记账：Token 真实 usage 优先，缺失本地估算并打标；
     * TokenCostGuard 累计场景日计数（场景未配置时配额虽不限，计数保留成本可见性）；
     * ai_execute_log 落 admin 可观测口径（bindType=agent）。
     */
    private String directCallMetered(WorkflowNode node, WorkflowContext context, Agent agent,
                                     String userPrompt, Long userId) {
        long start = System.currentTimeMillis();
        ModelConfig defaultConfig = modelConfigService.getDefaultChatConfig();
        if (defaultConfig == null) {
            throw new IllegalStateException("无法获取默认LLM模型配置");
        }
        ChatLanguageModel chatModel = modelConfigService.createChatModel(defaultConfig.getId());
        if (chatModel == null) {
            throw new IllegalStateException("无法创建LLM模型");
        }

        List<ChatMessage> messages = new ArrayList<>();
        if (agent.getSystemPrompt() != null && !agent.getSystemPrompt().isEmpty()) {
            messages.add(new SystemMessage(agent.getSystemPrompt()));
        }
        messages.add(new UserMessage(userPrompt));

        ChatResponse chatResponse = chatModel.chat(messages);
        String response = chatResponse.aiMessage().text();
        if (response == null || response.isBlank()) {
            throw new IllegalStateException("Agent节点LLM无输出");
        }

        meterDirectCall(agent, chatResponse, userPrompt, response, userId,
                System.currentTimeMillis() - start);
        return response;
    }

    /** 兜底直连的计量记账（失败不阻断节点主流程） */
    private void meterDirectCall(Agent agent, ChatResponse chatResponse, String userPrompt,
                                 String response, Long userId, long elapsedMs) {
        try {
            AiMetadata metadata = new AiMetadata();
            metadata.setAgentUsed(agent.getName());
            Integer total = null;
            try {
                if (chatResponse.tokenUsage() != null && chatResponse.tokenUsage().totalTokenCount() != null) {
                    total = chatResponse.tokenUsage().totalTokenCount();
                    metadata.setInputTokens(chatResponse.tokenUsage().inputTokenCount());
                    metadata.setOutputTokens(chatResponse.tokenUsage().outputTokenCount());
                }
            } catch (Exception ignored) {
            }
            if (total == null) {
                TokenMeter.Metered metered = tokenMeter.meterTexts(userPrompt, response);
                metadata.setInputTokens(metered.inputTokens());
                metadata.setOutputTokens(metered.outputTokens());
                total = metered.totalTokens();
                metadata.setTokenEstimated(true);
            }
            metadata.setTokenUsed(total);
            try {
                if (chatResponse.metadata() != null && chatResponse.metadata().modelName() != null) {
                    metadata.setModelUsed(chatResponse.metadata().modelName());
                }
            } catch (Exception ignored) {
            }
            tokenCostGuard.consume(GATEWAY_SCENE, total);
            executeLogService.record(
                    UUID.randomUUID().toString().replace("-", ""),
                    userId, GATEWAY_SCENE,
                    "AgentNodeExecutor", "agent", metadata,
                    userPrompt.length() > 500 ? userPrompt.substring(0, 500) : userPrompt,
                    response.length() > 500 ? response.substring(0, 500) : response,
                    "success", null, elapsedMs);
        } catch (Exception e) {
            log.warn("🤖 Agent节点兜底直连计量失败（不影响节点）: {}", e.getMessage());
        }
    }

    /** 从工作流上下文提取归属用户（输入参数 userId；无则系统触发记匿名桶） */
    private Long resolveUserId(WorkflowContext context) {
        try {
            Object userId = context.getInput() != null ? context.getInput().get("userId") : null;
            if (userId instanceof Number number) {
                return number.longValue();
            }
            if (userId instanceof String s && !s.isBlank()) {
                return Long.parseLong(s.trim());
            }
        } catch (Exception ignored) {
        }
        return null;
    }
}
