package com.moyun.portal.controller;

import java.util.List;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import com.moyun.core.base.AjaxResult;
import com.moyun.core.base.BaseController;
import com.moyun.portal.domain.entity.PortalBookRecommend;
import com.moyun.portal.domain.query.BookRecommendQuery;
import com.moyun.portal.service.IPortalBookRecommendService;

/**
 * 读书空间-书籍推荐管理（后台）
 * 运营位管理：首页 Banner / 热门推荐 / 分类置顶 / 限免专区 / 发现页 Banner
 *
 * @author moyun
 */
@Tag(name = "读书空间-书籍推荐管理", description = "后台书籍推荐（运营位）管理接口")
@RestController
@RequestMapping("/portal/admin/bookRecommend")
public class PortalBookRecommendAdminController extends BaseController {

    @Autowired
    private IPortalBookRecommendService bookRecommendService;

    @Operation(summary = "分页查询推荐列表")
    @PreAuthorize("@ss.hasPermi('portal:bookRecommend:list')")
    @GetMapping("/list")
    public AjaxResult list(BookRecommendQuery query) {
        Page<PortalBookRecommend> page = buildPage(query);
        Page<PortalBookRecommend> result = bookRecommendService.selectBookRecommendPage(page, query);
        return AjaxResult.success(result);
    }

    @Operation(summary = "推荐详情")
    @PreAuthorize("@ss.hasPermi('portal:bookRecommend:query')")
    @GetMapping("/{id}")
    public AjaxResult getInfo(@PathVariable Long id) {
        return AjaxResult.success(bookRecommendService.selectBookRecommendById(id));
    }

    @Operation(summary = "新增推荐")
    @PreAuthorize("@ss.hasPermi('portal:bookRecommend:add')")
    @PostMapping
    public AjaxResult add(@RequestBody PortalBookRecommend bookRecommend) {
        return toAjax(bookRecommendService.insertBookRecommend(bookRecommend));
    }

    @Operation(summary = "修改推荐")
    @PreAuthorize("@ss.hasPermi('portal:bookRecommend:edit')")
    @PutMapping
    public AjaxResult edit(@RequestBody PortalBookRecommend bookRecommend) {
        return toAjax(bookRecommendService.updateBookRecommend(bookRecommend));
    }

    @Operation(summary = "删除推荐")
    @PreAuthorize("@ss.hasPermi('portal:bookRecommend:remove')")
    @DeleteMapping("/{ids}")
    public AjaxResult remove(@PathVariable Long[] ids) {
        return toAjax(bookRecommendService.deleteBookRecommendByIds(ids));
    }

    @Operation(summary = "上下架切换")
    @PreAuthorize("@ss.hasPermi('portal:bookRecommend:edit')")
    @PutMapping("/{id}/toggle")
    public AjaxResult toggle(@PathVariable Long id, @RequestParam Boolean isActive) {
        return toAjax(bookRecommendService.toggleActive(id, isActive));
    }

    @Operation(summary = "批量排序")
    @PreAuthorize("@ss.hasPermi('portal:bookRecommend:edit')")
    @PutMapping("/sort")
    public AjaxResult sort(@RequestBody List<PortalBookRecommend> list) {
        return toAjax(bookRecommendService.batchUpdateSort(list));
    }

    private Page<PortalBookRecommend> buildPage(BookRecommendQuery query) {
        int pageNum = query.getPageNum() == null ? 1 : query.getPageNum();
        int pageSize = query.getPageSize() == null ? 10 : query.getPageSize();
        if (pageSize > 100) pageSize = 100;
        return new Page<>(pageNum, pageSize);
    }
}
