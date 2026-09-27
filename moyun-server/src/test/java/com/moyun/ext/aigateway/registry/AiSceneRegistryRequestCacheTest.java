package com.moyun.ext.aigateway.registry;

import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.moyun.ext.ai.entity.AiSceneConfig;
import com.moyun.ext.ai.mapper.AiSceneConfigMapper;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 场景配置读取：请求级记忆化（v13.19，报告 §6.2「每请求 2-3 次同表 DB 查询」）
 *
 * <h3>修复前</h3>
 * <p>{@code getConfig} 每次直查库，而一次通用入口请求会查 2~4 次同表：
 * {@code AiGatewayController.rejectIfNotOpen} 查主码 → {@code AiGatewayService.execute} 再
 * {@code getConfig(scene, task)}（查全码，未命中又回退查主码）。表小、单次是索引查询，
 * 但把"同一份配置反复查"当作常态，会让"每请求 N 次同表查询"随链路变长而失控。</p>
 *
 * <h3>修复后（请求级记忆化）</h3>
 * <ul>
 *   <li>同一次请求内同一 sceneCode 只查一次库，**负结果（未配置）也记住**；</li>
 *   <li>缓存放在 {@code RequestAttributes} 里 → 随请求销毁，**无 TTL、无跨请求残留**，
 *       "管理端改配置下次调用立即生效"的语义完全保留（本测试第 3 例专门锁定它）；</li>
 *   <li>非 HTTP 上下文（启动一致性检查、异步线程、单测直调）自动退化为每次直查。</li>
 * </ul>
 *
 * @author moyun
 */
class AiSceneRegistryRequestCacheTest {

    private static final String SCENE = "voice_interview";

    private AiSceneRegistry registry;
    private AiSceneConfigMapper configMapper;

    @AfterEach
    void tearDown() {
        RequestContextHolder.resetRequestAttributes();
    }

    private void newRegistry() {
        registry = new AiSceneRegistry();
        configMapper = mock(AiSceneConfigMapper.class);
        ReflectionTestUtils.setField(registry, "configMapper", configMapper);
    }

    /** 开启一次"新请求"（RequestAttributes 随请求销毁，用它来隔离请求边界） */
    private void beginRequest() {
        RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(new MockHttpServletRequest()));
    }

    private AiSceneConfig config(String sceneCode) {
        AiSceneConfig config = new AiSceneConfig();
        config.setSceneCode(sceneCode);
        config.setEnabled(true);
        return config;
    }

    @Test
    @DisplayName("同一请求内重复取同一场景 → 只查一次库，且返回同一实例")
    void sameSceneQueriedOncePerRequest() {
        newRegistry();
        beginRequest();
        when(configMapper.selectList(any(Wrapper.class))).thenReturn(List.of(config(SCENE)));

        AiSceneConfig first = registry.getConfig(SCENE);
        AiSceneConfig second = registry.getConfig(SCENE);

        assertNotNull(first);
        assertSame(first, second, "同请求内应复用同一配置实例（调用方只读，见类注释约定）");
        verify(configMapper, times(1)).selectList(any(Wrapper.class));
    }

    @Test
    @DisplayName("真实链路（控制器查主码 → 网关解析 scene:task）：全程只查 2 次库（修复前 3 次）")
    void fullRequestPathQueriesTwice() {
        newRegistry();
        beginRequest();
        // 主码有配置；全码（scene:task）未配置 → 网关侧解析会先查全码、再回退主码（命中记忆化）
        when(configMapper.selectList(any(Wrapper.class))).thenReturn(List.of(config(SCENE)), List.of());

        assertNotNull(registry.getConfig(SCENE), "① AiGatewayController.rejectIfNotOpen 查主码");
        assertNotNull(registry.getConfig(SCENE, "warmup"), "② 网关解析 scene:task：查全码未命中 → 回退主码（缓存命中）");
        assertNotNull(registry.getConfig(SCENE), "③ 后续任何主码读取都命中记忆化");
        assertNotNull(registry.getConfig(SCENE, "warmup"), "④ 同 (scene,task) 重复解析也全部命中");

        verify(configMapper, times(2)).selectList(any(Wrapper.class));
    }

    @Test
    @DisplayName("全码未命中时的回退语义不变：仍返回主码配置")
    void taskFallbackStillReturnsMainSceneConfig() {
        newRegistry();
        beginRequest();
        when(configMapper.selectList(any(Wrapper.class))).thenReturn(List.of(), List.of(config(SCENE)));

        assertEquals(SCENE, registry.getConfig(SCENE, "warmup").getSceneCode(), "全码未命中应回退到主码配置");
        verify(configMapper, times(2)).selectList(any(Wrapper.class));
    }

    @Test
    @DisplayName("跨请求不做缓存：管理端改了配置，下一次请求立即读到新值（即时生效语义保留）")
    void newRequestSeesFreshConfig() {
        newRegistry();
        beginRequest();
        AiSceneConfig oldOne = config(SCENE);
        oldOne.setSceneName("旧配置");
        when(configMapper.selectList(any(Wrapper.class))).thenReturn(List.of(oldOne));
        assertEquals("旧配置", registry.getConfig(SCENE).getSceneName());

        // 管理端改配置 → 新请求（新 RequestAttributes）必须读到新值，而不是请求级缓存里的旧值
        beginRequest();
        AiSceneConfig newOne = config(SCENE);
        newOne.setSceneName("新配置");
        when(configMapper.selectList(any(Wrapper.class))).thenReturn(List.of(newOne));
        assertEquals("新配置", registry.getConfig(SCENE).getSceneName(), "跨请求必须即时生效（无 TTL 残留）");
    }

    @Test
    @DisplayName("负结果记忆化：同请求内未配置场景不重复查库")
    void absentSceneIsMemoized() {
        newRegistry();
        beginRequest();
        when(configMapper.selectList(any(Wrapper.class))).thenReturn(List.of());

        assertNull(registry.getConfig("no_such_scene"));
        assertNull(registry.getConfig("no_such_scene"));
        verify(configMapper, times(1)).selectList(any(Wrapper.class));
    }

    @Test
    @DisplayName("非 HTTP 上下文（启动检查/异步线程/单测直调）退化为直查，不抛异常")
    void withoutRequestContextFallsBackToDirectQuery() {
        newRegistry();
        RequestContextHolder.resetRequestAttributes();
        when(configMapper.selectList(any(Wrapper.class))).thenReturn(List.of(config(SCENE)));

        assertNotNull(registry.getConfig(SCENE));
        assertNotNull(registry.getConfig(SCENE));
        verify(configMapper, times(2)).selectList(any(Wrapper.class));
    }
}
