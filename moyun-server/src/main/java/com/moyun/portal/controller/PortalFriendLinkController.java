package com.moyun.portal.controller;

import com.moyun.common.annotation.Log;
import com.moyun.core.base.BaseController;
import com.moyun.core.base.AjaxResult;
import com.moyun.core.base.TableDataInfo;
import com.moyun.common.enums.BusinessType;
import com.moyun.util.file.ExcelUtil;
import com.moyun.portal.domain.entity.PortalFriendLink;
import com.moyun.portal.service.IPortalFriendLinkService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "门户友情链接", description = "门户友情链接的增删改查操作接口")
@RestController
@RequestMapping("/api/friend-link")
public class PortalFriendLinkController extends BaseController {

    @Autowired
    private IPortalFriendLinkService portalFriendLinkService;

    @Operation(summary = "获取友情链接列表", description = "根据条件分页查询友情链接列表")
    @GetMapping("/list")
    public TableDataInfo list(PortalFriendLink portalFriendLink) {
        startPage();
        List<PortalFriendLink> list = portalFriendLinkService.selectPortalFriendLinkList(portalFriendLink);
        return getDataTable(list);
    }

    @Operation(summary = "导出友情链接", description = "导出友情链接数据到Excel文件")
    @Log(title = "门户友情链接", businessType = BusinessType.EXPORT)
    @PostMapping("/export")
    public void export(HttpServletResponse response, PortalFriendLink portalFriendLink) {
        List<PortalFriendLink> list = portalFriendLinkService.selectPortalFriendLinkList(portalFriendLink);
        ExcelUtil<PortalFriendLink> util = new ExcelUtil<PortalFriendLink>(PortalFriendLink.class);
        util.exportExcel(response, list, "门户友情链接数据");
    }

    @Operation(summary = "获取友情链接详情", description = "根据友情链接ID获取友情链接详细信息")
    @GetMapping(value = "/{id}")
    public AjaxResult getInfo(@Parameter(description = "友情链接ID") @PathVariable Long id) {
        return success(portalFriendLinkService.selectPortalFriendLinkById(id));
    }

    @Operation(summary = "新增友情链接", description = "创建新友情链接")
    @Log(title = "门户友情链接", businessType = BusinessType.INSERT)
    @PostMapping
    public AjaxResult add(@Validated @RequestBody PortalFriendLink portalFriendLink) {
        return toAjax(portalFriendLinkService.insertPortalFriendLink(portalFriendLink));
    }

    @Operation(summary = "修改友情链接", description = "更新友情链接信息")
    @Log(title = "门户友情链接", businessType = BusinessType.UPDATE)
    @PutMapping
    public AjaxResult edit(@Validated @RequestBody PortalFriendLink portalFriendLink) {
        return toAjax(portalFriendLinkService.updatePortalFriendLink(portalFriendLink));
    }

    @Operation(summary = "删除友情链接", description = "批量删除友情链接")
    @Log(title = "门户友情链接", businessType = BusinessType.DELETE)
    @DeleteMapping("/{ids}")
    public AjaxResult remove(@Parameter(description = "友情链接ID数组") @PathVariable Long[] ids) {
        return toAjax(portalFriendLinkService.deletePortalFriendLinkByIds(ids));
    }
}