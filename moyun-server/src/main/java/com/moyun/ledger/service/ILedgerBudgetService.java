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
}
