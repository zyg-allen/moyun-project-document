package com.moyun.ledger.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.moyun.core.base.AjaxResult;
import com.moyun.ledger.domain.entity.LedgerAppFeatureConfig;
import com.moyun.ledger.mapper.LedgerAppFeatureConfigMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 门户记账-小程序功能入口配置
 *
 * <p>"我的"页功能宫格由后台可视化运营：仅返回 visible=1 的入口，
 * 开发中功能默认隐藏；前端拉取失败时回退内置默认清单。
 *
 * @author moyun
 */
@RestController
@RequestMapping("/portal/ledger/app-features")
public class PortalLedgerAppFeatureController {

    @Autowired
    private LedgerAppFeatureConfigMapper featureMapper;

    /** 可见功能入口列表（按分组+排序） */
    @GetMapping
    public AjaxResult list() {
        List<LedgerAppFeatureConfig> rows = featureMapper.selectList(
                new LambdaQueryWrapper<LedgerAppFeatureConfig>()
                        .eq(LedgerAppFeatureConfig::getVisible, 1)
                        .orderByAsc(LedgerAppFeatureConfig::getGroupType)
                        .orderByAsc(LedgerAppFeatureConfig::getSortNum));
        return AjaxResult.success(rows);
    }
}
