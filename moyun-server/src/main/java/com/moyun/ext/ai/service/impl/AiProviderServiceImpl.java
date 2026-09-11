package com.moyun.ext.ai.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.moyun.ext.ai.entity.AiProvider;
import com.moyun.ext.ai.mapper.AiProviderMapper;
import com.moyun.ext.ai.service.AiProviderService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * AI 提供商注册表服务实现
 *
 * <p>全量内存缓存（提供商记录极少且低频变更），CRUD 时失效重建。
 * 未注册的 provider 按 OpenAI 兼容风格处理，保证存量数据与未知提供商不断链。</p>
 *
 * @author moyun
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AiProviderServiceImpl extends ServiceImpl<AiProviderMapper, AiProvider> implements AiProviderService {

    /** code → 注册表记录缓存（含禁用记录，能力判定时区分；null 值表示未注册） */
    private final Map<String, AiProvider> cache = new ConcurrentHashMap<>();

    /** 缓存是否已加载 */
    private volatile boolean loaded = false;

    @Override
    public AiProvider getByCode(String code) {
        if (!StringUtils.hasText(code)) {
            return null;
        }
        ensureLoaded();
        return cache.get(code.toLowerCase());
    }

    @Override
    public List<AiProvider> listEnabled() {
        ensureLoaded();
        return cache.values().stream()
                .filter(p -> Boolean.TRUE.equals(p.getEnabled()))
                .sorted(Comparator.comparingInt(p -> p.getSortOrder() == null ? 0 : p.getSortOrder()))
                .collect(Collectors.toList());
    }

    @Override
    public boolean supportsStreaming(String code) {
        AiProvider p = getByCode(code);
        return p != null && Boolean.TRUE.equals(p.getSupportsStreaming());
    }

    @Override
    public String apiStyle(String code) {
        AiProvider p = getByCode(code);
        if (p != null && StringUtils.hasText(p.getApiStyle())) {
            return p.getApiStyle();
        }
        // 未注册/未填风格 → 默认 OpenAI 兼容（绝大多数提供商），由运行时三级兜底保证不断链
        return AiProvider.STYLE_OPENAI_COMPATIBLE;
    }

    @Override
    public String defaultBaseUrl(String code) {
        AiProvider p = getByCode(code);
        return p == null ? null : p.getDefaultBaseUrl();
    }

    @Override
    public boolean requiresApiKey(String code) {
        AiProvider p = getByCode(code);
        return p == null || !Boolean.FALSE.equals(p.getRequiresApiKey());
    }

    @Override
    public void evictCache() {
        loaded = false;
        cache.clear();
    }

    @Override
    public boolean save(AiProvider entity) {
        boolean ok = super.save(entity);
        if (ok) {
            evictCache();
        }
        return ok;
    }

    @Override
    public boolean updateById(AiProvider entity) {
        boolean ok = super.updateById(entity);
        if (ok) {
            evictCache();
        }
        return ok;
    }

    @Override
    public boolean removeById(java.io.Serializable id) {
        boolean ok = super.removeById(id);
        if (ok) {
            evictCache();
        }
        return ok;
    }

    /** 懒加载全量注册表（DB 异常时保持空缓存，各判定方法按"未注册"回退，不阻断启动） */
    private synchronized void ensureLoaded() {
        if (loaded) {
            return;
        }
        try {
            cache.clear();
            for (AiProvider p : super.list()) {
                if (p.getCode() != null) {
                    cache.put(p.getCode().toLowerCase(), p);
                }
            }
            log.info("✅ AI 提供商注册表已加载：{} 个（{}）", cache.size(),
                    cache.keySet().stream().sorted().collect(Collectors.joining(", ")));
        } catch (Exception e) {
            log.error("⚠️ AI 提供商注册表加载失败（按未注册回退处理，不影响启动）", e);
        } finally {
            loaded = true;
        }
    }
}
