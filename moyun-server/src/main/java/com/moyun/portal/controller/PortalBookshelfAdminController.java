package com.moyun.portal.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import com.moyun.core.base.AjaxResult;
import com.moyun.core.base.BaseController;
import com.moyun.portal.domain.entity.PortalBookshelf;
import com.moyun.portal.domain.query.BookshelfQuery;
import com.moyun.portal.service.IPortalBookshelfService;

/**
 * 读书空间-书架管理（后台）
 * 仅提供查询和移除，便于后台查看用户书架
 *
 * @author moyun
 */
@Tag(name = "读书空间-书架管理", description = "后台书架管理接口（只读+移除）")
@RestController
@RequestMapping("/portal/admin/bookshelf")
public class PortalBookshelfAdminController extends BaseController {

    @Autowired
    private IPortalBookshelfService bookshelfService;

    @Operation(summary = "分页查询书架")
    @PreAuthorize("@ss.hasPermi('portal:bookshelf:list')")
    @GetMapping("/list")
    public AjaxResult list(BookshelfQuery query) {
        Page<PortalBookshelf> page = buildPage(query);
        Page<PortalBookshelf> result = bookshelfService.selectBookshelfPage(page, query);
        return AjaxResult.success(result);
    }

    @Operation(summary = "书架详情")
    @PreAuthorize("@ss.hasPermi('portal:bookshelf:query')")
    @GetMapping("/{id}")
    public AjaxResult getInfo(@PathVariable Long id) {
        return AjaxResult.success(bookshelfService.selectBookshelfById(id));
    }

    @Operation(summary = "移出书架（后台批量）")
    @PreAuthorize("@ss.hasPermi('portal:bookshelf:remove')")
    @DeleteMapping("/{ids}")
    public AjaxResult remove(@PathVariable Long[] ids) {
        return toAjax(bookshelfService.deleteBookshelfByIds(ids));
    }

    private Page<PortalBookshelf> buildPage(BookshelfQuery query) {
        int pageNum = query.getPageNum() == null ? 1 : query.getPageNum();
        int pageSize = query.getPageSize() == null ? 10 : query.getPageSize();
        if (pageSize > 100) pageSize = 100;
        return new Page<>(pageNum, pageSize);
    }
}
