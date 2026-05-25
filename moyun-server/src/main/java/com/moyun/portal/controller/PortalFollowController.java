package com.moyun.portal.controller;

import com.moyun.common.annotation.Log;
import com.moyun.core.base.BaseController;
import com.moyun.core.base.AjaxResult;
import com.moyun.core.base.TableDataInfo;
import com.moyun.common.enums.BusinessType;
import com.moyun.util.file.ExcelUtil;
import com.moyun.portal.domain.entity.PortalFollow;
import com.moyun.portal.service.IPortalFollowService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "门户关注", description = "门户关注的增删改查操作接口")
@RestController
@RequestMapping("/api/follow")
public class PortalFollowController extends BaseController {

    @Autowired
    private IPortalFollowService portalFollowService;

    @Operation(summary = "获取关注列表", description = "根据条件分页查询关注列表")
    @GetMapping("/list")
    public TableDataInfo list(PortalFollow portalFollow) {
        startPage();
        List<PortalFollow> list = portalFollowService.selectPortalFollowList(portalFollow);
        return getDataTable(list);
    }

    @Operation(summary = "导出关注", description = "导出关注数据到Excel文件")
    @Log(title = "门户关注", businessType = BusinessType.EXPORT)
    @PostMapping("/export")
    public void export(HttpServletResponse response, PortalFollow portalFollow) {
        List<PortalFollow> list = portalFollowService.selectPortalFollowList(portalFollow);
        ExcelUtil<PortalFollow> util = new ExcelUtil<PortalFollow>(PortalFollow.class);
        util.exportExcel(response, list, "门户关注数据");
    }

    @Operation(summary = "获取关注详情", description = "根据关注ID获取关注详细信息")
    @GetMapping(value = "/{id}")
    public AjaxResult getInfo(@Parameter(description = "关注ID") @PathVariable Long id) {
        return success(portalFollowService.selectPortalFollowById(id));
    }

    @Operation(summary = "新增关注", description = "创建新关注")
    @Log(title = "门户关注", businessType = BusinessType.INSERT)
    @PostMapping
    public AjaxResult add(@Validated @RequestBody PortalFollow portalFollow) {
        return toAjax(portalFollowService.insertPortalFollow(portalFollow));
    }

    @Operation(summary = "修改关注", description = "更新关注信息")
    @Log(title = "门户关注", businessType = BusinessType.UPDATE)
    @PutMapping
    public AjaxResult edit(@Validated @RequestBody PortalFollow portalFollow) {
        return toAjax(portalFollowService.updatePortalFollow(portalFollow));
    }

    @Operation(summary = "删除关注", description = "批量删除关注")
    @Log(title = "门户关注", businessType = BusinessType.DELETE)
    @DeleteMapping("/{ids}")
    public AjaxResult remove(@Parameter(description = "关注ID数组") @PathVariable Long[] ids) {
        return toAjax(portalFollowService.deletePortalFollowByIds(ids));
    }
}