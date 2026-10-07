package com.moyun.ext.ai.config;

import com.moyun.system.service.ISysConfigService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * RAG 参数运行时设置服务（v14.72 统一收口治理批次5）
 *
 * <p><b>解决的问题</b>：RAG 开关/阈值原固化在 {@link RagConfig}（yaml
 * {@code moyun-ai.rag.*}），改配置必须重启——意图识别、Self-RAG 验证、查询改写
 * 等能力在故障/成本异常时无法在线关闭。本服务把 RAG 参数升级为
 * <b>sys_config 优先、yaml 兜底</b>的双层配置：管理台「参数设置」改
 * {@code ai.rag.*} 即时生效（经 ISysConfigService 的 Redis 缓存 + 管理端保存刷新），
 * 未配置的键回落 yaml 默认值——部署零依赖，参数可渐进接管。</p>
 *
 * <p><b>读取契约</b>：</p>
 * <ul>
 *   <li>sys_config 键约定 {@code ai.rag.<字段名>}（与 {@code ai.global.enabled}
 *       既有命名一致）：值为合法字面量（true/false/数字）；</li>
 *   <li>键缺失/值空白/解析失败/仓储异常 → 回落 {@link RagConfig} 默认值
 *       （静默降级，RAG 主链路不因配置读取故障中断）；</li>
 *   <li>消费方<b>每轮对话实时读取</b>（不缓存长周期），管理台改参即时生效。</li>
 * </ul>
 *
 * <p><b>消费方</b>：DynamicChatServiceImpl（记忆滑窗/摘要/意图/Self-RAG 装配）、
 * RagRetrievalServiceImpl（混合检索/查询扩展/召回参数）、SelfRagServiceImpl
 * （验证批量阈值）。</p>
 *
 * @author laomao
 * @since 2026-10-05
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class RagSettingsService {

    /** sys_config 键前缀 */
    private static final String KEY_PREFIX = "ai.rag.";

    /** yaml 兜底配置（moyun-ai.rag.*，亦是全量默认值来源） */
    private final RagConfig ragConfig;
    /** 系统参数服务（Redis 缓存 + 管理端保存即刷新） */
    private final ISysConfigService sysConfigService;

    // ========== 检索策略 ==========

    public boolean enableHybridSearch() {
        return bool("enableHybridSearch", ragConfig.isEnableHybridSearch());
    }

    public boolean enableQueryExpansion() {
        return bool("enableQueryExpansion", ragConfig.isEnableQueryExpansion());
    }

    public boolean enableQueryRewriting() {
        return bool("enableQueryRewriting", ragConfig.isEnableQueryRewriting());
    }

    public int minRecallCount() {
        return intVal("minRecallCount", ragConfig.getMinRecallCount());
    }

    public double bm25Weight() {
        return doubleVal("bm25Weight", ragConfig.getBm25Weight());
    }

    public double vectorWeight() {
        return doubleVal("vectorWeight", ragConfig.getVectorWeight());
    }

    // ========== Self-RAG 验证 ==========

    public boolean enableSelfRag() {
        return bool("enableSelfRag", ragConfig.isEnableSelfRag());
    }

    public double selfRagMinRelevance() {
        return doubleVal("selfRagMinRelevance", ragConfig.getSelfRagMinRelevance());
    }

    public int selfRagMaxVerifyCount() {
        return intVal("selfRagMaxVerifyCount", ragConfig.getSelfRagMaxVerifyCount());
    }

    // ========== 记忆与编排 ==========

    public int maxMessages() {
        return intVal("maxMessages", ragConfig.getMaxMessages());
    }

    public boolean enableConversationSummary() {
        return bool("enableConversationSummary", ragConfig.isEnableConversationSummary());
    }

    public int summaryThreshold() {
        return intVal("summaryThreshold", ragConfig.getSummaryThreshold());
    }

    public int summaryKeepRecent() {
        return intVal("summaryKeepRecent", ragConfig.getSummaryKeepRecent());
    }

    public boolean enableIntentRecognition() {
        return bool("enableIntentRecognition", ragConfig.isEnableIntentRecognition());
    }

    // ==================== 内部实现 ====================

    private boolean bool(String name, boolean dft) {
        String raw = raw(name);
        if (raw == null || raw.isBlank()) {
            return dft;
        }
        return Boolean.parseBoolean(raw.trim());
    }

    private int intVal(String name, int dft) {
        String raw = raw(name);
        if (raw == null || raw.isBlank()) {
            return dft;
        }
        try {
            return Integer.parseInt(raw.trim());
        } catch (NumberFormatException e) {
            log.warn("[ai.rag] 参数 {} 值非法（{}），回落 yaml 默认 {}", name, raw, dft);
            return dft;
        }
    }

    private double doubleVal(String name, double dft) {
        String raw = raw(name);
        if (raw == null || raw.isBlank()) {
            return dft;
        }
        try {
            return Double.parseDouble(raw.trim());
        } catch (NumberFormatException e) {
            log.warn("[ai.rag] 参数 {} 值非法（{}），回落 yaml 默认 {}", name, raw, dft);
            return dft;
        }
    }

    /** sys_config 读取（异常静默回落，配置仓储故障不阻断 RAG 主链路） */
    private String raw(String name) {
        try {
            return sysConfigService.selectConfigByKey(KEY_PREFIX + name);
        } catch (Exception e) {
            log.warn("[ai.rag] 参数 {} 读取失败（回落 yaml 默认）: {}", name, e.getMessage());
            return null;
        }
    }
}
