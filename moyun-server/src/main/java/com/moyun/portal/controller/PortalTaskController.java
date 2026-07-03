package com.moyun.portal.controller;

import com.moyun.common.annotation.Anonymous;
import com.moyun.common.constant.HttpStatus;
import com.moyun.core.base.AjaxResult;
import com.moyun.core.base.BaseController;
import com.moyun.ext.cms.service.ITaskService;
import com.moyun.portal.util.PortalSecurityUtils;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

/**
 * 任务系统 Controller（门户端，阶段四 4.4）
 *
 * 公开接口：任务列表（未登录返回进度 0）；其余接口需登录。
 *
 * @author moyun
 */
@Tag(name = "任务系统", description = "任务列表、进度、领取奖励")
@RestController
@RequestMapping("/portal/task")
public class PortalTaskController extends BaseController {

    @Autowired
    private ITaskService taskService;

    private Long currentUserId() {
        return PortalSecurityUtils.getUserId();
    }

    @Operation(summary = "任务列表", description = "返回所有启用的任务（含当前用户进度，未登录时进度为 0）")
    @GetMapping("/list")
    @Anonymous
    public AjaxResult list() {
        return AjaxResult.success(taskService.listTasks(currentUserId()));
    }

    @Operation(summary = "我的任务", description = "返回当前用户任务进度（自动初始化当日 daily 任务）")
    @GetMapping("/my")
    public AjaxResult myTasks() {
        Long userId = currentUserId();
        if (userId == null) {
            return AjaxResult.error(HttpStatus.UNAUTHORIZED, "登录已过期，请重新登录");
        }
        return AjaxResult.success(taskService.myTasks(userId));
    }

    @Operation(summary = "领取任务奖励", description = "完成每日/成就任务后领取积分奖励")
    @PostMapping("/claim/{userTaskId}")
    public AjaxResult claim(@PathVariable("userTaskId") Long userTaskId) {
        Long userId = currentUserId();
        if (userId == null) {
            return AjaxResult.error(HttpStatus.UNAUTHORIZED, "登录已过期，请重新登录");
        }
        int points = taskService.claimReward(userId, userTaskId);
        return AjaxResult.success(points);
    }
}
