package com.moyun.portal.controller;

import com.moyun.common.annotation.Log;
import com.moyun.common.core.controller.BaseController;
import com.moyun.common.core.domain.AjaxResult;
import com.moyun.common.core.page.TableDataInfo;
import com.moyun.common.enums.BusinessType;
import com.moyun.common.utils.poi.ExcelUtil;
import com.moyun.portal.domain.PortalLike;
import com.moyun.portal.service.IPortalLikeService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "门户点赞", description = "门户点赞的增删改查操作接口")
@RestController
@RequestMapping("/api/like")
public class PortalLikeController extends BaseController {

    @Autowired
    private IPortalLikeService portalLikeService;

    @Operation(summary = "获取点赞列表", description = "根据条件分页查询点赞列表")
    @GetMapping("/list")
    public TableDataInfo list(PortalLike portalLike) {
        startPage();
        List<PortalLike> list = portalLikeService.selectPortalLikeList(portalLike);
        return getDataTable(list);
    }

    @Operation(summary = "导出点赞", description = "导出点赞数据到Excel文件")
    @Log(title = "门户点赞", businessType = BusinessType.EXPORT)
    @PostMapping("/export")
    public void export(HttpServletResponse response, PortalLike portalLike) {
        List<PortalLike> list = portalLikeService.selectPortalLikeList(portalLike);
        ExcelUtil<PortalLike> util = new ExcelUtil<PortalLike>(PortalLike.class);
        util.exportExcel(response, list, "门户点赞数据");
    }

    @Operation(summary = "获取点赞详情", description = "根据点赞ID获取点赞详细信息")
    @GetMapping(value = "/{id}")
    public AjaxResult getInfo(@Parameter(description = "点赞ID") @PathVariable Long id) {
        return success(portalLikeService.selectPortalLikeById(id));
    }

    @Operation(summary = "新增点赞", description = "创建新点赞")
    @Log(title = "门户点赞", businessType = BusinessType.INSERT)
    @PostMapping
    public AjaxResult add(@Validated @RequestBody PortalLike portalLike) {
        return toAjax(portalLikeService.insertPortalLike(portalLike));
    }

    @Operation(summary = "修改点赞", description = "更新点赞信息")
    @Log(title = "门户点赞", businessType = BusinessType.UPDATE)
    @PutMapping
    public AjaxResult edit(@Validated @RequestBody PortalLike portalLike) {
        return toAjax(portalLikeService.updatePortalLike(portalLike));
    }

    @Operation(summary = "删除点赞", description = "批量删除点赞")
    @Log(title = "门户点赞", businessType = BusinessType.DELETE)
    @DeleteMapping("/{ids}")
    public AjaxResult remove(@Parameter(description = "点赞ID数组") @PathVariable Long[] ids) {
        return toAjax(portalLikeService.deletePortalLikeByIds(ids));
    }
}