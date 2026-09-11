package com.moyun.ledger.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.moyun.ledger.domain.entity.LedgerScheduleLog;
import com.moyun.ledger.domain.entity.LedgerScheduleTask;

import java.util.List;
import java.util.Map;

/**
 * 定时记账服务
 *
 * <p>周期：daily=每天；weekly=每周几（dayOfWeek 1-7）；
 * monthly=每月几号（dayOfMonth 1-28）；interval=每N天（intervalDays）。
 * 执行：到期调用记账服务生成流水并写执行日志；失败留痕支持手动重试。
 *
 * @author moyun
 */
public interface ILedgerScheduleService extends IService<LedgerScheduleTask> {

    /** 新建任务（自动计算 nextExecDate） */
    Long createTask(Long userId, LedgerScheduleTask task);

    /** 修改任务（重算 nextExecDate） */
    void updateTask(Long userId, Long id, LedgerScheduleTask task);

    /** 启用/停用 */
    void toggleEnabled(Long userId, Long id);

    /** 删除（逻辑删） */
    void deleteTask(Long userId, Long id);

    /** 任务列表（含分类名/账户名冗余展示字段 + 汇总） */
    Map<String, Object> listTasks(Long userId);

    /** 任务执行日志（倒序） */
    List<LedgerScheduleLog> listLogs(Long userId, Long taskId);

    /** 手动执行一次（立即按 nextExecDate 记账并推进） */
    Map<String, Object> runNow(Long userId, Long id);

    /** 重试失败日志（按日志日期补记一笔） */
    void retryLog(Long userId, Long logId);

    /**
     * 扫描并执行所有到期任务（定时任务调用，全用户）
     *
     * @return {执行数, 成功数, 失败数}
     */
    Map<String, Integer> runDueTasks();
}