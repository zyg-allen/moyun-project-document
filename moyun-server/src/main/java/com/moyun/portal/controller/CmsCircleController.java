package com.moyun.portal.controller;

import com.moyun.common.annotation.Log;
import com.moyun.common.enums.BusinessType;
import com.moyun.core.base.AjaxResult;
import com.moyun.core.base.BaseController;
import com.moyun.core.base.page.PageDomain;
import com.moyun.ext.cms.domain.query.CircleQuery;
import com.moyun.ext.cms.service.ICircleService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * 圈子/兴趣小组 后台 Controller（社交深化与商业化 4.1）
 *
 * <p>供后台管理页面调用，提供圈子审核/删除、帖子管理能力。</p>
 *
 * <p>路径前缀 /cms/circle，权限标识 portal:circle:list / portal:circle:audit / portal:circle:remove。</p>
 *
 * @author moyun
 */
@Tag(name = "圈子管理", description = "圈子后台审核与管理接口")
@RestController
@RequestMapping("/cms/circle")
public class CmsCircleController extends BaseController {

    @Autowired
    private ICircleService circleService;

    @Operation(summary = "圈子分页列表", description = "分页查询圈子，支持按状态、分类、关键词筛选")
    @PreAuthorize("@ss.hasPermi('portal:circle:list')")
    @GetMapping("/list")
    public AjaxResult list(CircleQuery query) {
        return AjaxResult.success(circleService.cmsListCircles(query));
    }

    @Operation(summary = "审核圈子", description = "启用/禁用圈子，status=active/disabled")
    @PreAuthorize("@ss.hasPermi('portal:circle:audit')")
    @Log(title = "圈子审核", businessType = BusinessType.UPDATE)
    @PutMapping("/{id}/audit")
    public AjaxResult audit(@PathVariable Long id, @RequestBody Map<String, Object> body) {
        String status = body.get("status") == null ? null : String.valueOf(body.get("status"));
        try {
            return AjaxResult.success(circleService.cmsAuditCircle(id, status));
        } catch (RuntimeException e) {
            return AjaxResult.error(e.getMessage());
        }
    }

    @Operation(summary = "删除圈子", description = "后台删除圈子（级联删除成员与帖子）")
    @PreAuthorize("@ss.hasPermi('portal:circle:remove')")
    @Log(title = "圈子删除", businessType = BusinessType.DELETE)
    @DeleteMapping("/{id}")
    public AjaxResult delete(@PathVariable Long id) {
        return AjaxResult.success(circleService.cmsDeleteCircle(id));
    }

    // ==================== 帖子管理 ====================

    @Operation(summary = "帖子分页列表", description = "分页查询圈子帖子，支持按圈子ID、关键词、状态筛选")
    @PreAuthorize("@ss.hasPermi('portal:circlePost:list')")
    @GetMapping("/post/list")
    public AjaxResult postList(@RequestParam(required = false) Long circleId,
                               @RequestParam(required = false) String keyword,
                               @RequestParam(required = false) String status,
                               PageDomain query) {
        return AjaxResult.success(circleService.cmsListPosts(circleId, keyword, status, query));
    }

    @Operation(summary = "删除帖子", description = "后台删除圈子帖子")
    @PreAuthorize("@ss.hasPermi('portal:circlePost:remove')")
    @Log(title = "圈子帖子删除", businessType = BusinessType.DELETE)
    @DeleteMapping("/post/{id}")
    public AjaxResult deletePost(@PathVariable Long id) {
        return AjaxResult.success(circleService.cmsDeletePost(id));
    }
}
