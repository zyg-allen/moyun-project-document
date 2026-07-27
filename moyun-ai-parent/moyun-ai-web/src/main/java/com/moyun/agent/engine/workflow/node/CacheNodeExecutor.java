package com.moyun.agent.engine.workflow.node;

import com.moyun.agent.engine.workflow.WorkflowContext;
import com.moyun.agent.engine.workflow.WorkflowNode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

/**
 * 缓存节点执行器
 *
 * <p>支持缓存数据的读写操作，优先使用Redis，降级为内存缓存</p>
 * <p>支持的操作类型：</p>
 * <ul>
 *     <li>get - 读取缓存</li>
 *     <li>set - 写入缓存</li>
 *     <li>delete - 删除缓存</li>
 *     <li>exists - 检查缓存是否存在</li>
 * </ul>
 *
 * @author laomao
 * @since 2025-12-12
 */
@Slf4j
@Component
public class CacheNodeExecutor extends BaseNodeExecutor {

    @Autowired(required = false)
    private StringRedisTemplate redisTemplate;

    // 内存缓存（Redis不可用时的降级方案）
    private static final ConcurrentHashMap<String, CacheEntry> memoryCache = new ConcurrentHashMap<>();

    @Override
    public String getType() {
        return "cache";
    }

    @Override
    public NodeResult execute(WorkflowNode node, WorkflowContext context) {
        Map<String, Object> config = node.getConfig();
        if (config == null) {
            return NodeResult.fail("缓存节点配置为空");
        }

        try {
            String operation = (String) config.getOrDefault("operation", "get");
            String key = (String) config.get("key");
            String outputVariable = (String) config.getOrDefault("outputVariable", "cache_result");

            if (key == null || key.trim().isEmpty()) {
                return NodeResult.fail("缓存键为空");
            }

            // 替换变量
            key = replaceVariables(key, context);
            // 添加前缀防止冲突
            String fullKey = "workflow:cache:" + key;

            log.info("🗄️ 缓存节点执行: operation={}, key={}", operation, key);

            Object result;
            switch (operation.toLowerCase()) {
                case "get":
                    result = doGet(fullKey);
                    break;
                case "set":
                    String value = (String) config.get("value");
                    if (value != null) {
                        value = replaceVariables(value, context);
                    }
                    int ttl = getIntValue(config.get("ttl"), 3600);
                    result = doSet(fullKey, value, ttl);
                    break;
                case "delete":
                    result = doDelete(fullKey);
                    break;
                case "exists":
                    result = doExists(fullKey);
                    break;
                default:
                    return NodeResult.fail("不支持的操作类型: " + operation);
            }

            log.info("🗄️ 缓存操作完成: result={}", result);

            context.setVariable(outputVariable, result);
            return NodeResult.success(result);

        } catch (Exception e) {
            log.error("缓存节点执行失败", e);
            return NodeResult.fail("缓存操作失败: " + e.getMessage());
        }
    }

    private Object doGet(String key) {
        if (redisTemplate != null) {
            try {
                String value = redisTemplate.opsForValue().get(key);
                return value != null ? value : "";
            } catch (Exception e) {
                log.warn("Redis读取失败，使用内存缓存: {}", e.getMessage());
            }
        }
        // 降级到内存缓存
        CacheEntry entry = memoryCache.get(key);
        if (entry != null && !entry.isExpired()) {
            return entry.getValue();
        }
        return "";
    }

    private boolean doSet(String key, String value, int ttlSeconds) {
        if (redisTemplate != null) {
            try {
                redisTemplate.opsForValue().set(key, value, ttlSeconds, TimeUnit.SECONDS);
                return true;
            } catch (Exception e) {
                log.warn("Redis写入失败，使用内存缓存: {}", e.getMessage());
            }
        }
        // 降级到内存缓存
        memoryCache.put(key, new CacheEntry(value, ttlSeconds));
        return true;
    }

    private boolean doDelete(String key) {
        if (redisTemplate != null) {
            try {
                return Boolean.TRUE.equals(redisTemplate.delete(key));
            } catch (Exception e) {
                log.warn("Redis删除失败，使用内存缓存: {}", e.getMessage());
            }
        }
        return memoryCache.remove(key) != null;
    }

    private boolean doExists(String key) {
        if (redisTemplate != null) {
            try {
                return Boolean.TRUE.equals(redisTemplate.hasKey(key));
            } catch (Exception e) {
                log.warn("Redis检查失败，使用内存缓存: {}", e.getMessage());
            }
        }
        CacheEntry entry = memoryCache.get(key);
        return entry != null && !entry.isExpired();
    }

    private int getIntValue(Object value, int defaultValue) {
        if (value == null) return defaultValue;
        if (value instanceof Number) return ((Number) value).intValue();
        try {
            return Integer.parseInt(value.toString());
        } catch (NumberFormatException e) {
            return defaultValue;
        }
    }

    /**
     * 内存缓存条目
     */
    private static class CacheEntry {
        private final String value;
        private final long expireTime;

        CacheEntry(String value, int ttlSeconds) {
            this.value = value;
            this.expireTime = System.currentTimeMillis() + ttlSeconds * 1000L;
        }

        String getValue() {
            return value;
        }

        boolean isExpired() {
            return System.currentTimeMillis() > expireTime;
        }
    }
}
