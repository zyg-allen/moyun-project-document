package com.moyun.ledger.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.moyun.ledger.domain.entity.LedgerLiabilityAccount;

import java.util.List;

/**
 * 负债账户服务
 *
 * @author moyun
 */
public interface ILedgerLiabilityAccountService extends IService<LedgerLiabilityAccount> {

    /**
     * 新增负债账户（初始欠款自动生成 borrow 流水，保证全明细追溯）
     */
    LedgerLiabilityAccount createAccount(Long userId, LedgerLiabilityAccount account, Long initialBalance);

    /**
     * 修改负债账户（不含 balance，欠款变动必须走 borrow/repayment 记账）
     */
    void updateAccount(Long userId, LedgerLiabilityAccount account);

    /**
     * 删除负债账户（status=0 停用归档，流水永久保留）
     */
    void deleteAccount(Long userId, Long liabilityId);

    /**
     * 用户负债账户列表（在还中；可选含结清/归档）
     */
    List<LedgerLiabilityAccount> listByUser(Long userId, boolean includeArchived);
}
