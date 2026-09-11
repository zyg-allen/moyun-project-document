package com.moyun.ext.ai2.handler.impl;

import com.moyun.ext.ai.entity.Agent;
import com.moyun.ext.ai.entity.AiSceneConfig;
import com.moyun.ext.ai.mapper.AgentMapper;
import com.moyun.ext.ai.service.chat.ChatContextBuilderService;
import com.moyun.ext.ai.service.chat.ChatContextBuilderService.RagContextResult;
import com.moyun.ext.ai.service.chat.RagRetrievalService;
import com.moyun.ext.ai2.constant.AiErrorCodes;
import com.moyun.ext.ai2.model.AiExecuteRequest;
import com.moyun.ext.ai2.model.AiExecuteResponse;
import com.moyun.ext.ai2.model.ChatOutcome;
import com.moyun.ext.ai2.model.data.KnowledgeQaSceneData;
import dev.langchain4j.data.document.Metadata;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.rag.content.Content;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 知识问答场景Handler单元测试（v11.65）
 *
 * <p>覆盖：参数校验、知识库绑定三级优先级（input &gt; 场景配置 &gt; Agent）与三种传参形态
 * （List/JSON数组串/逗号串）、未绑定知识库快速失败、检索为空零 token 标准响应、
 * LLM 成功（引用溯源字段+摘录截断+metadata）、LLM 空回答失败、Rerank 分数 ThreadLocal
 * 必清理（网关线程池卫生）、Agent 加载失败按空壳默认、场景模板覆盖系统提示词。</p>
 */
class KnowledgeQaHandlerTest {

    private RagRetrievalService ragRetrievalService;
    private ChatContextBuilderService chatContextBuilderService;
    private AgentMapper agentMapper;

    /** LLM 打桩结果（子类覆写 chatDetailed，无需 mock 基类模型链） */
    private ChatOutcome llmOutcome;

    @BeforeEach
    void setUp() {
        ragRetrievalService = Mockito.mock(RagRetrievalService.class);
        chatContextBuilderService = Mockito.mock(ChatContextBuilderService.class);
        agentMapper = Mockito.mock(AgentMapper.class);

        when(chatContextBuilderService.buildSystemPrompt(any(), anyBoolean(), any())).thenReturn("SYSTEM_PROMPT");
        when(chatContextBuilderService.buildRagContext(any())).thenReturn(new RagContextResult("RAG_CONTEXT", 1));
        when(chatContextBuilderService.buildProcessedUserMessage(anyString(), anyString(), any(), anyBoolean()))
                .thenReturn("USER_PROMPT");

        llmOutcome = new ChatOutcome();
        llmOutcome.setText("这是基于知识库的回答");
        llmOutcome.setModelUsed("deepseek-chat");
        llmOutcome.setTokenUsed(100);
    }

    /** 构建被测 Handler（覆写 chatDetailed 打桩 LLM） */
    private KnowledgeQaHandler newHandler() {
        return new KnowledgeQaHandler(ragRetrievalService, chatContextBuilderService, agentMapper) {
            @Override
            protected ChatOutcome chatDetailed(String sceneCode, String systemPrompt, String userPrompt) {
                return llmOutcome;
            }
        };
    }

    private AiExecuteRequest request(String question, Map<String, Object> input) {
        AiExecuteRequest request = new AiExecuteRequest();
        request.setSceneCode("knowledge_qa");
        request.setUserInput(question);
        request.setInput(input);
        return request;
    }

    private Content content(String text, String fileName, String pageNumber, String segmentIndex) {
        Metadata metadata = Metadata.from("fileName", fileName)
                .put("fileType", "pdf")
                .put("pageNumber", pageNumber)
                .put("segmentIndex", segmentIndex)
                .put("knowledgeBaseId", "12");
        return Content.from(TextSegment.from(text, metadata));
    }

    // ==================== 参数校验 ====================

    @Test
    void validate_missingUserInput_rejected() {
        AiExecuteRequest request = request(null, Map.of());
        assertThrows(IllegalArgumentException.class, () -> newHandler().validate(request));
    }

    // ==================== 知识库未绑定快速失败 ====================

    @Test
    void execute_noKbBindingAnywhere_failsFast() {
        AiExecuteResponse<?> resp = newHandler().execute(request("什么是火箭发动机？", null), new AiSceneConfig());

        assertEquals(AiErrorCodes.INVALID_REQUEST, resp.getCode());
        assertTrue(resp.getMsg().contains("未绑定知识库"));
        verify(ragRetrievalService, never()).retrieveContents(anyString(), anyList(), any());
    }

    // ==================== 知识库三级优先级与传参形态 ====================

    @Test
    void execute_kbPriority_inputOverridesSceneConfigAndAgent() {
        AiSceneConfig config = new AiSceneConfig();
        config.setKnowledgeLibraryIds("[2]");
        Agent agent = new Agent();
        agent.setKnowledgeLibraryIds("[3]");
        when(agentMapper.selectById(any())).thenReturn(agent);
        when(ragRetrievalService.retrieveContents(anyString(), anyList(), any())).thenReturn(List.of());

        newHandler().execute(request("问题", Map.of("knowledgeLibraryIds", List.of(1))), config);

        ArgumentCaptor<List<Long>> captor = ArgumentCaptor.forClass(List.class);
        verify(ragRetrievalService).retrieveContents(eq("问题"), captor.capture(), any());
        assertEquals(List.of(1L), captor.getValue());
    }

    @Test
    void execute_kbPriority_sceneConfigOverridesAgent() {
        AiSceneConfig config = new AiSceneConfig();
        config.setKnowledgeLibraryIds("[2,5]");
        Agent agent = new Agent();
        agent.setKnowledgeLibraryIds("[3]");
        config.setAgentId(9L);
        when(agentMapper.selectById(9L)).thenReturn(agent);
        when(ragRetrievalService.retrieveContents(anyString(), anyList(), any())).thenReturn(List.of());

        newHandler().execute(request("问题", null), config);

        ArgumentCaptor<List<Long>> captor = ArgumentCaptor.forClass(List.class);
        verify(ragRetrievalService).retrieveContents(anyString(), captor.capture(), any());
        assertEquals(List.of(2L, 5L), captor.getValue());
    }

    @Test
    void execute_kbForms_jsonArrayStringAndCommaStringParsed() {
        when(ragRetrievalService.retrieveContents(anyString(), anyList(), any())).thenReturn(List.of());

        // JSON 数组串（场景配置形态）
        AiSceneConfig config = new AiSceneConfig();
        config.setKnowledgeLibraryIds("[7,8]");
        newHandler().execute(request("问题", null), config);
        ArgumentCaptor<List<Long>> jsonCaptor = ArgumentCaptor.forClass(List.class);
        verify(ragRetrievalService).retrieveContents(anyString(), jsonCaptor.capture(), any());
        assertEquals(List.of(7L, 8L), jsonCaptor.getValue());

        // 逗号分隔串（input 手填形态）
        Mockito.clearInvocations(ragRetrievalService);
        when(ragRetrievalService.retrieveContents(anyString(), anyList(), any())).thenReturn(List.of());
        newHandler().execute(request("问题", Map.of("knowledgeLibraryIds", "10, 20")), new AiSceneConfig());
        ArgumentCaptor<List<Long>> commaCaptor = ArgumentCaptor.forClass(List.class);
        verify(ragRetrievalService).retrieveContents(anyString(), commaCaptor.capture(), any());
        assertEquals(List.of(10L, 20L), commaCaptor.getValue());
    }

    @Test
    void execute_illegalKbEntries_ignoredWithoutFailure() {
        when(ragRetrievalService.retrieveContents(anyString(), anyList(), any())).thenReturn(List.of());
        newHandler().execute(request("问题", Map.of("knowledgeLibraryIds", List.of("abc", "5", ""))), new AiSceneConfig());

        ArgumentCaptor<List<Long>> captor = ArgumentCaptor.forClass(List.class);
        verify(ragRetrievalService).retrieveContents(anyString(), captor.capture(), any());
        assertEquals(List.of(5L), captor.getValue(), "非法项忽略，合法项保留");
    }

    // ==================== 检索为空：零 token 标准响应 ====================

    @Test
    void execute_emptyRetrieval_returnsStandardAnswerWithoutLlm() {
        when(ragRetrievalService.retrieveContents(anyString(), anyList(), any())).thenReturn(List.of());
        AiSceneConfig config = new AiSceneConfig();
        config.setKnowledgeLibraryIds("[1]");

        AiExecuteResponse<?> resp = newHandler().execute(request("问题", null), config);

        assertEquals(AiErrorCodes.SUCCESS, resp.getCode());
        KnowledgeQaSceneData data = assertInstanceOf(KnowledgeQaSceneData.class, resp.getData());
        assertTrue(data.getAnswer().contains("不在我的知识库范围内"));
        assertEquals(0, data.getRetrievalCount());
        assertTrue(data.getReferences().isEmpty());
        assertEquals(0, data.getImageCount());
        // 未消费提示词构建与 LLM（零 token）
        verify(chatContextBuilderService, never()).buildRagContext(any());
    }

    // ==================== 正常链路：回答 + 引用溯源 ====================

    @Test
    void execute_llmSuccess_returnsAnswerWithReferences() {
        Content c1 = content("火箭发动机是……（超过200字符的完整片段内容，用于验证摘录截断逻辑："
                .repeat(10) + "结尾", "火箭原理.pdf", "3", "5");
        Content c2 = content("推进剂组合", "燃料手册.pdf", "12", "7");
        List<Content> contents = List.of(c1, c2);
        when(ragRetrievalService.retrieveContents(anyString(), anyList(), any())).thenReturn(contents);
        when(ragRetrievalService.getContentRerankScores()).thenReturn(Map.of(c2, 0.87));
        AiSceneConfig config = new AiSceneConfig();
        config.setKnowledgeLibraryIds("[1]");

        AiExecuteResponse<?> resp = newHandler().execute(request("什么是火箭发动机？", null), config);

        assertEquals(AiErrorCodes.SUCCESS, resp.getCode());
        KnowledgeQaSceneData data = assertInstanceOf(KnowledgeQaSceneData.class, resp.getData());
        assertEquals("这是基于知识库的回答", data.getAnswer());
        assertEquals(2, data.getRetrievalCount());
        assertEquals(2, data.getReferences().size());

        KnowledgeQaSceneData.Reference r1 = data.getReferences().get(0);
        assertEquals("火箭原理.pdf", r1.getFileName());
        assertEquals("pdf", r1.getFileType());
        assertEquals("3", r1.getPageNumber());
        assertEquals("5", r1.getSegmentIndex());
        assertEquals("12", r1.getKnowledgeBaseId());
        assertNull(r1.getRerankScore(), "不在重排分数表中的片段分数为空");
        assertTrue(r1.getExcerpt().length() <= 201, "摘录截断到200字符+省略号");
        assertTrue(r1.getExcerpt().endsWith("…"));

        KnowledgeQaSceneData.Reference r2 = data.getReferences().get(1);
        assertEquals(0.87, r2.getRerankScore());
        assertEquals(0, data.getImageCount());

        // metadata 可观测（模型/Token 透出）
        assertEquals("deepseek-chat", resp.getMetadata().getModelUsed());
        assertEquals(100, resp.getMetadata().getTokenUsed());
    }

    @Test
    void execute_sceneTemplateOverridesSystemPrompt() {
        when(ragRetrievalService.retrieveContents(anyString(), anyList(), any()))
                .thenReturn(List.of(content("片段", "a.pdf", "1", "1")));
        AiSceneConfig config = new AiSceneConfig();
        config.setKnowledgeLibraryIds("[1]");
        config.setSystemPromptTemplate("你是{{role}}，请回答问题");

        AiExecuteRequest request = request("问题", Map.of("role", "航天专家"));
        newHandler().execute(request, config);

        // 模板渲染生效，未走 chat 链系统提示词
        verify(chatContextBuilderService, never()).buildSystemPrompt(any(), anyBoolean(), any());
    }

    // ==================== LLM 失败与降级 ====================

    @Test
    void execute_llmBlank_failsWithRetrievalContext() {
        llmOutcome = new ChatOutcome(); // text=null
        when(ragRetrievalService.retrieveContents(anyString(), anyList(), any()))
                .thenReturn(List.of(content("片段", "a.pdf", "1", "1")));
        AiSceneConfig config = new AiSceneConfig();
        config.setKnowledgeLibraryIds("[1]");

        AiExecuteResponse<?> resp = newHandler().execute(request("问题", null), config);

        assertEquals(AiErrorCodes.AI_CALL_FAILED, resp.getCode());
        assertTrue(resp.getMsg().contains("已检索到 1 条相关内容"));
    }

    @Test
    void execute_agentLoadFails_fallsBackToBareAgentDefaults() {
        AiSceneConfig config = new AiSceneConfig();
        config.setAgentId(99L);
        config.setKnowledgeLibraryIds("[1]"); // 场景级兜底，Agent 加载失败仍可用
        when(agentMapper.selectById(99L)).thenThrow(new RuntimeException("db down"));
        when(ragRetrievalService.retrieveContents(anyString(), anyList(), any())).thenReturn(List.of());

        AiExecuteResponse<?> resp = newHandler().execute(request("问题", null), config);

        assertEquals(AiErrorCodes.SUCCESS, resp.getCode(), "Agent 加载失败不阻断（空壳默认 RAG 配置）");
        verify(ragRetrievalService).retrieveContents(anyString(), eq(List.of(1L)), any(Agent.class));
    }

    // ==================== ThreadLocal 卫生 ====================

    @Test
    void execute_rerankScoresThreadLocal_alwaysCleared() {
        // 分支一：检索为空
        when(ragRetrievalService.retrieveContents(anyString(), anyList(), any())).thenReturn(List.of());
        AiSceneConfig config = new AiSceneConfig();
        config.setKnowledgeLibraryIds("[1]");
        newHandler().execute(request("问题", null), config);
        verify(ragRetrievalService).clearContentRerankScores();

        // 分支二：正常链路
        Mockito.clearInvocations(ragRetrievalService);
        when(ragRetrievalService.retrieveContents(anyString(), anyList(), any()))
                .thenReturn(List.of(content("片段", "a.pdf", "1", "1")));
        newHandler().execute(request("问题", null), config);
        verify(ragRetrievalService).clearContentRerankScores();
    }
}
