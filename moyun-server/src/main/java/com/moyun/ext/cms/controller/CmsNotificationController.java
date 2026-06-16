package com.moyun.ext.cms.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import com.moyun.common.annotation.Log;
import com.moyun.common.enums.BusinessType;
import com.moyun.core.base.AjaxResult;
import com.moyun.core.base.BaseController;
import com.moyun.ext.cms.domain.query.CmsNotificationQuery;
import com.moyun.ext.cms.domain.vo.CmsNotificationVO;
import com.moyun.ext.cms.service.ICmsNotificationService;
import com.moyun.portal.domain.entity.PortalNotification;
import com.moyun.util.bean.PageUtils;

/**
 * CMS通知管理Controller
 *
 * @author moyun
 */
@Tag(name = "CMS通知管理", description = "CMS通知管理接口")
@RestController
@RequestMapping("/cms/notification")
public class CmsNotificationController extends BaseController {
    @Autowired
    private ICmsNotificationService cmsNotificationService;

    /**
     * 获取通知列表
     */
    @Operation(summary = "获取通知列表", description = "根据条件分页查询通知列表")
    @PreAuthorize("@ss.hasPermi('cms:notification:list')")
    @GetMapping("/list")
    public AjaxResult list(CmsNotificationQuery query) {
        Page<CmsNotificationVO> page = PageUtils.buildPage(query);
        page = cmsNotificationService.selectNotificationPage(page, query);
        return success(page);
    }

    /**
     * 根据通知编号获取详细信息
     */
    @Operation(summary = "获取通知详情", description = "根据通知ID获取通知详细信息")
    @PreAuthorize("@ss.hasPermi('cms:notification:query')")
    @GetMapping(value = "/{id}")
    public AjaxResult getInfo(@Parameter(description = "通知ID") @PathVariable Long id) {
        return success(cmsNotificationService.selectNotificationById(id));
    }

    /**
     * 新增通知
     */
    @Operation(summary = "新增通知", description = "创建新通知")
    @PreAuthorize("@ss.hasPermi('cms:notification:add')")
    @Log(title = "通知管理", businessType = BusinessType.INSERT)
    @PostMapping
    public AjaxResult add(@Validated @RequestBody PortalNotification notification) {
        return toAjax(cmsNotificationService.insertNotification(notification));
    }

    /**
     * 发送系统通知给所有用户
     */
    @Operation(summary = "发送系统通知", description = "发送系统通知给所有用户")
    @PreAuthorize("@ss.hasPermi('cms:notification:add')")
    @Log(title = "通知管理", businessType = BusinessType.INSERT)
    @PostMapping("/send-all")
    public AjaxResult sendAll(@Validated @RequestBody PortalNotification notification) {
        return toAjax(cmsNotificationService.sendSystemNotification(notification));
    }

    /**
     * 修改通知
     */
    @Operation(summary = "修改通知", description = "更新通知信息")
    @PreAuthorize("@ss.hasPermi('cms:notification:edit')")
    @Log(title = "通知管理", businessType = BusinessType.UPDATE)
    @PutMapping
    public AjaxResult edit(@Validated @RequestBody PortalNotification notification) {
        return toAjax(cmsNotificationService.updateNotification(notification));
    }

    /**
     * 删除通知
     */
    @Operation(summary = "删除通知", description = "批量删除通知")
    @PreAuthorize("@ss.hasPermi('cms:notification:remove')")
    @Log(title = "通知管理", businessType = BusinessType.DELETE)
    @DeleteMapping("/{ids}")
    public AjaxResult remove(@Parameter(description = "通知ID数组") @PathVariable Long[] ids) {
        return toAjax(cmsNotificationService.deleteNotificationByIds(ids));
    }
}
