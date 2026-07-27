package com.moyun.agent.service.impl;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.moyun.agent.constant.RedisKeys;
import com.moyun.agent.entity.ModelConfig;
import com.moyun.agent.entity.WorkflowExecution;
import com.moyun.agent.mapper.TokenUsageLogMapper;
import com.moyun.agent.service.ModelConfigService;
import com.moyun.agent.service.TokenUsageService;
import jakarta.annotation.PreDestroy;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Token使用统计服务实现类
 *
 * <p>使用 Redis 缓存 + 定时批量写入数据库的方式提升性能</p>
 *
 * @author laomao
 */
@Slf4j
@Service
public class TokenUsageServiceImpl implements TokenUsageService {

    private final TokenUsageLogMapper tokenUsageLogMapper;
    private final StringRedisTemplate redisTemplate;
    private final ModelConfigService modelConfigService;
    private final ObjectMapper objectMapper;

    private final Map<String, BigDecimal[]> priceCache = new ConcurrentHashMap<>();
    private volatile long priceCacheExpireTime = 0;
    private static final long PRICE_CACHE_TTL = 5 * 60 * 1000;

    private static final BigDecimal DEFAULT_INPUT_PRICE = new BigDecimal("0.001");
    private static final BigDecimal DEFAULT_OUTPUT_PRICE = new BigDecimal("0.002");

    private final AtomicLong totalRecordsProcessed = new AtomicLong(0);
    private final AtomicLong totalRecordsFailed = new AtomicLong(0);
    private final AtomicLong lastFlushTime = new AtomicLong(System.currentTimeMillis());

    private static final double CHINESE_CHARS_PER_TOKEN = 1.5;
    private static final double ENGLISH_CHARS_PER_TOKEN = 4.0;

    public TokenUsageServiceImpl(TokenUsageLogMapper tokenUsageLogMapper,
                                 StringRedisTemplate redisTemplate,
                                 ModelConfigService modelConfigService) {
        this.tokenUsageLogMapper = tokenUsageLogMapper;
        this.redisTemplate = redisTemplate;
        this.modelConfigService = modelConfigService;
        this.objectMapper = new ObjectMapper();
        this.objectMapper.registerModule(new JavaTimeModule());
        this.objectMapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
    }

    @Override
    public void recordUsageAsync(Long conversationId, Long agentId, String modelName,
                                 String modelProvider, int inputTokens, int outputTokens,
                                 String requestType) {
        try {
            int totalTokens = inputTokens + outputTokens;
            BigDecimal cost = calculateCost(modelName, inputTokens, outputTokens);
            String type = requestType != null ? requestType : "chat";

            WorkflowExecution.TokenUsageLog usageLog = WorkflowExecution.TokenUsageLog.builder()
                    .conversationId(conversationId)
                    .agentId(agentId)
                    .modelName(modelName)
                    .modelProvider(modelProvider)
                    .inputTokens(inputTokens)
                    .outputTokens(outputTokens)
                    .totalTokens(totalTokens)
                    .cost(cost)
                    .requestType(type)
                    .createTime(LocalDateTime.now())
                    .build();

            String logJson = objectMapper.writeValueAsString(usageLog);
            redisTemplate.opsForList().rightPush(RedisKeys.TOKEN_USAGE_LOGS, logJson);

            updateRealtimeStats(inputTokens, outputTokens, totalTokens, cost, modelName, type);

        } catch (Exception e) {
            log.error("记录Token使用到Redis失败，降级直接写数据库", e);
            recordUsageDirectToDB(conversationId, agentId, modelName, modelProvider, inputTokens, outputTokens, requestType);
        }
    }

    private void updateRealtimeStats(int inputTokens, int outputTokens, int totalTokens,
                                     BigDecimal cost, String modelName, String type) {
        String today = LocalDate.now().toString();
        String statsKey = RedisKeys.tokenRealtimeStats(today);

        redisTemplate.opsForHash().increment(statsKey, "request_count", 1);
        redisTemplate.opsForHash().increment(statsKey, "total_input", inputTokens);
        redisTemplate.opsForHash().increment(statsKey, "total_output", outputTokens);
        redisTemplate.opsForHash().increment(statsKey, "total_tokens", totalTokens);
        redisTemplate.opsForHash().increment(statsKey, "total_cost_micro", cost.multiply(BigDecimal.valueOf(1000000)).longValue());

        String modelKey = RedisKeys.tokenRealtimeModel(today, modelName);
        redisTemplate.opsForHash().increment(modelKey, "request_count", 1);
        redisTemplate.opsForHash().increment(modelKey, "total_tokens", totalTokens);
        redisTemplate.opsForHash().increment(modelKey, "total_cost_micro", cost.multiply(BigDecimal.valueOf(1000000)).longValue());

        String typeKey = RedisKeys.tokenRealtimeType(today, type);
        redisTemplate.opsForHash().increment(typeKey, "request_count", 1);
        redisTemplate.opsForHash().increment(typeKey, "total_input", inputTokens);
        redisTemplate.opsForHash().increment(typeKey, "total_tokens", totalTokens);
        redisTemplate.opsForHash().increment(typeKey, "total_cost_micro", cost.multiply(BigDecimal.valueOf(1000000)).longValue());

        redisTemplate.expire(statsKey, RedisKeys.TOKEN_STATS_EXPIRE_DAYS, TimeUnit.DAYS);
        redisTemplate.expire(modelKey, RedisKeys.TOKEN_STATS_EXPIRE_DAYS, TimeUnit.DAYS);
        redisTemplate.expire(typeKey, RedisKeys.TOKEN_STATS_EXPIRE_DAYS, TimeUnit.DAYS);
    }

    private void recordUsageDirectToDB(Long conversationId, Long agentId, String modelName,
                                       String modelProvider, int inputTokens, int outputTokens,
                                       String requestType) {
        int totalTokens = inputTokens + outputTokens;
        BigDecimal cost = calculateCost(modelName, inputTokens, outputTokens);

        WorkflowExecution.TokenUsageLog usageLog = WorkflowExecution.TokenUsageLog.builder()
                .conversationId(conversationId)
                .agentId(agentId)
                .modelName(modelName)
                .modelProvider(modelProvider)
                .inputTokens(inputTokens)
                .outputTokens(outputTokens)
                .totalTokens(totalTokens)
                .cost(cost)
                .requestType(requestType != null ? requestType : "chat")
                .createTime(LocalDateTime.now())
                .build();

        tokenUsageLogMapper.insert(usageLog);
    }

    @Override
    public void recordEmbeddingUsageAsync(Long agentId, String modelName, String modelProvider,
                                          String text, String requestType) {
        int estimatedTokens = estimateTokens(text);
        recordUsageAsync(null, agentId, modelName, modelProvider, estimatedTokens, 0, requestType);
    }

    @Override
    public void recordWorkflowUsageAsync(Long workflowId, Long executionId, String nodeId,
                                         String modelName, String modelProvider,
                                         int inputTokens, int outputTokens, String requestType) {
        try {
            int totalTokens = inputTokens + outputTokens;
            BigDecimal cost = calculateCost(modelName, inputTokens, outputTokens);
            String type = requestType != null ? requestType : "workflow_llm";

            WorkflowExecution.TokenUsageLog usageLog = WorkflowExecution.TokenUsageLog.builder()
                    .workflowId(workflowId)
                    .workflowExecutionId(executionId)
                    .workflowNodeId(nodeId)
                    .modelName(modelName)
                    .modelProvider(modelProvider)
                    .inputTokens(inputTokens)
                    .outputTokens(outputTokens)
                    .totalTokens(totalTokens)
                    .cost(cost)
                    .requestType(type)
                    .createTime(LocalDateTime.now())
                    .build();

            String logJson = objectMapper.writeValueAsString(usageLog);
            redisTemplate.opsForList().rightPush(RedisKeys.TOKEN_USAGE_LOGS, logJson);

            updateRealtimeStats(inputTokens, outputTokens, totalTokens, cost, modelName, type);

        } catch (Exception e) {
            log.error("记录工作流Token使用失败", e);
        }
    }

    @Override
    public void recordEmbeddingBatchUsageAsync(Long agentId, String modelName, String modelProvider,
                                               List<String> texts, String requestType) {
        int totalTokens = 0;
        for (String text : texts) {
            totalTokens += estimateTokens(text);
        }
        recordUsageAsync(null, agentId, modelName, modelProvider, totalTokens, 0, requestType);
    }

    @Override
    @Scheduled(fixedRate = 60000)
    public void flushLogsToDB() {
        long startTime = System.currentTimeMillis();
        int successCount = 0;
        int failCount = 0;

        try {
            Long size = redisTemplate.opsForList().size(RedisKeys.TOKEN_USAGE_LOGS);
            if (size == null || size == 0) {
                return;
            }

            int batchSize = Math.min(size.intValue(), 500);
            List<String> logJsons = redisTemplate.opsForList().range(RedisKeys.TOKEN_USAGE_LOGS, 0, batchSize - 1);
            if (logJsons == null || logJsons.isEmpty()) {
                return;
            }

            redisTemplate.opsForList().trim(RedisKeys.TOKEN_USAGE_LOGS, batchSize, -1);

            List<WorkflowExecution.TokenUsageLog> logs = new ArrayList<>(logJsons.size());
            List<String> failedJsons = new ArrayList<>();

            for (String logJson : logJsons) {
                try {
                    WorkflowExecution.TokenUsageLog usageLog = objectMapper.readValue(logJson, WorkflowExecution.TokenUsageLog.class);
                    logs.add(usageLog);
                } catch (JsonProcessingException e) {
                    log.error("解析Token日志JSON失败", e);
                    failCount++;
                }
            }

            if (!logs.isEmpty()) {
                int insertBatchSize = 100;
                for (int i = 0; i < logs.size(); i += insertBatchSize) {
                    int end = Math.min(i + insertBatchSize, logs.size());
                    List<WorkflowExecution.TokenUsageLog> batch = logs.subList(i, end);

                    try {
                        for (WorkflowExecution.TokenUsageLog usageLog : batch) {
                            tokenUsageLogMapper.insert(usageLog);
                            successCount++;
                        }
                    } catch (Exception e) {
                        log.error("批量插入Token日志失败", e);
                        for (WorkflowExecution.TokenUsageLog usageLog : batch) {
                            try {
                                failedJsons.add(objectMapper.writeValueAsString(usageLog));
                                failCount++;
                            } catch (Exception ex) {
                                log.error("序列化失败日志失败", ex);
                            }
                        }
                    }
                }

                if (!failedJsons.isEmpty()) {
                    redisTemplate.opsForList().rightPushAll(RedisKeys.TOKEN_USAGE_LOGS, failedJsons);
                }
            }

            totalRecordsProcessed.addAndGet(successCount);
            totalRecordsFailed.addAndGet(failCount);
            lastFlushTime.set(System.currentTimeMillis());

            if (successCount > 0) {
                long duration = System.currentTimeMillis() - startTime;
                log.info("批量写入Token日志: 成功={}, 失败={}, 耗时={}ms", successCount, failCount, duration);
            }
        } catch (Exception e) {
            log.error("批量写入Token日志失败", e);
        }
    }

    @PreDestroy
    public void onShutdown() {
        log.info("应用关闭，开始刷新Token日志到数据库...");
        try {
            Long size = redisTemplate.opsForList().size(RedisKeys.TOKEN_USAGE_LOGS);
            if (size != null && size > 0) {
                for (int i = 0; i < size; i++) {
                    String logJson = redisTemplate.opsForList().leftPop(RedisKeys.TOKEN_USAGE_LOGS);
                    if (logJson == null) break;
                    try {
                        WorkflowExecution.TokenUsageLog usageLog = objectMapper.readValue(logJson, WorkflowExecution.TokenUsageLog.class);
                        tokenUsageLogMapper.insert(usageLog);
                    } catch (Exception e) {
                        log.error("关闭时写入日志失败", e);
                    }
                }
                log.info("关闭时刷新Token日志完成: {} 条", size);
            }
        } catch (Exception e) {
            log.error("关闭时刷新Token日志失败", e);
        }
    }

    @Override
    public int estimateTokens(String text) {
        if (text == null || text.isEmpty()) {
            return 0;
        }

        int chineseChars = 0;
        int otherChars = 0;

        for (char c : text.toCharArray()) {
            if (Character.UnicodeScript.of(c) == Character.UnicodeScript.HAN) {
                chineseChars++;
            } else {
                otherChars++;
            }
        }

        int chineseTokens = (int) Math.ceil(chineseChars / CHINESE_CHARS_PER_TOKEN);
        int otherTokens = (int) Math.ceil(otherChars / ENGLISH_CHARS_PER_TOKEN);

        return chineseTokens + otherTokens;
    }

    private BigDecimal calculateCost(String modelName, int inputTokens, int outputTokens) {
        BigDecimal[] prices = getModelPrices(modelName);

        BigDecimal inputCost = prices[0].multiply(BigDecimal.valueOf(inputTokens))
                .divide(BigDecimal.valueOf(1000), 6, RoundingMode.HALF_UP);
        BigDecimal outputCost = prices[1].multiply(BigDecimal.valueOf(outputTokens))
                .divide(BigDecimal.valueOf(1000), 6, RoundingMode.HALF_UP);

        return inputCost.add(outputCost);
    }

    private BigDecimal[] getModelPrices(String modelName) {
        if (System.currentTimeMillis() > priceCacheExpireTime) {
            refreshPriceCache();
        }

        BigDecimal[] prices = priceCache.get(modelName);
        if (prices != null) {
            return prices;
        }

        try {
            ModelConfig config = modelConfigService.getByModelName(modelName);
            if (config != null && config.getInputPrice() != null) {
                prices = new BigDecimal[]{
                    config.getInputPrice(),
                    config.getOutputPrice() != null ? config.getOutputPrice() : BigDecimal.ZERO
                };
                priceCache.put(modelName, prices);
                return prices;
            }
        } catch (Exception e) {
            log.warn("获取模型价格失败: {}", e.getMessage());
        }

        return new BigDecimal[]{DEFAULT_INPUT_PRICE, DEFAULT_OUTPUT_PRICE};
    }

    private synchronized void refreshPriceCache() {
        if (System.currentTimeMillis() <= priceCacheExpireTime) {
            return;
        }

        try {
            List<ModelConfig> configs = modelConfigService.list();
            priceCache.clear();
            for (ModelConfig config : configs) {
                if (config.getModelName() != null && config.getInputPrice() != null) {
                    priceCache.put(config.getModelName(), new BigDecimal[]{
                        config.getInputPrice(),
                        config.getOutputPrice() != null ? config.getOutputPrice() : BigDecimal.ZERO
                    });
                }
            }
            priceCacheExpireTime = System.currentTimeMillis() + PRICE_CACHE_TTL;
        } catch (Exception e) {
            log.error("刷新价格缓存失败", e);
            priceCacheExpireTime = System.currentTimeMillis() + 60000;
        }
    }

    @Override
    public Map<String, Object> getTodayStats() {
        return getTodayStatsFromRedis();
    }

    private Map<String, Object> getTodayStatsFromRedis() {
        try {
            String today = LocalDate.now().toString();
            String statsKey = RedisKeys.tokenRealtimeStats(today);

            Map<Object, Object> redisStats = redisTemplate.opsForHash().entries(statsKey);
            if (redisStats != null && !redisStats.isEmpty()) {
                Map<String, Object> stats = new HashMap<>();
                stats.put("request_count", parseLong(redisStats.get("request_count")));
                stats.put("total_input", parseLong(redisStats.get("total_input")));
                stats.put("total_output", parseLong(redisStats.get("total_output")));
                stats.put("total_tokens", parseLong(redisStats.get("total_tokens")));
                long costMicro = parseLong(redisStats.get("total_cost_micro"));
                stats.put("total_cost", BigDecimal.valueOf(costMicro).divide(BigDecimal.valueOf(1000000), 6, RoundingMode.HALF_UP));
                return stats;
            }
        } catch (Exception e) {
            log.warn("从Redis获取今日统计失败，降级查询数据库: {}", e.getMessage());
        }

        LocalDateTime startOfDay = LocalDate.now().atStartOfDay();
        LocalDateTime endOfDay = LocalDate.now().atTime(LocalTime.MAX);
        Map<String, Object> stats = tokenUsageLogMapper.getTotalStats(startOfDay, endOfDay);
        return stats != null ? stats : createEmptyStats();
    }

    @Override
    public Map<String, Object> getStats(LocalDate startDate, LocalDate endDate) {
        LocalDate today = LocalDate.now();

        if (!endDate.isBefore(today) && !startDate.isAfter(today)) {
            Map<String, Object> todayStats = getTodayStatsFromRedis();

            if (startDate.equals(today) && endDate.equals(today)) {
                return todayStats;
            }

            LocalDateTime startTime = startDate.atStartOfDay();
            LocalDateTime endTime = today.minusDays(1).atTime(LocalTime.MAX);

            if (!startDate.isAfter(today.minusDays(1))) {
                Map<String, Object> dbStats = tokenUsageLogMapper.getTotalStats(startTime, endTime);
                if (dbStats != null) {
                    return mergeStats(dbStats, todayStats);
                }
            }
            return todayStats;
        }

        LocalDateTime startTime = startDate.atStartOfDay();
        LocalDateTime endTime = endDate.atTime(LocalTime.MAX);
        Map<String, Object> stats = tokenUsageLogMapper.getTotalStats(startTime, endTime);
        return stats != null ? stats : createEmptyStats();
    }

    private Map<String, Object> mergeStats(Map<String, Object> stats1, Map<String, Object> stats2) {
        Map<String, Object> merged = new HashMap<>();
        merged.put("request_count", parseLong(stats1.get("request_count")) + parseLong(stats2.get("request_count")));
        merged.put("total_input", parseLong(stats1.get("total_input")) + parseLong(stats2.get("total_input")));
        merged.put("total_output", parseLong(stats1.get("total_output")) + parseLong(stats2.get("total_output")));
        merged.put("total_tokens", parseLong(stats1.get("total_tokens")) + parseLong(stats2.get("total_tokens")));

        BigDecimal cost1 = parseBigDecimal(stats1.get("total_cost"));
        BigDecimal cost2 = parseBigDecimal(stats2.get("total_cost"));
        merged.put("total_cost", cost1.add(cost2));

        return merged;
    }

    private long parseLong(Object value) {
        if (value == null) return 0L;
        if (value instanceof Number) return ((Number) value).longValue();
        try {
            return Long.parseLong(value.toString());
        } catch (NumberFormatException e) {
            return 0L;
        }
    }

    private BigDecimal parseBigDecimal(Object value) {
        if (value == null) return BigDecimal.ZERO;
        if (value instanceof BigDecimal) return (BigDecimal) value;
        if (value instanceof Number) return BigDecimal.valueOf(((Number) value).doubleValue());
        try {
            return new BigDecimal(value.toString());
        } catch (NumberFormatException e) {
            return BigDecimal.ZERO;
        }
    }

    private Map<String, Object> createEmptyStats() {
        Map<String, Object> empty = new HashMap<>();
        empty.put("total_input", 0L);
        empty.put("total_output", 0L);
        empty.put("total_tokens", 0L);
        empty.put("total_cost", BigDecimal.ZERO);
        empty.put("request_count", 0L);
        return empty;
    }

    @Override
    public List<Map<String, Object>> statByAgent(Long agentId, LocalDate startDate, LocalDate endDate) {
        LocalDateTime startTime = startDate.atStartOfDay();
        LocalDateTime endTime = endDate.atTime(LocalTime.MAX);
        return tokenUsageLogMapper.statByAgent(agentId, startTime, endTime);
    }

    @Override
    public List<Map<String, Object>> statByDate(LocalDate startDate, LocalDate endDate) {
        LocalDateTime startTime = startDate.atStartOfDay();
        LocalDateTime endTime = endDate.atTime(LocalTime.MAX);
        List<Map<String, Object>> dbStats = tokenUsageLogMapper.statByDate(startTime, endTime);

        LocalDate today = LocalDate.now();
        if (!endDate.isBefore(today) && !startDate.isAfter(today)) {
            Map<String, Object> todayStats = getTodayStatsFromRedis();
            if (parseLong(todayStats.get("request_count")) > 0) {
                boolean hasTodayInDb = dbStats.stream()
                        .anyMatch(s -> today.toString().equals(String.valueOf(s.get("stat_date"))));

                if (!hasTodayInDb) {
                    Map<String, Object> todayRow = new HashMap<>(todayStats);
                    todayRow.put("stat_date", today.toString());
                    todayRow.put("model_name", "all");
                    dbStats.add(0, todayRow);
                }
            }
        }

        return dbStats;
    }

    @Override
    public List<Map<String, Object>> statByModel(LocalDate startDate, LocalDate endDate) {
        LocalDateTime startTime = startDate.atStartOfDay();
        LocalDateTime endTime = endDate.atTime(LocalTime.MAX);
        List<Map<String, Object>> result = tokenUsageLogMapper.statByModel(startTime, endTime);
        return result != null ? result : new ArrayList<>();
    }

    @Override
    public List<Map<String, Object>> statByRequestType(LocalDate startDate, LocalDate endDate) {
        LocalDateTime startTime = startDate.atStartOfDay();
        LocalDateTime endTime = endDate.atTime(LocalTime.MAX);
        List<Map<String, Object>> dbStats = tokenUsageLogMapper.statByRequestType(startTime, endTime);

        LocalDate today = LocalDate.now();
        if (!endDate.isBefore(today) && !startDate.isAfter(today)) {
            try {
                String[] types = {"chat", "embedding_query", "embedding_document", "embedding_image",
                        "workflow_llm", "workflow_classifier", "workflow_extractor", "workflow_question"};
                for (String type : types) {
                    String typeKey = RedisKeys.tokenRealtimeType(today.toString(), type);
                    Map<Object, Object> redisTypeStats = redisTemplate.opsForHash().entries(typeKey);
                    if (redisTypeStats != null && !redisTypeStats.isEmpty()) {
                        long requestCount = parseLong(redisTypeStats.get("request_count"));
                        if (requestCount > 0) {
                            boolean hasInDb = dbStats.stream()
                                    .anyMatch(s -> type.equals(s.get("request_type")));
                            if (!hasInDb) {
                                Map<String, Object> typeRow = new HashMap<>();
                                typeRow.put("request_type", type);
                                typeRow.put("request_count", requestCount);
                                typeRow.put("total_input", parseLong(redisTypeStats.get("total_input")));
                                typeRow.put("total_tokens", parseLong(redisTypeStats.get("total_tokens")));
                                long costMicro = parseLong(redisTypeStats.get("total_cost_micro"));
                                typeRow.put("total_cost", BigDecimal.valueOf(costMicro).divide(BigDecimal.valueOf(1000000), 6, RoundingMode.HALF_UP));
                                dbStats.add(typeRow);
                            }
                        }
                    }
                }
            } catch (Exception e) {
                log.warn("从Redis获取类型统计失败: {}", e.getMessage());
            }
        }

        return dbStats != null ? dbStats : new ArrayList<>();
    }

    @Override
    public Map<String, Object> getOverview(LocalDate startDate, LocalDate endDate) {
        Map<String, Object> overview = new HashMap<>();
        overview.put("total", getStats(startDate, endDate));
        overview.put("byModel", statByModel(startDate, endDate));
        overview.put("byDate", statByDate(startDate, endDate));
        overview.put("byType", statByRequestType(startDate, endDate));
        return overview;
    }

    @Override
    public long getPendingLogsCount() {
        Long size = redisTemplate.opsForList().size(RedisKeys.TOKEN_USAGE_LOGS);
        return size != null ? size : 0;
    }

    @Override
    public Map<String, Object> getMetrics() {
        Map<String, Object> metrics = new HashMap<>();
        metrics.put("pendingLogs", getPendingLogsCount());
        metrics.put("totalProcessed", totalRecordsProcessed.get());
        metrics.put("totalFailed", totalRecordsFailed.get());
        metrics.put("lastFlushTime", lastFlushTime.get());
        metrics.put("priceCacheSize", priceCache.size());
        metrics.put("priceCacheExpireIn", Math.max(0, priceCacheExpireTime - System.currentTimeMillis()) / 1000);
        return metrics;
    }

    @Override
    public int manualFlush() {
        long before = getPendingLogsCount();
        flushLogsToDB();
        long after = getPendingLogsCount();
        return (int) (before - after);
    }

    @Override
    public void clearPriceCache() {
        priceCache.clear();
        priceCacheExpireTime = 0;
        log.info("价格缓存已清除");
    }
}
