package com.moyun.portal.controller;

import java.util.List;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import com.moyun.core.base.AjaxResult;
import com.moyun.core.base.BaseController;
import com.moyun.portal.domain.entity.PortalCategory;
import com.moyun.portal.domain.query.CategoryQuery;
import com.moyun.portal.service.IPortalCategoryService;

/**
 * 门户分类-后台 Controller
 *
 * <p>供后台管理页面（书籍/书单管理）调用，返回扁平分类列表用于下拉筛选。
 * 复用现有 PortalCategoryService.selectPortalCategoryList 方法。</p>
 *
 * <p>路径前缀 /portal/admin/categories，无需额外权限校验（登录即可访问，
 * 因为分类是公共元数据，所有 admin 都可读）。</p>
 *
 * @author moyun
 */
@Tag(name = "门户分类-后台", description = "后台分类下拉数据接口")
@RestController
@RequestMapping("/portal/admin/categories")
public class PortalCategoryAdminController extends BaseController {

    @Autowired
    private IPortalCategoryService portalCategoryService;

    @Operation(summary = "查询分类列表（扁平，用于下拉）")
    @PreAuthorize("@ss.hasPermi('portal:book:list')")
    @GetMapping("/list")
    public AjaxResult list(CategoryQuery query) {
        // 后台默认查正常状态的分类
        if (query.getStatus() == null) {
            query.setStatus("0");
        }
        List<PortalCategory> list = portalCategoryService.selectPortalCategoryList(query);
        return success(list);
    }

    @Operation(summary = "查询所有分类（含停用，用于全量下拉）")
    @PreAuthorize("@ss.hasPermi('portal:book:list')")
    @GetMapping("/all")
    public AjaxResult all() {
        CategoryQuery query = new CategoryQuery();
        List<PortalCategory> list = portalCategoryService.selectPortalCategoryList(query);
        return success(list);
    }
}
