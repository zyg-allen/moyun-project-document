package com.moyun.community.growth.controller.admin;

import com.moyun.common.annotation.Log;
import com.moyun.common.core.controller.BaseController;
import com.moyun.common.core.domain.AjaxResult;
import com.moyun.common.core.domain.TableDataInfo;
import com.moyun.common.enums.BusinessType;
import com.moyun.community.growth.entity.CmsLevelConfig;
import com.moyun.community.growth.service.ICmsLevelConfigService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/admin/growth/level")
public class CmsLevelConfigAdminController extends BaseController {

    @Autowired
    private ICmsLevelConfigService levelConfigService;

    @PreAuthorize("@ss.hasPermi('growth:level:list')")
    @GetMapping("/list")
    public TableDataInfo list() {
        startPage();
        List<CmsLevelConfig> list = levelConfigService.list();
        return getDataTable(list);
    }

    @PreAuthorize("@ss.hasPermi('growth:level:query')")
    @GetMapping(value = "/{levelId}")
    public AjaxResult getInfo(@PathVariable("levelId") Long levelId) {
        return success(levelConfigService.getById(levelId));
    }

    @PreAuthorize("@ss.hasPermi('growth:level:add')")
    @Log(title = "等级配置管理", businessType = BusinessType.INSERT)
    @PostMapping
    public AjaxResult add(@RequestBody CmsLevelConfig level) {
        return toAjax(levelConfigService.save(level));
    }

    @PreAuthorize("@ss.hasPermi('growth:level:edit')")
    @Log(title = "等级配置管理", businessType = BusinessType.UPDATE)
    @PutMapping
    public AjaxResult edit(@RequestBody CmsLevelConfig level) {
        return toAjax(levelConfigService.updateById(level));
    }

    @PreAuthorize("@ss.hasPermi('growth:level:remove')")
    @Log(title = "等级配置管理", businessType = BusinessType.DELETE)
    @DeleteMapping("/{levelIds}")
    public AjaxResult remove(@PathVariable Long[] levelIds) {
        return toAjax(levelConfigService.removeByIds(List.of(levelIds)));
    }
}