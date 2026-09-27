package com.moyun;

import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.moyun.common.config.MinioConfig;
import com.moyun.core.base.model.LoginUser;
import com.moyun.core.config.ServerConfig;
import com.moyun.ext.ai.entity.Agent;
import com.moyun.ext.ai.entity.KnowledgeBase;
import com.moyun.ext.ai.mapper.KnowledgeBaseMapper;
import com.moyun.ext.ai.service.AgentService;
import com.moyun.ext.ai.service.AiGlobalSwitch;
import com.moyun.ext.ai.service.KnowledgeConfigService;
import com.moyun.ext.ai.service.MinioService;
import com.moyun.ext.ai.service.chat.RagRetrievalService;
import com.moyun.ext.ai.service.impl.KnowledgeBaseServiceImpl;
import com.moyun.ext.aigateway.model.AiExecuteResponse;
import com.moyun.ext.aigateway.model.data.GenericSceneData;
import com.moyun.ext.aigateway.service.AiGatewayService;
import com.moyun.ext.aigateway.support.AiSceneJsonClient;
import com.moyun.ext.cms.domain.vo.VoiceInterviewVO;
import com.moyun.ext.cms.domain.vo.VoiceStartConfig;
import com.moyun.ext.cms.service.impl.PortalJobTemplateServiceImpl;
import com.moyun.ext.cms.service.impl.VoiceInterviewServiceImpl;
import com.moyun.ext.cms.service.interview.InterviewAgentClient;
import com.moyun.ext.cms.service.interview.InterviewChatMemoryService;
import com.moyun.ext.file.domain.entity.SysFile;
import com.moyun.ext.file.mapper.SysFileMapper;
import com.moyun.ext.file.service.impl.SysFileServiceImpl;
import com.moyun.portal.domain.entity.PortalTopic;
import com.moyun.portal.domain.entity.PortalVoiceInterview;
import com.moyun.portal.domain.entity.PortalVoiceInterviewQA;
import com.moyun.portal.mapper.PortalTopicMapper;
import com.moyun.portal.mapper.PortalVoiceInterviewEventMapper;
import com.moyun.portal.mapper.PortalVoiceInterviewMapper;
import com.moyun.portal.mapper.PortalVoiceInterviewQAMapper;
import com.moyun.portal.service.impl.PortalTopicServiceImpl;
import com.moyun.system.service.ISysConfigService;
import com.moyun.util.file.MinioUtils;
import org.apache.ibatis.builder.MapperBuilderAssistant;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.transaction.support.TransactionTemplate;

import javax.sql.DataSource;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * 运行时探针：证明 v13.14 收窄过的三处"事务内远程 IO"**真的**变成了"事务外 IO + 事务内 DB 写"
 *
 * <h3>为什么静态守卫之外还要运行时探针</h3>
 * <p>静态扫描只能看代码形态（有没有把 IO 写在 {@code @Transactional} 方法体里），
 * 看不到<b>运行时事务是否真的开着</b>：{@code TransactionTemplate} 的传播行为、嵌套事务、
 * 以及"注解没生效"（同类自调用）都会让静态结论失真。这里用
 * {@link TransactionSynchronizationManager#isActualTransactionActive()} 在<b>真实方法调用过程中</b>
 * 取两次观测：一次在远程 IO 打桩点、一次在 DB 写打桩点。</p>
 *
 * <h3>可观测性怎么来的</h3>
 * <p>用真实的 {@link DataSourceTransactionManager}（配一个假 {@code DataSource}，不会真连库）
 * 构造真实 {@link TransactionTemplate}——事务开启时 Spring 会把"当前线程有活跃事务"写进
 * {@code TransactionSynchronizationManager}，于是打桩点能观测到真实翻转。
 * {@link #probeCanObserveActiveTransaction()} 是这条探针自身的灵敏度自检：
 * 如果它失败，说明下面的断言全都在"永远观测到 false"的假象里通过，等于没测。</p>
 *
 * @author moyun
 */
class TransactionRemoteIoRuntimeProbeTest {

    /** 真实事务模板 + 假 DataSource（doBegin/commit/close 都只操作 mock，不产生真实连接） */
    private static TransactionTemplate realTransactionTemplate() {
        DataSource dataSource = mock(DataSource.class);
        try {
            when(dataSource.getConnection()).thenReturn(mock(Connection.class));
        } catch (SQLException e) {
            throw new IllegalStateException("桩 DataSource 不应抛 SQLException", e);
        }
        return new TransactionTemplate(new DataSourceTransactionManager(dataSource));
    }

    /**
     * {@code SysFileServiceImpl} 通过 {@code SecurityUtils} 取当前登录用户；
     * 单测里没有 SecurityContext 会抛"获取用户信息异常"，这里挂一个最小登录态（只提供 userId/username）。
     */
    @BeforeEach
    void bindLoginUser() {
        LoginUser loginUser = mock(LoginUser.class);
        when(loginUser.getUserId()).thenReturn(7L);
        when(loginUser.getUsername()).thenReturn("probe");
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(loginUser, null, List.of()));
    }

    @AfterEach
    void clearLoginUser() {
        SecurityContextHolder.clearContext();
    }

    @Test
    @DisplayName("探针灵敏度自检：事务块内必须观测到活跃事务（否则下面的断言等于白写）")
    void probeCanObserveActiveTransaction() {
        AtomicBoolean inTx = new AtomicBoolean(false);
        realTransactionTemplate().executeWithoutResult(
                status -> inTx.set(TransactionSynchronizationManager.isActualTransactionActive()));
        assertTrue(inTx.get(), "事务块内必须能观测到活跃事务，否则本探针的结论不可信");
        assertFalse(TransactionSynchronizationManager.isActualTransactionActive(),
                "事务结束后不应残留活跃事务（线程复用会串味）");
    }

    @Test
    @DisplayName("SysFileServiceImpl#uploadFile：MinIO 上传在事务外，sys_file 落库在事务内")
    void sysFileUploadStorageOutsideDbWriteInside() {
        SysFileServiceImpl service = new SysFileServiceImpl();
        SysFileMapper mapper = mock(SysFileMapper.class);
        MinioUtils minioUtils = mock(MinioUtils.class);
        MinioConfig minioConfig = new MinioConfig();
        minioConfig.setEnabled(true);
        minioConfig.setFallbackToLocal(false);
        minioConfig.setAutoFallback(false);
        minioConfig.setBucketName("moyun");
        ISysConfigService sysConfigService = mock(ISysConfigService.class);
        when(sysConfigService.selectConfigByKey("file.storage.mode")).thenReturn("minio");

        ReflectionTestUtils.setField(service, "sysFileMapper", mapper);
        ReflectionTestUtils.setField(service, "minioUtils", minioUtils);
        ReflectionTestUtils.setField(service, "minioConfig", minioConfig);
        ReflectionTestUtils.setField(service, "serverConfig", new ServerConfig());
        ReflectionTestUtils.setField(service, "sysConfigService", sysConfigService);
        ReflectionTestUtils.setField(service, "transactionTemplate", realTransactionTemplate());

        AtomicBoolean storageCalled = new AtomicBoolean(false);
        AtomicBoolean storageInTx = new AtomicBoolean(false);
        when(minioUtils.uploadFile(any())).thenAnswer(invocation -> {
            storageCalled.set(true);
            storageInTx.set(TransactionSynchronizationManager.isActualTransactionActive());
            return "http://minio.test/moyun/upload/2026/05/01/demo.txt";
        });

        AtomicBoolean dbCalled = new AtomicBoolean(false);
        AtomicBoolean dbInTx = new AtomicBoolean(false);
        when(mapper.insert(any(SysFile.class))).thenAnswer(invocation -> {
            dbCalled.set(true);
            dbInTx.set(TransactionSynchronizationManager.isActualTransactionActive());
            return 1;
        });

        MockMultipartFile file = new MockMultipartFile("file", "demo.txt", "text/plain",
                "hello moyun".getBytes(StandardCharsets.UTF_8));
        SysFile saved = service.uploadFile(file, "voice", "1");

        assertTrue(storageCalled.get(), "MinIO 上传必须真的被调用（否则断言空转）");
        assertFalse(storageInTx.get(), "MinIO 上传必须发生在事务外：旧实现整个方法 @Transactional，上传期间一直占着 DB 连接");
        assertTrue(dbCalled.get(), "sys_file 落库必须真的被调用");
        assertTrue(dbInTx.get(), "sys_file 落库必须仍在事务内（语义不变：DB 写失败仍回滚）");
        assertEquals("minio", saved.getStorageType());
        assertEquals("demo.txt", saved.getFileName());
        assertEquals("http://minio.test/moyun/upload/2026/05/01/demo.txt", saved.getFileUrl());

        // 证伪实验（运行时等价于"修复前红"）：旧实现是整个方法 @Transactional，等价于"外部事务包住 uploadFile"。
        // 同一探针、同一断言点，只改事务环境——必须翻转成 true，否则说明上面那条断言没有鉴别力。
        AtomicBoolean storageInTxWhenWrapped = new AtomicBoolean(false);
        when(minioUtils.uploadFile(any())).thenAnswer(invocation -> {
            storageInTxWhenWrapped.set(TransactionSynchronizationManager.isActualTransactionActive());
            return "http://minio.test/moyun/upload/2026/05/01/demo2.txt";
        });
        realTransactionTemplate().executeWithoutResult(status -> service.uploadFile(
                new MockMultipartFile("file", "demo2.txt", "text/plain", "x".getBytes(StandardCharsets.UTF_8)),
                "voice", "2"));
        assertTrue(storageInTxWhenWrapped.get(),
                "证伪实验失败：外部事务包住整个上传方法（等价于修复前的类/方法级 @Transactional）时，"
                        + "探针必须观测到存储 IO 处于活跃事务内");
    }

    @Test
    @DisplayName("KnowledgeBaseServiceImpl#uploadFileOnly：MinIO 上传在事务外，记录+默认配置在事务内")
    void knowledgeBaseUploadStorageOutsideDbWriteInside() throws Exception {
        KnowledgeBaseMapper mapper = mock(KnowledgeBaseMapper.class);
        MinioService minioService = mock(MinioService.class);
        KnowledgeConfigService knowledgeConfigService = mock(KnowledgeConfigService.class);
        KnowledgeBaseServiceImpl service = new KnowledgeBaseServiceImpl(
                null, null, null, null, null, null, null, null,
                minioService, null, null, knowledgeConfigService);
        ReflectionTestUtils.setField(service, "baseMapper", mapper);
        ReflectionTestUtils.setField(service, "transactionTemplate", realTransactionTemplate());

        AtomicBoolean storageCalled = new AtomicBoolean(false);
        AtomicBoolean storageInTx = new AtomicBoolean(false);
        when(minioService.uploadKnowledgeFile(any(), any())).thenAnswer(invocation -> {
            storageCalled.set(true);
            storageInTx.set(TransactionSynchronizationManager.isActualTransactionActive());
            return "knowledge/2026/05/01/demo.pdf";
        });

        AtomicBoolean dbCalled = new AtomicBoolean(false);
        AtomicBoolean dbInTx = new AtomicBoolean(false);
        when(mapper.insert(any(KnowledgeBase.class))).thenAnswer(invocation -> {
            dbCalled.set(true);
            dbInTx.set(TransactionSynchronizationManager.isActualTransactionActive());
            return 1;
        });

        AtomicBoolean configInTx = new AtomicBoolean(false);
        doAnswer(invocation -> {
            configInTx.set(TransactionSynchronizationManager.isActualTransactionActive());
            return null;
        }).when(knowledgeConfigService).createDefaultConfig(any());

        MockMultipartFile file = new MockMultipartFile("file", "demo.pdf", "application/pdf",
                "pdf-bytes".getBytes(StandardCharsets.UTF_8));
        var knowledge = service.uploadFileOnly(file);

        assertTrue(storageCalled.get(), "MinIO 上传必须真的被调用");
        assertFalse(storageInTx.get(), "MinIO 上传必须在事务外（v13.14 前整个方法 @Transactional）");
        assertTrue(dbCalled.get(), "知识库记录必须真的落库");
        assertTrue(dbInTx.get(), "知识库记录落库必须在事务内");
        assertTrue(configInTx.get(), "默认配置创建必须与记录落库同一个事务（原子性不变）");
        assertEquals("demo.pdf", knowledge.getFileName());
        assertEquals("knowledge/2026/05/01/demo.pdf", knowledge.getFilePath());

        // 证伪实验（运行时等价于"修复前红"）：旧实现是整个方法 @Transactional
        AtomicBoolean storageInTxWhenWrapped = new AtomicBoolean(false);
        when(minioService.uploadKnowledgeFile(any(), any())).thenAnswer(invocation -> {
            storageInTxWhenWrapped.set(TransactionSynchronizationManager.isActualTransactionActive());
            return "knowledge/2026/05/01/demo2.pdf";
        });
        realTransactionTemplate().executeWithoutResult(status -> {
            try {
                service.uploadFileOnly(new MockMultipartFile("file", "demo2.pdf", "application/pdf",
                        "pdf2".getBytes(StandardCharsets.UTF_8)));
            } catch (Exception e) {
                throw new IllegalStateException(e);
            }
        });
        assertTrue(storageInTxWhenWrapped.get(),
                "证伪实验失败：外部事务包住 uploadFileOnly（等价于修复前的方法级 @Transactional）时，"
                        + "探针必须观测到 MinIO 上传处于活跃事务内");
    }

    @Test
    @DisplayName("VoiceInterviewServiceImpl#start：RAG/预热/开场白 LLM 全在事务外，收口+建会话+首题在事务内")
    void voiceInterviewStartRemoteIoOutsideDbWriteInside() {
        VoiceInterviewServiceImpl service = new VoiceInterviewServiceImpl();
        PortalVoiceInterviewMapper interviewMapper = mock(PortalVoiceInterviewMapper.class);
        PortalVoiceInterviewQAMapper qaMapper = mock(PortalVoiceInterviewQAMapper.class);
        PortalVoiceInterviewEventMapper eventMapper = mock(PortalVoiceInterviewEventMapper.class);
        InterviewAgentClient agentClient = mock(InterviewAgentClient.class);
        AiSceneJsonClient aiSceneJsonClient = mock(AiSceneJsonClient.class);
        AiGlobalSwitch aiGlobalSwitch = mock(AiGlobalSwitch.class);
        RagRetrievalService ragRetrievalService = mock(RagRetrievalService.class);
        AgentService agentService = mock(AgentService.class);
        InterviewChatMemoryService memoryService = mock(InterviewChatMemoryService.class);
        ISysConfigService sysConfigService = mock(ISysConfigService.class);
        Agent agent = mock(Agent.class);
        when(agent.getId()).thenReturn(5L);
        when(agent.getMaxHistoryTurns()).thenReturn(5);
        when(agent.getKnowledgeLibraryIds()).thenReturn("1");
        when(agent.getSystemPrompt()).thenReturn("你是一位资深面试官");
        when(agentClient.resolveAgent(null)).thenReturn(agent);
        when(agentClient.isEnabled()).thenReturn(true);
        when(agentClient.agentName(5L)).thenReturn("AI 面试官");
        when(agentService.getKnowledgeBaseIds(5L)).thenReturn(List.of(1L));
        when(aiGlobalSwitch.isEnabled()).thenReturn(true);

        ReflectionTestUtils.setField(service, "interviewMapper", interviewMapper);
        ReflectionTestUtils.setField(service, "qaMapper", qaMapper);
        ReflectionTestUtils.setField(service, "eventMapper", eventMapper);
        ReflectionTestUtils.setField(service, "agentClient", agentClient);
        ReflectionTestUtils.setField(service, "aiSceneJsonClient", aiSceneJsonClient);
        ReflectionTestUtils.setField(service, "aiGlobalSwitch", aiGlobalSwitch);
        ReflectionTestUtils.setField(service, "ragRetrievalService", ragRetrievalService);
        ReflectionTestUtils.setField(service, "agentService", agentService);
        ReflectionTestUtils.setField(service, "memoryService", memoryService);
        ReflectionTestUtils.setField(service, "sysConfigService", sysConfigService);
        ReflectionTestUtils.setField(service, "objectMapper", new ObjectMapper());
        ReflectionTestUtils.setField(service, "transactionTemplate", realTransactionTemplate());

        AtomicBoolean ragCalled = new AtomicBoolean(false);
        AtomicBoolean ragInTx = new AtomicBoolean(false);
        when(ragRetrievalService.retrieveContents(any(), any(), any())).thenAnswer(invocation -> {
            ragCalled.set(true);
            ragInTx.set(TransactionSynchronizationManager.isActualTransactionActive());
            return List.of();
        });

        AtomicBoolean warmupCalled = new AtomicBoolean(false);
        AtomicBoolean warmupInTx = new AtomicBoolean(false);
        when(aiSceneJsonClient.executeForJson(any(), any(), any())).thenAnswer(invocation -> {
            warmupCalled.set(true);
            warmupInTx.set(TransactionSynchronizationManager.isActualTransactionActive());
            return null;
        });

        AtomicBoolean openingCalled = new AtomicBoolean(false);
        AtomicBoolean openingInTx = new AtomicBoolean(false);
        when(agentClient.chat(any(), any())).thenAnswer(invocation -> {
            openingCalled.set(true);
            openingInTx.set(TransactionSynchronizationManager.isActualTransactionActive());
            return "欢迎参加本次面试。\n\n请先做个自我介绍。";
        });

        AtomicBoolean insertCalled = new AtomicBoolean(false);
        AtomicBoolean insertInTx = new AtomicBoolean(false);
        when(interviewMapper.selectList(any())).thenReturn(List.of());
        when(interviewMapper.insert(any(PortalVoiceInterview.class))).thenAnswer(invocation -> {
            PortalVoiceInterview inserted = invocation.getArgument(0);
            insertCalled.set(true);
            insertInTx.set(TransactionSynchronizationManager.isActualTransactionActive());
            inserted.setId(1L);
            return 1;
        });
        AtomicBoolean qaInsertInTx = new AtomicBoolean(false);
        when(qaMapper.insert(any(PortalVoiceInterviewQA.class))).thenAnswer(invocation -> {
            PortalVoiceInterviewQA inserted = invocation.getArgument(0);
            qaInsertInTx.set(TransactionSynchronizationManager.isActualTransactionActive());
            inserted.setId(2L);
            return 1;
        });
        // 刻意保留在事务内的 Redis 滑窗初始化（口径见 TransactionRemoteIoGuardTest 类注释：
        // Redis 失败就该回滚"建会话"，否则会留下"有会话无记忆"的降级态）
        AtomicBoolean memoryInTx = new AtomicBoolean(false);
        doAnswer(invocation -> {
            memoryInTx.set(TransactionSynchronizationManager.isActualTransactionActive());
            return null;
        }).when(memoryService).initFirstTurn(any(), any(), any(), any(), any());

        VoiceStartConfig config = new VoiceStartConfig();
        config.setPosition("Java 后端");
        config.setDifficulty("medium");
        config.setQuestionCount(5);
        VoiceInterviewVO vo = service.start(7L, config);

        assertTrue(ragCalled.get(), "RAG 检索必须真的被调用");
        assertFalse(ragInTx.get(), "RAG 检索必须在事务外（旧实现整个 start() 是 @Transactional）");
        assertTrue(warmupCalled.get(), "warmup LLM 调用必须真的被调用");
        assertFalse(warmupInTx.get(), "warmup LLM 调用必须在事务外");
        assertTrue(openingCalled.get(), "开场白 LLM 调用必须真的被调用");
        assertFalse(openingInTx.get(), "开场白 LLM 调用必须在事务外（这是开面最慢的一步）");
        assertTrue(insertCalled.get(), "面试会话必须真的落库");
        assertTrue(insertInTx.get(), "面试会话落库必须在事务内");
        assertTrue(qaInsertInTx.get(), "首题落库必须与会话落库同一事务");
        assertTrue(memoryInTx.get(), "Redis 滑窗初始化按既定口径保留在事务内");
        assertEquals(1L, vo.getId());
        assertEquals("in_progress", vo.getStatus());
        assertTrue(vo.getGreetText().contains("自我介绍"), "开场白应回填到 VO.greetText");
    }

    // ========================================================================
    // 目标点名的另外两处：PortalJobTemplateServiceImpl#extractKeywords
    // 与 PortalTopicServiceImpl#aiGenerateTopicDraft。
    // 这两处的 LLM 调用**本来就在无事务方法里**（复核证据：方法上无 @Transactional；
    // 全仓唯一调用方分别是 CmsJobTemplateController#extractKeywords 与
    // CmsTopicController#aiGenerateTopicDraft，均为无事务 Controller；
    // 同类 @Transactional 方法 bindQuestions/auditTopic 等不调用它们）。
    // 因此这两条探针没有"修复前红"的历史——它们的作用是把"非缺陷"这一判定
    // 从"读代码得出的结论"变成"运行时可证伪的断言"，并配一组**证伪实验**：
    // 同一段代码一旦被事务包住，探针必须翻转，否则断言本身没有鉴别力。
    // ========================================================================

    @Test
    @DisplayName("PortalJobTemplateServiceImpl#extractKeywords：LLM 提取运行态无活跃事务（+ 证伪实验必须翻转）")
    void jobTemplateLlmKeywordExtractionRunsWithoutTransaction() throws Exception {
        PortalJobTemplateServiceImpl service = new PortalJobTemplateServiceImpl();
        AiSceneJsonClient aiSceneJsonClient = mock(AiSceneJsonClient.class);
        AiGlobalSwitch aiGlobalSwitch = mock(AiGlobalSwitch.class);
        when(aiGlobalSwitch.isEnabled()).thenReturn(true);
        ReflectionTestUtils.setField(service, "aiSceneJsonClient", aiSceneJsonClient);
        ReflectionTestUtils.setField(service, "aiGlobalSwitch", aiGlobalSwitch);

        ObjectMapper objectMapper = new ObjectMapper();
        AtomicBoolean llmCalled = new AtomicBoolean(false);
        AtomicBoolean llmInTx = new AtomicBoolean(false);
        when(aiSceneJsonClient.executeForJson(any(), any(), any())).thenAnswer(invocation -> {
            llmCalled.set(true);
            llmInTx.set(TransactionSynchronizationManager.isActualTransactionActive());
            return objectMapper.readTree("{\"keywords\":[\"Redis\",\"MySQL\"]}");
        });

        List<String> keywords = service.extractKeywords("熟悉 Redis 与 MySQL，负责缓存与持久化");
        assertTrue(llmCalled.get(), "LLM 关键词提取必须真的被调用（否则断言空转）");
        assertFalse(llmInTx.get(), "真实入口（无事务 Controller）下 LLM 调用不得出现活跃事务");
        assertEquals(List.of("Redis", "MySQL"), keywords, "结果清洗逻辑不受本批影响");

        // 证伪实验：包一层真实事务后再调用，探针必须观测到活跃事务
        AtomicBoolean wrappedInTx = new AtomicBoolean(false);
        when(aiSceneJsonClient.executeForJson(any(), any(), any())).thenAnswer(invocation -> {
            wrappedInTx.set(TransactionSynchronizationManager.isActualTransactionActive());
            return objectMapper.readTree("{\"keywords\":[\"Redis\"]}");
        });
        realTransactionTemplate().executeWithoutResult(status -> service.extractKeywords("JD 内容"));
        assertTrue(wrappedInTx.get(),
                "证伪实验失败：同一段代码被事务包住时探针仍未观测到事务 → 上面的断言没有鉴别力");
    }

    @Test
    @DisplayName("PortalTopicServiceImpl#aiGenerateTopicDraft：网关 LLM 调用运行态无活跃事务")
    void topicAiDraftGatewayCallRunsWithoutTransaction() {
        // 纯单测环境没有 MyBatis-Plus 启动流程，LambdaQueryWrapper 的 lambda 列解析依赖实体 TableInfo 缓存，
        // 需手动初始化（MP 官方单测做法，同 TipPayCallbackHandlerTest / VipServiceImplGrantCardTest）
        TableInfoHelper.initTableInfo(
                new MapperBuilderAssistant(new MybatisConfiguration(), ""), PortalTopic.class);

        PortalTopicServiceImpl service = new PortalTopicServiceImpl();
        PortalTopicMapper topicMapper = mock(PortalTopicMapper.class);
        AiGatewayService aiGatewayService = mock(AiGatewayService.class);
        ReflectionTestUtils.setField(service, "baseMapper", topicMapper);
        ReflectionTestUtils.setField(service, "aiGatewayService", aiGatewayService);
        when(topicMapper.selectList(any())).thenReturn(List.of());

        GenericSceneData generic = new GenericSceneData();
        generic.setStructured(Map.of("title", "标题", "description", "描述", "category", "后端"));

        AtomicBoolean gatewayCalled = new AtomicBoolean(false);
        AtomicBoolean gatewayInTx = new AtomicBoolean(false);
        when(aiGatewayService.execute(any())).thenAnswer(invocation -> {
            gatewayCalled.set(true);
            gatewayInTx.set(TransactionSynchronizationManager.isActualTransactionActive());
            AiExecuteResponse<GenericSceneData> response = AiExecuteResponse.success(generic);
            response.setRequestId("probe-req-1");
            return response;
        });

        Map<String, Object> draft = service.aiGenerateTopicDraft("后端");

        assertTrue(gatewayCalled.get(), "网关 LLM 调用必须真的被调用");
        assertFalse(gatewayInTx.get(), "真实入口（无事务 Controller）下网关调用不得出现活跃事务");
        assertEquals("标题", draft.get("title"));
        assertEquals("probe-req-1", draft.get("requestId"));
    }
}
