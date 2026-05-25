package com.moyun.portal.controller;

import com.moyun.common.annotation.Log;
import com.moyun.core.base.BaseController;
import com.moyun.core.base.AjaxResult;
import com.moyun.core.base.TableDataInfo;
import com.moyun.common.enums.BusinessType;
import com.moyun.util.file.ExcelUtil;
import com.moyun.portal.domain.entity.PortalComment;
import com.moyun.portal.service.IPortalCommentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "门户评论", description = "门户评论的增删改查操作接口")
@RestController
@RequestMapping("/api/comment")
public class PortalCommentController extends BaseController {

    @Autowired
    private IPortalCommentService portalCommentService;

    @Operation(summary = "获取评论列表", description = "根据条件分页查询评论列表")
    @GetMapping("/list")
    public TableDataInfo list(PortalComment portalComment) {
        startPage();
        List<PortalComment> list = portalCommentService.selectPortalCommentList(portalComment);
        return getDataTable(list);
    }

    @Operation(summary = "导出评论", description = "导出评论数据到Excel文件")
    @Log(title = "门户评论", businessType = BusinessType.EXPORT)
    @PostMapping("/export")
    public void export(HttpServletResponse response, PortalComment portalComment) {
        List<PortalComment> list = portalCommentService.selectPortalCommentList(portalComment);
        ExcelUtil<PortalComment> util = new ExcelUtil<PortalComment>(PortalComment.class);
        util.exportExcel(response, list, "门户评论数据");
    }

    @Operation(summary = "获取评论详情", description = "根据评论ID获取评论详细信息")
    @GetMapping(value = "/{id}")
    public AjaxResult getInfo(@Parameter(description = "评论ID") @PathVariable Long id) {
        return success(portalCommentService.selectPortalCommentById(id));
    }

    @Operation(summary = "新增评论", description = "创建新评论")
    @Log(title = "门户评论", businessType = BusinessType.INSERT)
    @PostMapping
    public AjaxResult add(@Validated @RequestBody PortalComment portalComment) {
        return toAjax(portalCommentService.insertPortalComment(portalComment));
    }

    @Operation(summary = "修改评论", description = "更新评论信息")
    @Log(title = "门户评论", businessType = BusinessType.UPDATE)
    @PutMapping
    public AjaxResult edit(@Validated @RequestBody PortalComment portalComment) {
        return toAjax(portalCommentService.updatePortalComment(portalComment));
    }

    @Operation(summary = "删除评论", description = "批量删除评论")
    @Log(title = "门户评论", businessType = BusinessType.DELETE)
    @DeleteMapping("/{ids}")
    public AjaxResult remove(@Parameter(description = "评论ID数组") @PathVariable Long[] ids) {
        return toAjax(portalCommentService.deletePortalCommentByIds(ids));
    }
}