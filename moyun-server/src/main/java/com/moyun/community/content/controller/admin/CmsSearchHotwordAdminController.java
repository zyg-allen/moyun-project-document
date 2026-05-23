package com.moyun.community.content.controller.admin;

import com.moyun.common.annotation.Log;
import com.moyun.common.core.controller.BaseController;
import com.moyun.common.core.domain.AjaxResult;
import com.moyun.common.core.domain.TableDataInfo;
import com.moyun.common.enums.BusinessType;
import com.moyun.community.content.entity.CmsSearchHotword;
import com.moyun.community.content.service.ICmsSearchService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/admin/search/hotword")
public class CmsSearchHotwordAdminController extends BaseController {

    @Autowired
    private ICmsSearchService searchService;

    @PreAuthorize("@ss.hasPermi('content:search:hotword:list')")
    @GetMapping("/list")
    public TableDataInfo list() {
        startPage();
        List<CmsSearchHotword> list = searchService.selectHotwordList();
        return getDataTable(list);
    }

    @PreAuthorize("@ss.hasPermi('content:search:hotword:query')")
    @GetMapping(value = "/{hotwordId}")
    public AjaxResult getInfo(@PathVariable("hotwordId") Long hotwordId) {
        return success();
    }

    @PreAuthorize("@ss.hasPermi('content:search:hotword:add')")
    @Log(title = "热搜词管理", businessType = BusinessType.INSERT)
    @PostMapping
    public AjaxResult add(@RequestBody CmsSearchHotword hotword) {
        return toAjax(true);
    }

    @PreAuthorize("@ss.hasPermi('content:search:hotword:edit')")
    @Log(title = "热搜词管理", businessType = BusinessType.UPDATE)
    @PutMapping
    public AjaxResult edit(@RequestBody CmsSearchHotword hotword) {
        return toAjax(true);
    }

    @PreAuthorize("@ss.hasPermi('content:search:hotword:remove')")
    @Log(title = "热搜词管理", businessType = BusinessType.DELETE)
    @DeleteMapping("/{hotwordIds}")
    public AjaxResult remove(@PathVariable Long[] hotwordIds) {
        return toAjax(true);
    }
}