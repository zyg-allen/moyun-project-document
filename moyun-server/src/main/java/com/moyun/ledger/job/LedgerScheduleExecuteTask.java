package com.moyun.ledger.job;

import com.moyun.ledger.service.ILedgerScheduleService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * 定时记账执行任务
 *
 * <p>每 10 分钟扫描一次到期任务（next_exec_date <= 今天 且启用中），
 * 逐个生成记账流水（复用余额联动/净资产快照逻辑），写执行日志并推进下次执行日。
 * 失败任务留痕不推进，用户可在 App 端手动重试。
 *
 * @author moyun
 */
@Component("ledgerScheduleExecuteTask")
public class LedgerScheduleExecuteTask {

    private static final Logger log = LoggerFactory.getLogger(LedgerScheduleExecuteTask.class);

    @Autowired
    private ILedgerScheduleService scheduleService;

    /** 每 10 分钟执行一次 */
    @Scheduled(cron = "0 */10 * * * ?")
    public void execute() {
        try {
            Map<String, Integer> stats = scheduleService.runDueTasks();
            if (stats.getOrDefault("executed", 0) > 0) {
                log.info("定时记账执行完成：到期={} 成功={} 失败={}",
                        stats.get("executed"), stats.get("success"), stats.get("fail"));
            }
        } catch (Exception e) {
            log.error("定时记账执行任务异常", e);
        }
    }
}