package com.moyun.community.growth.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.moyun.community.growth.entity.CmsPointsLog;

public interface ICmsPointsLogService extends IService<CmsPointsLog> {

    Long getTotalPoints(Long userId);
}
