package com.moyun.community.content.controller.admin;

import com.moyun.common.annotation.Log;
import com.moyun.common.core.controller.BaseController;
import com.moyun.common.core.domain.AjaxResult;
import com.moyun.common.core.domain.TableDataInfo;
import com.moyun.common.enums.BusinessType;
import com.moyun.community.content.entity.CmsBanner;
import com.moyun.community.content.service.ICmsBannerService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/admin/banner")
public class CmsBannerAdminController extends BaseController {

    @Autowired
    private ICmsBannerService bannerService;

    @PreAuthorize("@ss.hasPermi('content:banner:list')")
    @GetMapping("/list")
    public TableDataInfo list() {
        startPage();
        List<CmsBanner> list = bannerService.list();
        return getDataTable(list);
    }

    @PreAuthorize("@ss.hasPermi('content:banner:query')")
    @GetMapping(value = "/{bannerId}")
    public AjaxResult getInfo(@PathVariable("bannerId") Long bannerId) {
        return success(bannerService.getById(bannerId));
    }

    @PreAuthorize("@ss.hasPermi('content:banner:add')")
    @Log(title = "Banner管理", businessType = BusinessType.INSERT)
    @PostMapping
    public AjaxResult add(@RequestBody CmsBanner banner) {
        return toAjax(bannerService.save(banner));
    }

    @PreAuthorize("@ss.hasPermi('content:banner:edit')")
    @Log(title = "Banner管理", businessType = BusinessType.UPDATE)
    @PutMapping
    public AjaxResult edit(@RequestBody CmsBanner banner) {
        return toAjax(bannerService.updateById(banner));
    }

    @PreAuthorize("@ss.hasPermi('content:banner:remove')")
    @Log(title = "Banner管理", businessType = BusinessType.DELETE)
    @DeleteMapping("/{bannerIds}")
    public AjaxResult remove(@PathVariable Long[] bannerIds) {
        return toAjax(bannerService.removeByIds(List.of(bannerIds)));
    }
}