package com.moyun.community.growth.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.moyun.community.growth.entity.CmsLevelConfig;
import com.moyun.community.growth.mapper.CmsLevelConfigMapper;
import com.moyun.community.growth.service.ICmsLevelConfigService;
import org.springframework.stereotype.Service;

@Service
public class CmsLevelConfigServiceImpl extends ServiceImpl<CmsLevelConfigMapper, CmsLevelConfig> implements ICmsLevelConfigService {

    @Override
    public CmsLevelConfig getLevelByPoints(Long points) {
        LambdaQueryWrapper<CmsLevelConfig> wrapper = new LambdaQueryWrapper<>();
        wrapper.le(CmsLevelConfig::getMinPoints, points)
               .ge(CmsLevelConfig::getMaxPoints, points)
               .eq(CmsLevelConfig::getStatus, "normal")
               .orderByDesc(CmsLevelConfig::getLevel)
               .last("LIMIT 1");
        return getOne(wrapper);
    }

    @Override
    public CmsLevelConfig getFirstLevel() {
        LambdaQueryWrapper<CmsLevelConfig> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(CmsLevelConfig::getStatus, "normal")
               .orderByAsc(CmsLevelConfig::getLevel)
               .last("LIMIT 1");
        return getOne(wrapper);
    }

    @Override
    public CmsLevelConfig getNextLevel(Integer currentLevel) {
        LambdaQueryWrapper<CmsLevelConfig> wrapper = new LambdaQueryWrapper<>();
        wrapper.gt(CmsLevelConfig::getLevel, currentLevel)
               .eq(CmsLevelConfig::getStatus, "normal")
               .orderByAsc(CmsLevelConfig::getLevel)
               .last("LIMIT 1");
        return getOne(wrapper);
    }
}
