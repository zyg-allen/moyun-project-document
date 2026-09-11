package com.moyun.ledger.job;

import com.moyun.ledger.service.ILedgerMemoService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * 备忘录提醒任务
 *
 * <p>每 5 分钟扫描一次开启提醒且未完成、未发送的事项，
 * 按 remind_rule（准时/提前30分钟/1小时/2小时/1天/前一天9点）计算提醒时刻，
 * 到达即发送站内通知（pay 模块 INotificationService），reminded 标记防重。
 *
 * @author moyun
 */
@Component("ledgerMemoRemindTask")
public class LedgerMemoRemindTask {

    private static final Logger log = LoggerFactory.getLogger(LedgerMemoRemindTask.class);

    @Autowired
    private ILedgerMemoService memoService;

    /** 每 5 分钟执行一次 */
    @Scheduled(cron = "0 */5 * * * ?")
    public void execute() {
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