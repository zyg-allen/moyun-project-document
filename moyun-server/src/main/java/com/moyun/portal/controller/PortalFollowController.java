package com.moyun.portal.controller;

import jakarta.servlet.http.HttpServletResponse;
import java.util.List;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import com.moyun.common.annotation.Log;
import com.moyun.common.enums.BusinessType;
import com.moyun.core.base.AjaxResult;
import com.moyun.core.base.BaseController;
import com.moyun.portal.domain.entity.PortalFollow;
import com.moyun.portal.domain.query.FollowQuery;
import com.moyun.portal.service.IPortalFollowService;
import com.moyun.portal.util.PortalSecurityUtils;
import com.moyun.util.bean.PageUtils;
import com.moyun.util.file.ExcelUtil;

@Tag(name = "门户关注", description = "门户关注的增删改查操作接口")
@RestController
@RequestMapping("/portal/follow")
public class PortalFollowController extends BaseController {

    @Autowired
    private IPortalFollowService portalFollowService;

    @Operation(summary = "获取关注列表", description = "根据条件分页查询关注列表")
    @GetMapping("/list")
    public AjaxResult list(FollowQuery query) {
        Page<PortalFollow> page = PageUtils.buildPage(query);
        Page<PortalFollow> resultPage = portalFollowService.selectPortalFollowPage(page, query);
        return success(resultPage);
    }

    @Operation(summary = "导出关注", description = "导出关注数据到Excel文件")
    @Log(title = "门户关注", businessType = BusinessType.EXPORT)
    @PostMapping("/export")
    public void export(HttpServletResponse response, FollowQuery query) {
        List<PortalFollow> list = portalFollowService.selectPortalFollowList(query);
        ExcelUtil<PortalFollow> util = new ExcelUtil<PortalFollow>(PortalFollow.class);
        util.exportExcel(response, list, "门户关注数据");
    }

    @Operation(summary = "获取关注详情", description = "根据关注ID获取关注详细信息")
    @GetMapping(value = "/{id}")
    public AjaxResult getInfo(@Parameter(description = "关注ID") @PathVariable Long id) {
        return success(portalFollowService.selectPortalFollowById(id));
    }

    @Operation(summary = "新增关注", description = "创建新关注")
    @Log(title = "门户关注", businessType = BusinessType.INSERT)
    @PostMapping
    public AjaxResult add(@Validated @RequestBody PortalFollow portalFollow) {
        return toAjax(portalFollowService.insertPortalFollow(portalFollow));
    }

    @Operation(summary = "修改关注", description = "更新关注信息")
    @Log(title = "门户关注", businessType = BusinessType.UPDATE)
    @PutMapping
    public AjaxResult edit(@Validated @RequestBody PortalFollow portalFollow) {
        return toAjax(portalFollowService.updatePortalFollow(portalFollow));
    }

    @Operation(summary = "删除关注", description = "批量删除关注")
    @Log(title = "门户关注", businessType = BusinessType.DELETE)
    @DeleteMapping("/{ids}")
    public AjaxResult remove(@Parameter(description = "关注ID数组") @PathVariable Long[] ids) {
        return toAjax(portalFollowService.deletePortalFollowByIds(ids));
    }

    @Operation(summary = "关注/取消关注", description = "切换关注状态，同步更新粉丝数和关注数")
    @PostMapping("/toggle/{userId}")
    public AjaxResult toggleFollow(@Parameter(description = "被关注用户ID") @PathVariable Long userId) {
        Long currentUserId = PortalSecurityUtils.getUserId();
        if (currentUserId == null) {
            return error("请先登录");
        }
        return success(portalFollowService.toggleFollow(currentUserId, userId));
    }

    @Operation(summary = "检查是否已关注", description = "检查当前用户是否已关注目标用户")
    @GetMapping("/isFollowing/{userId}")
    public AjaxResult isFollowing(@Parameter(description = "目标用户ID") @PathVariable Long userId) {
        Long currentUserId = PortalSecurityUtils.getUserId();
        if (currentUserId == null) {
            java.util.Map<String, Object> result = new java.util.HashMap<>();
            result.put("isFollowing", false);
            return success(result);
        }
        boolean following = portalFollowService.isFollowing(currentUserId, userId);
        java.util.Map<String, Object> result = new java.util.HashMap<>();
        result.put("isFollowing", following);
        return success(result);
    }
}
