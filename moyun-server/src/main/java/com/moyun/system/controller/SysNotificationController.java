package com.moyun.system.controller;

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
import com.moyun.system.domain.entity.SysNotification;
import com.moyun.system.service.ISysNotificationService;

/**
 * 系统通知管理 Controller（后台台账）
 *
 * <p>原 {@code com.moyun.ext.cms.controller.CmsNotificationController}（/cms/notification，
 * cms:notification:*）已迁移至此：通知是全局系统级能力，不应归属 CMS 业务模块。</p>
 *
 * <p>归属调整：
 * <ul>
 *   <li>包：cms.controller → system.controller（与 SysUserController 等同级）</li>
 *   <li>路径：/cms/notification → /system/notification</li>
 *   <li>权限码：cms:notification:* → system:notification:*</li>
 *   <li>菜单归属：内容管理(CMS) → 系统管理</li>
 * </ul>
 * </p>
 *
 * <p>前后台权限区分：
 * <ul>
 *   <li>后台台账（本类，/system/notification，system:notification:*）：管理员管理全部通知记录、群发广播</li>
 *   <li>后台收件箱（{@link com.moyun.ext.cms.controller.SysNotificationInboxController}，
 *       /system/notification/inbox，system:notification:list）：管理员查看发给自己的通知</li>
 *   <li>门户查询（{@link com.moyun.portal.controller.PortalNotificationController}，
 *       /portal/notification）：门户用户查看自己的通知，走 portal 鉴权体系</li>
 * </ul>
 * 三者共用 {@link ISysNotificationService} 与 sys_notification 表，通过 user_type 区分。
 * </p>
 *
 * @author moyun
 */
@Tag(name = "系统通知管理", description = "系统通知台账管理接口（全局）")
@RestController
@RequestMapping("/system/notification")
public class SysNotificationController extends BaseController {

    @Autowired
    private ISysNotificationService sysNotificationService;

    /**
     * 获取通知列表（台账：全部记录）
     */
    @Operation(summary = "获取通知列表", description = "根据条件分页查询通知台账（全部记录，含发给门户/系统的）")
    @PreAuthorize("@ss.hasPermi('system:notification:list')")
    @GetMapping("/list")
    public AjaxResult list(SysNotification query) {
        Page<SysNotification> page = startPage();
        page = sysNotificationService.selectNotificationPage(page, query);
        return success(page);
    }

    /**
     * 根据通知编号获取详细信息
     */
    @Operation(summary = "获取通知详情", description = "根据通知ID获取通知详细信息")
    @PreAuthorize("@ss.hasPermi('system:notification:query')")
    @GetMapping(value = "/{id}")
    public AjaxResult getInfo(@Parameter(description = "通知ID") @PathVariable("id") Long id) {
        return success(sysNotificationService.selectNotificationById(id));
    }

    /**
     * 新增通知（个人通知：scope=user，需指定 userId 和 userType）
     * userType: portal=门户用户 / sys=系统用户
     */
    @Operation(summary = "新增通知", description = "创建新通知（scope=user 为个人通知，需指定 userId 和 userType）")
    @PreAuthorize("@ss.hasPermi('system:notification:add')")
    @Log(title = "通知管理", businessType = BusinessType.INSERT)
    @PostMapping
    public AjaxResult add(@Validated @RequestBody SysNotification notification) {
        // 默认为个人通知
        if (notification.getScope() == null) {
            notification.setScope("user");
        }
        // 个人通知默认发给门户用户（兼容旧调用方未传 userType 的情况）
        if ("user".equals(notification.getScope())
                && (notification.getUserType() == null || notification.getUserType().isEmpty())) {
            notification.setUserType("portal");
        }
        return toAjax(sysNotificationService.insertNotification(notification));
    }

    /**
     * 发送广播通知（scope=all，全局广播，单条记录）
     * 替代原逐条 insert 群发逻辑，广播通知只存一条主体记录
     */
    @Operation(summary = "发送广播通知", description = "群发系统通知给所有用户（scope=all，单条记录，已读按需记录）")
    @PreAuthorize("@ss.hasPermi('system:notification:add')")
    @Log(title = "通知管理", businessType = BusinessType.INSERT)
    @PostMapping("/send-all")
    public AjaxResult sendAll(@Validated @RequestBody SysNotification notification) {
        return toAjax(sysNotificationService.sendBroadcastNotification(notification));
    }

    /**
     * 修改通知
     */
    @Operation(summary = "修改通知", description = "更新通知信息")
    @PreAuthorize("@ss.hasPermi('system:notification:edit')")
    @Log(title = "通知管理", businessType = BusinessType.UPDATE)
    @PutMapping
    public AjaxResult edit(@Validated @RequestBody SysNotification notification) {
        return toAjax(sysNotificationService.updateNotification(notification));
    }

    /**
     * 删除通知
     */
    @Operation(summary = "删除通知", description = "批量删除通知")
    @PreAuthorize("@ss.hasPermi('system:notification:remove')")
    @Log(title = "通知管理", businessType = BusinessType.DELETE)
    @DeleteMapping("/{ids}")
    public AjaxResult remove(@Parameter(description = "通知ID数组") @PathVariable("ids") Long[] ids) {
        return toAjax(sysNotificationService.deleteNotificationByIds(ids));
    }
}
