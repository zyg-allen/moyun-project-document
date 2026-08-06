package com.moyun.ext.ai.service;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

/**
 * Token使用统计服务接口
 *
 * <p>使用 Redis 缓存 + 定时批量写入数据库的方式提升性能</p>
 *
 * @author laomao
 */
public interface TokenUsageService {

    /**
     * 记录Token使用（写入Redis，高性能）
     */
    void recordUsageAsync(Long conversationId, Long agentId, String modelName,
                          String modelProvider, int inputTokens, int outputTokens,
                          String requestType);

    /**
     * 记录Embedding使用情况
     */
    void recordEmbeddingUsageAsync(Long agentId, String modelName, String modelProvider,
                                   String text, String requestType);

    /**
     * 记录工作流Token使用
     */
    void recordWorkflowUsageAsync(Long workflowId, Long executionId, String nodeId,
                                  String modelName, String modelProvider,
                                  int inputTokens, int outputTokens, String requestType);

    /**
     * 记录Embedding批量使用情况
     */
    void recordEmbeddingBatchUsageAsync(Long agentId, String modelName, String modelProvider,
                                        List<String> texts, String requestType);

    /**
     * 定时任务：批量将Redis中的日志写入数据库
     */
    void flushLogsToDB();

    /**
     * 估算文本的Token数量
     */
    int estimateTokens(String text);

    /**
     * 获取今日统计
     */
    Map<String, Object> getTodayStats();

    /**
     * 获取指定日期范围的统计
     */
    Map<String, Object> getStats(LocalDate startDate, LocalDate endDate);

    /**
     * 按智能体统计
     */
    List<Map<String, Object>> statByAgent(Long agentId, LocalDate startDate, LocalDate endDate);

    /**
     * 按日期统计
     */
    List<Map<String, Object>> statByDate(LocalDate startDate, LocalDate endDate);

    /**
     * 按模型统计
     */
    List<Map<String, Object>> statByModel(LocalDate startDate, LocalDate endDate);

    /**
     * 按请求类型统计
     */
    List<Map<String, Object>> statByRequestType(LocalDate startDate, LocalDate endDate);

    /**
     * 获取统计概览
     */
    Map<String, Object> getOverview(LocalDate startDate, LocalDate endDate);

    /**
     * 获取Redis中待写入的日志数量
     */
    long getPendingLogsCount();

    /**
     * 获取服务监控指标
     */
    Map<String, Object> getMetrics();

    /**
     * 手动触发刷新日志到数据库
     */
    int manualFlush();

    /**
     * 清除价格缓存
     */
    void clearPriceCache();
}
