package com.moyun.ledger.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.moyun.ledger.domain.entity.LedgerAssetAccount;
import com.moyun.ledger.domain.entity.LedgerNetWorthSnapshot;
import com.moyun.ledger.domain.entity.LedgerTransaction;
import com.moyun.ledger.mapper.LedgerAssetAccountMapper;
import com.moyun.ledger.mapper.LedgerNetWorthSnapshotMapper;
import com.moyun.ledger.mapper.LedgerTransactionMapper;
import com.moyun.ledger.service.ILedgerAssetAccountService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * 资产账户服务实现
 *
 * <p>设计红线：balance 不允许直接编辑，初始余额以 adjust 流水留痕；
 * 删除即归档（status=0），流水永久保留。
 *
 * @author moyun
 */
@Service
public class LedgerAssetAccountServiceImpl extends ServiceImpl<LedgerAssetAccountMapper, LedgerAssetAccount>
        implements ILedgerAssetAccountService {

    @Autowired
    private LedgerNetWorthSnapshotMapper snapshotMapper;

    @Autowired
    private LedgerTransactionMapper transactionMapper;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public LedgerAssetAccount createAccount(Long userId, LedgerAssetAccount account, BigDecimal initialBalance) {
        account.setId(null);
        account.setUserId(userId);
        BigDecimal balance = initialBalance == null ? BigDecimal.ZERO : initialBalance;
        account.setBalance(balance);
        if (account.getIncludeInTotal() == null) {
            account.setIncludeInTotal(1);
        }
        if (account.getHideBalance() == null) {
            account.setHideBalance(0);
        }
        if (account.getSortOrder() == null) {
            account.setSortOrder(0);
        }
        if (account.getStatus() == null) {
            account.setStatus(LedgerAssetAccount.STATUS_ENABLED);
        }
        account.setVersion(0);
        save(account);
        // 初始余额自动生成 adjust 校准流水（全明细追溯）
        if (balance.compareTo(BigDecimal.ZERO) != 0) {
            LedgerTransaction txn = new LedgerTransaction();
            txn.setUserId(userId);
            txn.setType(LedgerTransaction.TYPE_ADJUST);
            txn.setAmount(balance);
            txn.setAccountId(account.getId());
            txn.setBalanceAfter(balance);
            txn.setDescription("初始余额");
            txn.setTransactionDate(LocalDate.now());
                txn.setIsBudget(0);
            txn.setStatus(LedgerTransaction.STATUS_NORMAL);
            // 直接落库（账户创建与流水同事务，无需走乐观锁路径）
            transactionMapper.insert(txn);
        }
        refreshSnapshot(userId, LocalDate.now());
        return account;
    }

    @Override
    public void updateAccount(Long userId, LedgerAssetAccount account) {
        LedgerAssetAccount exist = getOwned(userId, account.getId());
        if (exist == null) {
            throw new IllegalArgumentException("资产账户不存在或无权操作");
        }
        // balance 由记账联动维护，此处强制保持原值
        account.setBalance(exist.getBalance());
        account.setUserId(userId);
        account.setVersion(exist.getVersion());
        updateById(account);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteAccount(Long userId, Long accountId) {
        LedgerAssetAccount exist = getOwned(userId, accountId);
        if (exist == null) {
            throw new IllegalArgumentException("资产账户不存在或无权操作");
        }
        exist.setStatus(LedgerAssetAccount.STATUS_ARCHIVED);
        updateById(exist);
        refreshSnapshot(userId, LocalDate.now());
    }

    @Override
    public List<LedgerAssetAccount> listByUser(Long userId, boolean includeArchived) {
        LambdaQueryWrapper<LedgerAssetAccount> qw = new LambdaQueryWrapper<>();
        qw.eq(LedgerAssetAccount::getUserId, userId);
        if (!includeArchived) {
            qw.eq(LedgerAssetAccount::getStatus, LedgerAssetAccount.STATUS_ENABLED);
        }
        qw.orderByAsc(LedgerAssetAccount::getSortOrder).orderByDesc(LedgerAssetAccount::getId);
        return list(qw);
    }

    private LedgerAssetAccount getOwned(Long userId, Long accountId) {
        if (accountId == null) {
            return null;
        }
        LambdaQueryWrapper<LedgerAssetAccount> qw = new LambdaQueryWrapper<>();
        qw.eq(LedgerAssetAccount::getId, accountId).eq(LedgerAssetAccount::getUserId, userId);
        return getOne(qw);
    }

    /** 刷新当日净资产快照 */
    private void refreshSnapshot(Long userId, LocalDate date) {
        LambdaQueryWrapper<LedgerAssetAccount> aq = new LambdaQueryWrapper<>();
        aq.eq(LedgerAssetAccount::getUserId, userId)
                .eq(LedgerAssetAccount::getStatus, LedgerAssetAccount.STATUS_ENABLED)
                .eq(LedgerAssetAccount::getIncludeInTotal, 1);
        BigDecimal totalAsset = BigDecimal.ZERO;
        for (LedgerAssetAccount a : list(aq)) {
            totalAsset = totalAsset.add(a.getBalance());
        }
        LambdaQueryWrapper<LedgerNetWorthSnapshot> sq = new LambdaQueryWrapper<>();
        sq.eq(LedgerNetWorthSnapshot::getUserId, userId)
                .eq(LedgerNetWorthSnapshot::getSnapDate, date);
        LedgerNetWorthSnapshot exist = snapshotMapper.selectOne(sq);
        if (exist == null) {
            return; // 快照由记账服务/定时任务统一维护，此处仅已有快照时刷新资产侧
        }
        exist.setTotalAsset(totalAsset);
        exist.setNetWorth(totalAsset.subtract(exist.getTotalLiability()));
        snapshotMapper.updateById(exist);
    }
}
