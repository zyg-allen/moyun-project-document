package com.moyun.ledger.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.moyun.ledger.domain.entity.LedgerLiabilityAccount;
import com.moyun.ledger.domain.entity.LedgerTransaction;
import com.moyun.ledger.mapper.LedgerLiabilityAccountMapper;
import com.moyun.ledger.mapper.LedgerTransactionMapper;
import com.moyun.ledger.service.ILedgerLiabilityAccountService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Set;

/**
 * 负债账户服务实现
 *
 * <p>设计红线：balance 不允许直接编辑，初始欠款以 borrow 流水留痕；
 * 删除即归档（status=0），结清置 settle_flag=1（正向业务态），两者独立。
 *
 * @author moyun
 */
@Service
public class LedgerLiabilityAccountServiceImpl
        extends ServiceImpl<LedgerLiabilityAccountMapper, LedgerLiabilityAccount>
        implements ILedgerLiabilityAccountService {

    @Autowired
    private LedgerTransactionMapper transactionMapper;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public LedgerLiabilityAccount createAccount(Long userId, LedgerLiabilityAccount account, BigDecimal initialBalance) {
        account.setId(null);
        account.setUserId(userId);
        BigDecimal balance = initialBalance == null ? BigDecimal.ZERO : initialBalance;
        account.setBalance(balance);
        if (account.getPrincipal() == null) {
            account.setPrincipal(balance);
        }
        if (account.getIncludeInTotal() == null) {
            account.setIncludeInTotal(1);
        }
        if (account.getSortOrder() == null) {
            account.setSortOrder(0);
        }
        if (account.getStatus() == null) {
            account.setStatus(LedgerLiabilityAccount.STATUS_ENABLED);
        }
        account.setSettleFlag(balance.compareTo(BigDecimal.ZERO) == 0 ? 1 : 0);
        account.setVersion(0);
        save(account);
        // 初始欠款自动生成 borrow 流水（全明细追溯）
        if (balance.compareTo(BigDecimal.ZERO) != 0) {
            LedgerTransaction txn = new LedgerTransaction();
            txn.setUserId(userId);
            txn.setType(LedgerTransaction.TYPE_BORROW);
            txn.setAmount(balance);
            txn.setLiabilityId(account.getId());
            txn.setLiabilityBalanceAfter(balance);
            txn.setDescription("初始欠款");
            txn.setTransactionDate(LocalDate.now());
                txn.setIsBudget(0);
            txn.setStatus(LedgerTransaction.STATUS_NORMAL);
            transactionMapper.insert(txn);
        }
        return account;
    }

    @Override
    public void updateAccount(Long userId, LedgerLiabilityAccount account) {
        updateAccount(userId, account, java.util.Set.of());
    }

    @Override
    public void updateAccount(Long userId, LedgerLiabilityAccount account, Set<String> providedFields) {
        LedgerLiabilityAccount exist = getOwned(userId, account.getId());
        if (exist == null) {
            throw new IllegalArgumentException("负债账户不存在或无权操作");
        }
        LambdaUpdateWrapper<LedgerLiabilityAccount> uw = metaUpdateWrapper(userId, account, providedFields);
        if (uw == null) {
            return; // 无可更新字段 = 无变化（避免生成 `UPDATE t SET WHERE ...` 的非法 SQL）
        }
        int rows = baseMapper.update(null, uw);
        if (rows == 0) {
            throw new IllegalStateException("负债账户更新失败（记录可能已被删除），请刷新后重试");
        }
    }

    /**
     * 「账户业务属性」列级更新条件 —— **只更新可编辑列**
     *
     * <p>与 {@code LedgerAssetAccountServiceImpl#metaUpdateWrapper} 同一根因与同一改法，
     * 原缺陷（读整行 → {@code updateById} 写回整行）的真实复现见
     * {@code LedgerAccountMetaUpdateIsolationDbTest#liabilityMetaUpdate_doesNotOverwriteConcurrentBorrow}。</p>
     *
     * <h4>可空列的"显式清空"（修掉 App 清空失效）</h4>
     * <p>{@code monthlyPayment}/{@code repaymentDay}/{@code totalTerms} 等在 App 清空时**显式传 null**，
     * 原 {@code updateById} 的"null 则跳过"会让清空**静默不生效**。现在按
     * {@code providedFields}（请求体里显式出现的字段名）判定：显式提供即可写入 null 完成清空，
     * 未提供则保持原值——两者不再混淆。</p>
     *
     * <p>永久排除的列：{@code balance}、{@code paid_terms}（由记账联动原子维护）、
     * {@code version}（并发控制）、{@code user_id}、{@code status}、{@code settle_flag}
     * （归属与状态，归档只能走 {@code deleteAccount}）。</p>
     *
     * @param providedFields 请求体中**显式出现**的字段名（camelCase）
     * @return 已构建好 SET 子句的更新条件；无可更新字段时返回 {@code null}
     */
    private LambdaUpdateWrapper<LedgerLiabilityAccount> metaUpdateWrapper(Long userId, LedgerLiabilityAccount account,
                                                                         Set<String> providedFields) {
        Set<String> provided = providedFields == null ? Set.of() : providedFields;
        LambdaUpdateWrapper<LedgerLiabilityAccount> uw = new LambdaUpdateWrapper<>();
        uw.eq(LedgerLiabilityAccount::getId, account.getId())
                .eq(LedgerLiabilityAccount::getUserId, userId);
        boolean any = false;
        // —— NOT NULL 业务列：非 null 才更新 ——
        if (account.getName() != null) {
            uw.set(LedgerLiabilityAccount::getName, account.getName());
            any = true;
        }
        if (account.getType() != null) {
            uw.set(LedgerLiabilityAccount::getType, account.getType());
            any = true;
        }
        if (account.getIncludeInTotal() != null) {
            uw.set(LedgerLiabilityAccount::getIncludeInTotal, account.getIncludeInTotal());
            any = true;
        }
        if (account.getSortOrder() != null) {
            uw.set(LedgerLiabilityAccount::getSortOrder, account.getSortOrder());
            any = true;
        }
        // —— 可空业务列：显式提供即以传入值为准（null = 清空）——
        if (provided.contains("principal")) {
            uw.set(LedgerLiabilityAccount::getPrincipal, account.getPrincipal());
            any = true;
        }
        if (provided.contains("annualRate")) {
            uw.set(LedgerLiabilityAccount::getAnnualRate, account.getAnnualRate());
            any = true;
        }
        if (provided.contains("monthlyPayment")) {
            uw.set(LedgerLiabilityAccount::getMonthlyPayment, account.getMonthlyPayment());
            any = true;
        }
        if (provided.contains("repaymentDay")) {
            uw.set(LedgerLiabilityAccount::getRepaymentDay, account.getRepaymentDay());
            any = true;
        }
        if (provided.contains("totalTerms")) {
            uw.set(LedgerLiabilityAccount::getTotalTerms, account.getTotalTerms());
            any = true;
        }
        if (provided.contains("dueDate")) {
            uw.set(LedgerLiabilityAccount::getDueDate, account.getDueDate());
            any = true;
        }
        if (provided.contains("icon")) {
            uw.set(LedgerLiabilityAccount::getIcon, account.getIcon());
            any = true;
        }
        return any ? uw : null;
    }

    @Override
    public void deleteAccount(Long userId, Long liabilityId) {
        LedgerLiabilityAccount exist = getOwned(userId, liabilityId);
        if (exist == null) {
            throw new IllegalArgumentException("负债账户不存在或无权操作");
        }
        // 归档同理只改 status（原 updateById(exist) 会把快照里的 balance/version/settle_flag 一起写回）
        int rows = baseMapper.update(null, new LambdaUpdateWrapper<LedgerLiabilityAccount>()
                .eq(LedgerLiabilityAccount::getId, liabilityId)
                .eq(LedgerLiabilityAccount::getUserId, userId)
                .set(LedgerLiabilityAccount::getStatus, LedgerLiabilityAccount.STATUS_ARCHIVED));
        if (rows == 0) {
            throw new IllegalStateException("负债账户归档失败（记录可能已被删除），请刷新后重试");
        }
    }

    @Override
    public List<LedgerLiabilityAccount> listByUser(Long userId, boolean includeArchived) {
        LambdaQueryWrapper<LedgerLiabilityAccount> qw = new LambdaQueryWrapper<>();
        qw.eq(LedgerLiabilityAccount::getUserId, userId);
        if (!includeArchived) {
            qw.eq(LedgerLiabilityAccount::getStatus, LedgerLiabilityAccount.STATUS_ENABLED);
        }
        qw.orderByAsc(LedgerLiabilityAccount::getSortOrder).orderByDesc(LedgerLiabilityAccount::getId);
        return list(qw);
    }

    private LedgerLiabilityAccount getOwned(Long userId, Long liabilityId) {
        if (liabilityId == null) {
            return null;
        }
        LambdaQueryWrapper<LedgerLiabilityAccount> qw = new LambdaQueryWrapper<>();
        qw.eq(LedgerLiabilityAccount::getId, liabilityId)
                .eq(LedgerLiabilityAccount::getUserId, userId);
        return getOne(qw);
    }
}
