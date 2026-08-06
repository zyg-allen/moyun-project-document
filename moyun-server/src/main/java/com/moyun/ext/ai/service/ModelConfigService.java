package com.moyun.ext.ai.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.moyun.ext.ai.entity.ModelConfig;
import com.moyun.ext.ai.model.RerankModel;
import dev.langchain4j.model.chat.ChatLanguageModel;
import dev.langchain4j.model.chat.StreamingChatLanguageModel;
import dev.langchain4j.model.embedding.EmbeddingModel;

/**
 * 模型配置服务接口
 *
 * <p>提供AI模型配置管理和模型实例创建功能，支持 Chat、Embedding、Reranker 三种模型类型（多模态模型属于 Chat 类型）</p>
 *
 * @author laomao
 */
public interface ModelConfigService extends IService<ModelConfig> {

    /**
     * 根据配置ID创建聊天模型
     */
    ChatLanguageModel createChatModel(Long configId);

    /**
     * 根据配置ID创建聊天模型（支持覆盖参数）
     * <p>用于工作流节点等需要节点级参数覆盖的场景：节点配置的 temperature/maxTokens 优先于模型默认配置</p>
     * @param configId 配置ID
     * @param temperature 温度参数（null则使用配置中的值）
     * @param maxTokens 最大token数（null则使用配置中的值）
     */
    ChatLanguageModel createChatModel(Long configId, Double temperature, Integer maxTokens);

    /**
     * 根据配置ID创建流式聊天模型
     */
    StreamingChatLanguageModel createStreamingChatModel(Long configId);

    /**
     * 根据配置ID创建流式聊天模型（支持覆盖参数）
     * @param configId 配置ID
     * @param temperature 温度参数（null则使用配置中的值）
     * @param maxTokens 最大token数（null则使用配置中的值）
     */
    StreamingChatLanguageModel createStreamingChatModel(Long configId, Double temperature, Integer maxTokens);

    /**
     * 根据配置ID创建 Embedding 模型
     */
    EmbeddingModel createEmbeddingModel(Long configId);

    /**
     * 根据配置ID创建 Reranker 模型
     * @param configId 配置ID
     * @return Reranker 模型实例
     */
    RerankModel createRerankModel(Long configId);

    /**
     * 获取默认对话模型配置
     */
    ModelConfig getDefaultChatConfig();

    /**
     * 获取默认 Embedding 模型配置
     */
    ModelConfig getDefaultEmbeddingConfig();

    /**
     * 获取默认 Reranker 模型配置
     * @return 默认 Reranker 配置，如果没有则返回 null
     */
    ModelConfig getDefaultRerankConfig();

    /**
     * 获取默认多模态模型配置
     * @deprecated 多模态模型统一使用 chat 类型，请使用 {@link #getDefaultChatConfig()}
     */
    @Deprecated
    ModelConfig getDefaultMultimodalConfig();

    /**
     * 设置默认模型
     */
    boolean setDefault(Long configId);

    /**
     * 获取所有模型配置（按创建时间倒序）
     */
    java.util.List<ModelConfig> listOrderByCreateTimeDesc();

    /**
     * 根据模型名称获取配置
     */
    ModelConfig getByModelName(String modelName);

    /**
     * 统计启用的模型数量
     */
    long countEnabled();
}
