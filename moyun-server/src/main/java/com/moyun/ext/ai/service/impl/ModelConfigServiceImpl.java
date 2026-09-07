package com.moyun.ext.ai.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.moyun.ext.ai.constant.RedisKeys;
import com.moyun.ext.ai.entity.AiProvider;
import com.moyun.ext.ai.entity.ModelConfig;
import com.moyun.ext.ai.enums.ModelType;
import com.moyun.ext.ai.exception.BusinessException;
import com.moyun.ext.ai.exception.ErrorCode;
import com.moyun.ext.ai.mapper.ModelConfigMapper;
import com.moyun.ext.ai.model.DashScopeRerankModel;
import com.moyun.ext.ai.model.RerankModel;
import com.moyun.ext.ai.service.AiProviderService;
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
import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;
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

    /** 提供商注册表（V11.0.2：能力元数据全部查注册表，代码不再出现 provider 字符串分支） */
    private final AiProviderService providerService;

    /**
     * 启动时自动纠正：按提供商注册表修正 streaming_supported 标志
     *
     * <p>历史数据中 chat 模型被误标为不支持流式（如 configId=15），导致语音面试等流式链路报错。
     * 纠正规则：非 chat 类型 → false；chat + 注册表声明支持流式 → true；
     * chat + 未注册提供商 → 保留用户显式配置。保证后台动态配置 agent/工作流时不会因标志位误配导致链路中断。</p>
     */
    @PostConstruct
    public void migrateStreamingFlags() {
        try {
            List<ModelConfig> all = super.list();
            int fixed = 0;
            for (ModelConfig config : all) {
                Boolean old = config.getStreamingSupported();
                inferStreamingSupport(config);
                if (!Objects.equals(old, config.getStreamingSupported())) {
                    this.lambdaUpdate()
                            .set(ModelConfig::getStreamingSupported, config.getStreamingSupported())
                            .eq(ModelConfig::getId, config.getId())
                            .update();
                    // 同步清除单条缓存与默认配置缓存，避免 Redis 持续返回旧标志
                    clearModelConfigCache(config);
                    fixed++;
                    log.info("🔧 已纠正模型流式标志: configId={}, name={}, streamingSupported: {} → {}",
                            config.getId(), config.getName(), old, config.getStreamingSupported());
                }
            }
            if (fixed > 0) {
                log.info("✅ 模型流式标志纠正完成，共 {} 条记录", fixed);
            }
        } catch (Exception e) {
            log.error("⚠️ 模型流式标志纠正失败（不影响启动）", e);
        }
    }

    /**
     * 根据提供商注册表自动推断流式支持并回填 streamingSupported：
     * <ul>
     *   <li>非 chat 类型（embedding/rerank/asr）→ 无流式概念，置 false</li>
     *   <li>chat + 注册表声明支持流式（{@code ai_provider.supports_streaming}）→ true</li>
     *   <li>chat + 注册表声明不支持 → false</li>
     *   <li>chat + 未注册提供商 → 保留用户显式配置（不覆盖）</li>
     * </ul>
     */
    @Override
    public void inferStreamingSupport(ModelConfig config) {
        if (config == null) {
            return;
        }
        if (!ModelType.CHAT.getCode().equals(config.getModelType())) {
            config.setStreamingSupported(false);
            return;
        }
        AiProvider provider = providerService.getByCode(config.getProvider());
        if (provider != null) {
            config.setStreamingSupported(Boolean.TRUE.equals(provider.getSupportsStreaming()));
        }
    }

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
        return createChatModel(configId, null, null);
    }

    @Override
    public ChatLanguageModel createChatModel(Long configId, Double temperature, Integer maxTokens) {
        ModelConfig config = this.getById(configId);
        if (config == null || !config.getEnabled()) {
            throw new BusinessException(ErrorCode.MODEL_NOT_FOUND, "模型配置不存在或未启用");
        }

        // 如果提供了覆盖参数，创建新的配置对象（节点级参数优先于模型默认配置）
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

            return createChatModelFromConfig(overriddenConfig);
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

        if (!ModelType.EMBEDDING.getCode().equals(config.getModelType())) {
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

        if (!ModelType.RERANKER.getCode().equals(config.getModelType())) {
            throw new BusinessException(ErrorCode.MODEL_CONFIG_INVALID, "该配置不是 Reranker 模型");
        }

        return createRerankModelFromConfig(config);
    }

    @Override
    public ModelConfig getDefaultChatConfig() {
        return getDefaultConfigByType(ModelType.CHAT.getCode());
    }

    @Override
    public ModelConfig getDefaultEmbeddingConfig() {
        return getDefaultConfigByType(ModelType.EMBEDDING.getCode());
    }

    @Override
    public ModelConfig getDefaultRerankConfig() {
        return getDefaultConfigByType(ModelType.RERANKER.getCode());
    }

    @Override
    public ModelConfig getDefaultAsrConfig() {
        return getDefaultConfigByType(ModelType.ASR.getCode());
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
        wrapper.eq(ModelConfig::getModelType, ModelType.CHAT.getCode())
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
        if (entity != null) {
            // 兜底设置时间戳（ModelConfig 无 @TableField(fill) 注解）
            LocalDateTime now = LocalDateTime.now();
            if (entity.getCreateTime() == null) {
                entity.setCreateTime(now);
            }
            if (entity.getUpdateTime() == null) {
                entity.setUpdateTime(now);
            }
            if (entity.getApiKey() != null) {
                entity.setApiKey(ApiKeyCryptoUtils.encrypt(entity.getApiKey()));
            }
        }
        return super.save(entity);
    }

    /**
     * 重写saveBatch，入库前加密每个实体的 apiKey
     */
    @Override
    public boolean saveBatch(java.util.Collection<ModelConfig> entityList) {
        if (entityList != null) {
            LocalDateTime now = LocalDateTime.now();
            entityList.forEach(e -> {
                if (e != null) {
                    // 兜底设置时间戳（ModelConfig 无 @TableField(fill) 注解）
                    if (e.getCreateTime() == null) {
                        e.setCreateTime(now);
                    }
                    if (e.getUpdateTime() == null) {
                        e.setUpdateTime(now);
                    }
                    if (e.getApiKey() != null) {
                        e.setApiKey(ApiKeyCryptoUtils.encrypt(e.getApiKey()));
                    }
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
        if (entity != null) {
            // 兜底设置时间戳（ModelConfig 无 @TableField(fill) 注解）
            if (entity.getUpdateTime() == null) {
                entity.setUpdateTime(LocalDateTime.now());
            }
            if (entity.getApiKey() != null) {
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
     *
     * <p>V11.0.2：按提供商注册表的 apiStyle 分支（而非 provider 名）。
     * 任何 OpenAI 兼容提供商（DeepSeek/Moonshot/智谱等）注册后自动可用，零代码改动。</p>
     */
    private ChatLanguageModel createChatModelFromConfig(ModelConfig config) {
        String apiStyle = providerService.apiStyle(config.getProvider());

        switch (apiStyle) {
            case AiProvider.STYLE_OLLAMA_NATIVE:
                return createOllamaChatModel(config);
            case AiProvider.STYLE_OPENAI_COMPATIBLE:
            default:
                return createOpenAiCompatibleChatModel(config);
        }
    }

    /**
     * 根据配置创建流式聊天模型（按 apiStyle 分支，见 {@link #createChatModelFromConfig}）
     */
    private StreamingChatLanguageModel createStreamingChatModelFromConfig(ModelConfig config) {
        String apiStyle = providerService.apiStyle(config.getProvider());

        switch (apiStyle) {
            case AiProvider.STYLE_OLLAMA_NATIVE:
                return createOllamaStreamingChatModel(config);
            case AiProvider.STYLE_OPENAI_COMPATIBLE:
            default:
                return createOpenAiCompatibleStreamingChatModel(config);
        }
    }

    /**
     * 解析模型 baseUrl：用户配置优先 → 提供商注册表默认地址兜底（V11.0.2）
     */
    private String resolveBaseUrl(ModelConfig config) {
        if (config.getBaseUrl() != null && !config.getBaseUrl().isEmpty()) {
            return config.getBaseUrl();
        }
        return providerService.defaultBaseUrl(config.getProvider());
    }

    /**
     * 解析 apiKey：注册表声明免 Key 的本地端点（如 Ollama 兼容模式）传占位符，避免 NPE
     */
    private String resolveApiKey(ModelConfig config) {
        if (config.getApiKey() != null && !config.getApiKey().isEmpty()) {
            return config.getApiKey();
        }
        return providerService.requiresApiKey(config.getProvider()) ? config.getApiKey() : "no-api-key";
    }

    /**
     * 创建 OpenAI 兼容聊天模型（openai/dashscope/deepseek/moonshot 等所有兼容端点共用）
     */
    private ChatLanguageModel createOpenAiCompatibleChatModel(ModelConfig config) {
        OpenAiChatModel.OpenAiChatModelBuilder builder = OpenAiChatModel.builder()
                .apiKey(resolveApiKey(config))
                .modelName(config.getModelName());

        String baseUrl = resolveBaseUrl(config);
        if (baseUrl != null && !baseUrl.isEmpty()) {
            builder.baseUrl(baseUrl);
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
     * 创建 OpenAI 兼容流式聊天模型（所有兼容端点共用）
     */
    private StreamingChatLanguageModel createOpenAiCompatibleStreamingChatModel(ModelConfig config) {
        OpenAiStreamingChatModel.OpenAiStreamingChatModelBuilder builder = OpenAiStreamingChatModel.builder()
                .apiKey(resolveApiKey(config))
                .modelName(config.getModelName());

        String baseUrl = resolveBaseUrl(config);
        if (baseUrl != null && !baseUrl.isEmpty()) {
            builder.baseUrl(baseUrl);
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
     * 根据配置创建 Embedding 模型（V11.0.2：按 apiStyle 分支）
     */
    private EmbeddingModel createEmbeddingModelFromConfig(ModelConfig config) {
        String apiStyle = providerService.apiStyle(config.getProvider());

        switch (apiStyle) {
            case AiProvider.STYLE_OLLAMA_NATIVE:
                return createOllamaEmbeddingModel(config);
            case AiProvider.STYLE_OPENAI_COMPATIBLE:
            default:
                return createOpenAiCompatibleEmbeddingModel(config);
        }
    }

    /**
     * 创建 OpenAI 兼容 Embedding 模型（所有兼容端点共用，baseUrl 走注册表兜底）
     */
    private EmbeddingModel createOpenAiCompatibleEmbeddingModel(ModelConfig config) {
        OpenAiEmbeddingModel.OpenAiEmbeddingModelBuilder builder = OpenAiEmbeddingModel.builder()
                .apiKey(resolveApiKey(config))
                .modelName(config.getModelName());

        String baseUrl = resolveBaseUrl(config);
        if (baseUrl != null && !baseUrl.isEmpty()) {
            builder.baseUrl(baseUrl);
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
     * 根据 Reranker 模型类型创建
     *
     * <p>Rerank 为提供商私有协议（DashScope），无 OpenAI 兼容标准，按注册表 code 分发；
     * 未支持的可在此扩展或后续抽象为独立 SPI。</p>
     */
    private RerankModel createRerankModelFromConfig(ModelConfig config) {
        AiProvider provider = providerService.getByCode(config.getProvider());
        // Rerank 专用模型（DashScopeRerankModel 自定义协议）
        if (provider != null && "dashscope".equals(provider.getCode())) {
            return createDashScopeRerankModel(config);
        }
        throw new BusinessException(ErrorCode.MODEL_CONFIG_INVALID,
                "暂不支持该提供商的 Reranker: " + config.getProvider() + "，请在后台提供商管理中确认配置");
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
