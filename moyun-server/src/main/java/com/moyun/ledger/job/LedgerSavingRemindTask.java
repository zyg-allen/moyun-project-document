package com.moyun.ledger.job;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.moyun.core.redis.DistributedLockUtil;
import com.moyun.ledger.domain.entity.LedgerSavingPlan;
import com.moyun.ledger.domain.entity.LedgerSavingRecord;
import com.moyun.ledger.mapper.LedgerSavingPlanMapper;
import com.moyun.ledger.mapper.LedgerSavingRecordMapper;
import com.moyun.system.domain.entity.SysNotification;
import com.moyun.system.service.ISysNotificationService;

/**
 * 存钱计划到期提醒。
 *
 * <p>每天 09:00 扫一次：到期未存（status=0）的期次写一条站内通知
 * （复用 {@code sys_notification}，与系统通知、业务通知、意见反馈通知同一通道）。
 * 到期后第 3 天仍未处理再提醒一次，最多 2 次，避免打扰。</p>
 *
 * <p>并发保护：多实例下用分布式锁，抢不到锁的实例跳过本轮。</p>
 */
@Component("ledgerSavingRemindTask")
public class LedgerSavingRemindTask {

    private static final Logger log = LoggerFactory.getLogger(LedgerSavingRemindTask.class);

    private static final String LOCK_KEY = "moyun:lock:ledger:saving:remind";
    private static final Duration LOCK_TTL = Duration.ofMinutes(10);
    private static final int MAX_NOTIFY = 2;

    @Autowired
    private LedgerSavingRecordMapper recordMapper;

    @Autowired
    private LedgerSavingPlanMapper planMapper;

    @Autowired
    private ISysNotificationService notificationService;

    @Autowired
    private DistributedLockUtil lockUtil;

    /** 每天 09:00 执行 */
    @Scheduled(cron = "0 0 9 * * ?")
    public void execute() {
        try (DistributedLockUtil.Lock lock = lockUtil.tryLockWithWatchdog(LOCK_KEY, LOCK_TTL)) {
            if (lock == null) {
                log.debug("存钱提醒：其他实例正在执行，本轮跳过");
                return;
            }
            try {
                int sent = remindDue();
                if (sent > 0) {
                    log.info("存钱计划提醒发送完成：本次发送 {} 条", sent);
                }
            } catch (Exception e) {
                log.error("存钱计划提醒任务异常", e);
            }
        }
    }

    private int remindDue() {
        LocalDate today = LocalDate.now();
        LocalDateTime retryBefore = LocalDateTime.now().minusDays(2);
        LambdaQueryWrapper<LedgerSavingRecord> q = new LambdaQueryWrapper<>();
        q.eq(LedgerSavingRecord::getStatus, LedgerSavingRecord.STATUS_PENDING)
                .le(LedgerSavingRecord::getRecordDate, today)
                .lt(LedgerSavingRecord::getNotifyCount, MAX_NOTIFY)
                .and(w -> w.isNull(LedgerSavingRecord::getLastNotifiedAt)
                        .or().le(LedgerSavingRecord::getLastNotifiedAt, retryBefore))
                .orderByAsc(LedgerSavingRecord::getRecordDate)
                .last("LIMIT 500");
        List<LedgerSavingRecord> list = recordMapper.selectList(q);
        int sent = 0;
        for (LedgerSavingRecord r : list) {
            try {
                LedgerSavingPlan plan = planMapper.selectById(r.getPlanId());
                if (plan == null || plan.getStatus() == null || plan.getStatus() == LedgerSavingPlan.STATUS_DELETED) {
                    continue;
                }
                String name = plan.getName() == null ? "存钱计划" : plan.getName();
                String amount = r.getTargetAmount() == null ? "" : "，目标 " + r.getTargetAmount().stripTrailingZeros().toPlainString() + " 元";
                SysNotification n = new SysNotification();
                n.setType("system");
                n.setScope("user");
                n.setUserId(r.getUserId());
                n.setUserType("portal");
                n.setNoticeType("1");
                n.setStatus("0");
                n.setTitle("今天该存「" + name + "」第 " + r.getPeriodIndex() + " 期");
                n.setContent("本期" + amount + "。完成后可在存钱计划里标记已存，也可关联一笔转账流水。");
                n.setData("{\"bizType\":\"saving\",\"planId\":" + plan.getId()
                        + ",\"recordId\":" + r.getId() + "}");
                notificationService.insertNotification(n);

                r.setNotifyCount((r.getNotifyCount() == null ? 0 : r.getNotifyCount()) + 1);
                r.setLastNotifiedAt(LocalDateTime.now());
                recordMapper.updateById(r);
                sent++;
            } catch (Exception e) {
                log.warn("存钱提醒发送失败 recordId={}: {}", r.getId(), e.getMessage());
            }
        }
        return sent;
    }
}
