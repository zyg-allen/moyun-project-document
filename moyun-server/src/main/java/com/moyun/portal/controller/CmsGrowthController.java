package com.moyun.portal.controller;

import java.util.Arrays;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import com.moyun.common.annotation.Log;
import com.moyun.common.enums.BusinessType;
import com.moyun.core.base.AjaxResult;
import com.moyun.core.base.BaseController;
import com.moyun.portal.domain.entity.PortalAchievement;
import com.moyun.portal.domain.entity.PortalGrowthRule;
import com.moyun.portal.mapper.PortalAchievementMapper;
import com.moyun.portal.mapper.PortalGrowthRuleMapper;

/**
 * CMS成长配置管理Controller
 *
 * 提供成长规则、成就定义的后台管理能力。
 * 直接复用现有 Mapper，不引入额外 Service 层。
 *
 * @author moyun
 */
@Tag(name = "CMS成长配置管理", description = "成长规则与成就定义后台管理接口")
@RestController
@RequestMapping("/cms/growth")
public class CmsGrowthController extends BaseController {

    @Autowired
    private PortalGrowthRuleMapper portalGrowthRuleMapper;

    @Autowired
    private PortalAchievementMapper portalAchievementMapper;

    // ========================================================================
    // 成长规则管理
    // ========================================================================

    @Operation(summary = "获取成长规则分页列表", description = "分页查询成长规则，支持按模块和状态筛选")
    @PreAuthorize("@ss.hasPermi('cms:growth:list')")
    @GetMapping("/rule/list")
    public AjaxResult listRule(PortalGrowthRule query,
                               @RequestParam(defaultValue = "1") Integer pageNum,
                               @RequestParam(defaultValue = "10") Integer pageSize) {
        Page<PortalGrowthRule> page = new Page<>(pageNum, pageSize);
        LambdaQueryWrapper<PortalGrowthRule> wrapper = new LambdaQueryWrapper<>();
        if (query.getModule() != null && !query.getModule().isEmpty()) {
            wrapper.eq(PortalGrowthRule::getModule, query.getModule());
        }
        if (query.getStatus() != null && !query.getStatus().isEmpty()) {
            wrapper.eq(PortalGrowthRule::getStatus, query.getStatus());
        }
        wrapper.orderByAsc(PortalGrowthRule::getSort);
        page = portalGrowthRuleMapper.selectPage(page, wrapper);
        return success(page);
    }

    @Operation(summary = "获取成长规则详情", description = "根据规则ID获取详细信息")
    @PreAuthorize("@ss.hasPermi('cms:growth:query')")
    @GetMapping("/rule/{id}")
    public AjaxResult getRule(@Parameter(description = "规则ID") @PathVariable Long id) {
        return success(portalGrowthRuleMapper.selectById(id));
    }

    @Operation(summary = "新增成长规则", description = "创建新的成长规则")
    @PreAuthorize("@ss.hasPermi('cms:growth:add')")
    @Log(title = "成长规则", businessType = BusinessType.INSERT)
    @PostMapping("/rule")
    public AjaxResult addRule(@RequestBody PortalGrowthRule rule) {
        return toAjax(portalGrowthRuleMapper.insert(rule));
    }

    @Operation(summary = "修改成长规则", description = "更新成长规则信息")
    @PreAuthorize("@ss.hasPermi('cms:growth:edit')")
    @Log(title = "成长规则", businessType = BusinessType.UPDATE)
    @PutMapping("/rule")
    public AjaxResult editRule(@RequestBody PortalGrowthRule rule) {
        return toAjax(portalGrowthRuleMapper.updateById(rule));
    }

    @Operation(summary = "删除成长规则", description = "批量删除成长规则")
    @PreAuthorize("@ss.hasPermi('cms:growth:remove')")
    @Log(title = "成长规则", businessType = BusinessType.DELETE)
    @DeleteMapping("/rule/{ids}")
    public AjaxResult removeRule(@Parameter(description = "规则ID数组") @PathVariable Long[] ids) {
        return toAjax(portalGrowthRuleMapper.deleteBatchIds(Arrays.asList(ids)));
    }

    // ========================================================================
    // 成就定义管理
    // ========================================================================

    @Operation(summary = "获取成就分页列表", description = "分页查询成就定义，支持按模块和状态筛选")
    @PreAuthorize("@ss.hasPermi('cms:growth:list')")
    @GetMapping("/achievement/list")
    public AjaxResult listAchievement(PortalAchievement query,
                                      @RequestParam(defaultValue = "1") Integer pageNum,
                                      @RequestParam(defaultValue = "10") Integer pageSize) {
        Page<PortalAchievement> page = new Page<>(pageNum, pageSize);
        LambdaQueryWrapper<PortalAchievement> wrapper = new LambdaQueryWrapper<>();
        if (query.getModule() != null && !query.getModule().isEmpty()) {
            wrapper.eq(PortalAchievement::getModule, query.getModule());
        }
        if (query.getStatus() != null && !query.getStatus().isEmpty()) {
            wrapper.eq(PortalAchievement::getStatus, query.getStatus());
        }
        wrapper.orderByAsc(PortalAchievement::getSort);
        page = portalAchievementMapper.selectPage(page, wrapper);
        return success(page);
    }

    @Operation(summary = "获取成就详情", description = "根据成就ID获取详细信息")
    @PreAuthorize("@ss.hasPermi('cms:growth:query')")
    @GetMapping("/achievement/{id}")
    public AjaxResult getAchievement(@Parameter(description = "成就ID") @PathVariable Long id) {
        return success(portalAchievementMapper.selectById(id));
    }

    @Operation(summary = "新增成就", description = "创建新的成就定义")
    @PreAuthorize("@ss.hasPermi('cms:growth:add')")
    @Log(title = "成就定义", businessType = BusinessType.INSERT)
    @PostMapping("/achievement")
    public AjaxResult addAchievement(@RequestBody PortalAchievement achievement) {
        return toAjax(portalAchievementMapper.insert(achievement));
    }

    @Operation(summary = "修改成就", description = "更新成就定义信息")
    @PreAuthorize("@ss.hasPermi('cms:growth:edit')")
    @Log(title = "成就定义", businessType = BusinessType.UPDATE)
    @PutMapping("/achievement")
    public AjaxResult editAchievement(@RequestBody PortalAchievement achievement) {
        return toAjax(portalAchievementMapper.updateById(achievement));
    }

    @Operation(summary = "删除成就", description = "批量删除成就定义")
    @PreAuthorize("@ss.hasPermi('cms:growth:remove')")
    @Log(title = "成就定义", businessType = BusinessType.DELETE)
    @DeleteMapping("/achievement/{ids}")
    public AjaxResult removeAchievement(@Parameter(description = "成就ID数组") @PathVariable Long[] ids) {
        return toAjax(portalAchievementMapper.deleteBatchIds(Arrays.asList(ids)));
    }
}
