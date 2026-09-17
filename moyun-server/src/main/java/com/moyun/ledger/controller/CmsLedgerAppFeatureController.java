package com.moyun.ledger.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.moyun.core.base.AjaxResult;
import com.moyun.core.base.BaseController;
import com.moyun.util.security.SecurityUtils;
import com.moyun.ledger.domain.entity.LedgerAppFeatureConfig;
import com.moyun.ledger.mapper.LedgerAppFeatureConfigMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 记账小程序功能入口配置（管理端）
 *
 * <p>"我的"页功能宫格可视化运营：默认开发中功能隐藏（visible=0），
 * 管理员可调整展示/隐藏、名称、图标、排序、角标，ledger-app 启动时拉取生效。
 *
 * @author moyun
 */
@RestController
@RequestMapping("/cms/ledger/app-feature")
public class CmsLedgerAppFeatureController extends BaseController {

    @Autowired
    private LedgerAppFeatureConfigMapper featureMapper;

    /** 功能配置列表（全量，含隐藏项——后台需看到全部以便开启） */
    @PreAuthorize("@ss.hasPermi('cms:ledgerAppFeature:list')")
    @GetMapping("/list")
    public AjaxResult list() {
        List<LedgerAppFeatureConfig> rows = featureMapper.selectList(
                new LambdaQueryWrapper<LedgerAppFeatureConfig>()
                        .orderByAsc(LedgerAppFeatureConfig::getGroupType)
                        .orderByAsc(LedgerAppFeatureConfig::getSortNum));
        return AjaxResult.success(rows);
    }

    /**
     * 修改功能配置（名称/图标/颜色/排序/可见/角标/跳转链接——均为运营字段）
     * <p>remark：链接型入口（feature_key=portal 等）的跳转 URL 承载位
     */
    @PreAuthorize("@ss.hasPermi('cms:ledgerAppFeature:edit')")
    @PutMapping("/{id}")
    public AjaxResult edit(@PathVariable Long id, @RequestBody LedgerAppFeatureConfig body) {
        LedgerAppFeatureConfig db = featureMapper.selectById(id);
        if (db == null) {
            return AjaxResult.error("功能配置不存在");
        }
        LambdaUpdateWrapper<LedgerAppFeatureConfig> uw = new LambdaUpdateWrapper<>();
        uw.eq(LedgerAppFeatureConfig::getId, id)
                .set(body.getFeatureName() != null, LedgerAppFeatureConfig::getFeatureName, body.getFeatureName())
                .set(body.getIcon() != null, LedgerAppFeatureConfig::getIcon, body.getIcon())
                .set(body.getIconColor() != null, LedgerAppFeatureConfig::getIconColor, body.getIconColor())
                .set(body.getSortNum() != null, LedgerAppFeatureConfig::getSortNum, body.getSortNum())
                .set(body.getVisible() != null, LedgerAppFeatureConfig::getVisible, body.getVisible())
                .set(body.getBadge() != null, LedgerAppFeatureConfig::getBadge, body.getBadge())
                .set(body.getRemark() != null, LedgerAppFeatureConfig::getRemark, body.getRemark())
                .set(LedgerAppFeatureConfig::getUpdateBy, SecurityUtils.getUsername())
                .set(LedgerAppFeatureConfig::getUpdateTime, LocalDateTime.now());
        return toAjax(featureMapper.update(null, uw));
    }
}
