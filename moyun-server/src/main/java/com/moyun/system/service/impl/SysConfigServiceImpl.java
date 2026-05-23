package com.moyun.system.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.moyun.common.utils.StringUtils;
import com.moyun.system.domain.SysConfig;
import com.moyun.system.mapper.SysConfigMapper;
import com.moyun.system.service.ISysConfigService;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 参数配置 服务层实现
 *
 * @author ruoyi
 */
@Service
public class SysConfigServiceImpl extends ServiceImpl<SysConfigMapper, SysConfig> implements ISysConfigService {

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
     * 根据键名查询参数配置信息
     *
     * @param configKey 参数键名
     * @return 参数键值
     */
    @Override
    public String selectConfigByKey(String configKey) {
        if (StringUtils.isEmpty(configKey)) {
            return null;
        }
        LambdaQueryWrapper<SysConfig> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(SysConfig::getConfigKey, configKey);
        SysConfig config = baseMapper.selectOne(queryWrapper);
        return config != null ? config.getConfigValue() : null;
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
     * 加载参数缓存数据
     */
    @Override
    public void loadingConfigCache() {
        // TODO: 实现缓存加载逻辑
    }

    /**
     * 清空参数缓存数据
     */
    @Override
    public void clearConfigCache() {
        // TODO: 实现缓存清空逻辑
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