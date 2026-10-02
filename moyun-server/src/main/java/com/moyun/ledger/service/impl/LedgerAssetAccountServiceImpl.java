package com.moyun.ledger.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
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
import java.util.Set;

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
        updateAccount(userId, account, java.util.Set.of());
    }

    @Override
    public void updateAccount(Long userId, LedgerAssetAccount account, Set<String> providedFields) {
        LedgerAssetAccount exist = getOwned(userId, account.getId());
        if (exist == null) {
            throw new IllegalArgumentException("资产账户不存在或无权操作");
        }
        LambdaUpdateWrapper<LedgerAssetAccount> uw = metaUpdateWrapper(userId, account, providedFields);
        if (uw == null) {
            return; // 无可更新字段 = 无变化（避免生成 `UPDATE t SET WHERE ...` 的非法 SQL）
        }
        int rows = baseMapper.update(null, uw);
        if (rows == 0) {
            throw new IllegalStateException("资产账户更新失败（记录可能已被删除），请刷新后重试");
        }
    }

    /**
     * 「账户业务属性」列级更新条件 —— **只更新可编辑列**
     *
     * <h4>为什么用列级更新而非 {@code updateById(account)}</h4>
     * 若用 <b>读整行 → 改字段 → {@code updateById} 写回整行</b>：
     * <pre>
     * exist = getOwned(...);                      // 快照读（含 balance / version）
     * account.setBalance(exist.getBalance());     // 把"读到的"余额塞回待写实体
     * account.setVersion(exist.getVersion());
     * updateById(account);                        // 写回整行 → balance / version 一起被覆盖
     * </pre>
     * 而余额的真正维护方是 {@code LedgerTransactionServiceImpl.applyAssetDelta}
     * （{@code balance = balance + delta} + {@code version = version + 1} + {@code WHERE version = ?} 乐观锁）。
     * 两者叠加会造成两个后果（均已在 {@code LedgerAccountMetaUpdateIsolationDbTest} 真库复现）：
     * <ol>
     *   <li><b>抹账</b>：用户改账户名期间并发记了一笔账，改属性会把余额<strong>写回记账前的旧值</strong>
     *       —— 流水记着 +50、余额却没变，账实不符；</li>
     *   <li><b>乐观锁 ABA</b>：把 {@code version} 写回旧值后，"读到的 version"重新可用，
     *       两个并发记账都可能命中 {@code WHERE version = ?} → 重复叠加或丢更新。</li>
     * </ol>
     *
     * <h4>列范围规则</h4>
     * 可写 = 账户的<b>业务属性列</b>；永久排除三类：
     * <ul>
     *   <li>由记账联动维护：{@code balance}；</li>
     *   <li>并发控制：{@code version}；</li>
     *   <li>归属与状态：{@code user_id}、{@code status}（归档只能走 {@code deleteAccount}）。</li>
     * </ul>
     * 其余列沿用原 {@code updateById} 的"未提供（null）则不更新"语义，故**不产生能力回退**。
     *
     * <h4>可空列的"显式清空"</h4>
     * <p>{@code valuation} / {@code icon} 是可空列：{@code providedFields} 里出现即以其为准
     * （{@code null} = 清空）——这样"清空估值/图标"能真正落库，同时避免"未传就清空"的数据丢失。</p>
     *
     * @param providedFields 请求体中**显式出现**的字段名（camelCase，如 {@code valuation}）
     * @return 已构建好 SET 子句的更新条件；调用方需先确认非空（全 null 时不产生 SET，SQL 非法）
     */
    private LambdaUpdateWrapper<LedgerAssetAccount> metaUpdateWrapper(Long userId, LedgerAssetAccount account,
                                                                     Set<String> providedFields) {
        Set<String> provided = providedFields == null ? Set.of() : providedFields;
        LambdaUpdateWrapper<LedgerAssetAccount> uw = new LambdaUpdateWrapper<>();
        uw.eq(LedgerAssetAccount::getId, account.getId())
                // 归属校验下沉到 WHERE：与 getOwned 同源，避免"先查后改"之间被改归属
                .eq(LedgerAssetAccount::getUserId, userId);
        boolean any = false;
        // —— NOT NULL 业务列：非 null 才更新（传 null 无法置空，也无需置空）——
        if (account.getName() != null) {
            uw.set(LedgerAssetAccount::getName, account.getName());
            any = true;
        }
        if (account.getType() != null) {
            uw.set(LedgerAssetAccount::getType, account.getType());
            any = true;
        }
        if (account.getIncludeInTotal() != null) {
            uw.set(LedgerAssetAccount::getIncludeInTotal, account.getIncludeInTotal());
            any = true;
        }
        if (account.getHideBalance() != null) {
            uw.set(LedgerAssetAccount::getHideBalance, account.getHideBalance());
            any = true;
        }
        if (account.getSortOrder() != null) {
            uw.set(LedgerAssetAccount::getSortOrder, account.getSortOrder());
            any = true;
        }
        // —— 可空业务列：显式提供即以传入值为准（null = 清空）——
        if (provided.contains("valuation")) {
            uw.set(LedgerAssetAccount::getValuation, account.getValuation());
            any = true;
        }
        if (provided.contains("icon")) {
            uw.set(LedgerAssetAccount::getIcon, account.getIcon());
            any = true;
        }
        if (!any) {
            // 没有可更新字段：直接返回空条件，由调用方短路（避免生成 `UPDATE t SET WHERE ...` 的非法 SQL）
            return null;
        }
        return uw;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteAccount(Long userId, Long accountId) {
        LedgerAssetAccount exist = getOwned(userId, accountId);
        if (exist == null) {
            throw new IllegalArgumentException("资产账户不存在或无权操作");
        }
        // 归档同理只改 status：若用 updateById(exist) 会把快照里的 balance/version 一起写回，
        // 并发记账的结果会被抹掉（见 LedgerAccountMetaUpdateIsolationDbTest#assetArchive...）
        int rows = baseMapper.update(null, new LambdaUpdateWrapper<LedgerAssetAccount>()
                .eq(LedgerAssetAccount::getId, accountId)
                .eq(LedgerAssetAccount::getUserId, userId)
                .set(LedgerAssetAccount::getStatus, LedgerAssetAccount.STATUS_ARCHIVED));
        if (rows == 0) {
            throw new IllegalStateException("资产账户归档失败（记录可能已被删除），请刷新后重试");
        }
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
        // 只写资产侧两列：原 updateById(exist) 会把读到的 total_liability 一起写回，
        // 与负债侧并发时可能写入陈旧值（下一笔记账会双边重算，属短暂不一致，但没必要留这个面）
        snapshotMapper.update(null, new LambdaUpdateWrapper<LedgerNetWorthSnapshot>()
                .eq(LedgerNetWorthSnapshot::getId, exist.getId())
                .set(LedgerNetWorthSnapshot::getTotalAsset, totalAsset)
                .set(LedgerNetWorthSnapshot::getNetWorth,
                        totalAsset.subtract(exist.getTotalLiability())));
    }
}
