package com.moyun.ledger.job;

import com.moyun.core.redis.DistributedLockUtil;
import com.moyun.ledger.service.ILedgerMemoService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Duration;

/**
 * 备忘录提醒任务
 *
 * <p>每 5 分钟扫描一次开启提醒且未完成、未发送的事项，
 * 按 remind_rule（准时/提前30分钟/1小时/2小时/1天/前一天9点）计算提醒时刻，
 * 到达即触发提醒（原 pay 模块 INotificationService 已随收费功能下线），reminded 标记防重。</p>
 *
 * <p><b>并发保护</b>：{@code @Scheduled} 在每个 JVM 实例都会触发，
 * 而"扫描 → 判定到期 → 置 reminded → 发通知"之间存在读未提交窗口，
 * 多实例并发会给同一事项<b>重复发通知</b>（用户可见）。故用
 * {@link DistributedLockUtil} 做实例间互斥，抢不到锁的实例跳过本轮。</p>
 *
 * @author moyun
 */
@Component("ledgerMemoRemindTask")
public class LedgerMemoRemindTask {

    private static final Logger log = LoggerFactory.getLogger(LedgerMemoRemindTask.class);

    /** 分布式锁键：保证同一时刻只有一个实例在执行提醒扫描 */
    private static final String LOCK_KEY = "moyun:lock:ledger:memo:remind";

    /** 锁 TTL：略小于调度间隔（5 分钟） */
    private static final Duration LOCK_TTL = Duration.ofMinutes(4);

    @Autowired
    private ILedgerMemoService memoService;

    @Autowired
    private DistributedLockUtil lockUtil;

    /** 每 5 分钟执行一次 */
    @Scheduled(cron = "0 */5 * * * ?")
    public void execute() {
        try (DistributedLockUtil.Lock lock =
                     lockUtil.tryLockWithWatchdog(LOCK_KEY, LOCK_TTL)) {
            if (lock == null) {
                log.debug("备忘录提醒：其他实例正在执行，本轮跳过");
                return;
            }
            try {
                int sent = memoService.sendDueReminders();
                if (sent > 0) {
                    log.info("备忘录提醒发送完成：本次发送 {} 条", sent);
                }
            } catch (Exception e) {
                log.error("备忘录提醒任务异常", e);
            }
        }
    }
}