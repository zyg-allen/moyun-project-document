package com.moyun.ext.ai2.registry;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.moyun.ext.ai.entity.AiSceneConfig;
import com.moyun.ext.ai.exception.BusinessException;
import com.moyun.ext.ai.exception.ErrorCode;
import com.moyun.ext.ai.mapper.AiSceneConfigMapper;
import com.moyun.ext.ai2.handler.AiSceneHandler;
import lombok.extern.slf4j.Slf4j;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * AI场景注册中心
 *
 * <p>统一接入层的路由核心：Handler Bean 注册（Spring容器扫描）+ 场景配置加载（ai_scene_config 表）。
 * 依据《AI能力统一接入层 — 完整方案文档》V2.0 §5.2。</p>
 *
 * <p>v11.41：合并到 ai_scene_config 表（原 ai2_scene_registry 已废弃），绑定关系与执行配置统一管理。</p>
 *
 * <p>Handler 必须有对应配置行才会对外服务（配置行控制 enabled / 限流 / 缓存 / 降级等策略）。</p>
 *
 * @author laomao
 * @since 2026-09-09
 */
@Slf4j
@Component
public class AiSceneRegistry {

    /** Handler 路由表：sceneCode → Handler */
    private final Map<String, AiSceneHandler> handlerMap = new ConcurrentHashMap<>();

    /** 场景配置缓存：sceneCode → 配置 */
    private final Map<String, AiSceneConfig> configMap = new ConcurrentHashMap<>();

    @Autowired
    private List<AiSceneHandler> handlers;

    @Autowired
    private AiSceneConfigMapper configMapper;

    /**
     * 初始化：注册所有 Handler Bean + 加载场景配置
     */
    @PostConstruct
    public void init() {
        handlerMap.clear();
        for (AiSceneHandler handler : handlers) {
            String sceneCode = handler.getSceneCode();
            if (handlerMap.put(sceneCode, handler) != null) {
                log.warn("[ai2] 场景代码重复注册: {}，后者覆盖前者", sceneCode);
            }
            log.info("[ai2] 注册AI场景处理器: {} -> {}", sceneCode, handler.getClass().getSimpleName());
        }
        loadConfigs();
    }

    /**
     * 按场景代码获取 Handler
     *
     * @throws BusinessException 场景未注册（SCENE_NOT_FOUND）
     */
    public AiSceneHandler getHandler(String sceneCode) {
        AiSceneHandler handler = handlerMap.get(sceneCode);
        if (handler == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "未知AI场景: " + sceneCode);
        }
        return handler;
    }

    /**
     * 按场景代码获取启用配置
     *
     * @return 配置；无配置或未启用返回 null
     */
    public AiSceneConfig getConfig(String sceneCode) {
        return configMap.get(sceneCode);
    }

    /**
     * 加载所有启用的场景配置（按优先级排序，同 sceneCode 取优先级最高的一行）
     */
    public void loadConfigs() {
        List<AiSceneConfig> configs = configMapper.selectList(
                new LambdaQueryWrapper<AiSceneConfig>()
                        .eq(AiSceneConfig::getEnabled, true)
                        .orderByDesc(AiSceneConfig::getPriority));

        Map<String, AiSceneConfig> fresh = new LinkedHashMap<>();
        for (AiSceneConfig config : configs) {
            fresh.putIfAbsent(config.getSceneCode(), config);
        }
        configMap.clear();
        configMap.putAll(fresh);

        // 一致性检查：配置行存在但 Handler 未注册的，告警
        for (String sceneCode : fresh.keySet()) {
            if (!handlerMap.containsKey(sceneCode)) {
                log.warn("[ai2] 场景 {} 有配置但未注册 Handler（handler_bean_name 不匹配或未实现）", sceneCode);
            }
        }
        log.info("[ai2] 场景配置加载完成: {} 个场景（Handler总数: {}）", fresh.size(), handlerMap.size());
    }

    /**
     * 刷新（Handler 重新注册 + 配置重新加载，供管理端调用）
     */
    public synchronized void refresh() {
        init();
        log.info("[ai2] 场景注册中心刷新完成");
    }

    /**
     * 已注册场景总览（scene → {handler, config, outputMode}），管理/调试用
     */
    public List<Map<String, Object>> listScenes() {
        return handlerMap.entrySet().stream()
                .map(e -> {
                    Map<String, Object> m = new LinkedHashMap<>();
                    m.put("sceneCode", e.getKey());
                    m.put("handler", e.getValue().getClass().getSimpleName());
                    m.put("supportedOutputMode", e.getValue().getSupportedOutputMode());
                    AiSceneConfig config = configMap.get(e.getKey());
                    m.put("configured", config != null);
                    if (config != null) {
                        m.put("sceneName", config.getSceneName());
                        m.put("category", config.getSceneCategory());
                        m.put("outputMode", config.getOutputMode());
                        m.put("description", config.getDescription());
                    }
                    return m;
                })
                .sorted(Comparator.comparing(m -> String.valueOf(m.get("sceneCode"))))
                .toList();
    }
}
