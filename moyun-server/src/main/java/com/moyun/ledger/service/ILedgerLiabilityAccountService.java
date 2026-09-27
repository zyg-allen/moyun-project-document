package com.moyun.ledger.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.moyun.ledger.domain.entity.LedgerLiabilityAccount;

import java.math.BigDecimal;
import java.util.List;
import java.util.Set;

/**
 * 负债账户服务
 *
 * @author moyun
 */
public interface ILedgerLiabilityAccountService extends IService<LedgerLiabilityAccount> {

    /**
     * 新增负债账户（初始欠款自动生成 borrow 流水，保证全明细追溯）
     */
    LedgerLiabilityAccount createAccount(Long userId, LedgerLiabilityAccount account, BigDecimal initialBalance);

    /**
     * 修改负债账户（不含 balance / paid_terms，欠款与已还期数走 borrow/repayment 记账）
     *
     * <p>等价于 {@code updateAccount(userId, account, Set.of())}：只更新传入的非 null 字段，
     * <b>不清空</b>任何可空列。</p>
     */
    void updateAccount(Long userId, LedgerLiabilityAccount account);

    /**
     * 修改负债账户（列级更新；可空列支持"显式清空"）
     *
     * <p>{@code providedFields} 为请求体中**显式出现**的字段名，用于区分"未传"与"传了 null"：
     * 对可空业务列（{@code principal}/{@code annualRate}/{@code monthlyPayment}/{@code repaymentDay}/
     * {@code totalTerms}/{@code dueDate}/{@code icon}），显式提供即以传入值为准（{@code null} = 清空）——
     * 这正是 App 清空"每期还款额/还款日/总期数"所依赖的语义。
     * NOT NULL 列（name/type/includeInTotal/sortOrder）始终"非 null 才更新"。
     * {@code balance}/{@code paid_terms}/{@code version}/{@code user_id}/{@code status}/{@code settle_flag}
     * 永不在此接口写入（由记账联动或归档接口维护）。</p>
     */
    void updateAccount(Long userId, LedgerLiabilityAccount account, Set<String> providedFields);

    /**
     * 删除负债账户（status=0 停用归档，流水永久保留）
     */
    void deleteAccount(Long userId, Long liabilityId);

    /**
     * 用户负债账户列表（在还中；可选含结清/归档）
     */
    List<LedgerLiabilityAccount> listByUser(Long userId, boolean includeArchived);
}
