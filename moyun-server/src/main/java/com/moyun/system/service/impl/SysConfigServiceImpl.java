package com.moyun.system.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.moyun.core.config.redis.RedisCache;
import com.moyun.system.domain.entity.SysConfig;
import com.moyun.system.domain.entity.SysConfigLog;
import com.moyun.system.mapper.SysConfigLogMapper;
import com.moyun.system.mapper.SysConfigMapper;
import com.moyun.system.service.ISysConfigService;
import com.moyun.util.ip.IpUtils;
import com.moyun.util.security.SecurityUtils;
import com.moyun.util.string.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.TimeUnit;

/**
 * 参数配置 服务层实现
 * 缓存策略：
 * - key 格式：sys:config:{configKey}
 * - loadingConfigCache：全量加载 sys_config 到 Redis
 * - selectConfigByKey：先查缓存，未命中回源 DB 并回填
 * - 增删改后调用 clearConfigCache 或 resetConfigCache 刷新
 * - 所有缓存写入统一 TTL（30 分钟），防止绕过管理页直接改 DB 导致缓存与 DB 长期脱节
 *
 * @author allen-zyg
 */
@Service
public class SysConfigServiceImpl extends ServiceImpl<SysConfigMapper, SysConfig> implements ISysConfigService {

    /** 参数配置 Redis 缓存前缀 */
    private static final String CONFIG_CACHE_KEY_PREFIX = "sys:config:";

    /** 参数配置缓存 TTL（分钟）：过期后回源 DB，直接改库最多 30 分钟内生效 */
    private static final int CONFIG_CACHE_TTL_MINUTES = 30;

    @Autowired
    private RedisCache redisCache;

    @Autowired
    private SysConfigLogMapper configLogMapper;

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
            redisCache.setCacheObject(cacheKey, config.getConfigValue(), CONFIG_CACHE_TTL_MINUTES, TimeUnit.MINUTES);
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
     * 分页查询参数配置（MyBatis-Plus 标准分页）
     */
    @Override
    public IPage<SysConfig> selectConfigPage(IPage<SysConfig> page, SysConfig config) {
        return baseMapper.selectConfigPage(page, config);
    }

    /**
     * 新增参数配置
     *
     * @param config 参数配置信息
     * @return 结果
     */
    @Override
    public int insertConfig(SysConfig config) {
        int rows = baseMapper.insert(config);
        if (rows > 0 && StringUtils.isNotEmpty(config.getConfigKey())) {
            // 修复：原实现未清缓存，导致新增配置后 selectConfigByKey 仍读不到最新值
            redisCache.setCacheObject(CONFIG_CACHE_KEY_PREFIX + config.getConfigKey(), config.getConfigValue(), CONFIG_CACHE_TTL_MINUTES, TimeUnit.MINUTES);
        }
        return rows;
    }

    /**
     * 修改参数配置（同事务记录变更审计日志 sys_config_log）
     *
     * @param config 参数配置信息
     * @return 结果
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public int updateConfig(SysConfig config) {
        // 变更前快照（审计前后值用）
        SysConfig before = config.getConfigId() == null ? null : baseMapper.selectById(config.getConfigId());
        int rows = baseMapper.updateById(config);
        if (rows > 0) {
            // 修复：原实现只更新 DB 不清缓存，selectConfigByKey 仍返回旧值。
            // 此处针对单 key 删除（比 clearConfigCache 全清更精细，不影响其他配置缓存），
            // 下次读取时回源 DB 并回填新值。若 update 只改了部分字段未带 configKey，
            // 由调用方（Controller）保证触发 refreshCache。
            SysConfig fresh = baseMapper.selectById(config.getConfigId());
            if (config.getConfigKey() != null && fresh != null && StringUtils.isNotEmpty(fresh.getConfigKey())) {
                redisCache.setCacheObject(CONFIG_CACHE_KEY_PREFIX + fresh.getConfigKey(), fresh.getConfigValue(), CONFIG_CACHE_TTL_MINUTES, TimeUnit.MINUTES);
            }
            // 变更审计：与更新同一事务，留痕失败整笔回滚
            insertChangeLog(before, fresh);
        }
        return rows;
    }

    /**
     * 变更审计留痕：键值实际变化时写入 sys_config_log（操作人/时间/前后值）
     */
    private void insertChangeLog(SysConfig before, SysConfig after) {
        if (after == null) {
            return;
        }
        String oldValue = before == null ? null : before.getConfigValue();
        if (Objects.equals(oldValue, after.getConfigValue())) {
            return;
        }
        SysConfigLog configLog = new SysConfigLog();
        configLog.setConfigId(after.getConfigId());
        configLog.setConfigKey(after.getConfigKey());
        configLog.setOldValue(oldValue);
        configLog.setNewValue(after.getConfigValue());
        configLog.setOperateType("UPDATE");
        configLog.setOperName(resolveOperName(after));
        configLog.setOperIp(IpUtils.getIpAddr());
        configLog.setCreateTime(LocalDateTime.now());
        configLogMapper.insert(configLog);
    }

    /**
     * 解析操作人：优先当前登录管理员，无登录上下文（内部调用）回退 updateBy
     */
    private String resolveOperName(SysConfig after) {
        try {
            return SecurityUtils.getUsername();
        } catch (Exception ignored) {
            // 无登录上下文
        }
        return StringUtils.isNotEmpty(after.getUpdateBy()) ? after.getUpdateBy() : "system";
    }

    /**
     * 批量删除参数信息
     *
     * @param configIds 需要删除的参数ID
     */
    @Override
    public void deleteConfigByIds(Long[] configIds) {
        for (Long configId : configIds) {
            SysConfig config = baseMapper.selectById(configId);
            if (config != null && StringUtils.isNotEmpty(config.getConfigKey())) {
                // 修复：删除前先记录 key，删除后清对应缓存
                redisCache.deleteObject(CONFIG_CACHE_KEY_PREFIX + config.getConfigKey());
            }
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
                        config.getConfigValue(),
                        CONFIG_CACHE_TTL_MINUTES, TimeUnit.MINUTES);
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
