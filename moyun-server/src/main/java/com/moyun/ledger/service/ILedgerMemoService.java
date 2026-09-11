package com.moyun.ledger.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.moyun.ledger.domain.entity.LedgerMemo;

import java.util.List;

/**
 * 备忘录（待办事项）服务：首页待办展示与备忘录页共用
 *
 * @author moyun
 */
public interface ILedgerMemoService extends IService<LedgerMemo> {

    /**
     * 待办列表（未完成在前；未完成内按重要程度 urgent>high>normal>low，再按事项时间升序；
     * 已完成按完成时间倒序。limit=0 表示全部）
     */
    List<LedgerMemo> listMemos(Long userId, int limit);

    /** 新增待办（v11.34：title/eventTime/remindEnabled/remindRule/importance） */
    Long createMemo(Long userId, String title, String content,
                    java.time.LocalDateTime eventTime, Integer remindEnabled,
                    String remindRule, String importance);

    /** 更新待办（内容/完成状态/提醒设置任意，null 跳过） */
    void updateMemo(Long userId, Long id, String title, String content,
                    java.time.LocalDateTime eventTime, Integer remindEnabled,
                    String remindRule, String importance, Integer done);

    /** 切换完成状态 */
    void toggleDone(Long userId, Long id);

    /** 删除待办 */
    void deleteMemo(Long userId, Long id);

    /** 扫描并发送到期提醒（定时任务调用）：event_time-提前量 已到且未提醒的事项，发站内通知，reminded 置 1 */
    int sendDueReminders();
}