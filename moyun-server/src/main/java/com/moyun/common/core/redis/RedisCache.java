package com.moyun.common.core.redis;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.concurrent.TimeUnit;

/**
 * Redis缓存工具类
 *
 * @author ruoyi
 */
@Component
public class RedisCache {

    @Autowired
    private RedisTemplate<String, Object> redisTemplate;

    /**
     * 缓存基本对象
     *
     * @param key   缓存键值
     * @param value 缓存值
     */
    public <T> void setCacheObject(final String key, final T value) {
        redisTemplate.opsForValue().set(key, value);
    }

    /**
     * 缓存基本对象
     *
     * @param key      缓存键值
     * @param value    缓存值
     * @param timeout  时间
     * @param timeUnit 时间颗粒度
     */
    public <T> void setCacheObject(final String key, final T value, final Integer timeout, final TimeUnit timeUnit) {
        redisTemplate.opsForValue().set(key, value, timeout, timeUnit);
    }

    /**
     * 设置有效时间
     *
     * @param key     Redis键
     * @param timeout 超时时间
     * @return true=设置成功；false=设置失败
     */
    public boolean expire(final String key, final long timeout) {
        return expire(key, timeout, TimeUnit.SECONDS);
    }

    /**
     * 设置有效时间
     *
     * @param key     Redis键
     * @param timeout 超时时间
     * @param unit    时间单位
     * @return true=设置成功；false=设置失败
     */
    public boolean expire(final String key, final long timeout, final TimeUnit unit) {
        return Boolean.TRUE.equals(redisTemplate.expire(key, timeout, unit));
    }

    /**
     * 获得缓存的基本对象
     *
     * @param key 缓存键值
     * @return 缓存键值对应的数据
     */
    public <T> T getCacheObject(final String key) {
        @SuppressWarnings("unchecked")
        T value = (T) redisTemplate.opsForValue().get(key);
        return value;
    }

    /**
     * 删除单个对象
     *
     * @param key 缓存键值
     */
    public boolean deleteObject(final String key) {
        return Boolean.TRUE.equals(redisTemplate.delete(key));
    }

    /**
     * 删除集合对象
     *
     * @param collection 多个对象
     */
    public boolean deleteObject(final Collection<String> collection) {
        return redisTemplate.delete(collection) != null;
    }

    /**
     * 缓存List数据
     *
     * @param key      缓存的键值
     * @param dataList 待缓存的List数据
     * @return 缓存的对象
     */
    public <T> long setCacheList(final String key, final List<T> dataList) {
        if (dataList == null || dataList.isEmpty()) {
            return 0;
        }
        @SuppressWarnings("unchecked")
        Long count = redisTemplate.opsForList().rightPushAll(key, dataList.toArray());
        return count == null ? 0 : count;
    }

    /**
     * 获得缓存的list对象
     *
     * @param key 缓存的键值
     * @return 缓存键值对应的数据
     */
    @SuppressWarnings("unchecked")
    public <T> List<T> getCacheList(final String key) {
        return (List<T>) Objects.requireNonNullElse(redisTemplate.opsForList().range(key, 0, -1), new ArrayList<>());
    }

    /**
     * 缓存Set
     *
     * @param key     缓存键值
     * @param dataSet 缓存的数据
     * @return 缓存数据的对象
     */
    public <T> long setCacheSet(final String key, final Set<T> dataSet) {
        if (dataSet == null || dataSet.isEmpty()) {
            return 0;
        }
        @SuppressWarnings("unchecked")
        Long count = redisTemplate.opsForSet().add(key, dataSet.toArray());
        return count == null ? 0 : count;
    }

    /**
     * 获得缓存的set
     *
     * @param key 缓存键值
     */
    @SuppressWarnings("unchecked")
    public <T> Set<T> getCacheSet(final String key) {
        return (Set<T>) Objects.requireNonNullElse(redisTemplate.opsForSet().members(key), new HashSet<>());
    }

    /**
     * 缓存Map
     *
     * @param key     缓存键值
     * @param dataMap 缓存的数据
     */
    public <T> void setCacheMap(final String key, final Map<String, T> dataMap) {
        if (dataMap != null) {
            redisTemplate.opsForHash().putAll(key, dataMap);
        }
    }

    /**
     * 获得缓存的Map
     *
     * @param key 缓存键值
     */
    @SuppressWarnings("unchecked")
    public <T> Map<String, T> getCacheMap(final String key) {
        Map<Object, Object> entries = redisTemplate.opsForHash().entries(key);
        Map<String, T> result = new HashMap<>();
        for (Map.Entry<Object, Object> entry : entries.entrySet()) {
            result.put((String) entry.getKey(), (T) entry.getValue());
        }
        return result;
    }

    /**
     * 往Hash中存入数据
     *
     * @param key   Redis键
     * @param hKey  Hash键
     * @param value 值
     */
    public <T> void setCacheMapValue(final String key, final String hKey, final T value) {
        redisTemplate.opsForHash().put(key, hKey, value);
    }

    /**
     * 获取Hash中的数据
     *
     * @param key  Redis键
     * @param hKey Hash键
     * @return Hash中的对象
     */
    public <T> T getCacheMapValue(final String key, final String hKey) {
        @SuppressWarnings("unchecked")
        T value = (T) redisTemplate.opsForHash().get(key, hKey);
        return value;
    }

    /**
     * 获取多个Hash中的数据
     *
     * @param key   Redis键
     * @param hKeys Hash键集合
     * @return Hash对象集合
     */
    public <T> List<T> getMultiCacheMapValue(final String key, final Collection<String> hKeys) {
        @SuppressWarnings("unchecked")
        List<T> result = (List<T>) redisTemplate.opsForHash().multiGet(key, new ArrayList<>(hKeys));
        return Objects.requireNonNullElse(result, new ArrayList<>());
    }

    /**
     * 删除Hash中的数据
     *
     * @param key  Redis键
     * @param hKey Hash键
     */
    public void delCacheMapValue(final String key, final String hKey) {
        redisTemplate.opsForHash().delete(key, hKey);
    }

    /**
     * 获得缓存的基本对象列表
     * <p>
     * 注意：此方法使用Redis KEYS命令，在生产环境大数据量情况下可能导致Redis阻塞
     * 建议使用scan方法替代，或确保仅在开发/测试环境使用
     *
     * @param pattern 字符串前缀
     * @return 对象列表
     */
    public Collection<String> keys(final String pattern) {
        return redisTemplate.keys(pattern);
    }

    /**
     * 判断key是否存在
     *
     * @param key 缓存键值
     * @return 是否存在
     */
    public boolean hasKey(String key) {
        return Boolean.TRUE.equals(redisTemplate.hasKey(key));
    }
}
