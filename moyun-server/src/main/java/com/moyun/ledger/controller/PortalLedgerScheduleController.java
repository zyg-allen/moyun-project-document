package com.moyun.ledger.controller;

import com.moyun.core.base.AjaxResult;
import com.moyun.ledger.domain.entity.LedgerScheduleTask;
import com.moyun.ledger.service.ILedgerScheduleService;
import com.moyun.portal.util.PortalSecurityUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * 门户记账-定时记账控制器
 *
 * <p>定时列表 + 添加；到期由后台任务自动记账，失败日志支持手动重试。
 *
 * @author moyun
 */
@RestController
@RequestMapping("/portal/ledger/schedules")
public class PortalLedgerScheduleController {

    @Autowired
    private ILedgerScheduleService scheduleService;

    /** 任务列表（含分类名/账户名 + 启用统计） */
    @GetMapping
    public AjaxResult list() {
        Long userId = PortalSecurityUtils.getUserId();
        return AjaxResult.success(scheduleService.listTasks(userId));
    }

    /** 执行日志（taskId 可选=全部） */
    @GetMapping("/logs")
    public AjaxResult logs(@RequestParam(value = "taskId", required = false) Long taskId) {
        Long userId = PortalSecurityUtils.getUserId();
        return AjaxResult.success(Map.of("records", scheduleService.listLogs(userId, taskId)));
    }

    /** 新建任务 */
    @PostMapping
    public AjaxResult create(@RequestBody LedgerScheduleTask task) {
        Long userId = PortalSecurityUtils.getUserId();
        Long id = scheduleService.createTask(userId, task);
        return AjaxResult.success("创建成功", Map.of("id", id));
    }

    /** 修改任务 */
    @PutMapping("/{id}")
    public AjaxResult update(@PathVariable("id") Long id, @RequestBody LedgerScheduleTask task) {
        Long userId = PortalSecurityUtils.getUserId();
        scheduleService.updateTask(userId, id, task);
        return AjaxResult.success("已保存");
    }

    /** 启用/停用 */
    @PostMapping("/{id}/toggle")
    public AjaxResult toggle(@PathVariable("id") Long id) {
        Long userId = PortalSecurityUtils.getUserId();
        scheduleService.toggleEnabled(userId, id);
        return AjaxResult.success("已更新");
    }

    /** 立即执行一次（按下次执行日记账） */
    @PostMapping("/{id}/run")
    public AjaxResult run(@PathVariable("id") Long id) {
        Long userId = PortalSecurityUtils.getUserId();
        Map<String, Object> result = scheduleService.runNow(userId, id);
        return Boolean.TRUE.equals(result.get("success"))
                ? AjaxResult.success("执行成功，已生成流水", result)
                : AjaxResult.error("执行失败：" + result.get("failReason"), result);
    }

    /** 重试失败日志 */
    @PostMapping("/logs/{logId}/retry")
    public AjaxResult retry(@PathVariable("logId") Long logId) {
        Long userId = PortalSecurityUtils.getUserId();
        scheduleService.retryLog(userId, logId);
        return AjaxResult.success("重试成功");
    }

    /** 删除任务（逻辑删） */
    @DeleteMapping("/{id}")
    public AjaxResult delete(@PathVariable("id") Long id) {
        Long userId = PortalSecurityUtils.getUserId();
        scheduleService.deleteTask(userId, id);
        return AjaxResult.success("已删除");
    }
}