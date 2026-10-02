package com.moyun.ledger.job;

import com.moyun.core.redis.DistributedLockUtil;
import com.moyun.ledger.service.ILedgerScheduleService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.Map;

/**
 * 定时记账执行任务
 *
 * <p>每 10 分钟扫描一次到期任务（next_exec_date <= 今天 且启用中），
 * 逐个生成记账流水（复用余额联动/净资产快照逻辑），写执行日志并推进下次执行日。
 * 失败任务留痕不推进，用户可在 App 端手动重试。</p>
 *
 * <p><b>并发保护（本类关键点）</b>：{@code @Scheduled} 在<b>每个 JVM 实例</b>都会触发，
 * 而本任务会真实生成流水并扣减余额，若无分布式协调，
 * 多实例部署下会对同一到期任务<b>重复记账</b>。
 * 现由 {@link DistributedLockUtil} 做实例间互斥：抢不到锁的实例直接跳过本轮
 * （任务由持锁实例处理，下一轮再扫）。</p>
 *
 * <p>锁 TTL 取 9 分钟（略小于 10 分钟调度间隔），保证异常退出后下一轮能重新获取；
 * 使用看门狗续期以覆盖"到期任务很多、单轮执行超过 TTL"的情况。</p>
 *
 * @author moyun
 */
@Component("ledgerScheduleExecuteTask")
public class LedgerScheduleExecuteTask {

    private static final Logger log = LoggerFactory.getLogger(LedgerScheduleExecuteTask.class);

    /** 分布式锁键：全局唯一，保证同一时刻只有一个实例在执行定时记账 */
    private static final String LOCK_KEY = "moyun:lock:ledger:schedule:execute";

    /** 锁 TTL：略小于调度间隔（10 分钟），异常退出后下一轮可重新获取 */
    private static final Duration LOCK_TTL = Duration.ofMinutes(9);

    @Autowired
    private ILedgerScheduleService scheduleService;

    @Autowired
    private DistributedLockUtil lockUtil;

    /** 每 10 分钟执行一次 */
    @Scheduled(cron = "0 */10 * * * ?")
    public void execute() {
        // 多实例互斥：非阻塞获取，抢不到说明其他实例正在处理，本实例跳过本轮
        try (DistributedLockUtil.Lock lock =
                     lockUtil.tryLockWithWatchdog(LOCK_KEY, LOCK_TTL)) {
            if (lock == null) {
                log.debug("定时记账：其他实例正在执行，本轮跳过");
                return;
            }
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
}