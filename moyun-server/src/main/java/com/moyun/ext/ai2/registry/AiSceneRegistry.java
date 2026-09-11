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
 * <p>统一接入层的路由核心：Handler Bean 注册（Spring容器扫描）+ 场景配置读取（ai_scene_config 表）。
 * 依据《AI能力统一接入层 — 完整方案文档》V2.0 §5.2。</p>
 *
 * <p>v11.41：合并到 ai_scene_config 表（原 ai2_scene_registry 已废弃），绑定关系与执行配置统一管理。</p>
 *
 * <p>v11.48：配置不再内存缓存——getConfig 每次直查数据库（LLM 调用为秒级，一次索引查询开销可忽略），
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

    /**
     * 初始化：注册所有 Handler Bean + 启动时一致性检查（配置行与 Handler 的匹配告警）
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
        consistencyCheck();
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
     * 按场景代码获取启用配置（v11.48：直查数据库，管理端变更即时生效）
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
     * 刷新（Handler 重新注册；配置无缓存，无需重载。供管理端/调试调用）
     */
    public synchronized void refresh() {
        init();
        log.info("[ai2] 场景注册中心刷新完成");
    }

    /**
     * 启动一致性检查：配置行存在但 Handler 未注册的，告警
     */
    private void consistencyCheck() {
        List<String> sceneCodes = configMapper.selectList(new LambdaQueryWrapper<AiSceneConfig>()
                        .eq(AiSceneConfig::getEnabled, true))
                .stream().map(AiSceneConfig::getSceneCode).distinct().toList();
        for (String sceneCode : sceneCodes) {
            if (!handlerMap.containsKey(sceneCode)) {
                log.warn("[ai2] 场景 {} 有配置但未注册 Handler（handler_bean_name 不匹配或未实现）", sceneCode);
            }
        }
        log.info("[ai2] 场景配置一致性检查完成: {} 个启用场景（Handler总数: {}）",
                sceneCodes.size(), handlerMap.size());
    }

    /**
     * 已注册场景总览（scene → {handler, config, outputMode}），管理/调试用
     */
    public List<Map<String, Object>> listScenes() {
        Map<String, AiSceneConfig> configByScene = new LinkedHashMap<>();
        configMapper.selectList(new LambdaQueryWrapper<AiSceneConfig>()
                        .eq(AiSceneConfig::getEnabled, true)
                        .orderByDesc(AiSceneConfig::getPriority))
                .forEach(c -> configByScene.putIfAbsent(c.getSceneCode(), c));
        return handlerMap.entrySet().stream()
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
