package com.moyun.portal.controller;

import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

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
import com.moyun.portal.domain.entity.PortalGrowthLog;
import com.moyun.portal.domain.entity.PortalGrowthRule;
import com.moyun.portal.domain.entity.PortalUser;
import com.moyun.portal.domain.entity.PortalUserBadge;
import com.moyun.portal.domain.entity.PortalUserGrowth;
import com.moyun.portal.domain.entity.PortalUserStats;
import com.moyun.portal.mapper.PortalAchievementMapper;
import com.moyun.portal.mapper.PortalGrowthLogMapper;
import com.moyun.portal.mapper.PortalGrowthRuleMapper;
import com.moyun.portal.mapper.PortalUserBadgeMapper;
import com.moyun.portal.mapper.PortalUserGrowthMapper;
import com.moyun.portal.mapper.PortalUserMapper;
import com.moyun.portal.mapper.PortalUserStatsMapper;

/**
 * CMS成长体系后台管理Controller
 *
 * 提供成长规则、成就定义、用户成长、用户徽章、成长流水的后台管理能力。
 * 直接复用现有 Mapper，不引入额外 Service 层。
 *
 * @author moyun
 */
@Tag(name = "CMS成长体系管理", description = "成长体系后台管理接口")
@RestController
@RequestMapping("/cms/growth")
public class CmsGrowthController extends BaseController {

    @Autowired
    private PortalGrowthRuleMapper portalGrowthRuleMapper;

    @Autowired
    private PortalAchievementMapper portalAchievementMapper;

    @Autowired
    private PortalUserGrowthMapper portalUserGrowthMapper;

    @Autowired
    private PortalUserBadgeMapper portalUserBadgeMapper;

    @Autowired
    private PortalGrowthLogMapper portalGrowthLogMapper;

    @Autowired
    private PortalUserStatsMapper portalUserStatsMapper;

    @Autowired
    private PortalUserMapper portalUserMapper;

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

    // ========================================================================
    // 用户成长查询
    // ========================================================================

    @Operation(summary = "获取用户成长分页列表", description = "分页查询用户成长信息，支持按用户ID筛选，含用户昵称")
    @PreAuthorize("@ss.hasPermi('cms:growth:list')")
    @GetMapping("/user-growth/list")
    public AjaxResult listUserGrowth(@RequestParam(required = false) Long userId,
                                     @RequestParam(defaultValue = "1") Integer pageNum,
                                     @RequestParam(defaultValue = "10") Integer pageSize) {
        Page<PortalUserGrowth> page = new Page<>(pageNum, pageSize);
        LambdaQueryWrapper<PortalUserGrowth> wrapper = new LambdaQueryWrapper<>();
        if (userId != null) {
            wrapper.eq(PortalUserGrowth::getUserId, userId);
        }
        wrapper.orderByDesc(PortalUserGrowth::getGrowthValue);
        page = portalUserGrowthMapper.selectPage(page, wrapper);

        // 关联填充用户昵称
        List<PortalUserGrowth> records = page.getRecords();
        Page<Map<String, Object>> resultPage = new Page<>(page.getCurrent(), page.getSize(), page.getTotal());
        List<Map<String, Object>> resultRecords = new java.util.ArrayList<>(records.size());
        for (PortalUserGrowth growth : records) {
            Map<String, Object> map = new LinkedHashMap<>();
            map.put("id", growth.getId());
            map.put("userId", growth.getUserId());
            map.put("growthValue", growth.getGrowthValue());
            map.put("level", growth.getLevel());
            map.put("title", growth.getTitle());
            map.put("seasonValue", growth.getSeasonValue());
            map.put("createTime", growth.getCreateTime());
            map.put("updateTime", growth.getUpdateTime());
            String nickname = null;
            if (growth.getUserId() != null) {
                PortalUser user = portalUserMapper.selectPortalUserById(growth.getUserId());
                if (user != null) {
                    nickname = user.getNickname();
                }
            }
            map.put("nickname", nickname);
            resultRecords.add(map);
        }
        resultPage.setRecords(resultRecords);
        return success(resultPage);
    }

    @Operation(summary = "获取用户成长详情", description = "获取指定用户的成长信息及统计信息")
    @PreAuthorize("@ss.hasPermi('cms:growth:query')")
    @GetMapping("/user-growth/{userId}")
    public AjaxResult getUserGrowth(@Parameter(description = "用户ID") @PathVariable Long userId) {
        PortalUserGrowth growth = portalUserGrowthMapper.selectByUserId(userId);
        PortalUserStats stats = portalUserStatsMapper.selectByUserId(userId);
        Map<String, Object> result = new HashMap<>();
        result.put("growth", growth);
        result.put("stats", stats);
        return success(result);
    }

    // ========================================================================
    // 用户徽章管理
    // ========================================================================

    @Operation(summary = "获取用户徽章分页列表", description = "分页查询用户徽章记录，支持按用户ID筛选")
    @PreAuthorize("@ss.hasPermi('cms:growth:list')")
    @GetMapping("/badge/list")
    public AjaxResult listBadge(@RequestParam(required = false) Long userId,
                                @RequestParam(defaultValue = "1") Integer pageNum,
                                @RequestParam(defaultValue = "10") Integer pageSize) {
        Page<PortalUserBadge> page = new Page<>(pageNum, pageSize);
        LambdaQueryWrapper<PortalUserBadge> wrapper = new LambdaQueryWrapper<>();
        if (userId != null) {
            wrapper.eq(PortalUserBadge::getUserId, userId);
        }
        wrapper.orderByDesc(PortalUserBadge::getCreateTime);
        page = portalUserBadgeMapper.selectPage(page, wrapper);
        return success(page);
    }

    @Operation(summary = "手动授予徽章", description = "为指定用户手动授予某成就对应的徽章")
    @PreAuthorize("@ss.hasPermi('cms:growth:add')")
    @Log(title = "用户徽章", businessType = BusinessType.INSERT)
    @PostMapping("/badge")
    public AjaxResult grantBadge(@RequestBody Map<String, Object> body) {
        Long userId = Long.valueOf(String.valueOf(body.get("userId")));
        Long achievementId = Long.valueOf(String.valueOf(body.get("achievementId")));
        return toAjax(portalUserBadgeMapper.insertIfNotExists(userId, achievementId));
    }

    @Operation(summary = "撤销徽章", description = "撤销指定用户的某成就徽章")
    @PreAuthorize("@ss.hasPermi('cms:growth:remove')")
    @Log(title = "用户徽章", businessType = BusinessType.DELETE)
    @DeleteMapping("/badge")
    public AjaxResult revokeBadge(@RequestBody Map<String, Object> body) {
        Long userId = Long.valueOf(String.valueOf(body.get("userId")));
        Long achievementId = Long.valueOf(String.valueOf(body.get("achievementId")));
        LambdaQueryWrapper<PortalUserBadge> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(PortalUserBadge::getUserId, userId)
                .eq(PortalUserBadge::getAchievementId, achievementId);
        return toAjax(portalUserBadgeMapper.delete(wrapper));
    }

    // ========================================================================
    // 成长流水查询
    // ========================================================================

    @Operation(summary = "获取成长流水分页列表", description = "分页查询成长事件流水，支持按用户ID、模块、行为筛选")
    @PreAuthorize("@ss.hasPermi('cms:growth:list')")
    @GetMapping("/log/list")
    public AjaxResult listLog(PortalGrowthLog query,
                              @RequestParam(defaultValue = "1") Integer pageNum,
                              @RequestParam(defaultValue = "10") Integer pageSize) {
        Page<PortalGrowthLog> page = new Page<>(pageNum, pageSize);
        LambdaQueryWrapper<PortalGrowthLog> wrapper = new LambdaQueryWrapper<>();
        if (query.getUserId() != null) {
            wrapper.eq(PortalGrowthLog::getUserId, query.getUserId());
        }
        if (query.getModule() != null && !query.getModule().isEmpty()) {
            wrapper.eq(PortalGrowthLog::getModule, query.getModule());
        }
        if (query.getAction() != null && !query.getAction().isEmpty()) {
            wrapper.eq(PortalGrowthLog::getAction, query.getAction());
        }
        wrapper.orderByDesc(PortalGrowthLog::getCreateTime);
        page = portalGrowthLogMapper.selectPage(page, wrapper);
        return success(page);
    }
}
