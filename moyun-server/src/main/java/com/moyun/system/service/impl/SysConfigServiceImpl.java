package com.moyun.system.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.moyun.core.config.redis.RedisCache;
import com.moyun.system.domain.entity.SysConfig;
import com.moyun.system.mapper.SysConfigMapper;
import com.moyun.system.service.ISysConfigService;
import com.moyun.util.string.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Collection;
import java.util.List;

/**
 * 参数配置 服务层实现
 * 缓存策略：
 * - key 格式：sys:config:{configKey}
 * - loadingConfigCache：全量加载 sys_config 到 Redis
 * - selectConfigByKey：先查缓存，未命中回源 DB 并回填
 * - 增删改后调用 clearConfigCache 或 resetConfigCache 刷新
 *
 * @author ruoyi
 */
@Service
public class SysConfigServiceImpl extends ServiceImpl<SysConfigMapper, SysConfig> implements ISysConfigService {

    /** 参数配置 Redis 缓存前缀 */
    private static final String CONFIG_CACHE_KEY_PREFIX = "sys:config:";

    @Autowired
    private RedisCache redisCache;

    /**
     * 查询参数配置信息
     *
     * @param configId 参数配置ID
     * @return 参数配置信息
     */
    @Override
    public SysConfig selectConfigById(Long configId) {
        return baseMapper.selectById(configId);
    }

    /**
     * 根据键名查询参数配置信息（先查缓存，未命中回源并回填）
     *
     * @param configKey 参数键名
     * @return 参数键值
     */
    @Override
    public String selectConfigByKey(String configKey) {
        if (StringUtils.isEmpty(configKey)) {
            return null;
        }
        // 1. 查缓存
        String cacheKey = CONFIG_CACHE_KEY_PREFIX + configKey;
        String cachedValue = redisCache.getCacheObject(cacheKey);
        if (cachedValue != null) {
            return cachedValue;
        }
        // 2. 回源 DB
        LambdaQueryWrapper<SysConfig> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(SysConfig::getConfigKey, configKey);
        SysConfig config = baseMapper.selectOne(queryWrapper);
        if (config != null) {
            redisCache.setCacheObject(cacheKey, config.getConfigValue());
            return config.getConfigValue();
        }
        return null;
    }

    /**
     * 获取验证码开关
     *
     * @return true开启，false关闭
     */
    @Override
    public boolean selectCaptchaEnabled() {
        String captchaEnabled = selectConfigByKey("sys.account.captchaEnabled");
        return StringUtils.isNotEmpty(captchaEnabled) && "true".equalsIgnoreCase(captchaEnabled);
    }

    /**
     * 查询参数配置列表
     *
     * @param config 参数配置信息
     * @return 参数配置集合
     */
    @Override
    public List<SysConfig> selectConfigList(SysConfig config) {
        LambdaQueryWrapper<SysConfig> queryWrapper = new LambdaQueryWrapper<>();
        if (StringUtils.isNotEmpty(config.getConfigName())) {
            queryWrapper.like(SysConfig::getConfigName, config.getConfigName());
        }
        if (StringUtils.isNotEmpty(config.getConfigKey())) {
            queryWrapper.like(SysConfig::getConfigKey, config.getConfigKey());
        }
        if (StringUtils.isNotEmpty(config.getConfigType())) {
            queryWrapper.eq(SysConfig::getConfigType, config.getConfigType());
        }
        return baseMapper.selectList(queryWrapper);
    }

    /**
     * 新增参数配置
     *
     * @param config 参数配置信息
     * @return 结果
     */
    @Override
    public int insertConfig(SysConfig config) {
        return baseMapper.insert(config);
    }

    /**
     * 修改参数配置
     *
     * @param config 参数配置信息
     * @return 结果
     */
    @Override
    public int updateConfig(SysConfig config) {
        return baseMapper.updateById(config);
    }

    /**
     * 批量删除参数信息
     *
     * @param configIds 需要删除的参数ID
     */
    @Override
    public void deleteConfigByIds(Long[] configIds) {
        for (Long configId : configIds) {
            baseMapper.deleteById(configId);
        }
    }

    /**
     * 加载参数缓存数据：全量从 DB 加载到 Redis
     */
    @Override
    public void loadingConfigCache() {
        List<SysConfig> configs = baseMapper.selectList(new LambdaQueryWrapper<>());
        for (SysConfig config : configs) {
            if (StringUtils.isNotEmpty(config.getConfigKey())) {
                redisCache.setCacheObject(
                        CONFIG_CACHE_KEY_PREFIX + config.getConfigKey(),
                        config.getConfigValue());
            }
        }
    }

    /**
     * 清空参数缓存数据：删除所有 sys:config:* 键
     */
    @Override
    public void clearConfigCache() {
        Collection<String> keys = redisCache.keys(CONFIG_CACHE_KEY_PREFIX + "*");
        if (keys != null && !keys.isEmpty()) {
            redisCache.deleteObject(keys);
        }
    }

    /**
     * 重置参数缓存数据
     */
    @Override
    public void resetConfigCache() {
        clearConfigCache();
        loadingConfigCache();
    }

    /**
     * 校验参数键名是否唯一
     *
     * @param config 参数信息
     * @return 结果
     */
    @Override
    public boolean checkConfigKeyUnique(SysConfig config) {
        Long configId = config.getConfigId() == null ? -1L : config.getConfigId();
        LambdaQueryWrapper<SysConfig> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(SysConfig::getConfigKey, config.getConfigKey());
        SysConfig info = baseMapper.selectOne(queryWrapper);
        return info == null || info.getConfigId().equals(configId);
    }
}
