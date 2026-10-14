package com.moyun.ledger.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.moyun.ledger.domain.entity.LedgerBudget;

import java.util.List;

/**
 * 记账预算服务
 *
 * @author moyun
 */
public interface ILedgerBudgetService extends IService<LedgerBudget> {

    /**
     * 查询指定月份的预算列表（总预算 + 分类预算）
     *
     * @param userId 门户用户ID
     * @param year   年（null=当年）
     * @param month  月（null=当月）
     */
    List<LedgerBudget> listByMonth(Long userId, Integer year, Integer month);

    /**
     * 设置预算（总预算 categoryId=null，分类预算非空；存在即更新，不存在即插入）
     */
    void saveBudget(Long userId, LedgerBudget budget);
    /**
     * 取消预算（按年月 + 分类精确删除）。
     *
     * <p>存在的意义：预算金额只允许大于 0，因此"取消预算"不能靠填 0 表达，
     * 必须有显式删除入口 —— 否则总预算一旦设置就无法取消。</p>
     *
     * <p>注：ledger_budget 无软删列，直接物理删除；预算属可再生的用户配置，
     * 删除不涉及历史流水，无审计追溯需求。</p>
     *
     * @param userId     当前用户
     * @param year       年份
     * @param month      月份（1-12）
     * @param categoryId 分类 id；为空表示月度总预算
     * @return 是否确实删除了记录（false = 本来就没有）
     */
    boolean removeBudget(Long userId, Integer year, Integer month, Long categoryId);

}
