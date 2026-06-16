package com.moyun.portal.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import com.moyun.core.base.AjaxResult;
import com.moyun.core.base.BaseController;
import com.moyun.portal.domain.entity.PortalBook;
import com.moyun.portal.domain.query.BookQuery;
import com.moyun.portal.service.IPortalBookService;
import com.moyun.util.bean.PageUtils;

/**
 * 读书空间-书籍管理Controller
 *
 * @author moyun
 */
@Tag(name = "读书空间-书籍管理", description = "书籍管理相关接口")
@RestController
@RequestMapping("/portal/admin/books")
public class PortalBookAdminController extends BaseController {

    @Autowired
    private IPortalBookService portalBookService;

    @Operation(summary = "查询书籍列表")
    @GetMapping("/list")
    public AjaxResult list(BookQuery query) {
        Page<PortalBook> page = PageUtils.buildPage(query);
        Page<PortalBook> result = portalBookService.selectPortalBookPage(page, query);
        return success(result);
    }

    @Operation(summary = "获取书籍详情")
    @GetMapping("/{id}")
    public AjaxResult getById(@PathVariable Long id) {
        return success(portalBookService.selectPortalBookById(id));
    }

    @Operation(summary = "新增书籍")
    @PostMapping
    public AjaxResult add(@RequestBody PortalBook portalBook) {
        int result = portalBookService.insertPortalBook(portalBook);
        return result > 0 ? success("新增成功") : error("新增失败");
    }

    @Operation(summary = "修改书籍")
    @PutMapping
    public AjaxResult update(@RequestBody PortalBook portalBook) {
        int result = portalBookService.updatePortalBook(portalBook);
        return result > 0 ? success("修改成功") : error("修改失败");
    }

    @Operation(summary = "删除书籍")
    @DeleteMapping("/{id}")
    public AjaxResult delete(@PathVariable Long id) {
        int result = portalBookService.deletePortalBookById(id);
        return result > 0 ? success("删除成功") : error("删除失败");
    }

    @Operation(summary = "批量删除书籍")
    @DeleteMapping("/ids/{ids}")
    public AjaxResult deleteBatch(@PathVariable Long[] ids) {
        int result = portalBookService.deletePortalBookByIds(ids);
        return result > 0 ? success("删除成功") : error("删除失败");
    }
}
