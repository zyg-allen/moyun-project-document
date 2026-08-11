package com.moyun.ext.ai.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.moyun.ext.ai.config.KnowledgeDefaults;
import com.moyun.ext.ai.entity.KnowledgeConfig;
import com.moyun.ext.ai.entity.KnowledgeLibraryConfig;
import com.moyun.ext.ai.mapper.KnowledgeConfigMapper;
import com.moyun.ext.ai.mapper.KnowledgeConfigTemplateMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

/**
 * {@link KnowledgeConfigServiceImpl} 单元测试
 *
 * <p>测试策略：
 * <ul>
 *   <li>聚焦于 {@code resolveEffectiveConfig} 的三层合并语义：
 *       "KnowledgeDefaults 默认 ← 库级默认 ← 文档级覆盖"（P2-2 阶段 3：硬编码已迁至配置类）</li>
 *   <li>通过 mock {@link KnowledgeConfigMapper} 控制文档级配置返回值</li>
 *   <li>通过 {@link ReflectionTestUtils} 注入真实 {@link KnowledgeDefaults} 实例（携带 Java 默认值）</li>
 *   <li>验证旧版有损转换遗漏的 embeddingModel / rerankModel 字段已正确合并（P2-2 P0 修复回归基线）</li>
 *   <li>验证 P2-2 阶段 2 中 reprocessFile 路径所依赖的 null libraryConfig 场景</li>
 * </ul>
 *
 * @author moyun
 */
@DisplayName("知识库配置服务 KnowledgeConfigService")
@ExtendWith(MockitoExtension.class)
class KnowledgeConfigServiceImplTest {

    @Mock
    private KnowledgeConfigMapper configMapper;

    @Mock
    private KnowledgeConfigTemplateMapper templateMapper;

    @InjectMocks
    private KnowledgeConfigServiceImpl service;

    private static final Long KNOWLEDGE_ID = 1001L;

    @BeforeEach
    void setUp() {
        // 默认无文档级配置（many scenarios need null docConfig）
        lenient().when(configMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(null);

        // P2-2 阶段 3：createDefaultConfigObject 已委托给 KnowledgeDefaults，
        // @InjectMocks 不会自动注入 @ConfigurationProperties Bean，需手动注入真实实例（携带 Java 默认值）
        KnowledgeDefaults knowledgeDefaults = new KnowledgeDefaults();
        ReflectionTestUtils.setField(service, "knowledgeDefaults", knowledgeDefaults);
    }

    // ====================================================================
    // resolveEffectiveConfig - 三层合并
    // ====================================================================

    @Nested
    @DisplayName("resolveEffectiveConfig 三层合并")
    class ResolveEffectiveConfig {

        @Test
        @DisplayName("libraryConfig=null 且 docConfig=null → 返回 KnowledgeDefaults 默认（永不为 null）")
        void shouldReturnHardcodedDefaultsWhenBothNull() {
            KnowledgeConfig result = service.resolveEffectiveConfig(KNOWLEDGE_ID, null);

            assertThat(result).isNotNull();
            assertThat(result.getKnowledgeId()).isEqualTo(KNOWLEDGE_ID);
            // P2-2 阶段 3：默认值来自 KnowledgeDefaults（统一为 800/100/hybrid/10，
            // 与 KnowledgeLibraryServiceImpl.createLibrary 默认值对齐）
            assertThat(result.getSegmentMode()).isEqualTo("general");
            assertThat(result.getSegmentSeparator()).isEqualTo("\n\n");
            assertThat(result.getSegmentMaxLength()).isEqualTo(800);
            assertThat(result.getSegmentOverlapLength()).isEqualTo(100);
            assertThat(result.getPreprocessReplaceSpaces()).isTrue();
            assertThat(result.getPreprocessRemoveUrls()).isTrue();
            assertThat(result.getPreprocessRemoveExtraNewlines()).isTrue();
            assertThat(result.getIndexMode()).isEqualTo("high_quality");
            assertThat(result.getRetrievalMode()).isEqualTo("hybrid");
            assertThat(result.getRetrievalTopK()).isEqualTo(10);
            assertThat(result.getRerankEnabled()).isFalse();
            // 文档级独有字段也由 KnowledgeDefaults 提供默认值（与 getKnowledgeConfig 对齐）
            assertThat(result.getChunkingStrategy()).isEqualTo("fixed");
            assertThat(result.getDocumentType()).isEqualTo("general");
            assertThat(result.getEnableSmartBoundary()).isTrue();
            // 时间戳兜底（P0-1 规范）
            assertThat(result.getCreatedAt()).isNotNull();
            assertThat(result.getUpdatedAt()).isNotNull();
        }

        @Test
        @DisplayName("libraryConfig 非空 + docConfig=null → 库级默认覆盖硬编码默认")
        void shouldApplyLibraryConfigOverDefaults() {
            KnowledgeLibraryConfig lib = new KnowledgeLibraryConfig();
            lib.setSegmentMode("fixed");
            lib.setSegmentMaxLength(800);
            lib.setSegmentOverlapLength(150);
            lib.setEmbeddingModel("bge-large-zh");
            lib.setRerankModel("bge-reranker-base");
            lib.setRerankEnabled(true);

            KnowledgeConfig result = service.resolveEffectiveConfig(KNOWLEDGE_ID, lib);

            // 库级配置覆盖默认值
            assertThat(result.getSegmentMode()).isEqualTo("fixed");
            assertThat(result.getSegmentMaxLength()).isEqualTo(800);
            assertThat(result.getSegmentOverlapLength()).isEqualTo(150);
            assertThat(result.getEmbeddingModel()).isEqualTo("bge-large-zh");
            assertThat(result.getRerankModel()).isEqualTo("bge-reranker-base");
            assertThat(result.getRerankEnabled()).isTrue();
            // 未被库级覆盖的字段保持默认值
            assertThat(result.getSegmentSeparator()).isEqualTo("\n\n");
            assertThat(result.getIndexMode()).isEqualTo("high_quality");
        }

        @Test
        @DisplayName("libraryConfig=null + docConfig 非空 → 文档级覆盖硬编码默认")
        void shouldApplyDocConfigOverDefaults() {
            KnowledgeConfig docConfig = new KnowledgeConfig();
            docConfig.setSegmentMode("adaptive");
            docConfig.setSegmentMaxLength(512);
            docConfig.setChunkingStrategy("semantic");
            docConfig.setDocumentType("technical");
            docConfig.setEmbeddingModel("text-embedding-3-small");

            when(configMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(docConfig);

            KnowledgeConfig result = service.resolveEffectiveConfig(KNOWLEDGE_ID, null);

            // 文档级覆盖默认值
            assertThat(result.getSegmentMode()).isEqualTo("adaptive");
            assertThat(result.getSegmentMaxLength()).isEqualTo(512);
            assertThat(result.getChunkingStrategy()).isEqualTo("semantic");
            assertThat(result.getDocumentType()).isEqualTo("technical");
            assertThat(result.getEmbeddingModel()).isEqualTo("text-embedding-3-small");
            // 未被文档级覆盖的字段保持默认值
            assertThat(result.getSegmentOverlapLength()).isEqualTo(100);
            assertThat(result.getRerankEnabled()).isFalse();
        }

        @Test
        @DisplayName("三层同时存在 → 文档级覆盖库级覆盖默认（embeddingModel/rerankModel 不丢失）")
        void shouldMergeThreeLayersWithoutLosingFields() {
            // 旧版有损转换 bug 的回归基线：embeddingModel 和 rerankModel 必须从 libraryConfig 正确合并
            KnowledgeLibraryConfig lib = new KnowledgeLibraryConfig();
            lib.setSegmentMode("fixed");
            lib.setSegmentMaxLength(800);
            lib.setEmbeddingModel("bge-large-zh");
            lib.setRerankModel("bge-reranker-base");
            lib.setRerankEnabled(true);
            lib.setRetrievalTopK(5);

            // 文档级仅覆盖部分字段
            KnowledgeConfig docConfig = new KnowledgeConfig();
            docConfig.setSegmentMode("adaptive");  // 覆盖库级的 "fixed"
            docConfig.setChunkingStrategy("semantic");  // 文档级独有字段
            docConfig.setDocumentType("technical");  // 文档级独有字段
            // docConfig 未设 embeddingModel/rerankModel/rerankEnabled/retrievalTopK

            when(configMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(docConfig);

            KnowledgeConfig result = service.resolveEffectiveConfig(KNOWLEDGE_ID, lib);

            // 文档级覆盖库级
            assertThat(result.getSegmentMode()).isEqualTo("adaptive");  // 来自 docConfig
            assertThat(result.getChunkingStrategy()).isEqualTo("semantic");  // 来自 docConfig
            assertThat(result.getDocumentType()).isEqualTo("technical");  // 来自 docConfig

            // 旧版 bug 回归点：库级的 embeddingModel/rerankModel 必须保留（不能丢失）
            assertThat(result.getEmbeddingModel()).isEqualTo("bge-large-zh");  // 来自 libConfig
            assertThat(result.getRerankModel()).isEqualTo("bge-reranker-base");  // 来自 libConfig
            assertThat(result.getRerankEnabled()).isTrue();  // 来自 libConfig
            assertThat(result.getRetrievalTopK()).isEqualTo(5);  // 来自 libConfig

            // 库级覆盖默认
            assertThat(result.getSegmentMaxLength()).isEqualTo(800);  // 来自 libConfig
        }

        @Test
        @DisplayName("libraryConfig 部分字段为 null → 仅合并非 null 字段，其余保持默认")
        void shouldMergeOnlyNonNullLibraryFields() {
            KnowledgeLibraryConfig lib = new KnowledgeLibraryConfig();
            // 仅设 segmentMode，其他字段为 null
            lib.setSegmentMode("custom");

            KnowledgeConfig result = service.resolveEffectiveConfig(KNOWLEDGE_ID, lib);

            assertThat(result.getSegmentMode()).isEqualTo("custom");  // 来自 libConfig
            // 其他字段保持默认（P2-2 阶段 3：统一为 800/100）
            assertThat(result.getSegmentMaxLength()).isEqualTo(800);
            assertThat(result.getSegmentOverlapLength()).isEqualTo(100);
            assertThat(result.getIndexMode()).isEqualTo("high_quality");
        }

        @Test
        @DisplayName("knowledgeId 正确传递到结果对象")
        void shouldPropagateKnowledgeIdToResult() {
            KnowledgeConfig result = service.resolveEffectiveConfig(KNOWLEDGE_ID, null);
            assertThat(result.getKnowledgeId()).isEqualTo(KNOWLEDGE_ID);
        }

        @Test
        @DisplayName("时间戳兜底：createdAt/UpdatedAt 永不为 null")
        void shouldAlwaysHaveTimestamps() {
            KnowledgeConfig result = service.resolveEffectiveConfig(KNOWLEDGE_ID, null);
            assertThat(result.getCreatedAt()).isNotNull();
            assertThat(result.getUpdatedAt()).isNotNull();
        }
    }
}
