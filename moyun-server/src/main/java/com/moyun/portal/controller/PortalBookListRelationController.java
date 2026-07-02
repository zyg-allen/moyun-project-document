package com.moyun.portal.controller;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import com.moyun.common.annotation.Log;
import com.moyun.common.enums.BusinessType;
import com.moyun.core.base.AjaxResult;
import com.moyun.core.base.BaseController;
import com.moyun.portal.domain.entity.PortalBook;
import com.moyun.portal.domain.entity.PortalBookListItem;
import com.moyun.portal.service.IPortalBookListService;

/**
 * 读书空间-书单-书籍关系管理 Controller
 *
 * <p>提供书单内书籍的单本增删改、批量排序、可添加书籍（分页）等关系维护接口。
 * 路径前缀与 PortalBookListAdminController 相同（/portal/admin/book-lists），
 * 通过子路径 /{bookListId}/items、/items/{id}、/{bookListId}/sort 区分，无路径冲突。</p>
 *
 * @author moyun
 */
@Tag(name = "读书空间-书单书籍关系管理", description = "书单内书籍的添加/修改/删除/排序等关系维护接口")
@RestController
@RequestMapping("/portal/admin/book-lists")
public class PortalBookListRelationController extends BaseController {

    @Autowired
    private IPortalBookListService portalBookListService;

    /**
     * 查询书单内书籍列表（带书籍详情）
     */
    @Operation(summary = "查询书单内书籍列表（带书籍详情）")
    @PreAuthorize("@ss.hasPermi('portal:bookList:edit')")
    @GetMapping("/{bookListId}/items")
    public AjaxResult listItems(@Parameter(description = "书单ID") @PathVariable Long bookListId) {
        List<Map<String, Object>> items = portalBookListService.selectBookListItemsWithDetail(bookListId);
        return success(items);
    }

    /**
     * 新增书单项（单本）
     * body: { bookId, sortOrder, remark }
     */
    @Operation(summary = "新增书单项", description = "向书单添加单本书籍，body: bookId/sortOrder/remark")
    @PreAuthorize("@ss.hasPermi('portal:bookList:edit')")
    @Log(title = "读书空间-书单书籍", businessType = BusinessType.INSERT)
    @PostMapping("/{bookListId}/items")
    public AjaxResult addItem(@Parameter(description = "书单ID") @PathVariable Long bookListId,
                              @RequestBody Map<String, Object> body) {
        Long bookId = toLong(body.get("bookId"));
        if (bookId == null) {
            return error("bookId 不能为空");
        }
        Integer sortOrder = toInteger(body.get("sortOrder"));
        String remark = body.get("remark") == null ? null : String.valueOf(body.get("remark"));
        int rows = portalBookListService.addBookListItem(bookListId, bookId, sortOrder, remark);
        return rows > 0 ? success("添加成功") : error("添加失败（可能书籍已在书单内）");
    }

    /**
     * 修改书单项（按 item 主键）
     * body: { sortOrder, remark }
     */
    @Operation(summary = "修改书单项", description = "按书单项ID修改排序与说明，body: sortOrder/remark")
    @PreAuthorize("@ss.hasPermi('portal:bookList:edit')")
    @Log(title = "读书空间-书单书籍", businessType = BusinessType.UPDATE)
    @PutMapping("/items/{id}")
    public AjaxResult updateItem(@Parameter(description = "书单项ID") @PathVariable Long id,
                                 @RequestBody Map<String, Object> body) {
        Integer sortOrder = toInteger(body.get("sortOrder"));
        String remark = body.get("remark") == null ? null : String.valueOf(body.get("remark"));
        int rows = portalBookListService.updateBookListItem(id, sortOrder, remark);
        return rows > 0 ? success("修改成功") : error("修改失败");
    }

    /**
     * 删除书单项（按 item 主键）
     */
    @Operation(summary = "删除书单项", description = "按书单项ID从书单移除单本书籍")
    @PreAuthorize("@ss.hasPermi('portal:bookList:edit')")
    @Log(title = "读书空间-书单书籍", businessType = BusinessType.DELETE)
    @DeleteMapping("/items/{id}")
    public AjaxResult deleteItem(@Parameter(description = "书单项ID") @PathVariable Long id) {
        int rows = portalBookListService.deleteBookListItem(id);
        return rows > 0 ? success("删除成功") : error("删除失败");
    }

    /**
     * 批量排序
     * body: [{ id, sortOrder }]
     */
    @Operation(summary = "批量更新书单内书籍排序", description = "body: [{id, sortOrder}]")
    @PreAuthorize("@ss.hasPermi('portal:bookList:edit')")
    @Log(title = "读书空间-书单书籍排序", businessType = BusinessType.UPDATE)
    @PutMapping("/{bookListId}/sort")
    public AjaxResult updateSort(@Parameter(description = "书单ID") @PathVariable Long bookListId,
                                 @RequestBody List<Map<String, Object>> sortItems) {
        if (sortItems == null || sortItems.isEmpty()) {
            return error("排序项不能为空");
        }
        List<PortalBookListItem> items = new ArrayList<>();
        for (Map<String, Object> entry : sortItems) {
            Long id = toLong(entry.get("id"));
            Integer sortOrder = toInteger(entry.get("sortOrder"));
            if (id == null || sortOrder == null) {
                continue;
            }
            PortalBookListItem item = new PortalBookListItem();
            item.setId(id);
            item.setSort(sortOrder);
            items.add(item);
        }
        int rows = portalBookListService.updateBookListSort(bookListId, items);
        return rows > 0 ? success("排序更新成功") : error("排序更新失败");
    }

    /**
     * 查询可添加到书单的书籍（排除已在书单内的，分页）
     */
    @Operation(summary = "查询可添加到书单的书籍（分页）", description = "返回不在该书单内的可选书籍，支持书名/作者/分类筛选")
    @PreAuthorize("@ss.hasPermi('portal:bookList:edit')")
    @GetMapping("/{bookListId}/available-books")
    public AjaxResult listAvailableBooks(@Parameter(description = "书单ID") @PathVariable Long bookListId,
                                          @Parameter(description = "书名") @RequestParam(required = false) String title,
                                          @Parameter(description = "作者") @RequestParam(required = false) String author,
                                          @Parameter(description = "分类ID") @RequestParam(required = false) Long categoryId,
                                          @Parameter(description = "页码") @RequestParam(defaultValue = "1") int pageNum,
                                          @Parameter(description = "每页条数") @RequestParam(defaultValue = "10") int pageSize) {
        Page<PortalBook> page = new Page<>(pageNum, pageSize);
        Page<PortalBook> result = portalBookListService.selectAvailableBooksPage(page, bookListId, title, author, categoryId);
        return AjaxResult.success(result);
    }

    // ===== 类型转换工具方法 =====

    private Long toLong(Object value) {
        if (value == null) {
            return null;
        }
        try {
            return Long.valueOf(String.valueOf(value));
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private Integer toInteger(Object value) {
        if (value == null) {
            return null;
        }
        try {
            return Integer.valueOf(String.valueOf(value));
        } catch (NumberFormatException e) {
            return null;
        }
    }
}
