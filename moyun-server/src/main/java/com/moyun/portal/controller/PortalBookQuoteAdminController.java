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
import com.moyun.portal.domain.entity.PortalBookQuote;
import com.moyun.portal.domain.query.BookQuoteQuery;
import com.moyun.portal.service.IPortalBookQuoteService;
import com.moyun.util.bean.PageUtils;

/**
 * 读书空间-金句管理Controller
 *
 * <p>路径前缀 /portal/admin/ 由核心 SecurityConfig 处理 admin token，
 * 方法级权限通过 @PreAuthorize("@ss.hasPermi(...)") 校验。</p>
 *
 * @author moyun
 */
@Tag(name = "读书空间-金句管理", description = "金句摘录管理相关接口")
@RestController
@RequestMapping("/portal/admin/book-quotes")
public class PortalBookQuoteAdminController extends BaseController {

    @Autowired
    private IPortalBookQuoteService portalBookQuoteService;

    @Operation(summary = "查询金句列表")
    @PreAuthorize("@ss.hasPermi('portal:bookQuote:list')")
    @GetMapping("/list")
    public AjaxResult list(BookQuoteQuery query) {
        Page<PortalBookQuote> page = PageUtils.buildPage(query);
        Page<PortalBookQuote> result = portalBookQuoteService.selectPortalBookQuotePage(page, query);
        return success(result);
    }

    @Operation(summary = "获取金句详情")
    @PreAuthorize("@ss.hasPermi('portal:bookQuote:query')")
    @GetMapping("/{id}")
    public AjaxResult getById(@PathVariable Long id) {
        PortalBookQuote quote = portalBookQuoteService.selectPortalBookQuoteById(id);
        return success(quote);
    }

    @Operation(summary = "新增金句")
    @PreAuthorize("@ss.hasPermi('portal:bookQuote:add')")
    @Log(title = "读书空间-金句", businessType = BusinessType.INSERT)
    @PostMapping
    public AjaxResult add(@RequestBody PortalBookQuote quote) {
        int result = portalBookQuoteService.insertPortalBookQuote(quote);
        return result > 0 ? success("新增成功") : error("新增失败");
    }

    @Operation(summary = "修改金句")
    @PreAuthorize("@ss.hasPermi('portal:bookQuote:edit')")
    @Log(title = "读书空间-金句", businessType = BusinessType.UPDATE)
    @PutMapping
    public AjaxResult update(@RequestBody PortalBookQuote quote) {
        int result = portalBookQuoteService.updatePortalBookQuote(quote);
        return result > 0 ? success("修改成功") : error("修改失败");
    }

    @Operation(summary = "删除金句")
    @PreAuthorize("@ss.hasPermi('portal:bookQuote:remove')")
    @Log(title = "读书空间-金句", businessType = BusinessType.DELETE)
    @DeleteMapping("/{id}")
    public AjaxResult delete(@PathVariable Long id) {
        int result = portalBookQuoteService.deletePortalBookQuoteById(id);
        return result > 0 ? success("删除成功") : error("删除失败");
    }

    @Operation(summary = "批量删除金句")
    @PreAuthorize("@ss.hasPermi('portal:bookQuote:remove')")
    @Log(title = "读书空间-金句", businessType = BusinessType.DELETE)
    @DeleteMapping("/ids/{ids}")
    public AjaxResult deleteBatch(@PathVariable Long[] ids) {
        int result = portalBookQuoteService.deletePortalBookQuoteByIds(ids);
        return result > 0 ? success("删除成功") : error("删除失败");
    }
}
