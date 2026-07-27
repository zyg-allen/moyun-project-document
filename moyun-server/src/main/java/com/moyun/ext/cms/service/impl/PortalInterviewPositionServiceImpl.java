package com.moyun.ext.cms.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.moyun.ext.cms.service.IPortalInterviewPositionService;
import com.moyun.portal.domain.entity.PortalInterviewPosition;
import com.moyun.portal.mapper.PortalInterviewPositionMapper;
import com.moyun.util.string.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.List;

/**
 * 面试岗位字典 Service 实现
 *
 * @author moyun
 */
@Service
public class PortalInterviewPositionServiceImpl implements IPortalInterviewPositionService {

    @Autowired
    private PortalInterviewPositionMapper positionMapper;

    @Override
    public List<PortalInterviewPosition> listActivePositions() {
        LambdaQueryWrapper<PortalInterviewPosition> qw = Wrappers.<PortalInterviewPosition>lambdaQuery()
                .eq(PortalInterviewPosition::getStatus, "active")
                .orderByAsc(PortalInterviewPosition::getSort)
                .orderByAsc(PortalInterviewPosition::getId);
        List<PortalInterviewPosition> list = positionMapper.selectList(qw);
        return list == null ? Collections.emptyList() : list;
    }

    @Override
    public PortalInterviewPosition findByName(String name) {
        if (StringUtils.isEmpty(name)) {
            return null;
        }
        String trimmed = name.trim();
        // 1. 精确匹配（如 "Java后端工程师"）
        LambdaQueryWrapper<PortalInterviewPosition> exact = Wrappers.<PortalInterviewPosition>lambdaQuery()
                .eq(PortalInterviewPosition::getName, trimmed)
                .eq(PortalInterviewPosition::getStatus, "active")
                .last("LIMIT 1");
        PortalInterviewPosition hit = positionMapper.selectOne(exact);
        if (hit != null) {
            return hit;
        }
        // 2. 模糊兜底（如 "后端" → "Java后端工程师"）：name LIKE %trimmed%
        //    用于用户档案自由文本输入或前端历史硬编码值，提升画像必备技能召回命中率
        LambdaQueryWrapper<PortalInterviewPosition> like = Wrappers.<PortalInterviewPosition>lambdaQuery()
                .like(PortalInterviewPosition::getName, trimmed)
                .eq(PortalInterviewPosition::getStatus, "active")
                .orderByAsc(PortalInterviewPosition::getSort)
                .last("LIMIT 1");
        return positionMapper.selectOne(like);
    }

    @Override
    public PortalInterviewPosition findByCode(String code) {
        if (StringUtils.isEmpty(code)) {
            return null;
        }
        LambdaQueryWrapper<PortalInterviewPosition> qw = Wrappers.<PortalInterviewPosition>lambdaQuery()
                .eq(PortalInterviewPosition::getCode, code.trim())
                .eq(PortalInterviewPosition::getStatus, "active")
                .last("LIMIT 1");
        return positionMapper.selectOne(qw);
    }
}
