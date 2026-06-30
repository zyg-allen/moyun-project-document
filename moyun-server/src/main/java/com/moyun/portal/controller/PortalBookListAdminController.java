package com.moyun.portal.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import com.moyun.common.annotation.Log;
import com.moyun.common.enums.BusinessType;
import com.moyun.core.base.AjaxResult;
import com.moyun.core.base.BaseController;
import com.moyun.portal.domain.entity.PortalBookList;
import com.moyun.portal.domain.query.BookListQuery;
import com.moyun.portal.service.IPortalBookListService;
import com.moyun.util.bean.PageUtils;

/**
 * 读书空间-书单管理Controller
 *
 * 注：原 addBookToList / removeBookFromList 两个接口为死接口（书单-书籍关系管理已迁移至
 * PortalBookListRelationController 统一维护），本 Controller 仅保留书单 CRUD，
 * 对应的 Service/Mapper/XML 方法暂不删除，保留以备复用。
 *
 * <p>路径前缀 /portal/admin/ 由核心 SecurityConfig 处理 admin token，
 * 方法级权限通过 @PreAuthorize("@ss.hasPermi(...)") 校验。</p>
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
    @PreAuthorize("@ss.hasPermi('portal:bookList:list')")
    @GetMapping("/list")
    public AjaxResult list(BookListQuery query) {
        Page<PortalBookList> page = PageUtils.buildPage(query);
        Page<PortalBookList> result = portalBookListService.selectPortalBookListPage(page, query);
        return success(result);
    }

    @Operation(summary = "获取书单详情")
    @PreAuthorize("@ss.hasPermi('portal:bookList:query')")
    @GetMapping("/{id}")
    public AjaxResult getById(@PathVariable Long id) {
        PortalBookList bookList = portalBookListService.selectPortalBookListById(id);
        return success(bookList);
    }

    @Operation(summary = "新增书单")
    @PreAuthorize("@ss.hasPermi('portal:bookList:add')")
    @Log(title = "读书空间-书单", businessType = BusinessType.INSERT)
    @PostMapping
    public AjaxResult add(@RequestBody PortalBookList bookList) {
        int result = portalBookListService.insertPortalBookList(bookList);
        return result > 0 ? success("新增成功") : error("新增失败");
    }

    @Operation(summary = "修改书单")
    @PreAuthorize("@ss.hasPermi('portal:bookList:edit')")
    @Log(title = "读书空间-书单", businessType = BusinessType.UPDATE)
    @PutMapping
    public AjaxResult update(@RequestBody PortalBookList bookList) {
        int result = portalBookListService.updatePortalBookList(bookList);
        return result > 0 ? success("修改成功") : error("修改失败");
    }

    @Operation(summary = "删除书单")
    @PreAuthorize("@ss.hasPermi('portal:bookList:remove')")
    @Log(title = "读书空间-书单", businessType = BusinessType.DELETE)
    @DeleteMapping("/{id}")
    public AjaxResult delete(@PathVariable Long id) {
        int result = portalBookListService.deletePortalBookListById(id);
        return result > 0 ? success("删除成功") : error("删除失败");
    }

    @Operation(summary = "批量删除书单")
    @PreAuthorize("@ss.hasPermi('portal:bookList:remove')")
    @Log(title = "读书空间-书单", businessType = BusinessType.DELETE)
    @DeleteMapping("/ids/{ids}")
    public AjaxResult deleteBatch(@PathVariable Long[] ids) {
        int result = portalBookListService.deletePortalBookListByIds(ids);
        return result > 0 ? success("删除成功") : error("删除失败");
    }
}
