package com.moyun.community.content.controller.app;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.moyun.common.utils.SecurityUtils;
import com.moyun.community.common.domain.ApiResponse;
import com.moyun.community.common.domain.PageResult;
import com.moyun.community.content.entity.CmsNotification;
import com.moyun.community.content.mapper.CmsNotificationMapper;
import com.moyun.community.content.model.vo.PortalNotificationVo;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

/**
 * 前台通知 Controller
 *
 * @author moyun
 */
@Tag(name = "通知模块", description = "通知管理接口")
@RestController
@RequestMapping("/api/notifications")
@RequiredArgsConstructor
public class PortalNotificationController {

    private final CmsNotificationMapper notificationMapper;

    /**
     * 获取通知列表
     */
    @Operation(summary = "获取通知列表", description = "分页获取当前用户的通知列表")
    @GetMapping
    public ApiResponse<PageResult<PortalNotificationVo>> getNotificationList(
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "20") Integer pageSize) {

        Long userId = SecurityUtils.getUserId();
        if (userId == null) {
            return ApiResponse.error(401, "未登录");
        }

        Page<CmsNotification> pageObj = new Page<>(page, pageSize);
        LambdaQueryWrapper<CmsNotification> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(CmsNotification::getUserId, userId);
        wrapper.orderByDesc(CmsNotification::getCreateTime);

        Page<CmsNotification> resultPage = notificationMapper.selectPage(pageObj, wrapper);

        List<PortalNotificationVo> voList = new ArrayList<>();
        for (CmsNotification notification : resultPage.getRecords()) {
            PortalNotificationVo vo = new PortalNotificationVo();
            vo.setId(notification.getNotificationId());
            vo.setUserId(notification.getUserId());
            vo.setType(notification.getType());
            vo.setTitle(notification.getTitle());
            vo.setContent(notification.getContent());
            vo.setIsRead(notification.getIsRead() != null);
            vo.setRelatedId(notification.getRelatedId());
            vo.setCreatedAt(notification.getCreateTime() != null 
                    ? notification.getCreateTime().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME) 
                    : null);
            voList.add(vo);
        }

        PageResult<PortalNotificationVo> pageResult = PageResult.of(
                voList,
                resultPage.getTotal(),
                page,
                pageSize
        );

        return ApiResponse.success(pageResult);
    }

    /**
     * 获取未读通知数量
     */
    @Operation(summary = "获取未读数量", description = "获取当前用户未读通知数量")
    @GetMapping("/unread-count")
    public ApiResponse<Integer> getUnreadCount() {
        Long userId = SecurityUtils.getUserId();
        if (userId == null) {
            return ApiResponse.success(0);
        }

        LambdaQueryWrapper<CmsNotification> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(CmsNotification::getUserId, userId);
        wrapper.eq(CmsNotification::getIsRead, false);

        long count = notificationMapper.selectCount(wrapper);

        return ApiResponse.success((int) count);
    }

    /**
     * 标记全部已读
     */
    @Operation(summary = "标记全部已读", description = "将当前用户的所有通知标记为已读")
    @PostMapping("/mark-all-read")
    public ApiResponse<Void> markAllRead() {
        Long userId = SecurityUtils.getUserId();
        if (userId == null) {
            return ApiResponse.error(401, "未登录");
        }

        // 更新所有未读通知为已读
        LambdaQueryWrapper<CmsNotification> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(CmsNotification::getUserId, userId);
        wrapper.eq(CmsNotification::getIsRead, false);

        // 实际项目中应该批量更新，这里简化处理
        List<CmsNotification> notifications = notificationMapper.selectList(wrapper);
        for (CmsNotification notification : notifications) {
            notification.setIsRead(1);
            notificationMapper.updateById(notification);
        }

        return ApiResponse.success();
    }

    /**
     * 标记单个通知已读
     */
    @Operation(summary = "标记单个已读", description = "将指定的通知标记为已读")
    @PostMapping("/{notificationId}/read")
    public ApiResponse<Void> markAsRead(@PathVariable Long notificationId) {
        Long userId = SecurityUtils.getUserId();
        if (userId == null) {
            return ApiResponse.error(401, "未登录");
        }

        CmsNotification notification = notificationMapper.selectById(notificationId);
        if (notification == null) {
            return ApiResponse.error(404, "通知不存在");
        }

        if (!notification.getUserId().equals(userId)) {
            return ApiResponse.error(403, "无权操作此通知");
        }

        notification.setIsRead(1);
        notificationMapper.updateById(notification);

        return ApiResponse.success();
    }

    /**
     * 删除通知
     */
    @Operation(summary = "删除通知", description = "删除指定的通知")
    @DeleteMapping("/{notificationId}")
    public ApiResponse<Void> deleteNotification(@PathVariable Long notificationId) {
        Long userId = SecurityUtils.getUserId();
        if (userId == null) {
            return ApiResponse.error(401, "未登录");
        }

        CmsNotification notification = notificationMapper.selectById(notificationId);
        if (notification == null) {
            return ApiResponse.error(404, "通知不存在");
        }

        if (!notification.getUserId().equals(userId)) {
            return ApiResponse.error(403, "无权删除此通知");
        }

        notificationMapper.deleteById(notificationId);

        return ApiResponse.success();
    }
}
