package com.moyun.portal.controller;

import com.moyun.common.annotation.Log;
import com.moyun.core.base.BaseController;
import com.moyun.core.base.AjaxResult;
import com.moyun.core.base.TableDataInfo;
import com.moyun.common.enums.BusinessType;
import com.moyun.util.file.ExcelUtil;
import com.moyun.portal.domain.entity.PortalBookmark;
import com.moyun.portal.service.IPortalBookmarkService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "门户收藏", description = "门户收藏的增删改查操作接口")
@RestController
@RequestMapping("/portal/bookmark")
public class PortalBookmarkController extends BaseController {

    @Autowired
    private IPortalBookmarkService portalBookmarkService;

    @Operation(summary = "获取收藏列表", description = "根据条件分页查询收藏列表")
    @GetMapping("/list")
    public TableDataInfo list(PortalBookmark portalBookmark) {
        startPage();
        List<PortalBookmark> list = portalBookmarkService.selectPortalBookmarkList(portalBookmark);
        return getDataTable(list);
    }

    @Operation(summary = "导出收藏", description = "导出收藏数据到Excel文件")
    @Log(title = "门户收藏", businessType = BusinessType.EXPORT)
    @PostMapping("/export")
    public void export(HttpServletResponse response, PortalBookmark portalBookmark) {
        List<PortalBookmark> list = portalBookmarkService.selectPortalBookmarkList(portalBookmark);
        ExcelUtil<PortalBookmark> util = new ExcelUtil<PortalBookmark>(PortalBookmark.class);
        util.exportExcel(response, list, "门户收藏数据");
    }

    @Operation(summary = "获取收藏详情", description = "根据收藏ID获取收藏详细信息")
    @GetMapping(value = "/{id}")
    public AjaxResult getInfo(@Parameter(description = "收藏ID") @PathVariable Long id) {
        return success(portalBookmarkService.selectPortalBookmarkById(id));
    }

    @Operation(summary = "新增收藏", description = "创建新收藏")
    @Log(title = "门户收藏", businessType = BusinessType.INSERT)
    @PostMapping
    public AjaxResult add(@Validated @RequestBody PortalBookmark portalBookmark) {
        return toAjax(portalBookmarkService.insertPortalBookmark(portalBookmark));
    }

    @Operation(summary = "修改收藏", description = "更新收藏信息")
    @Log(title = "门户收藏", businessType = BusinessType.UPDATE)
    @PutMapping
    public AjaxResult edit(@Validated @RequestBody PortalBookmark portalBookmark) {
        return toAjax(portalBookmarkService.updatePortalBookmark(portalBookmark));
    }

    @Operation(summary = "删除收藏", description = "批量删除收藏")
    @Log(title = "门户收藏", businessType = BusinessType.DELETE)
    @DeleteMapping("/{ids}")
    public AjaxResult remove(@Parameter(description = "收藏ID数组") @PathVariable Long[] ids) {
        return toAjax(portalBookmarkService.deletePortalBookmarkByIds(ids));
    }
}