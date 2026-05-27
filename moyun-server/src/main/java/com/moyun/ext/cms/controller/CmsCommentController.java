package com.moyun.ext.cms.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.moyun.common.annotation.Log;
import com.moyun.common.enums.BusinessType;
import com.moyun.core.base.AjaxResult;
import com.moyun.core.base.BaseController;
import com.moyun.core.base.TableDataInfo;
import com.moyun.ext.cms.domain.query.CmsCommentQuery;
import com.moyun.ext.cms.domain.vo.CmsCommentVO;
import com.moyun.ext.cms.service.ICmsCommentService;
import com.moyun.portal.domain.entity.PortalComment;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

/**
 * CMS评论管理Controller
 *
 * @author moyun
 */
@Tag(name = "CMS评论管理", description = "CMS评论管理接口")
@RestController
@RequestMapping("/cms/comment")
public class CmsCommentController extends BaseController {
    @Autowired
    private ICmsCommentService cmsCommentService;

    /**
     * 获取评论列表
     */
    @Operation(summary = "获取评论列表", description = "根据条件分页查询评论列表")
    @PreAuthorize("@ss.hasPermi('cms:comment:list')")
    @GetMapping("/list")
    public TableDataInfo list(CmsCommentQuery query) {
        startPage();
        Page<CmsCommentVO> page = cmsCommentService.selectCommentPage(query);
        return getDataTable(page.getRecords(), page.getTotal());
    }

    /**
     * 根据评论编号获取详细信息
     */
    @Operation(summary = "获取评论详情", description = "根据评论ID获取评论详细信息")
    @PreAuthorize("@ss.hasPermi('cms:comment:query')")
    @GetMapping(value = "/{id}")
    public AjaxResult getInfo(@Parameter(description = "评论ID") @PathVariable Long id) {
        return success(cmsCommentService.selectCommentById(id));
    }

    /**
     * 审核评论
     */
    @Operation(summary = "审核评论", description = "审核评论内容")
    @PreAuthorize("@ss.hasPermi('cms:comment:edit')")
    @Log(title = "评论管理", businessType = BusinessType.UPDATE)
    @PutMapping("/audit")
    public AjaxResult audit(@RequestBody PortalComment comment) {
        return toAjax(cmsCommentService.auditComment(comment));
    }

    /**
     * 删除评论
     */
    @Operation(summary = "删除评论", description = "批量删除评论")
    @PreAuthorize("@ss.hasPermi('cms:comment:remove')")
    @Log(title = "评论管理", businessType = BusinessType.DELETE)
    @DeleteMapping("/{ids}")
    public AjaxResult remove(@Parameter(description = "评论ID数组") @PathVariable Long[] ids) {
        return toAjax(cmsCommentService.deleteCommentByIds(ids));
    }
}
