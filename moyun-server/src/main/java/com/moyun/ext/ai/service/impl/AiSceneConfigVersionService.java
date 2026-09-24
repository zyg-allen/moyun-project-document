package com.moyun.ext.ai.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.moyun.ext.ai.entity.AiSceneConfig;
import com.moyun.ext.ai.entity.AiSceneConfigHistory;
import com.moyun.ext.ai.mapper.AiSceneConfigHistoryMapper;
import com.moyun.ext.ai.mapper.AiSceneConfigMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * AI 场景配置版本服务（配置版本化 · 网关整改 2A.1）
 *
 * <p>prompt 大规模迁入配置行前的回滚安全网：管理端保存自动快照 + 一键回滚历史版本。</p>
 *
 * <ul>
 *   <li>新增：config_version=1 并落首版快照（首次编辑即可回滚）；</li>
 *   <li>更新：快照旧版本 → config_version+1 → 更新（同一事务）；</li>
 *   <li>回滚：读目标版本快照写回当前行（config_version 继续递增并快照，可回滚回滚）；</li>
 *   <li>会话一致性：网关会话通道按会话首轮锁定的 config_version 调
 *       {@link #loadSnapshot} 读快照，回滚仅影响新会话。</li>
 * </ul>
 *
 * @author moyun
 * @since 2026-09-24
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AiSceneConfigVersionService {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    private final AiSceneConfigMapper configMapper;
    private final AiSceneConfigHistoryMapper historyMapper;

    /**
     * 新增配置并落首版快照（config_version=1）
     */
    @Transactional
    public void createWithSnapshot(AiSceneConfig config, String operator) {
        config.setConfigVersion(1);
        configMapper.insert(config);
        snapshot(config, operator);
    }

    /**
     * 更新配置：快照旧版本 → config_version+1 → 更新（同一事务，原子生效）
     */
    @Transactional
    public void updateWithSnapshot(AiSceneConfig incoming, String operator) {
        AiSceneConfig before = configMapper.selectById(incoming.getId());
        if (before == null) {
            throw new IllegalStateException("配置不存在: " + incoming.getId());
        }
        snapshot(before, operator);
        incoming.setConfigVersion(currentVersion(before) + 1);
        configMapper.updateById(incoming);
    }

    /**
     * 版本历史列表（新版本在前）
     */
    public List<AiSceneConfigHistory> listHistory(Long configId) {
        return historyMapper.selectList(new LambdaQueryWrapper<AiSceneConfigHistory>()
                .eq(AiSceneConfigHistory::getConfigId, configId)
                .orderByDesc(AiSceneConfigHistory::getConfigVersion));
    }

    /**
     * 一键回滚：目标版本快照写回当前行（config_version 继续递增并快照回滚结果，
     * 回滚操作本身可再次回滚）。进行中会话不受影响（网关按会话锁定版本读快照）。
     *
     * @return 回滚后的当前配置
     */
    @Transactional
    public AiSceneConfig rollback(Long configId, Integer targetVersion, String operator) {
        AiSceneConfig current = configMapper.selectById(configId);
        if (current == null) {
            throw new IllegalStateException("配置不存在: " + configId);
        }
        AiSceneConfigHistory history = historyMapper.selectOne(
                new LambdaQueryWrapper<AiSceneConfigHistory>()
                        .eq(AiSceneConfigHistory::getConfigId, configId)
                        .eq(AiSceneConfigHistory::getConfigVersion, targetVersion)
                        .orderByDesc(AiSceneConfigHistory::getId)
                        .last("LIMIT 1"));
        if (history == null) {
            throw new IllegalStateException("目标版本无快照: v" + targetVersion);
        }
        AiSceneConfig target = deserialize(history.getSnapshot());
        // 快照整体写回（显式全列 set——updateById 忽略 null 字段，旧值为空的列无法清空）
        int newVersion = currentVersion(current) + 1;
        configMapper.update(null, fullColumnUpdate(configId, target, newVersion));
        // 回滚后的行重新落快照（作为新版本，支持回滚的回滚）
        AiSceneConfig after = configMapper.selectById(configId);
        snapshot(after, operator);
        log.info("[ai-scene] 配置回滚: configId={}, scene={}, v{} -> v{}(cfg v{})",
                configId, current.getSceneCode(), currentVersion(current),
                targetVersion, newVersion);
        return after;
    }

    /** 全列回滚 UpdateWrapper（scene_code/id 为行身份不回写；JSON 列空串归 NULL） */
    private LambdaUpdateWrapper<AiSceneConfig> fullColumnUpdate(Long configId,
                                                                AiSceneConfig t, int newVersion) {
        return new LambdaUpdateWrapper<AiSceneConfig>()
                .eq(AiSceneConfig::getId, configId)
                .set(AiSceneConfig::getSceneName, t.getSceneName())
                .set(AiSceneConfig::getDescription, t.getDescription())
                .set(AiSceneConfig::getSceneCategory, t.getSceneCategory())
                .set(AiSceneConfig::getAgentId, t.getAgentId())
                .set(AiSceneConfig::getModelConfigId, t.getModelConfigId())
                .set(AiSceneConfig::getKnowledgeLibraryIds, t.getKnowledgeLibraryIds())
                .set(AiSceneConfig::getToolIds, t.getToolIds())
                .set(AiSceneConfig::getWorkflowId, t.getWorkflowId())
                .set(AiSceneConfig::getConfigJson, t.getConfigJson())
                .set(AiSceneConfig::getHandlerBeanName, t.getHandlerBeanName())
                .set(AiSceneConfig::getHandlerMethod, t.getHandlerMethod())
                .set(AiSceneConfig::getSystemPromptTemplate, t.getSystemPromptTemplate())
                .set(AiSceneConfig::getUserPromptTemplate, t.getUserPromptTemplate())
                .set(AiSceneConfig::getPromptPlaceholders, normalizeJson(t.getPromptPlaceholders()))
                .set(AiSceneConfig::getOutputMode, t.getOutputMode())
                .set(AiSceneConfig::getOutputSchema, normalizeJson(t.getOutputSchema()))
                .set(AiSceneConfig::getOutputParser, t.getOutputParser())
                .set(AiSceneConfig::getMaxTokens, t.getMaxTokens())
                .set(AiSceneConfig::getTemperature, t.getTemperature())
                .set(AiSceneConfig::getTimeoutSeconds, t.getTimeoutSeconds())
                .set(AiSceneConfig::getRetryCount, t.getRetryCount())
                .set(AiSceneConfig::getRateLimitKey, t.getRateLimitKey())
                .set(AiSceneConfig::getRateLimitCount, t.getRateLimitCount())
                .set(AiSceneConfig::getRateLimitTime, t.getRateLimitTime())
                .set(AiSceneConfig::getDailyTokenLimit, t.getDailyTokenLimit())
                .set(AiSceneConfig::getEnableOutputFilter, t.getEnableOutputFilter())
                .set(AiSceneConfig::getFallbackModelId, t.getFallbackModelId())
                .set(AiSceneConfig::getFallbackResponse, t.getFallbackResponse())
                .set(AiSceneConfig::getEnableCache, t.getEnableCache())
                .set(AiSceneConfig::getCacheTtl, t.getCacheTtl())
                .set(AiSceneConfig::getVersion, t.getVersion())
                .set(AiSceneConfig::getConfigVersion, newVersion)
                .set(AiSceneConfig::getWeight, t.getWeight())
                .set(AiSceneConfig::getPriority, t.getPriority())
                .set(AiSceneConfig::getIsDefault, t.getIsDefault())
                .set(AiSceneConfig::getEnabled, t.getEnabled())
                .set(AiSceneConfig::getOpenApi, t.getOpenApi());
    }

    /** MySQL JSON 列不接受空字符串（快照历史数据防御：空白归 NULL） */
    private String normalizeJson(String json) {
        return json == null || json.isBlank() ? null : json;
    }

    /**
     * 按场景代码 + 版本号读快照（网关会话通道：进行中会话按锁定版本读取；
     * 同版本多次快照取最新一条）
     *
     * @return 快照反序列化的配置；无快照或解析失败返回 null（调用方回落当前配置）
     */
    public AiSceneConfig loadSnapshot(String sceneCode, int version) {
        try {
            AiSceneConfigHistory history = historyMapper.selectOne(
                    new LambdaQueryWrapper<AiSceneConfigHistory>()
                            .eq(AiSceneConfigHistory::getSceneCode, sceneCode)
                            .eq(AiSceneConfigHistory::getConfigVersion, version)
                            .orderByDesc(AiSceneConfigHistory::getId)
                            .last("LIMIT 1"));
            return history == null ? null : deserialize(history.getSnapshot());
        } catch (Exception e) {
            log.warn("[ai-scene] 读取版本快照失败（回落当前配置）: scene={}, v{}: {}",
                    sceneCode, version, e.getMessage());
            return null;
        }
    }

    /** 落快照（配置行完整 JSON，含当时 config_version） */
    private void snapshot(AiSceneConfig config, String operator) {
        try {
            AiSceneConfigHistory history = new AiSceneConfigHistory();
            history.setConfigId(config.getId());
            history.setSceneCode(config.getSceneCode());
            history.setConfigVersion(currentVersion(config));
            history.setSnapshot(MAPPER.writeValueAsString(config));
            history.setOperator(operator != null ? operator : "");
            historyMapper.insert(history);
        } catch (Exception e) {
            throw new IllegalStateException("配置快照失败: " + e.getMessage(), e);
        }
    }

    private AiSceneConfig deserialize(String snapshotJson) {
        try {
            return MAPPER.readValue(snapshotJson, AiSceneConfig.class);
        } catch (Exception e) {
            throw new IllegalStateException("快照解析失败: " + e.getMessage(), e);
        }
    }

    private int currentVersion(AiSceneConfig config) {
        return config.getConfigVersion() != null ? config.getConfigVersion() : 1;
    }
}
