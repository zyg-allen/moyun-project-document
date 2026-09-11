package com.moyun.ledger.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.moyun.common.exception.system.ServiceException;
import com.moyun.ledger.domain.dto.TransactionCreateDTO;
import com.moyun.ledger.domain.entity.LedgerAssetAccount;
import com.moyun.ledger.domain.entity.LedgerCategory;
import com.moyun.ledger.domain.entity.LedgerScheduleLog;
import com.moyun.ledger.domain.entity.LedgerScheduleTask;
import com.moyun.ledger.mapper.LedgerAssetAccountMapper;
import com.moyun.ledger.mapper.LedgerCategoryMapper;
import com.moyun.ledger.mapper.LedgerScheduleLogMapper;
import com.moyun.ledger.mapper.LedgerScheduleTaskMapper;
import com.moyun.ledger.service.ILedgerScheduleService;
import com.moyun.ledger.service.ILedgerTransactionService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 定时记账服务实现
 *
 * <p>执行链路：到期任务 → ILedgerTransactionService.createTransaction（复用余额联动/净资产快照逻辑）
 * → 写执行日志 → 推进 nextExecDate → end_date 到期自动停用。
 * 失败（如余额不足）不推进重试（避免反复扣减），留痕 ledger_schedule_log 由用户手动重试。
 *
 * @author moyun
 */
@Service
public class LedgerScheduleServiceImpl extends ServiceImpl<LedgerScheduleTaskMapper, LedgerScheduleTask>
        implements ILedgerScheduleService {

    private static final Logger log = LoggerFactory.getLogger(LedgerScheduleServiceImpl.class);

    @Autowired
    private LedgerScheduleLogMapper logMapper;

    @Autowired
    private LedgerCategoryMapper categoryMapper;

    @Autowired
    private LedgerAssetAccountMapper assetAccountMapper;

    @Autowired
    private ILedgerTransactionService transactionService;

    @Override
    public Long createTask(Long userId, LedgerScheduleTask task) {
        validate(task);
        task.setUserId(userId);
        task.setStatus(LedgerScheduleTask.STATUS_NORMAL);
        if (task.getEnabled() == null) {
            task.setEnabled(1);
        }
        if (task.getExecTime() == null || task.getExecTime().isBlank()) {
            task.setExecTime("08:00");
        }
        if (task.getStartDate() == null) {
            task.setStartDate(LocalDate.now());
        }
        // 下次执行日：从开始日期起，按周期推算的第一个执行日
        task.setNextExecDate(firstExecDate(task, task.getStartDate()));
        this.save(task);
        return task.getId();
    }

    @Override
    public void updateTask(Long userId, Long id, LedgerScheduleTask task) {
        LedgerScheduleTask db = getOwned(userId, id);
        validate(task);
        db.setName(task.getName());
        db.setType(task.getType());
        db.setAmount(task.getAmount());
        db.setCategoryId(task.getCategoryId());
        db.setAccountId(task.getAccountId());
        db.setDescription(task.getDescription());
        db.setCycle(task.getCycle());
        db.setDayOfWeek(task.getDayOfWeek());
        db.setDayOfMonth(task.getDayOfMonth());
        db.setIntervalDays(task.getIntervalDays());
        db.setExecTime(task.getExecTime());
        db.setStartDate(task.getStartDate());
        db.setEndDate(task.getEndDate());
        db.setNextExecDate(firstExecDate(task, task.getStartDate()));
        this.updateById(db);
    }

    @Override
    public void toggleEnabled(Long userId, Long id) {
        LedgerScheduleTask db = getOwned(userId, id);
        db.setEnabled(db.getEnabled() != null && db.getEnabled() == 1 ? 0 : 1);
        // 重新启用时重算下次执行日（避免停留在过去的日期立即补记）
        if (db.getEnabled() == 1) {
            db.setNextExecDate(firstExecDate(db, LocalDate.now()));
        }
        this.updateById(db);
    }

    @Override
    public void deleteTask(Long userId, Long id) {
        LedgerScheduleTask db = getOwned(userId, id);
        db.setStatus(LedgerScheduleTask.STATUS_DELETED);
        this.updateById(db);
    }

    @Override
    public Map<String, Object> listTasks(Long userId) {
        LambdaQueryWrapper<LedgerScheduleTask> q = new LambdaQueryWrapper<>();
        q.eq(LedgerScheduleTask::getUserId, userId)
                .eq(LedgerScheduleTask::getStatus, LedgerScheduleTask.STATUS_NORMAL)
                .orderByDesc(LedgerScheduleTask::getEnabled)
                .orderByAsc(LedgerScheduleTask::getNextExecDate);
        List<LedgerScheduleTask> tasks = this.list(q);
        fillNames(tasks);

        int enabledCount = 0;
        for (LedgerScheduleTask t : tasks) {
            if (t.getEnabled() != null && t.getEnabled() == 1) {
                enabledCount++;
            }
        }
        Map<String, Object> result = new HashMap<>();
        result.put("tasks", tasks);
        result.put("enabledCount", enabledCount);
        result.put("totalCount", tasks.size());
        return result;
    }

    @Override
    public List<LedgerScheduleLog> listLogs(Long userId, Long taskId) {
        LambdaQueryWrapper<LedgerScheduleLog> q = new LambdaQueryWrapper<>();
        q.eq(LedgerScheduleLog::getUserId, userId);
        if (taskId != null && taskId > 0) {
            q.eq(LedgerScheduleLog::getTaskId, taskId);
        }
        q.orderByDesc(LedgerScheduleLog::getExecDate).orderByDesc(LedgerScheduleLog::getId);
        q.last("LIMIT 50");
        return logMapper.selectList(q);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Map<String, Object> runNow(Long userId, Long id) {
        LedgerScheduleTask task = getOwned(userId, id);
        if (task.getNextExecDate() == null) {
            throw new ServiceException("任务无可执行日期");
        }
        // v11.35.2：立即执行 = 现在记这笔账，交易日期用今天。
        // nextExecDate 可能是未来日期（如明天开始的周期），若沿用会导致流水落在未来，
        // 本月收支统计（[月初,今天]）不包含它，出现"列表可见但总支出不统计"。
        Map<String, Object> result = executeOnce(task, LocalDate.now());
        this.updateById(task);
        return result;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void retryLog(Long userId, Long logId) {
        LedgerScheduleLog scheduleLog = logMapper.selectById(logId);
        if (scheduleLog == null || !userId.equals(scheduleLog.getUserId())) {
            throw new ServiceException("执行日志不存在");
        }
        if (LedgerScheduleLog.STATUS_SUCCESS == scheduleLog.getStatus()) {
            throw new ServiceException("该日志已成功，无需重试");
        }
        LedgerScheduleTask task = this.getById(scheduleLog.getTaskId());
        if (task == null || !userId.equals(task.getUserId())) {
            throw new ServiceException("关联任务不存在");
        }
        Long txId;
        try {
            txId = transactionService.createTransaction(userId, buildDto(task, scheduleLog.getExecDate()));
        } catch (Exception e) {
            scheduleLog.setRetryCount((scheduleLog.getRetryCount() == null ? 0 : scheduleLog.getRetryCount()) + 1);
            scheduleLog.setFailReason(truncate(e.getMessage()));
            logMapper.updateById(scheduleLog);
            throw new ServiceException("重试失败：" + e.getMessage());
        }
        scheduleLog.setStatus(LedgerScheduleLog.STATUS_SUCCESS);
        scheduleLog.setTransactionId(txId);
        scheduleLog.setRetryCount((scheduleLog.getRetryCount() == null ? 0 : scheduleLog.getRetryCount()) + 1);
        scheduleLog.setFailReason(null);
        logMapper.updateById(scheduleLog);
    }

    @Override
    public Map<String, Integer> runDueTasks() {
        LocalDate today = LocalDate.now();
        LambdaQueryWrapper<LedgerScheduleTask> q = new LambdaQueryWrapper<>();
        q.eq(LedgerScheduleTask::getEnabled, 1)
                .eq(LedgerScheduleTask::getStatus, LedgerScheduleTask.STATUS_NORMAL)
                .isNotNull(LedgerScheduleTask::getNextExecDate)
                .le(LedgerScheduleTask::getNextExecDate, today);
        List<LedgerScheduleTask> dueTasks = this.list(q);
        int success = 0;
        int fail = 0;
        for (LedgerScheduleTask task : dueTasks) {
            try {
                // 每个任务独立事务边界由 executeOnce 内部记账服务保证；此处逐个捕获，单任务失败不影响其他任务
                Map<String, Object> r = executeOnce(task, null);
                this.updateById(task);
                if (Boolean.TRUE.equals(r.get("success"))) {
                    success++;
                } else {
                    fail++;
                }
            } catch (Exception e) {
                fail++;
                log.error("定时记账执行异常 taskId={}", task.getId(), e);
            }
        }
        Map<String, Integer> stats = new HashMap<>();
        stats.put("executed", dueTasks.size());
        stats.put("success", success);
        stats.put("fail", fail);
        return stats;
    }

    /**
     * 执行单个任务一次：记账 + 写日志 + 推进 nextExecDate + 到期停用
     * 注意：task 对象会被就地修改（nextExecDate/enabled），调用方负责落库
     *
     * @param forcedExecDate 强制记账日期：runNow 立即执行传今天；runDueTasks 定时到期传 null（用 nextExecDate 真实到期日）
     */
    private Map<String, Object> executeOnce(LedgerScheduleTask task, LocalDate forcedExecDate) {
        LocalDate execDate = forcedExecDate != null ? forcedExecDate : task.getNextExecDate();
        Map<String, Object> result = new HashMap<>();
        LedgerScheduleLog scheduleLog = new LedgerScheduleLog();
        scheduleLog.setTaskId(task.getId());
        scheduleLog.setUserId(task.getUserId());
        scheduleLog.setExecDate(execDate);
        scheduleLog.setAmount(task.getAmount());
        try {
            Long txId = transactionService.createTransaction(task.getUserId(), buildDto(task, execDate));
            scheduleLog.setStatus(LedgerScheduleLog.STATUS_SUCCESS);
            scheduleLog.setTransactionId(txId);
            result.put("success", true);
            result.put("transactionId", txId);
        } catch (Exception e) {
            scheduleLog.setStatus(LedgerScheduleLog.STATUS_FAILED);
            scheduleLog.setFailReason(truncate(e.getMessage()));
            result.put("success", false);
            result.put("failReason", e.getMessage());
        }
        scheduleLog.setRetryCount(0);
        logMapper.insert(scheduleLog);

        if (Boolean.TRUE.equals(result.get("success"))) {
            advance(task);
        }
        return result;
    }

    /** 推进下次执行日期；end_date 到期自动停用 */
    private void advance(LedgerScheduleTask task) {
        LocalDate next = nextExecDate(task, task.getNextExecDate());
        if (task.getEndDate() != null && (next == null || next.isAfter(task.getEndDate()))) {
            task.setEnabled(0);
            task.setNextExecDate(null);
        } else {
            task.setNextExecDate(next);
        }
    }

    /** 从 from 起按周期推算下一个执行日（含 from 当天） */
    private LocalDate firstExecDate(LedgerScheduleTask task, LocalDate from) {
        return nextExecDate(task, from.minusDays(1));
    }

    /** 从 from 的次日开始按周期推算第一个执行日 */
    private LocalDate nextExecDate(LedgerScheduleTask task, LocalDate from) {
        LocalDate candidate = from.plusDays(1);
        switch (task.getCycle()) {
            case LedgerScheduleTask.CYCLE_DAILY:
                return candidate;
            case LedgerScheduleTask.CYCLE_WEEKLY: {
                int dow = task.getDayOfWeek() == null ? 1 : task.getDayOfWeek();
                while (candidate.getDayOfWeek() != DayOfWeek.of(dow)) {
                    candidate = candidate.plusDays(1);
                }
                return candidate;
            }
            case LedgerScheduleTask.CYCLE_MONTHLY: {
                int dom = task.getDayOfMonth() == null ? 1 : Math.min(task.getDayOfMonth(), 28);
                LocalDate base = candidate.withDayOfMonth(dom);
                return base.isBefore(candidate) ? base.plusMonths(1) : base;
            }
            case LedgerScheduleTask.CYCLE_INTERVAL: {
                int days = task.getIntervalDays() == null || task.getIntervalDays() < 1 ? 1 : task.getIntervalDays();
                return from.plusDays(days);
            }
            default:
                return candidate;
        }
    }

    /** 校验任务参数 */
    private void validate(LedgerScheduleTask task) {
        if (task.getName() == null || task.getName().isBlank()) {
            throw new ServiceException("请输入任务名称");
        }
        if (task.getAmount() == null || task.getAmount().compareTo(BigDecimal.ZERO) <= 0) {
            throw new ServiceException("金额必须大于0");
        }
        if (!LedgerScheduleTask.TYPE_INCOME.equals(task.getType())
                && !LedgerScheduleTask.TYPE_EXPENSE.equals(task.getType())) {
            throw new ServiceException("记账类型仅支持支出/收入");
        }
        String cycle = task.getCycle();
        if (cycle == null || cycle.isBlank()) {
            throw new ServiceException("请选择执行周期");
        }
        switch (cycle) {
            case LedgerScheduleTask.CYCLE_WEEKLY -> {
                if (task.getDayOfWeek() == null || task.getDayOfWeek() < 1 || task.getDayOfWeek() > 7) {
                    throw new ServiceException("请选择每周几");
                }
            }
            case LedgerScheduleTask.CYCLE_MONTHLY -> {
                if (task.getDayOfMonth() == null || task.getDayOfMonth() < 1 || task.getDayOfMonth() > 28) {
                    throw new ServiceException("每月几号须为 1-28");
                }
            }
            case LedgerScheduleTask.CYCLE_INTERVAL -> {
                if (task.getIntervalDays() == null || task.getIntervalDays() < 1) {
                    throw new ServiceException("间隔天数须≥1");
                }
            }
            default -> { }
        }
        if (task.getEndDate() != null && task.getStartDate() != null
                && task.getEndDate().isBefore(task.getStartDate())) {
            throw new ServiceException("结束时间不能早于开始时间");
        }
    }

    private TransactionCreateDTO buildDto(LedgerScheduleTask task, LocalDate execDate) {
        TransactionCreateDTO dto = new TransactionCreateDTO();
        dto.setType(task.getType());
        dto.setAmount(task.getAmount());
        dto.setCategoryId(task.getCategoryId());
        dto.setAccountId(task.getAccountId());
        dto.setDescription(task.getDescription() != null && !task.getDescription().isBlank()
                ? task.getDescription() : ("定时记账-" + task.getName()));
        dto.setTransactionDate(execDate);
        dto.setTransactionTime(task.getExecTime());
        dto.setIsBudget(1);
        return dto;
    }

    /** 填充分类名/账户名展示字段 */
    private void fillNames(List<LedgerScheduleTask> tasks) {
        for (LedgerScheduleTask t : tasks) {
            if (t.getCategoryId() != null) {
                LedgerCategory c = categoryMapper.selectById(t.getCategoryId());
                if (c != null) {
                    t.setCategoryName(c.getName());
                }
            }
            if (t.getAccountId() != null) {
                LedgerAssetAccount a = assetAccountMapper.selectById(t.getAccountId());
                if (a != null) {
                    t.setAccountName(a.getName());
                }
            }
        }
    }

    private LedgerScheduleTask getOwned(Long userId, Long id) {
        LedgerScheduleTask task = this.getById(id);
        if (task == null || !userId.equals(task.getUserId())
                || LedgerScheduleTask.STATUS_DELETED == task.getStatus()) {
            throw new ServiceException("定时记账任务不存在");
        }
        return task;
    }

    private String truncate(String msg) {
        if (msg == null) {
            return "未知原因";
        }
        return msg.length() > 200 ? msg.substring(0, 200) : msg;
    }
}