package com.moyun.community.growth.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.moyun.community.growth.entity.CmsLevelConfig;

public interface ICmsLevelConfigService extends IService<CmsLevelConfig> {

    CmsLevelConfig getLevelByPoints(Long points);

    CmsLevelConfig getFirstLevel();

    CmsLevelConfig getNextLevel(Integer currentLevel);
}
