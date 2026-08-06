package com.moyun.ext.ai.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.moyun.ext.ai.constant.RedisKeys;
import com.moyun.ext.ai.entity.ModelConfig;
import com.moyun.ext.ai.exception.BusinessException;
import com.moyun.ext.ai.exception.ErrorCode;
import com.moyun.ext.ai.mapper.ModelConfigMapper;
import com.moyun.ext.ai.model.DashScopeRerankModel;
import com.moyun.ext.ai.model.RerankModel;
import com.moyun.ext.ai.service.ModelConfigService;
import com.moyun.ext.ai.util.ApiKeyCryptoUtils;
import com.moyun.ext.ai.util.JsonUtils;
import dev.langchain4j.model.chat.ChatLanguageModel;
import dev.langchain4j.model.chat.StreamingChatLanguageModel;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.model.ollama.OllamaChatModel;
import dev.langchain4j.model.ollama.OllamaStreamingChatModel;
import dev.langchain4j.model.ollama.OllamaEmbeddingModel;
import dev.langchain4j.model.openai.OpenAiChatModel;
import dev.langchain4j.model.openai.OpenAiStreamingChatModel;
import dev.langchain4j.model.openai.OpenAiEmbeddingModel;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.Duration;
import java.util.List;
import java.util.concurrent.TimeUnit;
import jakarta.annotation.PostConstruct;

/**
 * 模型配置服务实现
 * 
 * <p>提供模型配置的CRUD操作，支持创建各类AI模型实例。
 * 使用Redis缓存热点配置数据，提升查询性能。</p>
 * 
 * @author laomao
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ModelConfigServiceImpl extends ServiceImpl<ModelConfigMapper, ModelConfig> implements ModelConfigService {

    private final StringRedisTemplate redisTemplate;

    /**
     * 启动时自动迁移：将历史明文 apiKey 加密为 ENC: 密文
     *
     * <p>扫描所有 model_config 记录，若 apiKey 不为空且非 ENC: 前缀（明文），
     * 则加密更新回 DB。仅迁移一次，后续 save/updateById 已自动加密。</p>
     */
    @PostConstruct
    public void migratePlainTextApiKey() {
        try {
            // 用 mapper 直接查询（绕过 getById 缓存），获取 DB 原始密文
            List<ModelConfig> allConfigs = super.list();
            int migrated = 0;
            for (ModelConfig config : allConfigs) {
                String apiKey = config.getApiKey();
                if (apiKey != null && !apiKey.isEmpty() && !ApiKeyCryptoUtils.isEncrypted(apiKey)) {
                    // 明文 apiKey，加密后更新 DB（直接走 mapper，避免触发缓存逻辑）
                    String encrypted = ApiKeyCryptoUtils.encrypt(apiKey);
                    this.lambdaUpdate()
                        .set(ModelConfig::getApiKey, encrypted)
                        .eq(ModelConfig::getId, config.getId())
                        .update();
                    migrated++;
                    log.info("🔐 已加密迁移 apiKey: configId={}, name={}", config.getId(), config.getName());
                }
            }
            if (migrated > 0) {
                log.info("✅ apiKey 加密迁移完成，共迁移 {} 条记录", migrated);
            } else {
                log.debug("✅ 无需迁移 apiKey（全部已加密或为空）");
            }
        } catch (Exception e) {
            log.error("⚠️ apiKey 加密迁移失败，应用仍可启动，但明文 apiKey 未加密", e);
        }
    }

    @Override
    public ChatLanguageModel createChatModel(Long configId) {
        ModelConfig config = this.getById(configId);
        if (config == null || !config.getEnabled()) {
            throw new BusinessException(ErrorCode.MODEL_NOT_FOUND, "模型配置不存在或未启用");
        }

        return createChatModelFromConfig(config);
    }

    @Override
    public StreamingChatLanguageModel createStreamingChatModel(Long configId) {
        return createStreamingChatModel(configId, null, null);
    }

    @Override
    public StreamingChatLanguageModel createStreamingChatModel(Long configId, Double temperature, Integer maxTokens) {
        ModelConfig config = this.getById(configId);
        if (config == null || !config.getEnabled()) {
            throw new BusinessException(ErrorCode.MODEL_NOT_FOUND, "模型配置不存在或未启用");
        }

        if (!config.getStreamingSupported()) {
            throw new BusinessException(ErrorCode.MODEL_CONFIG_INVALID, "该模型不支持流式输出");
        }

        // 如果提供了覆盖参数，创建新的配置对象
        if (temperature != null || maxTokens != null) {
            ModelConfig overriddenConfig = new ModelConfig();
            // 复制所有字段
            overriddenConfig.setId(config.getId());
            overriddenConfig.setName(config.getName());
            overriddenConfig.setProvider(config.getProvider());
            overriddenConfig.setModelName(config.getModelName());
            overriddenConfig.setApiKey(config.getApiKey());
            overriddenConfig.setBaseUrl(config.getBaseUrl());
            overriddenConfig.setModelType(config.getModelType());
            overriddenConfig.setTimeout(config.getTimeout());
            overriddenConfig.setStreamingSupported(config.getStreamingSupported());
            overriddenConfig.setEnabled(config.getEnabled());
            overriddenConfig.setIsDefault(config.getIsDefault());

            // 覆盖参数
            overriddenConfig.setTemperature(temperature != null ? temperature : config.getTemperature());
            overriddenConfig.setMaxTokens(maxTokens != null ? maxTokens : config.getMaxTokens());

            return createStreamingChatModelFromConfig(overriddenConfig);
        }

        return createStreamingChatModelFromConfig(config);
    }

    @Override
    public EmbeddingModel createEmbeddingModel(Long configId) {
        ModelConfig config = this.getById(configId);
        if (config == null || !config.getEnabled()) {
            throw new BusinessException(ErrorCode.MODEL_NOT_FOUND, "模型配置不存在或未启用");
        }

        if (!"embedding".equals(config.getModelType())) {
            throw new BusinessException(ErrorCode.MODEL_CONFIG_INVALID, "该配置不是 Embedding 模型");
        }

        return createEmbeddingModelFromConfig(config);
    }

    @Override
    public RerankModel createRerankModel(Long configId) {
        ModelConfig config = this.getById(configId);
        if (config == null || !config.getEnabled()) {
            throw new BusinessException(ErrorCode.MODEL_NOT_FOUND, "模型配置不存在或未启用");
        }

        if (!"reranker".equals(config.getModelType())) {
            throw new BusinessException(ErrorCode.MODEL_CONFIG_INVALID, "该配置不是 Reranker 模型");
        }

        return createRerankModelFromConfig(config);
    }

    @Override
    public ModelConfig getDefaultChatConfig() {
        return getDefaultConfigByType("chat");
    }

    @Override
    public ModelConfig getDefaultEmbeddingConfig() {
        return getDefaultConfigByType("embedding");
    }

    @Override
    public ModelConfig getDefaultRerankConfig() {
        return getDefaultConfigByType("reranker");
    }

    @Override
    public ModelConfig getDefaultMultimodalConfig() {
        // 多模态模型统一使用 chat 类型，但需要支持图片（VL模型）
        ModelConfig defaultChat = getDefaultChatConfig();
        
        // 检查默认 chat 模型是否支持图片（通过模型名判断）
        if (defaultChat != null && isVisionModel(defaultChat.getModelName())) {
            return defaultChat;
        }
        
        // 如果默认模型不支持图片，查找第一个支持图片的 chat 模型
        log.info("🔍 默认chat模型不支持图片，查找VL模型...");
        LambdaQueryWrapper<ModelConfig> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(ModelConfig::getModelType, "chat")
               .eq(ModelConfig::getEnabled, true)
               .and(w -> w.like(ModelConfig::getModelName, "-vl")
                         .or().like(ModelConfig::getModelName, "vl-")
                         .or().like(ModelConfig::getModelName, "vision"))
               .last("LIMIT 1");
        
        ModelConfig vlModel = this.getOne(wrapper);
        if (vlModel != null) {
            // 解密 apiKey（DB 中是 ENC: 密文）
            if (vlModel.getApiKey() != null) {
                vlModel.setApiKey(ApiKeyCryptoUtils.decrypt(vlModel.getApiKey()));
            }
            log.info("✅ 找到VL模型用于图片理解: {} ({})", vlModel.getName(), vlModel.getModelName());
            return vlModel;
        }
        
        // 如果没有VL模型，返回默认chat模型（可能会失败，但让调用方处理）
        log.warn("⚠️ 未找到支持图片的VL模型，将使用默认chat模型（可能无法处理图片）");
        return defaultChat;
    }
    
    /**
     * 判断模型是否支持图片（视觉模型）
     */
    private boolean isVisionModel(String modelName) {
        if (modelName == null) return false;
        String lower = modelName.toLowerCase();
        return lower.contains("-vl") || lower.contains("vl-") || lower.contains("vision") || lower.contains("gpt-4o");
    }

    /**
     * 根据类型获取默认模型配置（带缓存）
     *
     * @param modelType 模型类型：chat/embedding
     * @return 默认模型配置，如果没有默认配置则返回第一个启用的配置
     */
    private ModelConfig getDefaultConfigByType(String modelType) {
        String cacheKey = RedisKeys.modelConfigDefault(modelType);

        // 1. 尝试从缓存获取
        String cached = redisTemplate.opsForValue().get(cacheKey);
        if (StringUtils.hasText(cached)) {
            ModelConfig config = JsonUtils.fromJson(cached, ModelConfig.class);
            if (config != null) {
                log.debug("✅ 默认模型配置缓存命中: type={}", modelType);
                return config;
            }
        }

        // 2. 从数据库查询
        LambdaQueryWrapper<ModelConfig> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(ModelConfig::getModelType, modelType)
               .eq(ModelConfig::getIsDefault, true)
               .eq(ModelConfig::getEnabled, true)
               .last("LIMIT 1");

        ModelConfig config = this.getOne(wrapper);

        // 如果没有默认配置，返回第一个启用的配置
        if (config == null) {
            wrapper = new LambdaQueryWrapper<>();
            wrapper.eq(ModelConfig::getModelType, modelType)
                   .eq(ModelConfig::getEnabled, true)
                   .last("LIMIT 1");
            config = this.getOne(wrapper);
        }

        // 3. 解密 apiKey（DB 中是 ENC: 密文）
        if (config != null && config.getApiKey() != null) {
            config.setApiKey(ApiKeyCryptoUtils.decrypt(config.getApiKey()));
        }

        // 4. 写入缓存（缓存明文 apiKey）
        if (config != null) {
            cacheModelConfig(cacheKey, config);
        }

        return config;
    }

    /**
     * 缓存模型配置到Redis
     */
    private void cacheModelConfig(String key, ModelConfig config) {
        String json = JsonUtils.toJson(config);
        if (json != null) {
            try {
                redisTemplate.opsForValue().set(key, json, RedisKeys.MODEL_CONFIG_EXPIRE_MINUTES, TimeUnit.MINUTES);
                log.debug("💾 模型配置已缓存: configId={}", config.getId());
            } catch (Exception e) {
                log.warn("⚠️  缓存模型配置失败: {}", e.getMessage());
            }
        }
    }

    /**
     * 清除模型配置相关缓存
     */
    private void clearModelConfigCache(ModelConfig config) {
        try {
            // 清除ID缓存
            redisTemplate.delete(RedisKeys.modelConfig(config.getId()));
            // 清除默认配置缓存
            redisTemplate.delete(RedisKeys.modelConfigDefault(config.getModelType()));
        } catch (Exception e) {
            log.warn("清除模型配置缓存失败: {}", e.getMessage());
        }
    }

    /**
     * 重写getById，添加缓存支持 + apiKey 透明解密
     *
     * <p>缓存中存储的是解密后的明文 apiKey（与运行时 createChatModel 等方法期望的一致），
     * DB 中存储的是 ENC: 密文。</p>
     */
    @Override
    public ModelConfig getById(java.io.Serializable id) {
        String cacheKey = RedisKeys.modelConfig((Long) id);

        // 1. 尝试从缓存获取（缓存中已是解密后的明文）
        String cached = redisTemplate.opsForValue().get(cacheKey);
        if (StringUtils.hasText(cached)) {
            ModelConfig config = JsonUtils.fromJson(cached, ModelConfig.class);
            if (config != null) {
                log.debug("✅ 模型配置缓存命中: configId={}", id);
                return config;
            }
        }

        // 2. 缓存未命中，从数据库查询
        log.debug("🔍 缓存未命中，查询数据库: configId={}", id);
        ModelConfig config = super.getById(id);

        // 3. 解密 apiKey（DB 中是 ENC: 密文，解密为明文供运行时使用）
        if (config != null && config.getApiKey() != null) {
            config.setApiKey(ApiKeyCryptoUtils.decrypt(config.getApiKey()));
        }

        // 4. 写入缓存（缓存明文 apiKey）
        if (config != null) {
            cacheModelConfig(cacheKey, config);
        }

        return config;
    }

    /**
     * 重写save，入库前加密 apiKey
     */
    @Override
    public boolean save(ModelConfig entity) {
        if (entity != null && entity.getApiKey() != null) {
            entity.setApiKey(ApiKeyCryptoUtils.encrypt(entity.getApiKey()));
        }
        return super.save(entity);
    }

    /**
     * 重写saveBatch，入库前加密每个实体的 apiKey
     */
    @Override
    public boolean saveBatch(java.util.Collection<ModelConfig> entityList) {
        if (entityList != null) {
            entityList.forEach(e -> {
                if (e != null && e.getApiKey() != null) {
                    e.setApiKey(ApiKeyCryptoUtils.encrypt(e.getApiKey()));
                }
            });
        }
        return super.saveBatch(entityList);
    }

    /**
     * 重写updateById，同步更新缓存 + 处理 apiKey 加密
     *
     * <p>处理逻辑：
     * <ul>
     *   <li>前端若传掩码（如 "sk-1****abcd"），检测到不含 ENC: 前缀且为掩码格式时，
     *       从 DB 读取原密文保留（不覆盖），避免把掩码当成新 apiKey 入库。</li>
     *   <li>前端若传明文新 apiKey，正常加密入库。</li>
     * </ul>
     */
    @Override
    public boolean updateById(ModelConfig entity) {
        if (entity != null && entity.getApiKey() != null) {
            String newApiKey = entity.getApiKey();
            // 掩码格式判断：含 **** 且非 ENC: 前缀
            boolean isMasked = newApiKey.contains("****") && !ApiKeyCryptoUtils.isEncrypted(newApiKey);
            if (isMasked) {
                // 用户未修改 apiKey（前端回显掩码），从 DB 读取原密文保留
                ModelConfig existing = super.getById(entity.getId());
                if (existing != null && existing.getApiKey() != null) {
                    entity.setApiKey(existing.getApiKey()); // 保留原密文
                } else {
                    entity.setApiKey(null);
                }
            } else if (!ApiKeyCryptoUtils.isEncrypted(newApiKey)) {
                // 用户传入了新的明文 apiKey，加密入库
                entity.setApiKey(ApiKeyCryptoUtils.encrypt(newApiKey));
            }
            // 若已经是 ENC: 前缀（理论上不会从前端传入），原样保留
        }
        boolean result = super.updateById(entity);
        if (result) {
            clearModelConfigCache(entity);
        }
        return result;
    }

    /**
     * 重写removeById，同步删除缓存
     */
    @Override
    public boolean removeById(java.io.Serializable id) {
        ModelConfig config = super.getById(id);
        boolean result = super.removeById(id);
        if (result && config != null) {
            clearModelConfigCache(config);
        }
        return result;
    }

    @Override
    @Transactional
    public boolean setDefault(Long configId) {
        ModelConfig config = this.getById(configId);
        if (config == null) {
            throw new BusinessException(ErrorCode.MODEL_NOT_FOUND);
        }

        // 取消同类型的其他默认设置
        this.lambdaUpdate()
            .set(ModelConfig::getIsDefault, false)
            .eq(ModelConfig::getModelType, config.getModelType())
            .update();

        // 设置新的默认配置
        boolean result = this.lambdaUpdate()
            .set(ModelConfig::getIsDefault, true)
            .eq(ModelConfig::getId, configId)
            .update();

        // 清除默认配置缓存
        if (result) {
            redisTemplate.delete(RedisKeys.modelConfigDefault(config.getModelType()));
        }

        return result;
    }

    /**
     * 获取所有模型配置列表（按创建时间降序）
     * 
     * <p>性能优化：添加合理的数量限制，避免一次查询过多数据</p>
     * 
     * @return 模型配置列表
     */
    @Override
    public List<ModelConfig> listOrderByCreateTimeDesc() {
        LambdaQueryWrapper<ModelConfig> wrapper = new LambdaQueryWrapper<>();
        wrapper.orderByDesc(ModelConfig::getCreateTime)
               .last("LIMIT 500"); // 限制最多返回500条，避免OOM
        return this.list(wrapper);
    }

    /**
     * 根据配置创建聊天模型
     */
    private ChatLanguageModel createChatModelFromConfig(ModelConfig config) {
        String provider = config.getProvider().toLowerCase();

        switch (provider) {
            case "openai":
                return createOpenAiChatModel(config);
            case "ollama":
                return createOllamaChatModel(config);
            case "dashscope":
                return createDashscopeChatModel(config);
            default:
                throw new BusinessException(ErrorCode.MODEL_CONFIG_INVALID, "不支持的模型提供商: " + provider);
        }
    }

    /**
     * 根据配置创建流式聊天模型
     */
    private StreamingChatLanguageModel createStreamingChatModelFromConfig(ModelConfig config) {
        String provider = config.getProvider().toLowerCase();

        switch (provider) {
            case "openai":
                return createOpenAiStreamingChatModel(config);
            case "ollama":
                return createOllamaStreamingChatModel(config);
            case "dashscope":
                return createDashscopeStreamingChatModel(config);
            default:
                throw new BusinessException(ErrorCode.MODEL_CONFIG_INVALID, "不支持的模型提供商: " + provider);
        }
    }

    /**
     * 创建 OpenAI 聊天模型
     */
    private ChatLanguageModel createOpenAiChatModel(ModelConfig config) {
        OpenAiChatModel.OpenAiChatModelBuilder builder = OpenAiChatModel.builder()
                .apiKey(config.getApiKey())
                .modelName(config.getModelName());

        if (config.getBaseUrl() != null && !config.getBaseUrl().isEmpty()) {
            builder.baseUrl(config.getBaseUrl());
        }
        if (config.getTemperature() != null) {
            builder.temperature(config.getTemperature());
        }
        if (config.getMaxTokens() != null) {
            builder.maxTokens(config.getMaxTokens());
        }
        if (config.getTimeout() != null) {
            builder.timeout(Duration.ofSeconds(config.getTimeout()));
        }

        return builder.build();
    }

    /**
     * 创建 OpenAI 流式聊天模型
     */
    private StreamingChatLanguageModel createOpenAiStreamingChatModel(ModelConfig config) {
        OpenAiStreamingChatModel.OpenAiStreamingChatModelBuilder builder = OpenAiStreamingChatModel.builder()
                .apiKey(config.getApiKey())
                .modelName(config.getModelName());

        if (config.getBaseUrl() != null && !config.getBaseUrl().isEmpty()) {
            builder.baseUrl(config.getBaseUrl());
        }
        if (config.getTemperature() != null) {
            builder.temperature(config.getTemperature());
        }
        if (config.getMaxTokens() != null) {
            builder.maxTokens(config.getMaxTokens());
        }
        if (config.getTimeout() != null) {
            builder.timeout(Duration.ofSeconds(config.getTimeout()));
        }

        return builder.build();
    }

    /**
     * 创建 Ollama 聊天模型
     */
    private ChatLanguageModel createOllamaChatModel(ModelConfig config) {
        OllamaChatModel.OllamaChatModelBuilder builder = OllamaChatModel.builder()
                .modelName(config.getModelName());

        if (config.getBaseUrl() != null && !config.getBaseUrl().isEmpty()) {
            builder.baseUrl(config.getBaseUrl());
        }
        if (config.getTemperature() != null) {
            builder.temperature(config.getTemperature());
        }
        if (config.getTimeout() != null) {
            builder.timeout(Duration.ofSeconds(config.getTimeout()));
        }

        return builder.build();
    }

    /**
     * 创建 Ollama 流式聊天模型
     */
    private StreamingChatLanguageModel createOllamaStreamingChatModel(ModelConfig config) {
        OllamaStreamingChatModel.OllamaStreamingChatModelBuilder builder = OllamaStreamingChatModel.builder()
                .modelName(config.getModelName());

        if (config.getBaseUrl() != null && !config.getBaseUrl().isEmpty()) {
            builder.baseUrl(config.getBaseUrl());
        }
        if (config.getTemperature() != null) {
            builder.temperature(config.getTemperature());
        }
        if (config.getTimeout() != null) {
            builder.timeout(Duration.ofSeconds(config.getTimeout()));
        }

        return builder.build();
    }

    /**
     * 通义千问 OpenAI 兼容模式默认 baseURL
     */
    private static final String DASHSCOPE_DEFAULT_BASE_URL = "https://dashscope.aliyuncs.com/compatible-mode/v1";

    /**
     * 解析 dashscope 的 baseURL：优先使用用户配置，未配置则用默认值
     */
    private String resolveDashscopeBaseUrl(ModelConfig config) {
        return (config.getBaseUrl() != null && !config.getBaseUrl().isEmpty())
                ? config.getBaseUrl()
                : DASHSCOPE_DEFAULT_BASE_URL;
    }

    /**
     * 创建通义千问聊天模型（使用OpenAI兼容模式）
     */
    private ChatLanguageModel createDashscopeChatModel(ModelConfig config) {
        OpenAiChatModel.OpenAiChatModelBuilder builder = OpenAiChatModel.builder()
                .apiKey(config.getApiKey())
                .modelName(config.getModelName())
                .baseUrl(resolveDashscopeBaseUrl(config));

        if (config.getTemperature() != null) {
            builder.temperature(config.getTemperature());
        }
        if (config.getMaxTokens() != null) {
            builder.maxTokens(config.getMaxTokens());
        }
        if (config.getTimeout() != null) {
            builder.timeout(Duration.ofSeconds(config.getTimeout()));
        }

        return builder.build();
    }

    /**
     * 创建通义千问流式聊天模型（使用OpenAI兼容模式）
     */
    private StreamingChatLanguageModel createDashscopeStreamingChatModel(ModelConfig config) {
        OpenAiStreamingChatModel.OpenAiStreamingChatModelBuilder builder = OpenAiStreamingChatModel.builder()
                .apiKey(config.getApiKey())
                .modelName(config.getModelName())
                .baseUrl(resolveDashscopeBaseUrl(config));

        if (config.getTemperature() != null) {
            builder.temperature(config.getTemperature());
        }
        if (config.getMaxTokens() != null) {
            builder.maxTokens(config.getMaxTokens());
        }
        if (config.getTimeout() != null) {
            builder.timeout(Duration.ofSeconds(config.getTimeout()));
        }

        return builder.build();
    }

    /**
     * 根据配置创建 Embedding 模型
     */
    private EmbeddingModel createEmbeddingModelFromConfig(ModelConfig config) {
        String provider = config.getProvider().toLowerCase();

        switch (provider) {
            case "openai":
                return createOpenAiEmbeddingModel(config);
            case "ollama":
                return createOllamaEmbeddingModel(config);
            case "dashscope":
                return createDashscopeEmbeddingModel(config);
            default:
                throw new BusinessException(ErrorCode.MODEL_CONFIG_INVALID, "不支持的模型提供商: " + provider);
        }
    }

    /**
     * 创建 OpenAI Embedding 模型
     */
    private EmbeddingModel createOpenAiEmbeddingModel(ModelConfig config) {
        OpenAiEmbeddingModel.OpenAiEmbeddingModelBuilder builder = OpenAiEmbeddingModel.builder()
                .apiKey(config.getApiKey())
                .modelName(config.getModelName());

        if (config.getBaseUrl() != null && !config.getBaseUrl().isEmpty()) {
            builder.baseUrl(config.getBaseUrl());
        }
        if (config.getTimeout() != null) {
            builder.timeout(Duration.ofSeconds(config.getTimeout()));
        }

        return builder.build();
    }

    /**
     * 创建 Ollama Embedding 模型
     */
    private EmbeddingModel createOllamaEmbeddingModel(ModelConfig config) {
        OllamaEmbeddingModel.OllamaEmbeddingModelBuilder builder = OllamaEmbeddingModel.builder()
                .modelName(config.getModelName());

        if (config.getBaseUrl() != null && !config.getBaseUrl().isEmpty()) {
            builder.baseUrl(config.getBaseUrl());
        }
        if (config.getTimeout() != null) {
            builder.timeout(Duration.ofSeconds(config.getTimeout()));
        }

        return builder.build();
    }

    /**
     * 创建通义千问 Embedding 模型（使用OpenAI兼容模式）
     */
    private EmbeddingModel createDashscopeEmbeddingModel(ModelConfig config) {
        // 通义千问使用OpenAI兼容模式的Embedding API
        OpenAiEmbeddingModel.OpenAiEmbeddingModelBuilder builder = OpenAiEmbeddingModel.builder()
                .apiKey(config.getApiKey())
                .modelName(config.getModelName())
                .baseUrl(resolveDashscopeBaseUrl(config));

        if (config.getTimeout() != null) {
            builder.timeout(Duration.ofSeconds(config.getTimeout()));
        }

        return builder.build();
    }

    /**
     * 根据配置创建 Reranker 模型
     */
    private RerankModel createRerankModelFromConfig(ModelConfig config) {
        String provider = config.getProvider().toLowerCase();

        switch (provider) {
            case "dashscope":
                return createDashScopeRerankModel(config);
            // 未来可以扩展其他提供商（如 Cohere、Jina 等）
            default:
                throw new BusinessException(ErrorCode.MODEL_CONFIG_INVALID, "不支持的 Reranker 提供商: " + provider);
        }
    }

    /**
     * 创建 DashScope Reranker 模型
     */
    private RerankModel createDashScopeRerankModel(ModelConfig config) {
        log.info("创建 DashScope Reranker 模型: {}", config.getModelName());
        return new DashScopeRerankModel(
            config.getApiKey(),
            config.getBaseUrl(),
            config.getModelName()
        );
    }

    @Override
    public ModelConfig getByModelName(String modelName) {
        if (modelName == null || modelName.isEmpty()) {
            return null;
        }
        return this.getOne(new LambdaQueryWrapper<ModelConfig>()
                .eq(ModelConfig::getModelName, modelName)
                .eq(ModelConfig::getEnabled, true)
                .last("LIMIT 1"));
    }

    @Override
    public long countEnabled() {
        return this.count(new LambdaQueryWrapper<ModelConfig>()
                .eq(ModelConfig::getEnabled, true));
    }
}
