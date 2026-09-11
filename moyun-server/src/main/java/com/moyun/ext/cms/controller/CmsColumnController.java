package com.moyun.ext.cms.controller;

import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.moyun.common.annotation.Log;
import com.moyun.common.enums.BusinessType;
import com.moyun.core.base.AjaxResult;
import com.moyun.core.base.BaseController;
import com.moyun.ext.cms.domain.query.ColumnQuery;
import com.moyun.ext.cms.domain.vo.ArticleSimpleVO;
import com.moyun.ext.cms.domain.vo.ColumnListItemVO;
import com.moyun.ext.cms.service.ICmsColumnService;
import com.moyun.portal.domain.entity.PortalColumn;
import com.moyun.util.bean.PageUtils;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

/**
 * CMS 专栏后台管理 Controller
 * <p>
 * 提供专栏列表/详情/创建/更新/删除/审核（状态流转 draft→published→archived）。
 * 路径前缀 /cms/column。
 *
 * @author moyun
 */
@Tag(name = "CMS专栏管理", description = "专栏后台管理接口")
@RestController
@RequestMapping("/cms/column")
public class CmsColumnController extends BaseController {

    @Autowired
    private ICmsColumnService cmsColumnService;

    @Operation(summary = "查询专栏列表", description = "分页查询专栏（含所有状态、作者信息）")
    @PreAuthorize("@ss.hasPermi('portal:column:list')")
    @GetMapping("/list")
    public AjaxResult list(ColumnQuery query) {
        Page<ColumnListItemVO> page = PageUtils.startPage();
        cmsColumnService.selectColumnPage(page, query);
        return success(page);
    }

    @Operation(summary = "获取专栏详情", description = "根据ID获取专栏详情")
    @PreAuthorize("@ss.hasPermi('portal:column:query')")
    @GetMapping("/{id}")
    public AjaxResult getInfo(@PathVariable Long id) {
        return success(cmsColumnService.selectColumnById(id));
    }

    @Operation(summary = "新增专栏", description = "新增专栏")
    @PreAuthorize("@ss.hasPermi('portal:column:add')")
    @Log(title = "专栏", businessType = BusinessType.INSERT)
    @PostMapping
    public AjaxResult add(@Validated @RequestBody PortalColumn column) {
        return toAjax(cmsColumnService.insertColumn(column));
    }

    @Operation(summary = "修改专栏", description = "修改专栏")
    @PreAuthorize("@ss.hasPermi('portal:column:edit')")
    @Log(title = "专栏", businessType = BusinessType.UPDATE)
    @PutMapping
    public AjaxResult edit(@Validated @RequestBody PortalColumn column) {
        return toAjax(cmsColumnService.updateColumn(column));
    }

    @Operation(summary = "删除专栏", description = "批量删除专栏")
    @PreAuthorize("@ss.hasPermi('portal:column:remove')")
    @Log(title = "专栏", businessType = BusinessType.DELETE)
    @DeleteMapping("/{ids}")
    public AjaxResult remove(@PathVariable Long[] ids) {
        return toAjax(cmsColumnService.deleteColumnByIds(ids));
    }

    @Operation(summary = "更新专栏状态", description = "状态流转（archived 等普通流转，不走审核字段写入）")
    @PreAuthorize("@ss.hasPermi('portal:column:edit')")
    @Log(title = "专栏", businessType = BusinessType.UPDATE)
    @PutMapping("/{id}/status")
    public AjaxResult changeStatus(@PathVariable Long id, @RequestBody Map<String, Object> body) {
        String status = body.get("status") == null ? null : String.valueOf(body.get("status"));
        return toAjax(cmsColumnService.updateColumnStatus(id, status));
    }

    @Operation(summary = "分页查询专栏已绑定文章", description = "CMS后台：维护文章弹窗用，含作者昵称/用户名")
    @PreAuthorize("@ss.hasPermi('portal:column:edit')")
    @GetMapping("/{id}/articles")
    public AjaxResult listColumnArticles(@PathVariable("id") Long id,
                                         @RequestParam(value = "keyword", required = false) String keyword) {
        Page<ArticleSimpleVO> page = PageUtils.startPage();
        cmsColumnService.selectColumnArticlesPage(page, id, keyword);
        return success(page);
    }

    @Operation(summary = "批量绑定文章到专栏", description = "CMS后台：将选定文章加入专栏，自动跳过已绑定")
    @PreAuthorize("@ss.hasPermi('portal:column:edit')")
    @Log(title = "专栏-文章绑定", businessType = BusinessType.INSERT)
    @PostMapping("/{id}/articles")
    public AjaxResult bindArticles(@PathVariable("id") Long id, @RequestBody Map<String, Object> body) {
        Object articleIdsObj = body.get("articleIds");
        if (articleIdsObj == null) {
            return error("articleIds 不能为空");
        }
        @SuppressWarnings("unchecked")
        List<Integer> rawIds = (List<Integer>) articleIdsObj;
        List<Long> articleIds = new java.util.ArrayList<>(rawIds.size());
        for (Object o : rawIds) {
            if (o != null) {
                articleIds.add(Long.valueOf(String.valueOf(o)));
            }
        }
        return cmsColumnService.batchBindArticles(id, articleIds);
    }



    @Operation(summary = "将文章移出专栏", description = "CMS后台：维护文章弹窗移除按钮")
    @PreAuthorize("@ss.hasPermi('portal:column:edit')")
    @Log(title = "专栏-文章解绑", businessType = BusinessType.DELETE)
    @DeleteMapping("/{id}/articles/{articleId}")
    public AjaxResult removeColumnArticle(@PathVariable("id") Long id, @PathVariable Long articleId) {
        return toAjax(cmsColumnService.removeColumnArticle(id, articleId));
    }
}
