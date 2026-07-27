package com.moyun.agent.util;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

/**
 * 缓存工具类
 * 
 * <p>提供统一的缓存操作接口，封装Redis缓存的常用模式：</p>
 * <ul>
 *     <li>查询缓存，未命中则从数据源加载并缓存</li>
 *     <li>更新缓存</li>
 *     <li>删除缓存</li>
 * </ul>
 * 
 * <p>线程安全：基于StringRedisTemplate，线程安全</p>
 * 
 * @author laomao
 * @since 2025-11-30
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class CacheUtils {
    
    private final StringRedisTemplate redisTemplate;
    
    /**
     * 获取缓存数据，缓存未命中时从数据源加载
     * 
     * <p>Cache-Aside模式：</p>
     * <ol>
     *     <li>先查询缓存</li>
     *     <li>缓存命中则直接返回</li>
     *     <li>缓存未命中则调用dataLoader从数据源加载</li>
     *     <li>将加载的数据写入缓存</li>
     * </ol>
     * 
     * @param key 缓存键
     * @param clazz 目标类型
     * @param dataLoader 数据加载器（缓存未命中时调用）
     * @param expireMinutes 过期时间（分钟）
     * @param <T> 泛型类型
     * @return 数据对象，如果加载失败返回null
     */
    public <T> T getOrLoad(String key, Class<T> clazz, Supplier<T> dataLoader, long expireMinutes) {
        // 1. 尝试从缓存获取
        String cached = redisTemplate.opsForValue().get(key);
        if (StringUtils.hasText(cached)) {
            T data = JsonUtils.fromJson(cached, clazz);
            if (data != null) {
                log.debug("✅ 缓存命中: key={}", key);
                return data;
            }
        }
        
        // 2. 缓存未命中，从数据源加载
        log.debug("🔍 缓存未命中，加载数据: key={}", key);
        T data = dataLoader.get();
        
        // 3. 写入缓存
        if (data != null) {
            set(key, data, expireMinutes);
        }
        
        return data;
    }
    
    /**
     * 设置缓存
     * 
     * @param key 缓存键
     * @param value 值对象
     * @param expireMinutes 过期时间（分钟）
     * @param <T> 泛型类型
     */
    public <T> void set(String key, T value, long expireMinutes) {
        String json = JsonUtils.toJson(value);
        if (json != null) {
            try {
                redisTemplate.opsForValue().set(key, json, expireMinutes, TimeUnit.MINUTES);
                log.debug("💾 数据已缓存: key={}, ttl={}分钟", key, expireMinutes);
            } catch (Exception e) {
                log.warn("⚠️  缓存数据失败: key={}, error={}", key, e.getMessage());
            }
        }
    }
    
    /**
     * 删除缓存
     * 
     * @param key 缓存键
     */
    public void delete(String key) {
        try {
            Boolean deleted = redisTemplate.delete(key);
            log.debug("🧹 删除缓存: key={}, deleted={}", key, deleted);
        } catch (Exception e) {
            log.warn("⚠️  删除缓存失败: key={}, error={}", key, e.getMessage());
        }
    }
    
    /**
     * 批量删除缓存
     * 
     * @param keys 缓存键列表
     */
    public void deleteAll(String... keys) {
        if (keys == null || keys.length == 0) {
            return;
        }
        
        try {
            Long deleted = redisTemplate.delete(java.util.Arrays.asList(keys));
            log.debug("🧹 批量删除缓存: count={}", deleted);
        } catch (Exception e) {
            log.warn("⚠️  批量删除缓存失败: error={}", e.getMessage());
        }
    }
    
    /**
     * 检查缓存是否存在
     * 
     * @param key 缓存键
     * @return true表示存在，false表示不存在
     */
    public boolean exists(String key) {
        try {
            Boolean exists = redisTemplate.hasKey(key);
            return Boolean.TRUE.equals(exists);
        } catch (Exception e) {
            log.warn("⚠️  检查缓存失败: key={}, error={}", key, e.getMessage());
            return false;
        }
    }
    
    /**
     * 设置缓存过期时间
     * 
     * @param key 缓存键
     * @param expireMinutes 过期时间（分钟）
     * @return true表示成功，false表示失败
     */
    public boolean expire(String key, long expireMinutes) {
        try {
            Boolean result = redisTemplate.expire(key, expireMinutes, TimeUnit.MINUTES);
            return Boolean.TRUE.equals(result);
        } catch (Exception e) {
            log.warn("⚠️  设置过期时间失败: key={}, error={}", key, e.getMessage());
            return false;
        }
    }
}
