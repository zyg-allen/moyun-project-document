package com.moyun.system.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.moyun.common.annotation.Log;
import com.moyun.common.enums.BusinessType;
import com.moyun.core.base.AjaxResult;
import com.moyun.core.base.BaseController;
import com.moyun.core.base.TableDataInfo;
import com.moyun.system.domain.entity.SysNotification;
import com.moyun.system.service.ISysNotificationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 通知公告 信息操作处理
 * 合并后内部改用 sys_notification 表（scope=all 广播）
 * 保留原 /system/notice 路径和权限点，对 RuoYi 框架透明
 *
 * @author moyun
 */
@Tag(name = "通知公告管理", description = "通知公告的增删改查操作接口")
@RestController
@RequestMapping("/system/notice")
public class SysNoticeController extends BaseController {

    @Autowired
    private ISysNotificationService sysNotificationService;

    /**
     * 获取通知公告列表
     */
    @Operation(summary = "获取通知公告列表", description = "分页查询通知公告列表（scope=all 广播）")
    @PreAuthorize("@ss.hasPermi('system:notice:list')")
    @GetMapping("/list")
    public TableDataInfo list(SysNotification query) {
        // 系统公告默认查询广播通知
        query.setScope("all");
        Page<SysNotification> page = startPage();
        page = sysNotificationService.selectNotificationPage(page, query);
        return getDataTable(page.getRecords(), page.getTotal());
    }

    /**
     * 获取首页顶部公告列表
     */
    @Operation(summary = "获取首页顶部公告列表", description = "获取首页顶部展示的公告列表（scope=all，status=0）")
    @GetMapping("/listTop")
    public AjaxResult listTop() {
        SysNotification query = new SysNotification();
        query.setScope("all");
        query.setStatus("0");
        List<SysNotification> list = sysNotificationService.selectNotificationList(query);
        return success(list);
    }

    /**
     * 根据通知公告编号获取详细信息
     */
    @Operation(summary = "获取通知公告详情", description = "根据通知公告ID获取详细信息")
    @PreAuthorize("@ss.hasPermi('system:notice:query')")
    @GetMapping(value = "/{noticeId}")
    public AjaxResult getInfo(@Parameter(description = "通知公告ID") @PathVariable Long noticeId) {
        return success(sysNotificationService.selectNotificationById(noticeId));
    }

    /**
     * 新增通知公告
     */
    @Operation(summary = "新增通知公告", description = "创建新通知公告（scope=all 广播）")
    @PreAuthorize("@ss.hasPermi('system:notice:add')")
    @Log(title = "通知公告", businessType = BusinessType.INSERT)
    @PostMapping
    public AjaxResult add(@Validated @RequestBody SysNotification notification) {
        notification.setCreateBy(getUsername());
        // 系统公告强制为广播
        notification.setScope("all");
        notification.setUserId(null);
        if (notification.getType() == null) {
            notification.setType(notification.getNoticeType() != null && "2".equals(notification.getNoticeType())
                    ? "announcement" : "notice");
        }
        return toAjax(sysNotificationService.insertNotification(notification));
    }

    /**
     * 修改通知公告
     */
    @Operation(summary = "修改通知公告", description = "更新通知公告信息")
    @PreAuthorize("@ss.hasPermi('system:notice:edit')")
    @Log(title = "通知公告", businessType = BusinessType.UPDATE)
    @PutMapping
    public AjaxResult edit(@Validated @RequestBody SysNotification notification) {
        notification.setUpdateBy(getUsername());
        return toAjax(sysNotificationService.updateNotification(notification));
    }

    /**
     * 删除通知公告
     */
    @Operation(summary = "删除通知公告", description = "批量删除通知公告")
    @PreAuthorize("@ss.hasPermi('system:notice:remove')")
    @Log(title = "通知公告", businessType = BusinessType.DELETE)
    @DeleteMapping("/{noticeIds}")
    public AjaxResult remove(@Parameter(description = "通知公告ID数组") @PathVariable Long[] noticeIds) {
        return toAjax(sysNotificationService.deleteNotificationByIds(noticeIds));
    }
}
