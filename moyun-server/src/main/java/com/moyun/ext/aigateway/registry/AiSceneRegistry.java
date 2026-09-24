package com.moyun.ext.aigateway.registry;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.moyun.ext.ai.entity.AiSceneConfig;
import com.moyun.ext.ai.mapper.AiSceneConfigMapper;
import com.moyun.ext.aigateway.handler.AiSceneHandler;
import com.moyun.ext.aigateway.service.DefaultSceneExecutor;
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
 * <p>统一接入层的路由核心：Handler Bean 注册（Spring容器扫描）+ 场景配置读取（ai_scene_config 表）。
 * 依据《AI能力统一接入层 — 完整方案文档》V2.0 §5.2。</p>
 *
 * <p>合并到 ai_scene_config 表（原 ai2_scene_registry 已废弃），绑定关系与执行配置统一管理。</p>
 *
 * <p>配置不再内存缓存——getConfig 每次直查数据库（LLM 调用为秒级，一次索引查询开销可忽略），
 * 管理端改提示词模板/输出结构/绑定关系后<strong>下次调用立即生效</strong>，无需重启或手动刷新。</p>
 *
 * <p>Handler 必须有对应配置行才会对外服务（配置行控制 enabled / 限流 / 缓存 / 降级等策略）。</p>
 *
 * @author laomao
 * @since 2026-09-09
 */
@Slf4j
@Component
public class AiSceneRegistry {

    /** Handler 路由表：sceneCode → Handler（Bean 静态注册，启动后不变） */
    private final Map<String, AiSceneHandler> handlerMap = new ConcurrentHashMap<>();

    @Autowired
    private List<AiSceneHandler> handlers;

    @Autowired
    private AiSceneConfigMapper configMapper;

    /** 配置驱动默认执行器（无 SPI Handler 的场景回落，AI统一网关整改 2A.3） */
    @Autowired
    private DefaultSceneExecutor defaultSceneExecutor;

    /**
     * 初始化：注册所有 Handler Bean + 启动时一致性检查（配置行与 Handler 的匹配告警）
     */
    @PostConstruct
    public void init() {
        handlerMap.clear();
        for (AiSceneHandler handler : handlers) {
            String sceneCode = handler.getSceneCode();
            // DefaultSceneExecutor 不进路由表：它按"未命中回落"承载无 SPI Handler 的场景
            if (DefaultSceneExecutor.SCENE_CODE.equals(sceneCode)) {
                continue;
            }
            if (handlerMap.put(sceneCode, handler) != null) {
                log.warn("[aigateway] 场景代码重复注册: {}，后者覆盖前者", sceneCode);
            }
            log.info("[aigateway] 注册AI场景处理器: {} -> {}", sceneCode, handler.getClass().getSimpleName());
        }
        consistencyCheck();
    }

    /**
     * 按场景代码获取 Handler（AI统一网关整改 2A.4 支持全码回退）：
     * 全码（scene:task）→ 主码 → SPI Handler 优先（逃生舱扩展点），
     * 均未命中回落 {@link DefaultSceneExecutor} 配置驱动执行
     * （场景差异由 ai_scene_config 声明，不再要求每场景一个 Handler）。
     *
     * <p>未配置场景不会走到本方法（getConfig 前置拦截 SCENE_NOT_FOUND）。</p>
     */
    public AiSceneHandler getHandler(String sceneCode) {
        AiSceneHandler handler = handlerMap.get(sceneCode);
        if (handler == null) {
            String mainCode = mainSceneCode(sceneCode);
            if (mainCode != null && !mainCode.equals(sceneCode)) {
                handler = handlerMap.get(mainCode);
            }
        }
        return handler != null ? handler : defaultSceneExecutor;
    }

    /**
     * 按场景代码获取启用配置（直查数据库，管理端变更即时生效）
     *
     * <p>同场景多版本时按 priority DESC 取第一条（与原内存缓存口径一致）。</p>
     *
     * @return 配置；无配置或未启用返回 null
     */
    public AiSceneConfig getConfig(String sceneCode) {
        List<AiSceneConfig> configs = configMapper.selectList(
                new LambdaQueryWrapper<AiSceneConfig>()
                        .eq(AiSceneConfig::getSceneCode, sceneCode)
                        .eq(AiSceneConfig::getEnabled, true)
                        .orderByDesc(AiSceneConfig::getPriority)
                        .last("LIMIT 1"));
        return configs.isEmpty() ? null : configs.get(0);
    }

    /**
     * task 拆行配置解析（AI统一网关整改 2A.4）：先按全码 {@code scene:task} 查，
     * 未命中回退主场景码。业务调用方式不变（主码 + input.task）。
     *
     * @param sceneCode 主场景码（或全码——已含冒号时直接按原文查）
     * @param task      子任务码（input.task，可空）
     * @return 命中的配置行（全码行或主码行）；均未命中返回 null
     */
    public AiSceneConfig getConfig(String sceneCode, String task) {
        if (sceneCode != null && task != null && !task.isBlank() && !sceneCode.contains(":")) {
            AiSceneConfig full = getConfig(sceneCode + ":" + task.trim());
            if (full != null) {
                return full;
            }
        }
        return getConfig(sceneCode);
    }

    /** 场景码的冒号主码拆分（非拆行码原样返回） */
    public static String mainSceneCode(String sceneCode) {
        if (sceneCode == null) {
            return null;
        }
        int idx = sceneCode.indexOf(':');
        return idx > 0 ? sceneCode.substring(0, idx) : sceneCode;
    }

    /**
     * 刷新（Handler 重新注册；配置无缓存，无需重载。供管理端/调试调用）
     */
    public synchronized void refresh() {
        init();
        log.info("[aigateway] 场景注册中心刷新完成");
    }

    /**
     * 启动一致性检查：有配置行的场景必然可执行（SPI Handler 或 DefaultSceneExecutor 回落），
     * 仅统计汇总；SPI Handler 注册但无配置行的场景无法对外服务，告警提示。
     */
    private void consistencyCheck() {
        List<String> configuredScenes = configMapper.selectList(new LambdaQueryWrapper<AiSceneConfig>()
                        .eq(AiSceneConfig::getEnabled, true))
                .stream().map(AiSceneConfig::getSceneCode).distinct().toList();
        for (String sceneCode : handlerMap.keySet()) {
            if (!configuredScenes.contains(sceneCode)) {
                log.warn("[aigateway] 场景 {} 已注册 Handler 但无启用配置行（无法对外服务）", sceneCode);
            }
        }
        long executorBacked = configuredScenes.stream().filter(s -> !handlerMap.containsKey(s)).count();
        log.info("[aigateway] 场景配置一致性检查完成: {} 个启用场景（SPI Handler: {}，配置驱动回落: {}）",
                configuredScenes.size(), handlerMap.size(), executorBacked);
    }

    /**
     * 已注册场景总览（scene → {handler, config, outputMode}），管理/调试用。
     * 覆盖 SPI Handler 场景 + 配置驱动场景（有配置行但无 Handler，回落 DefaultSceneExecutor）。
     */
    public List<Map<String, Object>> listScenes() {
        Map<String, AiSceneConfig> configByScene = new LinkedHashMap<>();
        configMapper.selectList(new LambdaQueryWrapper<AiSceneConfig>()
                        .eq(AiSceneConfig::getEnabled, true)
                        .orderByDesc(AiSceneConfig::getPriority))
                .forEach(c -> configByScene.putIfAbsent(c.getSceneCode(), c));

        // 路由视图：SPI Handler 场景 + 配置驱动场景（配置行存在但无 Handler）
        Map<String, AiSceneHandler> routed = new LinkedHashMap<>(handlerMap);
        configByScene.keySet().forEach(scene ->
                routed.putIfAbsent(scene, defaultSceneExecutor));

        return routed.entrySet().stream()
                .map(e -> {
                    Map<String, Object> m = new LinkedHashMap<>();
                    m.put("sceneCode", e.getKey());
                    m.put("handler", e.getValue().getClass().getSimpleName());
                    m.put("supportedOutputMode", e.getValue().getSupportedOutputMode());
                    AiSceneConfig config = configByScene.get(e.getKey());
                    m.put("configured", config != null);
                    if (config != null) {
                        m.put("sceneName", config.getSceneName());
                        m.put("category", config.getSceneCategory());
                        m.put("outputMode", config.getOutputMode());
                        m.put("outputParser", config.getOutputParser());
                        m.put("description", config.getDescription());
                        m.put("openApi", Boolean.TRUE.equals(config.getOpenApi()));
                    }
                    return m;
                })
                .sorted(Comparator.comparing(m -> String.valueOf(m.get("sceneCode"))))
                .toList();
    }
}
