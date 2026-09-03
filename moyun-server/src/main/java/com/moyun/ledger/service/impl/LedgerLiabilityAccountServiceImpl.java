package com.moyun.ledger.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.moyun.ledger.domain.entity.LedgerLiabilityAccount;
import com.moyun.ledger.domain.entity.LedgerTransaction;
import com.moyun.ledger.mapper.LedgerLiabilityAccountMapper;
import com.moyun.ledger.mapper.LedgerTransactionMapper;
import com.moyun.ledger.service.ILedgerLiabilityAccountService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

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
    public LedgerLiabilityAccount createAccount(Long userId, LedgerLiabilityAccount account, Long initialBalance) {
        account.setId(null);
        account.setUserId(userId);
        long balance = initialBalance == null ? 0L : initialBalance;
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
        account.setSettleFlag(balance == 0 ? 1 : 0);
        account.setVersion(0);
        save(account);
        // 初始欠款自动生成 borrow 流水（全明细追溯）
        if (balance != 0) {
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
        LedgerLiabilityAccount exist = getOwned(userId, account.getId());
        if (exist == null) {
            throw new IllegalArgumentException("负债账户不存在或无权操作");
        }
        // balance 由记账联动维护，此处强制保持原值
        account.setBalance(exist.getBalance());
        account.setSettleFlag(exist.getSettleFlag());
        account.setUserId(userId);
        account.setVersion(exist.getVersion());
        updateById(account);
    }

    @Override
    public void deleteAccount(Long userId, Long liabilityId) {
        LedgerLiabilityAccount exist = getOwned(userId, liabilityId);
        if (exist == null) {
            throw new IllegalArgumentException("负债账户不存在或无权操作");
        }
        exist.setStatus(LedgerLiabilityAccount.STATUS_ARCHIVED);
        updateById(exist);
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
