package com.moyun.ext.cms.controller;

import java.util.Map;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.moyun.common.annotation.Log;
import com.moyun.common.enums.BusinessType;
import com.moyun.core.base.AjaxResult;
import com.moyun.core.base.BaseController;
import com.moyun.ext.cms.domain.query.ColumnQuery;
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
    @PreAuthorize("@ss.hasAnyPermi('portal:column:list,cms:column:audit')")
    @GetMapping("/list")
    public AjaxResult list(ColumnQuery query) {
        Page<ColumnListItemVO> page = PageUtils.startPage();
        cmsColumnService.selectColumnPage(page, query);
        return success(page);
    }

    @Operation(summary = "获取专栏详情", description = "根据ID获取专栏详情")
    @PreAuthorize("@ss.hasAnyPermi('portal:column:query,cms:column:audit')")
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

    @Operation(summary = "审核专栏", description = "审核待处理专栏：published=通过 / rejected=驳回，支持审核意见，结果通知作者")
    @PreAuthorize("@ss.hasPermi('cms:column:audit')")
    @Log(title = "专栏审核", businessType = BusinessType.UPDATE)
    @PutMapping("/{id}/audit")
    public AjaxResult audit(@PathVariable Long id, @RequestBody Map<String, Object> body) {
        String status = body.get("status") == null ? null : String.valueOf(body.get("status"));
        String auditRemark = body.get("auditRemark") == null ? null : String.valueOf(body.get("auditRemark"));
        Long auditorId = getUserId();
        try {
            cmsColumnService.auditColumn(id, status, auditRemark, auditorId);
            return success();
        } catch (RuntimeException e) {
            return error(e.getMessage() != null ? e.getMessage() : "审核失败");
        }
    }
}
