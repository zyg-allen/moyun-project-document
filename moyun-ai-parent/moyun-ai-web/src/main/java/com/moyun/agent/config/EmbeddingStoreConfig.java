package com.moyun.agent.config;

import com.moyun.agent.service.ModelConfigService;
import com.moyun.agent.store.ElasticsearchEmbeddingStore;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.store.embedding.EmbeddingStore;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * 向量存储配置类
 *
 * <p>配置Embedding向量存储，使用Elasticsearch作为向量数据库</p>
 *
 * @author laomao
 */
@Slf4j
@Configuration
public class EmbeddingStoreConfig {

    @Autowired
    private ModelConfigService modelConfigService;

    @Autowired
    private ElasticsearchEmbeddingStore elasticsearchEmbeddingStore;

    /**
     * 创建向量存储Bean
     *
     * <p>从数据库获取默认Embedding模型配置，初始化Elasticsearch向量存储</p>
     *
     * @return 向量存储实例
     */
    @Bean
    public EmbeddingStore<TextSegment> embeddingStore() {
        // 从数据库动态获取默认的 Embedding 模型配置
        var config = modelConfigService.getDefaultEmbeddingConfig();

        if (config != null) {
            log.info("✅ 使用 Embedding 模型: {}, provider: {}", config.getModelName(), config.getProvider());
            // 注意：不在此处调用 embeddingModel.dimension()，避免启动时发起远程 API 调用。
            // 实际向量维度由 ElasticsearchEmbeddingStore 在首次写入数据时根据真实向量维度自动创建索引。
        } else {
            log.warn("⚠️ 未找到默认 Embedding 模型配置，向量维度将在首次写入时自动确定");
        }

        // 使用 Elasticsearch 向量存储
        log.info("✅ Elasticsearch 向量存储初始化成功");
        return elasticsearchEmbeddingStore;
    }

}
