package com.moyun.community.content.controller.admin;

import com.moyun.common.annotation.Log;
import com.moyun.common.core.controller.BaseController;
import com.moyun.common.core.domain.AjaxResult;
import com.moyun.common.core.domain.TableDataInfo;
import com.moyun.common.enums.BusinessType;
import com.moyun.community.content.entity.CmsTag;
import com.moyun.community.content.service.ICmsTagService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/admin/tag")
public class CmsTagAdminController extends BaseController {

    @Autowired
    private ICmsTagService tagService;

    @PreAuthorize("@ss.hasPermi('content:tag:list')")
    @GetMapping("/list")
    public TableDataInfo list() {
        startPage();
        List<CmsTag> list = tagService.list();
        return getDataTable(list);
    }

    @PreAuthorize("@ss.hasPermi('content:tag:query')")
    @GetMapping(value = "/{tagId}")
    public AjaxResult getInfo(@PathVariable("tagId") Long tagId) {
        return success(tagService.getById(tagId));
    }

    @PreAuthorize("@ss.hasPermi('content:tag:add')")
    @Log(title = "标签管理", businessType = BusinessType.INSERT)
    @PostMapping
    public AjaxResult add(@RequestBody CmsTag tag) {
        return toAjax(tagService.save(tag));
    }

    @PreAuthorize("@ss.hasPermi('content:tag:edit')")
    @Log(title = "标签管理", businessType = BusinessType.UPDATE)
    @PutMapping
    public AjaxResult edit(@RequestBody CmsTag tag) {
        return toAjax(tagService.updateById(tag));
    }

    @PreAuthorize("@ss.hasPermi('content:tag:remove')")
    @Log(title = "标签管理", businessType = BusinessType.DELETE)
    @DeleteMapping("/{tagIds}")
    public AjaxResult remove(@PathVariable Long[] tagIds) {
        return toAjax(tagService.removeByIds(List.of(tagIds)));
    }
}