package com.moyun.portal.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import com.moyun.core.base.AjaxResult;
import com.moyun.core.base.BaseController;
import com.moyun.portal.domain.entity.PortalBookList;
import com.moyun.portal.domain.query.BookListQuery;
import com.moyun.portal.service.IPortalBookListService;
import com.moyun.util.bean.PageUtils;

/**
 * 读书空间-书单管理Controller
 *
 * @author moyun
 */
@Tag(name = "读书空间-书单管理", description = "书单管理相关接口")
@RestController
@RequestMapping("/portal/admin/book-lists")
public class PortalBookListAdminController extends BaseController {

    @Autowired
    private IPortalBookListService portalBookListService;

    @Operation(summary = "查询书单列表")
    @GetMapping("/list")
    public AjaxResult list(BookListQuery query) {
        Page<PortalBookList> page = PageUtils.buildPage(query);
        Page<PortalBookList> result = portalBookListService.selectPortalBookListPage(page, query);
        return success(result);
    }

    @Operation(summary = "获取书单详情")
    @GetMapping("/{id}")
    public AjaxResult getById(@PathVariable Long id) {
        PortalBookList bookList = portalBookListService.selectPortalBookListById(id);
        return success(bookList);
    }

    @Operation(summary = "新增书单")
    @PostMapping
    public AjaxResult add(@RequestBody PortalBookList bookList) {
        int result = portalBookListService.insertPortalBookList(bookList);
        return result > 0 ? success("新增成功") : error("新增失败");
    }

    @Operation(summary = "修改书单")
    @PutMapping
    public AjaxResult update(@RequestBody PortalBookList bookList) {
        int result = portalBookListService.updatePortalBookList(bookList);
        return result > 0 ? success("修改成功") : error("修改失败");
    }

    @Operation(summary = "删除书单")
    @DeleteMapping("/{id}")
    public AjaxResult delete(@PathVariable Long id) {
        int result = portalBookListService.deletePortalBookListById(id);
        return result > 0 ? success("删除成功") : error("删除失败");
    }

    @Operation(summary = "批量删除书单")
    @DeleteMapping("/{ids}")
    public AjaxResult deleteBatch(@PathVariable Long[] ids) {
        int result = portalBookListService.deletePortalBookListByIds(ids);
        return result > 0 ? success("删除成功") : error("删除失败");
    }

    @Operation(summary = "添加书籍到书单")
    @PostMapping("/{listId}/books/{bookId}")
    public AjaxResult addBookToList(@PathVariable Long listId, @PathVariable Long bookId,
                                     @RequestParam(required = false) Integer sort,
                                     @RequestParam(required = false) String note) {
        int result = portalBookListService.addBookToBookList(listId, bookId, sort, note);
        return result > 0 ? success("添加成功") : error("添加失败");
    }

    @Operation(summary = "从书单移除书籍")
    @DeleteMapping("/{listId}/books/{bookId}")
    public AjaxResult removeBookFromList(@PathVariable Long listId, @PathVariable Long bookId) {
        int result = portalBookListService.removeBookFromBookList(listId, bookId);
        return result > 0 ? success("移除成功") : error("移除失败");
    }
}
