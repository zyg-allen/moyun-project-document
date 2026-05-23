package com.moyun.community.growth.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.moyun.community.growth.entity.CmsPointsLog;
import com.moyun.community.growth.mapper.CmsPointsLogMapper;
import com.moyun.community.growth.service.ICmsPointsLogService;
import org.springframework.stereotype.Service;

@Service
public class CmsPointsLogServiceImpl extends ServiceImpl<CmsPointsLogMapper, CmsPointsLog> implements ICmsPointsLogService {

    @Override
    public Long getTotalPoints(Long userId) {
        LambdaQueryWrapper<CmsPointsLog> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(CmsPointsLog::getUserId, userId);
        wrapper.eq(CmsPointsLog::getStatus, "completed");
        Long total = baseMapper.selectSumPoints(userId);
        return total != null ? total : 0L;
    }
}
