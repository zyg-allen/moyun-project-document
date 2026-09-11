package com.moyun.ext.ai2.handler.impl;

import com.fasterxml.jackson.core.type.TypeReference;
import com.moyun.ext.ai.entity.Agent;
import com.moyun.ext.ai.entity.AiSceneConfig;
import com.moyun.ext.ai.mapper.AgentMapper;
import com.moyun.ext.ai.service.chat.ChatContextBuilderService;
import com.moyun.ext.ai.service.chat.ChatContextBuilderService.RagContextResult;
import com.moyun.ext.ai.service.chat.RagRetrievalService;
import com.moyun.ext.ai2.constant.AiErrorCodes;
import com.moyun.ext.ai2.handler.AbstractAiSceneHandler;
import com.moyun.ext.ai2.model.AiExecuteRequest;
import com.moyun.ext.ai2.model.AiExecuteResponse;
import com.moyun.ext.ai2.model.ChatOutcome;
import com.moyun.ext.ai2.model.data.KnowledgeQaSceneData;
import com.moyun.ext.ai2.model.data.KnowledgeQaSceneData.Reference;
import dev.langchain4j.rag.content.Content;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * 知识问答场景Handler（scene = knowledge_qa，v11.65）
 *
 * <p>将 RAG 知识问答能力收编进统一网关（原仅 admin chat 可用），完整复用既有检索底座：</p>
 * <ul>
 *   <li><b>多路召回</b>：{@link RagRetrievalService#retrieveContents}——查询改写 +
 *       混合检索（向量 + BM25 + RRF 融合 + 知识库权重）+ Rerank 重排 + 相邻分片合并，
 *       RAG 参数（minScore/maxResults/召回倍数/权重）全部读 Agent 配置，与 chat 链一致。</li>
 *   <li><b>提示词构建</b>：{@link ChatContextBuilderService#buildSystemPrompt}（Agent 人设 +
 *       知识库使用规则）与 {@link ChatContextBuilderService#buildRagContext}（参考资料上下文）
 *       原样复用。注意：人设经 Agent 实体统一构建，不走网关注入的 input.agentPersona
 *       （避免重复注入）。</li>
 *   <li><b>引用溯源</b>：检索片段 metadata（fileName/fileType/pageNumber/segmentIndex/
 *       knowledgeBaseId/imagePath）+ Rerank 分数透出到响应 references，消费端可渲染来源。</li>
 * </ul>
 *
 * <p>知识库绑定优先级：{@code input.knowledgeLibraryIds}（调用方指定）&gt;
 * ai_scene_config.knowledge_library_ids（场景级）&gt; agent.knowledgeLibraryIds（Agent 级）。</p>
 *
 * <p>输入契约：userInput=问题（必填）；input.knowledgeLibraryIds=知识库 ID（可选，
 * 支持数组/JSON数组串/逗号分隔）。</p>
 *
 * <p>已知边界：单轮问答（同步）；多轮对话历史与流式输出随网关多轮消息契约（P2）扩展。
 * 检索为空时不调 LLM 零 token 返回标准范围外话术。</p>
 *
 * @author laomao
 * @since 2026-09-11
 */
@Slf4j
@Component("knowledgeQaHandler")
@RequiredArgsConstructor
public class KnowledgeQaHandler extends AbstractAiSceneHandler {

    /** 摘录截断长度（引用预览用，避免响应体膨胀） */
    private static final int EXCERPT_MAX_LENGTH = 200;

    private final RagRetrievalService ragRetrievalService;
    private final ChatContextBuilderService chatContextBuilderService;
    private final AgentMapper agentMapper;

    @Override
    public String getSceneCode() {
        return "knowledge_qa";
    }

    @Override
    public void validate(AiExecuteRequest request) {
        requireUserInput(request);
    }

    @Override
    public AiExecuteResponse<?> execute(AiExecuteRequest request, AiSceneConfig config) {
        String question = requireUserInput(request);

        // 1. 加载 Agent（RAG 参数与人设来源；未绑定时用空壳 Agent 走全局默认配置）
        Agent agent = loadAgent(config);

        // 2. 知识库解析（优先级：input > 场景配置 > Agent 绑定）
        List<Long> kbIds = resolveKnowledgeBaseIds(request, config, agent);
        if (kbIds.isEmpty()) {
            return AiExecuteResponse.failure(AiErrorCodes.INVALID_REQUEST,
                    "知识问答场景未绑定知识库：请在场景配置/Agent 绑定，或通过 input.knowledgeLibraryIds 指定");
        }

        Map<Content, Double> rerankScores = null;
        try {
            // 3. 多路召回（查询改写 + 向量/BM25 混合 + RRF 融合 + Rerank + 分片合并）
            List<Content> contents = ragRetrievalService.retrieveContents(question, kbIds, agent);

            KnowledgeQaSceneData data = new KnowledgeQaSceneData();
            data.setRetrievalCount(contents != null ? contents.size() : 0);

            // 4. 检索为空：零 token 返回标准范围外话术（与 chat 链语义一致）
            if (contents == null || contents.isEmpty()) {
                data.setAnswer("抱歉，这个问题不在我的知识库范围内。请尝试更换问法，或补充相关资料到知识库。");
                data.setReferences(List.of());
                data.setImageCount(0);
                return AiExecuteResponse.success(data);
            }

            rerankScores = ragRetrievalService.getContentRerankScores();

            // 5. 引用溯源：metadata + Rerank 分数
            List<Reference> references = buildReferences(contents, rerankScores);
            data.setReferences(references);
            data.setImageCount((int) references.stream().filter(r -> r.getImagePath() != null).count());

            // 6. 提示词构建（复用 chat 链：系统提示词含 Agent 人设 + 知识库规则；RAG 上下文注入用户消息）
            String systemPrompt;
            if (config != null && config.getSystemPromptTemplate() != null
                    && !config.getSystemPromptTemplate().isBlank()) {
                systemPrompt = renderTemplate(config.getSystemPromptTemplate(), request);
            } else {
                systemPrompt = chatContextBuilderService.buildSystemPrompt(agent, false, null);
            }
            RagContextResult ragContext = chatContextBuilderService.buildRagContext(contents);
            String userPrompt = chatContextBuilderService.buildProcessedUserMessage(
                    ragContext.getContext(), question, null, true);

            // 7. LLM 生成回答（场景感知模型链：Agent 绑定模型 → 直绑 → 默认）
            ChatOutcome outcome = chatDetailed(getSceneCode(), systemPrompt, userPrompt);
            String answer = cleanLlmText(outcome != null ? outcome.getText() : null);
            if (answer.isEmpty()) {
                log.warn("[ai2:{}] LLM 未返回有效回答（模型不可用或空内容）", getSceneCode());
                return AiExecuteResponse.failure(AiErrorCodes.AI_CALL_FAILED,
                        "AI 服务暂不可用，请稍后重试（已检索到 " + contents.size() + " 条相关内容）");
            }
            data.setAnswer(answer);

            AiExecuteResponse<KnowledgeQaSceneData> response = AiExecuteResponse.success(data);
            response.setMetadata(buildMetadata(outcome));
            return response;
        } finally {
            // 8. 线程池卫生：重排分数为 ThreadLocal，必须清理（网关线程复用会串台）
            ragRetrievalService.clearContentRerankScores();
        }
    }

    /**
     * 加载场景绑定的 Agent（RAG 参数与人设来源）。未绑定/加载失败返回空壳 Agent，
     * 检索服务对 null 字段自动回落全局默认配置。
     */
    private Agent loadAgent(AiSceneConfig config) {
        if (config == null || config.getAgentId() == null) {
            return new Agent();
        }
        try {
            Agent agent = agentMapper.selectById(config.getAgentId());
            if (agent != null) {
                return agent;
            }
        } catch (Exception e) {
            log.warn("[ai2:{}] Agent 加载失败（按默认 RAG 配置执行）: {}", getSceneCode(), e.getMessage());
        }
        return new Agent();
    }

    /**
     * 知识库 ID 解析（优先级：input > 场景配置 > Agent）。
     * 支持形态：List（数字/字符串元素）、JSON 数组串（"[1,2]"）、逗号分隔串（"1,2"）。
     */
    private List<Long> resolveKnowledgeBaseIds(AiExecuteRequest request, AiSceneConfig config, Agent agent) {
        // 1. input 调用方指定（List 直取；字符串尝试 JSON 数组再回落逗号分隔）
        Object inputIds = request.getInput() != null ? request.getInput().get("knowledgeLibraryIds") : null;
        List<Long> parsed = parseKbIds(inputIds);
        if (!parsed.isEmpty()) {
            return parsed;
        }
        // 2. 场景配置
        parsed = parseKbIds(config != null ? config.getKnowledgeLibraryIds() : null);
        if (!parsed.isEmpty()) {
            return parsed;
        }
        // 3. Agent 绑定
        return parseKbIds(agent.getKnowledgeLibraryIds());
    }

    /** 解析知识库 ID 集合（兼容 List / JSON 数组串 / 逗号分隔串 / 单数字串），非法返回空列表 */
    private List<Long> parseKbIds(Object raw) {
        List<Long> result = new ArrayList<>();
        if (raw == null) {
            return result;
        }
        if (raw instanceof List<?> list) {
            for (Object item : list) {
                addKbId(result, item != null ? String.valueOf(item) : null);
            }
            return result;
        }
        String text = String.valueOf(raw).trim();
        if (text.isEmpty()) {
            return result;
        }
        if (text.startsWith("[")) {
            try {
                List<Object> ids = MAPPER.readValue(text, new TypeReference<>() {
                });
                for (Object item : ids) {
                    addKbId(result, item != null ? String.valueOf(item) : null);
                }
                return result;
            } catch (Exception e) {
                log.warn("[ai2:{}] 知识库ID JSON解析失败: {}", getSceneCode(), text);
                return result;
            }
        }
        for (String part : text.split(",")) {
            addKbId(result, part);
        }
        return result;
    }

    private void addKbId(List<Long> result, String value) {
        if (value == null) {
            return;
        }
        String trimmed = value.trim();
        if (trimmed.isEmpty()) {
            return;
        }
        try {
            result.add(Long.parseLong(trimmed));
        } catch (NumberFormatException ignored) {
            log.warn("[ai2:{}] 非法知识库ID已忽略: {}", getSceneCode(), trimmed);
        }
    }

    /**
     * 检索片段 → 引用来源列表（metadata 字段与 chat 引用链一致）
     */
    private List<Reference> buildReferences(List<Content> contents, Map<Content, Double> rerankScores) {
        List<Reference> references = new ArrayList<>();
        for (Content content : contents) {
            Reference ref = new Reference();
            var metadata = content.textSegment() != null ? content.textSegment().metadata() : null;
            if (metadata != null) {
                ref.setFileName(metadata.getString("fileName"));
                ref.setFileType(metadata.getString("fileType"));
                ref.setPageNumber(metadata.getString("pageNumber"));
                ref.setSegmentIndex(metadata.getString("segmentIndex"));
                ref.setKnowledgeBaseId(metadata.getString("knowledgeBaseId"));
                ref.setImagePath(metadata.getString("imagePath"));
            }
            if (rerankScores != null) {
                Double score = rerankScores.get(content);
                if (score != null) {
                    ref.setRerankScore(Math.round(score * 10000.0) / 10000.0);
                }
            }
            String text = content.textSegment() != null ? content.textSegment().text() : "";
            ref.setExcerpt(text != null && text.length() > EXCERPT_MAX_LENGTH
                    ? text.substring(0, EXCERPT_MAX_LENGTH) + "…" : text);
            references.add(ref);
        }
        return references;
    }
}
