package com.moyun.ext.aigateway.support;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.moyun.ext.ai.entity.Agent;
import com.moyun.ext.ai.entity.AiSceneConfig;
import com.moyun.ext.ai.mapper.AgentMapper;
import com.moyun.ext.ai.service.AiSceneResolver;
import com.moyun.ext.ai.service.ToolCallingService;
import com.moyun.ext.aigateway.model.AiExecuteRequest;
import com.moyun.ext.aigateway.model.AiExecuteResponse;
import com.moyun.ext.aigateway.model.AiMetadata;
import com.moyun.ext.aigateway.model.ConversationStreamCommand;
import com.moyun.ext.aigateway.model.data.GenericSceneData;
import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.data.message.ChatMessage;
import dev.langchain4j.data.message.SystemMessage;
import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.model.chat.ChatLanguageModel;
import dev.langchain4j.model.chat.StreamingChatLanguageModel;
import dev.langchain4j.model.chat.response.ChatResponse;
import dev.langchain4j.model.chat.response.StreamingChatResponseHandler;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.regex.Pattern;

/**
 * 受限自主规划执行器（v14.72 统一收口 · agent 自主规划通道）
 *
 * <p><b>定位</b>：统一网关的第三条执行通道（与直连模型、工作流并列）——供
 * <b>开放任务</b>使用：模型自主"计划 → 调工具 → 观察 → 综合"，适合无法预先
 * 固化执行路线的场景。固定执行路线/固定输出格式的场景应绑工作流或模型直连
 * （成本可预算），不应开启本通道。</p>
 *
 * <p><b>成本硬约束</b>（agent 自主规划不可靠的解法——不是禁止而是限损）：</p>
 * <ul>
 *   <li><b>轮数硬顶</b>：maxIterations（config_json.agentMaxIterations，默认 3，
 *       硬上限 8）——工具调用→观察循环最多 N 轮，超出强制综合收口；</li>
 *   <li><b>Token 预算硬顶</b>：tokenBudget（config_json.agentTokenBudget，默认
 *       12000，最小 1000）——逐轮累计真实/估算 token，超预算立即终止循环，
 *       场景级 daily_token_limit 熔断仍在网关外层兜底；</li>
 *   <li><b>零工具单发</b>：Agent 未绑定工具时不进本通道（Handler 单发 LLM 通道
 *       成本更低），由 {@link #supports} 前置拦截。</li>
 * </ul>
 *
 * <p><b>开启方式（配置驱动，默认关闭）</b>：场景行绑定 Agent + config_json 含
 * {@code {"autonomousPlanning": true}} + Agent 绑定工具。存量场景零变化。</p>
 *
 * <p><b>循环协议</b>：复用既有 {@code [TOOL_CALL]{...}[/TOOL_CALL]} 文本协议
 * （ToolCallingService），工具结果以用户消息回喂模型继续推理；无工具调用即终答。</p>
 *
 * @author laomao
 * @since 2026-10-05
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class AgentPlanExecutor {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    private final AgentMapper agentMapper;
    private final AiSceneResolver sceneResolver;
    private final TokenMeter tokenMeter;
    /** per-agent 流式模型路由（v14.72 P2-2 终答流式下发） */
    private final AgentModelRouter agentModelRouter;
    /** ObjectProvider 延迟解析：ToolCallingService 依赖链含工作流引擎（其 Agent 节点又依赖网关），防 Bean 循环 */
    private final ObjectProvider<ToolCallingService> toolCallingProvider;

    /** 默认/硬上限工具调用轮数 */
    private static final int DEFAULT_MAX_ITERATIONS = 3;
    private static final int HARD_MAX_ITERATIONS = 8;
    /** 默认/下限 Token 预算 */
    private static final int DEFAULT_TOKEN_BUDGET = 12_000;
    private static final int MIN_TOKEN_BUDGET = 1_000;

    /** 最终答案中的工具调用标记清洗（对用户不可见） */
    private static final Pattern TOOL_CALL_MARKUP = Pattern.compile(
            "\\[TOOL_CALL\\].*?\\[/TOOL_CALL\\]", Pattern.DOTALL);

    /** 无 Agent 人设时的最小任务视角 */
    private static final String DEFAULT_SYSTEM_PROMPT =
            "你是一个严谨的任务执行助手，严格按用户消息中的指令完成任务。";

    /** 流式协议：规划轮信息已足够时模型输出 [READY]（终答由系统另行安排流式下发） */
    private static final String READY_MARK = "[READY]";

    /** 流式终答指令（规划完成后追加，驱动流式模型输出最终答案） */
    private static final String STREAM_CLOSING_INSTRUCTION =
            "已获得全部所需信息，请现在直接输出完整、可直接消费的最终答案。不要再调用工具，也不要输出 [READY] 标记。";

    /**
     * 是否走自主规划通道：绑定 Agent + config_json 显式开启 autonomousPlanning
     * + Agent 绑定了工具（零工具的开放任务单发 LLM 即可，走 Handler 通道更省）。
     */
    public boolean supports(AiSceneConfig config) {
        if (config == null || config.getAgentId() == null) {
            return false;
        }
        Map<String, Object> cfg = parseConfigJson(config.getConfigJson());
        if (!Boolean.TRUE.equals(cfg.get("autonomousPlanning"))) {
            return false;
        }
        try {
            return toolCallingProvider.getObject().hasTools(config.getAgentId());
        } catch (Exception e) {
            log.warn("[aigateway:agent-plan] 工具绑定检查失败（不走规划通道）: agentId={}: {}",
                    config.getAgentId(), e.getMessage());
            return false;
        }
    }

    /**
     * 受限自主规划执行（计划 → 工具 → 观察 → 综合，双硬顶收口）
     */
    public AiExecuteResponse<?> execute(AiExecuteRequest request, AiSceneConfig config) {
        long start = System.currentTimeMillis();
        String sceneCode = config.getSceneCode();

        Agent agent = agentMapper.selectById(config.getAgentId());
        if (agent == null || Boolean.FALSE.equals(agent.getEnabled())) {
            return AiExecuteResponse.failure(
                    com.moyun.ext.aigateway.constant.AiErrorCodes.AI_CALL_FAILED,
                    "智能体不存在或未启用");
        }

        String userInput = request.getUserInput();
        if (userInput == null || userInput.isBlank()) {
            throw new IllegalArgumentException("自主规划场景缺少 userInput（任务描述）");
        }

        Map<String, Object> cfg = parseConfigJson(config.getConfigJson());
        int maxIterations = clampInt(cfg.get("agentMaxIterations"), DEFAULT_MAX_ITERATIONS, 1, HARD_MAX_ITERATIONS);
        int tokenBudget = clampInt(cfg.get("agentTokenBudget"), DEFAULT_TOKEN_BUDGET, MIN_TOKEN_BUDGET, Integer.MAX_VALUE);

        ToolCallingService toolCalling = toolCallingProvider.getObject();
        ChatLanguageModel model = sceneResolver.resolveChatModel(sceneCode);
        if (model == null) {
            return AiExecuteResponse.failure(
                    com.moyun.ext.aigateway.constant.AiErrorCodes.AI_CALL_FAILED,
                    "场景无可用模型绑定（agent/model）");
        }

        String systemPrompt = buildSystemPrompt(agent, toolCalling, maxIterations);
        List<ChatMessage> messages = new ArrayList<>();
        messages.add(new SystemMessage(systemPrompt));
        messages.add(new UserMessage(userInput));
        String lastPrompt = userInput;

        // 计量累计（真实 usage 优先，缺失估算并打标）
        int totalIn = 0;
        int totalOut = 0;
        boolean estimated = false;
        int usedIterations = 0;
        List<AiMetadata.ToolCallInfo> toolCalls = new ArrayList<>();

        String finalAnswer = null;
        String modelUsed = null;
        for (int i = 0; i < maxIterations; i++) {
            usedIterations = i + 1;
            ChatResponse resp = model.chat(messages);
            String text = resp.aiMessage() != null ? resp.aiMessage().text() : null;
            if (text == null || text.isBlank()) {
                log.warn("[aigateway:agent-plan] 第{}轮模型无输出: scene={}", usedIterations, sceneCode);
                break;
            }
            // token 计量：真实优先
            Integer total = null;
            try {
                if (resp.tokenUsage() != null && resp.tokenUsage().totalTokenCount() != null) {
                    total = resp.tokenUsage().totalTokenCount();
                    if (resp.tokenUsage().inputTokenCount() != null) {
                        totalIn += resp.tokenUsage().inputTokenCount();
                    }
                    if (resp.tokenUsage().outputTokenCount() != null) {
                        totalOut += resp.tokenUsage().outputTokenCount();
                    }
                }
            } catch (Exception ignored) {
            }
            if (total == null) {
                TokenMeter.Metered metered = tokenMeter.meterTexts(lastPrompt, text);
                totalIn += metered.inputTokens();
                totalOut += metered.outputTokens();
                total = metered.totalTokens();
                estimated = true;
            }
            try {
                if (resp.metadata() != null && resp.metadata().modelName() != null) {
                    modelUsed = resp.metadata().modelName();
                }
            } catch (Exception ignored) {
            }

            // Token 预算硬顶：超预算立即收口，不再进入下一轮工具调用
            if (totalIn + totalOut > tokenBudget) {
                log.warn("[aigateway:agent-plan] Token预算触顶({} > {})，强制收口: scene={}, iter={}",
                        totalIn + totalOut, tokenBudget, sceneCode, usedIterations);
                finalAnswer = stripToolMarkup(text);
                break;
            }

            // 工具调用检测（无标记 = 终答）
            ToolCallingService.ToolCallResult call;
            try {
                call = toolCalling.detectAndExecute(text, com.moyun.ext.ai.engine.tool.ToolContext.builder()
                        .agentId(agent.getId())
                        .userQuery(userInput)
                        .build());
            } catch (Exception e) {
                log.warn("[aigateway:agent-plan] 工具调用检测失败（按终答处理）: {}", e.getMessage());
                finalAnswer = stripToolMarkup(text);
                break;
            }
            if (call == null || !call.isHasToolCall()) {
                finalAnswer = text;
                break;
            }

            AiMetadata.ToolCallInfo info = new AiMetadata.ToolCallInfo();
            info.setToolName(call.getToolName());
            info.setResult(call.getToolResult());
            toolCalls.add(info);

            // 观察：工具结果回喂，继续下一轮
            messages.add(new AiMessage(text));
            String feedback = "【工具执行结果】" + call.getToolName() + ": "
                    + (call.getErrorMessage() != null ? "执行失败 - " + call.getErrorMessage()
                        : String.valueOf(call.getToolResult()))
                    + "\n\n请基于以上结果继续任务。若信息已足够完成原任务，必须立即输出最终答案，不得再调用工具。";
            messages.add(new UserMessage(feedback));
            lastPrompt = feedback;
        }

        // 轮数耗尽仍有待处理工具结果 → 强制综合收口（一次，无工具检测）
        if (finalAnswer == null) {
            log.warn("[aigateway:agent-plan] 轮数耗尽({})，强制综合: scene={}", maxIterations, sceneCode);
            String closing = "已达到工具调用轮数上限，请立即基于已获得的信息输出最终答案，不要再调用工具。";
            messages.add(new UserMessage(closing));
            lastPrompt = closing;
            ChatResponse resp = model.chat(messages);
            String text = resp.aiMessage() != null ? resp.aiMessage().text() : null;
            finalAnswer = text != null ? stripToolMarkup(text) : "";
            try {
                if (resp.tokenUsage() != null && resp.tokenUsage().totalTokenCount() != null) {
                    totalIn += resp.tokenUsage().inputTokenCount() != null ? resp.tokenUsage().inputTokenCount() : 0;
                    totalOut += resp.tokenUsage().outputTokenCount() != null ? resp.tokenUsage().outputTokenCount() : 0;
                } else {
                    TokenMeter.Metered metered = tokenMeter.meterTexts(lastPrompt, finalAnswer);
                    totalIn += metered.inputTokens();
                    totalOut += metered.outputTokens();
                    estimated = true;
                }
            } catch (Exception ignored) {
            }
        }

        if (finalAnswer == null || finalAnswer.isBlank()) {
            return AiExecuteResponse.failure(
                    com.moyun.ext.aigateway.constant.AiErrorCodes.AI_CALL_FAILED, "AI服务暂不可用");
        }

        GenericSceneData data = new GenericSceneData();
        data.setContent(finalAnswer);
        data.setSource("agent_plan");

        AiMetadata metadata = new AiMetadata();
        metadata.setModelUsed(modelUsed);
        metadata.setAgentUsed(agent.getName());
        metadata.setInputTokens(totalIn);
        metadata.setOutputTokens(totalOut);
        metadata.setTokenUsed(totalIn + totalOut);
        metadata.setTokenEstimated(estimated);
        metadata.setToolCalls(toolCalls);
        metadata.setRetryCount(usedIterations);

        AiExecuteResponse<GenericSceneData> response = AiExecuteResponse.success(data);
        response.setMetadata(metadata);
        log.info("[aigateway:agent-plan] 完成: scene={}, agent={}, iters={}/{}, tools={}, tokens={}/{}, elapsed={}ms",
                sceneCode, agent.getName(), usedIterations, maxIterations, toolCalls.size(),
                totalIn, totalOut, System.currentTimeMillis() - start);
        return response;
    }

    /**
     * 会话流式执行（v14.72 P2-2：自主规划通道流式化）。
     *
     * <p>由网关 {@code executeConversationStream} 在限流/Token 熔断/注入防护之后分派，
     * 治理（限流/配额/执行日志/记忆写回/成本累计）仍由网关在回调中统一收口。</p>
     *
     * <p><b>协议</b>：规划循环同步执行（计划 → 工具 → 观察，双硬顶与同步通道一致），
     * 工具轮输出不向前端转发；规划轮约定模型在信息足够时仅输出 {@code [READY]}，
     * 由系统追加终答指令后改用<b>流式模型</b>增量下发最终答案（onToken）。
     * 模型不守约直接给出完整答案时按终答接受，一次性转发（不二次调用，避免成本翻倍）。</p>
     *
     * @param cmd                  会话流式命令（userInput 即任务描述）
     * @param config               场景配置（supports 已由网关校验）
     * @param conversationMessages 网关装配的本轮会话消息（滑窗+摘要+瞬态指令，含本轮输入）
     * @param onToken              终答增量回调（仅终答文本；工具轮/规划轮不转发）
     * @param onComplete           完成回调（全文 + 计量元数据；记忆写回与治理记账由网关完成）
     * @param onError              失败回调
     */
    public void executeConversationStream(ConversationStreamCommand cmd,
                                          AiSceneConfig config,
                                          List<ChatMessage> conversationMessages,
                                          Consumer<String> onToken,
                                          BiConsumer<String, AiMetadata> onComplete,
                                          Consumer<Throwable> onError) {
        long start = System.currentTimeMillis();
        String sceneCode = config.getSceneCode();
        AtomicBoolean done = new AtomicBoolean(false);

        try {
            Agent agent = agentMapper.selectById(config.getAgentId());
            if (agent == null || Boolean.FALSE.equals(agent.getEnabled())) {
                onError.accept(new IllegalStateException("智能体不存在或未启用"));
                return;
            }
            String userInput = cmd.getUserInput();
            if (userInput == null || userInput.isBlank()) {
                onError.accept(new IllegalArgumentException("自主规划场景缺少 userInput（任务描述）"));
                return;
            }

            Map<String, Object> cfg = parseConfigJson(config.getConfigJson());
            int maxIterations = clampInt(cfg.get("agentMaxIterations"), DEFAULT_MAX_ITERATIONS, 1, HARD_MAX_ITERATIONS);
            int tokenBudget = clampInt(cfg.get("agentTokenBudget"), DEFAULT_TOKEN_BUDGET, MIN_TOKEN_BUDGET, Integer.MAX_VALUE);

            ToolCallingService toolCalling = toolCallingProvider.getObject();
            ChatLanguageModel model = sceneResolver.resolveChatModel(sceneCode);
            if (model == null) {
                onError.accept(new IllegalStateException("场景无可用模型绑定（agent/model）"));
                return;
            }
            StreamingChatLanguageModel streamingModel = agentModelRouter.createStreamingModel(agent);
            if (streamingModel == null) {
                onError.accept(new IllegalStateException("无可用流式模型，请联系管理员配置"));
                return;
            }

            // 消息装配：人设+工具协议（流式变体纪律）在会话窗口之前
            List<ChatMessage> messages = new ArrayList<>();
            messages.add(new SystemMessage(buildStreamSystemPrompt(agent, toolCalling, maxIterations)));
            if (conversationMessages != null) {
                messages.addAll(conversationMessages);
            }

            int totalIn = 0;
            int totalOut = 0;
            boolean estimated = false;
            int usedIterations = 0;
            String modelUsed = null;
            String directAnswer = null;
            boolean ready = false;
            List<AiMetadata.ToolCallInfo> toolCalls = new ArrayList<>();

            // —— 规划循环（同步；工具轮输出不转发前端）——
            for (int i = 0; i < maxIterations; i++) {
                usedIterations = i + 1;
                ChatResponse resp = model.chat(messages);
                String text = resp.aiMessage() != null ? resp.aiMessage().text() : null;
                if (text == null || text.isBlank()) {
                    log.warn("[aigateway:agent-plan:stream] 第{}轮模型无输出: scene={}", usedIterations, sceneCode);
                    break;
                }
                // token 计量：真实优先
                Integer total = null;
                try {
                    if (resp.tokenUsage() != null && resp.tokenUsage().totalTokenCount() != null) {
                        total = resp.tokenUsage().totalTokenCount();
                        if (resp.tokenUsage().inputTokenCount() != null) {
                            totalIn += resp.tokenUsage().inputTokenCount();
                        }
                        if (resp.tokenUsage().outputTokenCount() != null) {
                            totalOut += resp.tokenUsage().outputTokenCount();
                        }
                    }
                } catch (Exception ignored) {
                }
                if (total == null) {
                    TokenMeter.Metered metered = tokenMeter.meterTexts(userInput, text);
                    totalIn += metered.inputTokens();
                    totalOut += metered.outputTokens();
                    estimated = true;
                }
                try {
                    if (resp.metadata() != null && resp.metadata().modelName() != null) {
                        modelUsed = resp.metadata().modelName();
                    }
                } catch (Exception ignored) {
                }

                // Token 预算硬顶：超预算不再进入下一轮，该轮文本按终答收口（不再发生新调用）
                if (totalIn + totalOut > tokenBudget) {
                    log.warn("[aigateway:agent-plan:stream] Token预算触顶({} > {})，按终答收口: scene={}, iter={}",
                            totalIn + totalOut, tokenBudget, sceneCode, usedIterations);
                    directAnswer = stripToolMarkup(text);
                    break;
                }

                // 工具调用检测
                ToolCallingService.ToolCallResult call;
                try {
                    call = toolCalling.detectAndExecute(text, com.moyun.ext.ai.engine.tool.ToolContext.builder()
                            .agentId(agent.getId())
                            .userQuery(userInput)
                            .build());
                } catch (Exception e) {
                    log.warn("[aigateway:agent-plan:stream] 工具调用检测失败（按终答处理）: {}", e.getMessage());
                    directAnswer = stripToolMarkup(text);
                    break;
                }
                if (call == null || !call.isHasToolCall()) {
                    String stripped = stripToolMarkup(text);
                    if (text.contains(READY_MARK)) {
                        ready = true;
                    } else {
                        // 模型不守约直接给出完整答案：接受为终答，一次性转发（不二次调用）
                        directAnswer = stripped;
                    }
                    break;
                }

                AiMetadata.ToolCallInfo info = new AiMetadata.ToolCallInfo();
                info.setToolName(call.getToolName());
                info.setResult(call.getToolResult());
                toolCalls.add(info);

                messages.add(new AiMessage(text));
                messages.add(new UserMessage("【工具执行结果】" + call.getToolName() + ": "
                        + (call.getErrorMessage() != null ? "执行失败 - " + call.getErrorMessage()
                            : String.valueOf(call.getToolResult()))
                        + "\n\n请基于以上结果继续任务。若信息已足够完成原任务，仅输出 " + READY_MARK
                        + "（不要输出最终答案，系统将另行安排）；否则继续调用必要的工具。"));
            }

            // —— 终答下发 ——
            // 模型规划轮直接给出完整答案（或预算触顶收口）：一次性转发
            if (directAnswer != null && !directAnswer.isBlank()) {
                safelyAccept(onToken, directAnswer);
                AiMetadata metadata = buildMetadata(agent, modelUsed, totalIn, totalOut, estimated, usedIterations, toolCalls);
                if (done.compareAndSet(false, true)) {
                    onComplete.accept(directAnswer, metadata);
                }
                log.info("[aigateway:agent-plan:stream] 完成(直答): scene={}, iters={}/{}, tools={}, elapsed={}ms",
                        sceneCode, usedIterations, maxIterations, toolCalls.size(), System.currentTimeMillis() - start);
                return;
            }

            // READY / 轮数耗尽：追加终答指令，流式模型增量下发（真流式）
            messages.add(new UserMessage(STREAM_CLOSING_INSTRUCTION));
            final StringBuilder buffer = new StringBuilder();
            final int fTotalIn = totalIn;
            final int fTotalOut = totalOut;
            final boolean fEstimated = estimated;
            final int fUsedIterations = usedIterations;
            final String fModelUsed = modelUsed;
            final List<AiMetadata.ToolCallInfo> fToolCalls = toolCalls;
            final Agent fAgent = agent;
            final boolean fReady = ready;
            streamingModel.chat(messages, new StreamingChatResponseHandler() {
                @Override
                public void onPartialResponse(String partialResponse) {
                    if (partialResponse == null || partialResponse.isEmpty()) {
                        return;
                    }
                    buffer.append(partialResponse);
                    safelyAccept(onToken, partialResponse);
                }

                @Override
                public void onCompleteResponse(ChatResponse response) {
                    if (!done.compareAndSet(false, true)) {
                        return;
                    }
                    int in = fTotalIn;
                    int out = fTotalOut;
                    boolean est = fEstimated;
                    try {
                        TokenMeter.Metered metered = tokenMeter.meter(
                                response == null ? null : response.tokenUsage(),
                                messages, buffer.toString());
                        in += metered.inputTokens();
                        out += metered.outputTokens();
                        est |= metered.estimated();
                    } catch (Exception e) {
                        log.warn("[aigateway:agent-plan:stream] 终答计量失败（不影响业务）: {}", e.getMessage());
                    }
                    String answer = buffer.toString();
                    AiMetadata metadata = buildMetadata(fAgent,
                            response != null && response.metadata() != null && response.metadata().modelName() != null
                                    ? response.metadata().modelName() : fModelUsed,
                            in, out, est, fUsedIterations, fToolCalls);
                    onComplete.accept(answer, metadata);
                    log.info("[aigateway:agent-plan:stream] 完成(流式): scene={}, iters={}/{}, tools={}, ready={}, elapsed={}ms",
                            sceneCode, fUsedIterations, maxIterations, fToolCalls.size(), fReady,
                            System.currentTimeMillis() - start);
                }

                @Override
                public void onError(Throwable error) {
                    if (!done.compareAndSet(false, true)) {
                        return;
                    }
                    log.error("[aigateway:agent-plan:stream] 终答流式失败: scene={}", sceneCode, error);
                    onError.accept(error);
                }
            });
        } catch (Exception e) {
            log.error("[aigateway:agent-plan:stream] 执行失败: scene={}", sceneCode, e);
            if (done.compareAndSet(false, true)) {
                onError.accept(e);
            }
        }
    }

    /** 回调内异常吞掉记日志，不影响流式主流程 */
    private void safelyAccept(Consumer<String> callback, String value) {
        try {
            callback.accept(value);
        } catch (Exception e) {
            log.error("[aigateway:agent-plan:stream] onToken 回调异常: {}", e.getMessage());
        }
    }

    /** 流式变体计量元数据（网关完成回调中做成本累计与执行日志） */
    private AiMetadata buildMetadata(Agent agent, String modelUsed, int totalIn, int totalOut,
                                     boolean estimated, int usedIterations, List<AiMetadata.ToolCallInfo> toolCalls) {
        AiMetadata metadata = new AiMetadata();
        metadata.setModelUsed(modelUsed);
        metadata.setAgentUsed(agent.getName());
        metadata.setInputTokens(totalIn);
        metadata.setOutputTokens(totalOut);
        metadata.setTokenUsed(totalIn + totalOut);
        metadata.setTokenEstimated(estimated);
        metadata.setToolCalls(toolCalls);
        metadata.setRetryCount(usedIterations);
        return metadata;
    }

    /**
     * 流式变体系统提示词：与同步通道同源（人设+工具协议），纪律改为
     * 「信息足够时仅输出 [READY]，终答由系统另行安排」——避免规划轮生成完整终答
     * （同步通道此时代码直接复用该文本，流式通道无法增量下发已生成的文本）。
     */
    private String buildStreamSystemPrompt(Agent agent, ToolCallingService toolCalling, int maxIterations) {
        String persona = agent.getSystemPrompt() != null && !agent.getSystemPrompt().isBlank()
                ? agent.getSystemPrompt() : DEFAULT_SYSTEM_PROMPT;
        String toolPrompt = toolCalling.buildToolPrompt(agent.getId());
        return persona
                + toolPrompt
                + "\n\n【任务纪律】本任务为系统自动执行的开放任务：最多可进行 " + maxIterations
                + " 轮工具调用；每轮调用前先判断是否必要——信息已足够时仅输出 " + READY_MARK
                + "（不要输出最终答案，系统将另行安排），最终答案将单独生成；"
                + "禁止为调用而调用、禁止重复调用同一工具获取相同信息。";
    }

    /**
     * 系统提示词：Agent 人设 + 工具协议（复用 ToolCallingService.buildToolPrompt）
     * + 任务边界与终止纪律（约束自主规划的"跑偏"与"舍不得结束"）。
     */
    private String buildSystemPrompt(Agent agent, ToolCallingService toolCalling, int maxIterations) {
        String persona = agent.getSystemPrompt() != null && !agent.getSystemPrompt().isBlank()
                ? agent.getSystemPrompt() : DEFAULT_SYSTEM_PROMPT;
        String toolPrompt = toolCalling.buildToolPrompt(agent.getId());
        return persona
                + toolPrompt
                + "\n\n【任务纪律】本任务为系统自动执行的开放任务：最多可进行 " + maxIterations
                + " 轮工具调用；每轮调用前先判断是否必要——信息已足够时必须直接给出最终答案，"
                + "禁止为调用而调用、禁止重复调用同一工具获取相同信息；最终答案必须是完整、可直接消费的成果，"
                + "不得包含 [TOOL_CALL] 标记。";
    }

    private String stripToolMarkup(String text) {
        return TOOL_CALL_MARKUP.matcher(text).replaceAll("").trim();
    }

    private Map<String, Object> parseConfigJson(String configJson) {
        if (configJson == null || configJson.isBlank()) {
            return Map.of();
        }
        try {
            return MAPPER.readValue(configJson, new com.fasterxml.jackson.core.type.TypeReference<Map<String, Object>>() {
            });
        } catch (Exception e) {
            log.warn("[aigateway:agent-plan] config_json 解析失败（按未开启处理）: {}", e.getMessage());
            return Map.of();
        }
    }

    private int clampInt(Object value, int defaultValue, int min, int max) {
        if (!(value instanceof Number number)) {
            return defaultValue;
        }
        int v = number.intValue();
        return Math.max(min, Math.min(max, v));
    }
}
