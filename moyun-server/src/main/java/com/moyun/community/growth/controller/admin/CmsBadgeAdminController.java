package com.moyun.community.growth.controller.admin;

import com.moyun.common.annotation.Log;
import com.moyun.common.core.controller.BaseController;
import com.moyun.common.core.domain.AjaxResult;
import com.moyun.common.core.domain.TableDataInfo;
import com.moyun.common.enums.BusinessType;
import com.moyun.community.growth.entity.CmsBadge;
import com.moyun.community.growth.service.ICmsBadgeService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/admin/growth/badge")
public class CmsBadgeAdminController extends BaseController {

    @Autowired
    private ICmsBadgeService badgeService;

    @PreAuthorize("@ss.hasPermi('growth:badge:list')")
    @GetMapping("/list")
    public TableDataInfo list() {
        startPage();
        List<CmsBadge> list = badgeService.list();
        return getDataTable(list);
    }

    @PreAuthorize("@ss.hasPermi('growth:badge:query')")
    @GetMapping(value = "/{badgeId}")
    public AjaxResult getInfo(@PathVariable("badgeId") Long badgeId) {
        return success(badgeService.getById(badgeId));
    }

    @PreAuthorize("@ss.hasPermi('growth:badge:add')")
    @Log(title = "徽章管理", businessType = BusinessType.INSERT)
    @PostMapping
    public AjaxResult add(@RequestBody CmsBadge badge) {
        return toAjax(badgeService.save(badge));
    }

    @PreAuthorize("@ss.hasPermi('growth:badge:edit')")
    @Log(title = "徽章管理", businessType = BusinessType.UPDATE)
    @PutMapping
    public AjaxResult edit(@RequestBody CmsBadge badge) {
        return toAjax(badgeService.updateById(badge));
    }

    @PreAuthorize("@ss.hasPermi('growth:badge:remove')")
    @Log(title = "徽章管理", businessType = BusinessType.DELETE)
    @DeleteMapping("/{badgeIds}")
    public AjaxResult remove(@PathVariable Long[] badgeIds) {
        return toAjax(badgeService.removeByIds(List.of(badgeIds)));
    }
}