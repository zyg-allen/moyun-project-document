package com.moyun.ext.ai2.support;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.moyun.ext.ai.entity.ModelConfig;
import com.moyun.ext.ai.service.ModelConfigService;
import com.moyun.ext.ai2.model.AiExecuteResponse;
import dev.langchain4j.model.embedding.EmbeddingModel;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ScanOptions;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Duration;
import java.util.HashMap;
import java.util.HexFormat;
import java.util.Map;

/**
 * 语义缓存（第2层增强能力）
 *
 * <p>依据《AI能力统一接入层 — 完整方案文档》V2.0 §7.2。两级命中策略：</p>
 * <ol>
 *   <li>精确命中：输入 MD5 直接命中（零成本）</li>
 *   <li>语义命中：输入 Embedding 与已缓存条目的余弦相似度 &gt; 0.95（需配置默认 Embedding 模型，
 *       未配置时自动降级为仅精确命中）</li>
 * </ol>
 *
 * <p>缓存键：{@code ai2:cache:{scene}:{md5(input)}}，TTL 取场景配置 cache_ttl。</p>
 *
 * @author laomao
 * @since 2026-09-09
 */
@Slf4j
@Component
public class SemanticCache {

    /** 语义命中相似度阈值 */
    private static final double SIMILARITY_THRESHOLD = 0.95;
    /** 语义扫描的最大候选条数（控制成本） */
    private static final int SCAN_LIMIT = 100;

    private static final ObjectMapper MAPPER = new ObjectMapper();

    @Autowired
    private RedisTemplate<String, String> redisTemplate;

    @Autowired(required = false)
    private ModelConfigService modelConfigService;

    /** Embedding 模型懒加载缓存（null 表示不可用） */
    private volatile EmbeddingModel embeddingModel;
    private volatile boolean embeddingChecked = false;

    /**
     * 场景是否启用缓存（由场景配置 enable_cache 决定）
     */
    public boolean isEnabled(Integer enableCache) {
        return enableCache != null && enableCache == 1;
    }

    /**
     * 查询缓存：精确命中 → 语义命中 → 未命中
     *
     * @param scene    场景代码
     * @param inputKey 输入摘要（通常为 input 的规范化 JSON）
     * @param inputText 用于语义比对的文本（通常取 input 中的主文本参数）
     * @return 命中的响应（metadata.fromCache 已置 true）；未命中返回 null
     */
    @SuppressWarnings("unchecked")
    public AiExecuteResponse<Object> get(String scene, String inputKey, String inputText) {
        String exactKey = buildKey(scene, inputKey);
        try {
            String cached = redisTemplate.opsForValue().get(exactKey);
            if (cached != null) {
                CacheEntry entry = MAPPER.readValue(cached, CacheEntry.class);
                entry.getResponse().setMetadata(markFromCache(entry.getResponse()));
                log.debug("[ai2:cache] 精确命中: scene={}", scene);
                return entry.getResponse();
            }

            // 语义命中（需 Embedding 模型）
            EmbeddingModel model = getEmbeddingModel();
            if (model == null || inputText == null || inputText.isBlank()) {
                return null;
            }
            float[] queryVector = model.embed(inputText).content().vector();
            var keys = redisTemplate.scan(ScanOptions.scanOptions()
                    .match("ai2:cache:" + scene + ":*").count(SCAN_LIMIT).build());
            int compared = 0;
            while (keys != null && keys.hasNext() && compared < SCAN_LIMIT) {
                String key = keys.next();
                String value = redisTemplate.opsForValue().get(key);
                if (value == null) {
                    continue;
                }
                compared++;
                CacheEntry entry = MAPPER.readValue(value, CacheEntry.class);
                if (entry.getEmbedding() == null) {
                    continue;
                }
                double similarity = cosine(queryVector, entry.getEmbedding());
                if (similarity > SIMILARITY_THRESHOLD) {
                    entry.getResponse().setMetadata(markFromCache(entry.getResponse()));
                    log.info("[ai2:cache] 语义命中: scene={}, similarity={}", scene,
                            String.format("%.3f", similarity));
                    return entry.getResponse();
                }
            }
        } catch (Exception e) {
            log.warn("[ai2:cache] 缓存查询异常（降级直连）: {}", e.getMessage());
        }
        return null;
    }

    /**
     * 写入缓存
     *
     * @param ttlSeconds 缓存时间（秒），null 则不缓存
     */
    public void put(String scene, String inputKey, String inputText,
                    AiExecuteResponse<?> response, Integer ttlSeconds) {
        if (ttlSeconds == null || ttlSeconds <= 0) {
            return;
        }
        try {
            CacheEntry entry = new CacheEntry();
            entry.setResponse(castResponse(response));
            EmbeddingModel model = getEmbeddingModel();
            if (model != null && inputText != null && !inputText.isBlank()) {
                try {
                    entry.setEmbedding(model.embed(inputText).content().vector());
                } catch (Exception e) {
                    log.debug("[ai2:cache] Embedding计算失败，仅精确命中: {}", e.getMessage());
                }
            }
            redisTemplate.opsForValue().set(buildKey(scene, inputKey),
                    MAPPER.writeValueAsString(entry), Duration.ofSeconds(ttlSeconds));
        } catch (Exception e) {
            log.warn("[ai2:cache] 缓存写入异常: {}", e.getMessage());
        }
    }

    // ==================== 内部实现 ====================

    private String buildKey(String scene, String inputKey) {
        return "ai2:cache:" + scene + ":" + md5(inputKey);
    }

    private EmbeddingModel getEmbeddingModel() {
        if (!embeddingChecked) {
            synchronized (this) {
                if (!embeddingChecked) {
                    try {
                        ModelConfig config = modelConfigService != null
                                ? modelConfigService.getDefaultEmbeddingConfig() : null;
                        if (config != null) {
                            embeddingModel = modelConfigService.createEmbeddingModel(config.getId());
                        }
                    } catch (Exception e) {
                        log.info("[ai2:cache] Embedding模型不可用，语义缓存降级为精确命中: {}", e.getMessage());
                    }
                    embeddingChecked = true;
                }
            }
        }
        return embeddingModel;
    }

    private com.moyun.ext.ai2.model.AiMetadata markFromCache(AiExecuteResponse<?> response) {
        com.moyun.ext.ai2.model.AiMetadata metadata = response.getMetadata();
        if (metadata == null) {
            metadata = new com.moyun.ext.ai2.model.AiMetadata();
        }
        metadata.setFromCache(true);
        return metadata;
    }

    @SuppressWarnings("unchecked")
    private AiExecuteResponse<Object> castResponse(AiExecuteResponse<?> response) {
        return (AiExecuteResponse<Object>) response;
    }

    private double cosine(float[] a, float[] b) {
        if (a == null || b == null || a.length != b.length) {
            return -1;
        }
        double dot = 0;
        double normA = 0;
        double normB = 0;
        for (int i = 0; i < a.length; i++) {
            dot += a[i] * b[i];
            normA += (double) a[i] * a[i];
            normB += (double) b[i] * b[i];
        }
        if (normA == 0 || normB == 0) {
            return -1;
        }
        return dot / (Math.sqrt(normA) * Math.sqrt(normB));
    }

    private String md5(String text) {
        try {
            MessageDigest digest = MessageDigest.getInstance("MD5");
            return HexFormat.of().formatHex(digest.digest(
                    text.getBytes(StandardCharsets.UTF_8)));
        } catch (Exception e) {
            return String.valueOf(text.hashCode());
        }
    }

    /**
     * 缓存条目（响应 + 输入向量）
     */
    @lombok.Data
    public static class CacheEntry {
        private AiExecuteResponse<Object> response;
        private float[] embedding;
        private Map<String, Object> extra = new HashMap<>();
    }
}
