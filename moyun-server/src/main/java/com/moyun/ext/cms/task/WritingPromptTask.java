package com.moyun.ext.cms.task;

import com.moyun.ext.cms.service.ICmsWritingPromptService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.time.LocalDate;

/**
 * 写作提示定时任务（AI 每日生成）
 *
 * <p>调度入口由 sys_job 统一管理（Quartz），见 DML 增量：
 * {@code writingPromptTask.generateDailyPrompt()} — 每天 00:10 执行。
 *
 * <p>职责：为"今天 + 明天"各生成一条写作提示（已存在自动跳过）。
 * 提前一天生成明天数据，即使任务偶发失败，前台"今日主题"仍有兜底。
 * 可在后台「监控 → 定时任务」中暂停/立即执行/调整 cron。
 *
 * @author moyun
 */
@Component
public class WritingPromptTask {

    private static final Logger log = LoggerFactory.getLogger(WritingPromptTask.class);

    @Autowired
    private ICmsWritingPromptService writingPromptService;

    /**
     * 每日生成写作提示（今天 + 明天）。
     * <p>调度：sys_job cron 0 10 0 * * ?
     */
    public void generateDailyPrompt() {
        LocalDate today = LocalDate.now();
        LocalDate tomorrow = today.plusDays(1);
        log.info("[WritingPromptTask] 开始每日写作提示生成 today={} tomorrow={}", today, tomorrow);
        try {
            writingPromptService.aiGenerateForDate(today);
            writingPromptService.aiGenerateForDate(tomorrow);
            log.info("[WritingPromptTask] 每日写作提示生成完成");
        } catch (Exception e) {
            log.error("[WritingPromptTask] 每日写作提示生成失败", e);
        }
    }
}
