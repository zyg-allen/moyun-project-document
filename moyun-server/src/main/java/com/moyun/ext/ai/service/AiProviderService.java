package com.moyun.ext.ai.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.moyun.ext.ai.entity.AiProvider;

import java.util.List;

/**
 * AI 提供商注册表服务（V11.0.2 配置驱动改造核心）
 *
 * <p>所有"提供商能力"的判定入口：模型工厂按 {@link AiProvider#getApiStyle()} 分支、
 * 流式支持查 {@link AiProvider#getSupportsStreaming()}、连接测试按 apiStyle 分发。
 * 业务代码禁止再出现硬编码的 provider 字符串分支。</p>
 *
 * @author moyun
 */
public interface AiProviderService extends IService<AiProvider> {

    /** 按 code 查注册表（含禁用记录；未注册返回 null） */
    AiProvider getByCode(String code);

    /** 列出启用的提供商（admin 下拉 / portal 可选清单；按 sortOrder 升序） */
    List<AiProvider> listEnabled();

    /**
     * 该提供商是否支持流式输出（未注册的提供商默认不支持，由运行时三级兜底链路接管）
     */
    boolean supportsStreaming(String code);

    /**
     * 解析提供商 API 风格；未注册时回退 {@link AiProvider#STYLE_OPENAI_COMPATIBLE}
     * （绝大多数提供商均为 OpenAI 兼容端点，保证存量配置不断链）
     */
    String apiStyle(String code);

    /**
     * 提供商默认 Base URL（模型配置 baseUrl 留空时兜底）；未注册返回 null
     */
    String defaultBaseUrl(String code);

    /** 提供商是否必须配置 API Key（未注册默认必须） */
    boolean requiresApiKey(String code);

    /** 新增/修改/删除后清空注册表缓存（CRUD 走本 Service 时自动调用） */
    void evictCache();
}
