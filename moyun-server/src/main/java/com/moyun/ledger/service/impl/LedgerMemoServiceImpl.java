package com.moyun.ledger.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.moyun.common.exception.system.ServiceException;
import com.moyun.ledger.domain.entity.LedgerMemo;
import com.moyun.ledger.mapper.LedgerMemoMapper;
import com.moyun.ledger.service.ILedgerMemoService;
import com.moyun.pay.service.INotificationService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 备忘录服务实现
 *
 * <p>v11.34：事项标题/事项时间/是否提醒/提醒方式/重要程度。
 * 提醒扫描：每 5 分钟由 LedgerMemoRemindTask 调用 sendDueReminders()，
 * 按 remind_rule 计算 event_time 的提前量，时刻到达即发送站内通知（防重 reminded 标记）。
 *
 * @author moyun
 */
@Service
public class LedgerMemoServiceImpl extends ServiceImpl<LedgerMemoMapper, LedgerMemo>
        implements ILedgerMemoService {

    private static final Logger log = LoggerFactory.getLogger(LedgerMemoServiceImpl.class);

    private static final DateTimeFormatter TIME_FMT = DateTimeFormatter.ofPattern("MM-dd HH:mm");

    /** 提醒方式 → 提前分钟数（advance_1d_9am 特殊处理：前一天 09:00） */
    private static final Map<String, Integer> RULE_MINUTES = Map.of(
            LedgerMemo.RULE_ON_TIME, 0,
            LedgerMemo.RULE_ADVANCE_30M, 30,
            LedgerMemo.RULE_ADVANCE_1H, 60,
            LedgerMemo.RULE_ADVANCE_2H, 120,
            LedgerMemo.RULE_ADVANCE_1D, 24 * 60
    );

    private static final Set<String> VALID_RULES = Set.of(
            LedgerMemo.RULE_ON_TIME, LedgerMemo.RULE_ADVANCE_30M, LedgerMemo.RULE_ADVANCE_1H,
            LedgerMemo.RULE_ADVANCE_2H, LedgerMemo.RULE_ADVANCE_1D, LedgerMemo.RULE_ADVANCE_1D_9AM);

    private static final Set<String> VALID_IMPORTANCE = Set.of(
            LedgerMemo.IMPORTANCE_LOW, LedgerMemo.IMPORTANCE_NORMAL,
            LedgerMemo.IMPORTANCE_HIGH, LedgerMemo.IMPORTANCE_URGENT);

    /** 重要程度排序权重（越大越靠前） */
    private static final Map<String, Integer> IMPORTANCE_ORDER = Map.of(
            LedgerMemo.IMPORTANCE_URGENT, 4, LedgerMemo.IMPORTANCE_HIGH, 3,
            LedgerMemo.IMPORTANCE_NORMAL, 2, LedgerMemo.IMPORTANCE_LOW, 1);

    @Autowired
    private INotificationService notificationService;

    @Override
    public List<LedgerMemo> listMemos(Long userId, int limit) {
        LambdaQueryWrapper<LedgerMemo> q = new LambdaQueryWrapper<>();
        q.eq(LedgerMemo::getUserId, userId);
        if (limit > 0) {
            q.last("LIMIT " + (limit * 3)); // 取样放大后内存排序再截断
        }
        List<LedgerMemo> all = this.list(q);
        Comparator<LedgerMemo> pendingFirst = Comparator
                .comparing((LedgerMemo m) -> m.getDone() != null && m.getDone() == 1 ? 1 : 0)
                .thenComparing(m -> -(IMPORTANCE_ORDER.getOrDefault(m.getImportance(), 2)))
                .thenComparing(m -> m.getEventTime() == null ? LocalDateTime.MAX : m.getEventTime())
                .thenComparing(m -> m.getCreateTime() == null ? LocalDateTime.MIN : m.getCreateTime());
        all.sort(pendingFirst);
        return limit > 0 ? all.subList(0, Math.min(limit, all.size())) : all;
    }

    @Override
    public Long createMemo(Long userId, String title, String content, LocalDateTime eventTime,
                           Integer remindEnabled, String remindRule, String importance) {
        String titleText = title == null ? "" : title.trim();
        String contentText = content == null ? "" : content.trim();
        if (titleText.isEmpty() && contentText.isEmpty()) {
            throw new ServiceException("请输入事项或内容");
        }
        if (titleText.isEmpty()) {
            titleText = contentText.length() > 50 ? contentText.substring(0, 50) : contentText;
        }
        LedgerMemo memo = new LedgerMemo();
        memo.setUserId(userId);
        memo.setTitle(titleText);
        memo.setContent(contentText);
        memo.setDone(0);
        memo.setTodoDate(LocalDate.now());
        applyRemindSettings(memo, eventTime, remindEnabled, remindRule, importance);
        memo.setReminded(0);
        this.save(memo);
        return memo.getId();
    }

    @Override
    public void updateMemo(Long userId, Long id, String title, String content, LocalDateTime eventTime,
                           Integer remindEnabled, String remindRule, String importance, Integer done) {
        LedgerMemo memo = getOwned(userId, id);
        if (title != null && !title.trim().isEmpty()) {
            memo.setTitle(title.trim());
        }
        if (content != null && !content.trim().isEmpty()) {
            memo.setContent(content.trim());
        }
        boolean remindChanged = applyRemindSettings(memo, eventTime, remindEnabled, remindRule, importance);
        if (remindChanged) {
            memo.setReminded(0); // 提醒设置变更后允许重新提醒
        }
        if (done != null) {
            memo.setDone(done);
        }
        this.updateById(memo);
    }

    /** 应用并校验提醒设置；返回设置是否发生变化（供重置 reminded） */
    private boolean applyRemindSettings(LedgerMemo memo, LocalDateTime eventTime,
                                        Integer remindEnabled, String remindRule, String importance) {
        boolean changed = false;
        if (eventTime != null) {
            changed |= !eventTime.equals(memo.getEventTime());
            memo.setEventTime(eventTime);
        }
        if (remindEnabled != null) {
            changed |= !remindEnabled.equals(memo.getRemindEnabled());
            memo.setRemindEnabled(remindEnabled);
        }
        if (remindRule != null) {
            if (!VALID_RULES.contains(remindRule)) {
                throw new ServiceException("不支持的提醒方式");
            }
            changed |= !remindRule.equals(memo.getRemindRule());
            memo.setRemindRule(remindRule);
        }
        if (importance != null) {
            if (!VALID_IMPORTANCE.contains(importance)) {
                throw new ServiceException("不支持的重要程度");
            }
            changed |= !importance.equals(memo.getImportance());
            memo.setImportance(importance);
        }
        // 开启提醒必须有事項时间和提醒方式
        if (memo.getRemindEnabled() != null && memo.getRemindEnabled() == 1) {
            if (memo.getEventTime() == null) {
                throw new ServiceException("开启提醒需要先设置事项时间");
            }
            if (memo.getRemindRule() == null) {
                memo.setRemindRule(LedgerMemo.RULE_ON_TIME);
                changed = true;
            }
        }
        if (memo.getImportance() == null) {
            memo.setImportance(LedgerMemo.IMPORTANCE_NORMAL);
        }
        return changed;
    }

    @Override
    public void toggleDone(Long userId, Long id) {
        LedgerMemo memo = getOwned(userId, id);
        memo.setDone(memo.getDone() != null && memo.getDone() == 1 ? 0 : 1);
        this.updateById(memo);
    }

    @Override
    public void deleteMemo(Long userId, Long id) {
        LedgerMemo memo = getOwned(userId, id);
        this.removeById(memo.getId());
    }

    @Override
    public int sendDueReminders() {
        LocalDateTime now = LocalDateTime.now();
        LambdaQueryWrapper<LedgerMemo> q = new LambdaQueryWrapper<>();
        q.eq(LedgerMemo::getRemindEnabled, 1)
                .eq(LedgerMemo::getDone, 0)
                .eq(LedgerMemo::getReminded, 0)
                .isNotNull(LedgerMemo::getEventTime);
        List<LedgerMemo> candidates = this.list(q);
        int sent = 0;
        for (LedgerMemo memo : candidates) {
            try {
                LocalDateTime remindAt = calcRemindTime(memo);
                if (remindAt == null || remindAt.isAfter(now)) {
                    continue;
                }
                // 事项已过期超过 1 天则不再提醒，直接标记
                boolean expired = memo.getEventTime().isBefore(now.minusDays(1));
                if (!expired) {
                    String timeText = memo.getEventTime().format(TIME_FMT);
                    String importanceText = importanceText(memo.getImportance());
                    notificationService.send(memo.getUserId(), "TODO", "memo-" + memo.getId(),
                            "待办提醒：" + memo.getTitle(),
                            "您有一个【" + importanceText + "】的待办事项将于 " + timeText + " 进行：" + memo.getTitle()
                                    + (memo.getContent() != null && !memo.getContent().isBlank() ? "：" + memo.getContent() : ""));
                    sent++;
                }
                memo.setReminded(1);
                this.updateById(memo);
            } catch (Exception e) {
                log.warn("待办提醒发送失败 memoId={}: {}", memo.getId(), e.getMessage());
            }
        }
        return sent;
    }

    /** 按提醒方式计算提醒时刻 */
    private LocalDateTime calcRemindTime(LedgerMemo memo) {
        LocalDateTime event = memo.getEventTime();
        String rule = memo.getRemindRule() == null ? LedgerMemo.RULE_ON_TIME : memo.getRemindRule();
        if (LedgerMemo.RULE_ADVANCE_1D_9AM.equals(rule)) {
            // 前一天上午 9 点
            return event.minusDays(1).withHour(9).withMinute(0).withSecond(0).withNano(0);
        }
        int minutes = RULE_MINUTES.getOrDefault(rule, 0);
        return event.minusMinutes(minutes);
    }

    private String importanceText(String importance) {
        return switch (importance == null ? LedgerMemo.IMPORTANCE_NORMAL : importance) {
            case LedgerMemo.IMPORTANCE_URGENT -> "紧急";
            case LedgerMemo.IMPORTANCE_HIGH -> "重要";
            case LedgerMemo.IMPORTANCE_LOW -> "不重要";
            default -> "一般";
        };
    }

    private LedgerMemo getOwned(Long userId, Long id) {
        LedgerMemo memo = this.getById(id);
        if (memo == null || !userId.equals(memo.getUserId())) {
            throw new ServiceException("待办不存在");
        }
        return memo;
    }
}