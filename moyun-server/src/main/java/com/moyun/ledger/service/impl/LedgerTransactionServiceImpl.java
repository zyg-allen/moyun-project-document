package com.moyun.ledger.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.moyun.ledger.domain.dto.TransactionCreateDTO;
import com.moyun.ledger.domain.entity.LedgerAssetAccount;
import com.moyun.ledger.domain.entity.LedgerBudget;
import com.moyun.ledger.domain.entity.LedgerLiabilityAccount;
import com.moyun.ledger.domain.entity.LedgerNetWorthSnapshot;
import com.moyun.ledger.domain.entity.LedgerTransaction;
import com.moyun.ledger.domain.query.TransactionQuery;
import com.moyun.ledger.mapper.LedgerAssetAccountMapper;
import com.moyun.ledger.mapper.LedgerLiabilityAccountMapper;
import com.moyun.ledger.mapper.LedgerNetWorthSnapshotMapper;
import com.moyun.ledger.mapper.LedgerTransactionMapper;
import com.moyun.ledger.service.ILedgerTransactionService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;

/**
 * 记账流水服务实现（核心联动逻辑）
 *
 * <p>联动规则矩阵（设计方案 V1.2 第 5.1 节）：
 * <pre>
 * income    资产+amount              净资产+amount
 * expense   资产-amount              净资产-amount
 * transfer  资产A-amount 资产B+amount 净资产不变（守恒）
 * repayment 资产-amount 负债-amount   净资产不变
 * borrow    资产+amount 负债+amount   净资产不变（信用卡消费 account 可空，仅负债+）
 * adjust    资产设为指定值（差额=amount） 净资产随差额
 * </pre>
 *
 * <p>事务红线：创建/修改/删除均在单 @Transactional 方法内完成
 * 余额双边更新 + 流水写入 + 快照记录，保证原子性（项目硬约束）。
 *
 * @author moyun
 */
@Service
public class LedgerTransactionServiceImpl extends ServiceImpl<LedgerTransactionMapper, LedgerTransaction>
        implements ILedgerTransactionService {

    private static final Logger log = LoggerFactory.getLogger(LedgerTransactionServiceImpl.class);

    @Autowired
    private LedgerAssetAccountMapper assetAccountMapper;

    @Autowired
    private LedgerLiabilityAccountMapper liabilityAccountMapper;

    @Autowired
    private com.moyun.ledger.mapper.LedgerCategoryMapper categoryMapper;

    @Autowired
    private LedgerNetWorthSnapshotMapper snapshotMapper;

    @Autowired
    private com.moyun.ledger.mapper.LedgerBudgetMapper budgetMapper;

    @Autowired
    private com.moyun.core.config.redis.RedisCache redisCache;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long createTransaction(Long userId, TransactionCreateDTO dto) {
        validate(dto);
        LedgerTransaction txn = new LedgerTransaction();
        txn.setUserId(userId);
        applyDto(txn, dto);
        applyBalanceEffect(txn, null);
        save(txn);
        refreshNetWorthSnapshot(userId, txn.getTransactionDate());
        return txn.getId();
    }

    /**
     * 记账后站内预算提醒（设计方案 §13 预算提醒，订阅消息模板外的站内兜底通道）
     *
     * <p>当月预算使用率 ≥80% 提示「即将超支」、≥100% 提示「已超支」；
     * 每阈值每自然月仅提示一次（Redis 去重，key 有效期至月底）。
     *
     * @param userId 门户用户ID
     * @return 提示文案；无需提示返回 null
     */
    @Override
    public String checkBudgetAlert(Long userId) {
        try {
            LocalDate now = LocalDate.now();
            LocalDate monthStart = now.withDayOfMonth(1);

            // 1. 当月总预算（category_id IS NULL）
            LambdaQueryWrapper<LedgerBudget> bw = new LambdaQueryWrapper<>();
            bw.eq(LedgerBudget::getUserId, userId)
                    .eq(LedgerBudget::getYear, now.getYear())
                    .eq(LedgerBudget::getMonth, now.getMonthValue())
                    .isNull(LedgerBudget::getCategoryId);
            LedgerBudget budget = budgetMapper.selectOne(bw);
            if (budget == null || budget.getAmount() == null || budget.getAmount() <= 0) {
                return null;
            }

            // 2. 当月计入预算的支出合计
            LambdaQueryWrapper<LedgerTransaction> tw = new LambdaQueryWrapper<>();
            tw.eq(LedgerTransaction::getUserId, userId)
                    .eq(LedgerTransaction::getStatus, LedgerTransaction.STATUS_NORMAL)
                    .eq(LedgerTransaction::getType, LedgerTransaction.TYPE_EXPENSE)
                    .eq(LedgerTransaction::getIsBudget, 1)
                    .between(LedgerTransaction::getTransactionDate, monthStart, now);
            long used = list(tw).stream().mapToLong(LedgerTransaction::getAmount).sum();

            // 3. 阈值判断（≥100 优先）+ Redis 去重
            String monthKey = now.getYear() + String.format("%02d", now.getMonthValue());
            int pct = (int) Math.floorDiv(used * 100, budget.getAmount());
            // 注意：Duration.between 不支持 LocalDate（无时间单位），需转 LocalDateTime 计算 TTL
            long ttlSeconds = java.time.temporal.ChronoUnit.SECONDS.between(
                    now.atStartOfDay(), monthStart.plusMonths(1).atStartOfDay());
            if (pct >= 100) {
                String key = "ledger:budget:alert:" + userId + ":" + monthKey + ":100";
                if (redisCache.getCacheObject(key) == null) {
                    redisCache.setCacheObject(key, 1);
                    redisCache.expire(key, ttlSeconds);
                    return "本月预算已超支：" + centToYuanText(used) + " / " + centToYuanText(budget.getAmount());
                }
            } else if (pct >= 80) {
                String key = "ledger:budget:alert:" + userId + ":" + monthKey + ":80";
                if (redisCache.getCacheObject(key) == null) {
                    redisCache.setCacheObject(key, 1);
                    redisCache.expire(key, ttlSeconds);
                    return "本月预算已使用 " + pct + "%：" + centToYuanText(used) + " / " + centToYuanText(budget.getAmount());
                }
            }
            return null;
        } catch (Exception e) {
            // 提醒失败不影响记账主流程
            log.warn("预算提醒计算失败 userId={}", userId, e);
            return null;
        }
    }

    private String centToYuanText(long cent) {
        return java.math.BigDecimal.valueOf(cent, 2).toPlainString() + " 元";
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateTransaction(Long userId, Long id, TransactionCreateDTO dto) {
        validate(dto);
        LedgerTransaction old = getOwnedTransaction(userId, id);
        if (old == null) {
            throw new IllegalArgumentException("流水不存在或无权操作");
        }
        if (LedgerTransaction.STATUS_DELETED == old.getStatus()) {
            throw new IllegalArgumentException("已删除的流水不可修改");
        }
        // 1. 旧记录反向冲正（冲正不计快照列，直接回退余额）
        reverseBalanceEffect(old);
        // 2. 新记录正向重放
        LedgerTransaction txn = new LedgerTransaction();
        txn.setId(old.getId());
        txn.setUserId(userId);
        txn.setClientUuid(old.getClientUuid());
        applyDto(txn, dto);
        applyBalanceEffect(txn, null);
        // 3. 覆盖更新（保留 id/创建时间）
        txn.setCreateTime(old.getCreateTime());
        updateById(txn);
        // 4. 冲正旧日期 + 重放新日期的净资产快照
        if (!old.getTransactionDate().equals(txn.getTransactionDate())) {
            refreshNetWorthSnapshot(userId, old.getTransactionDate());
        }
        refreshNetWorthSnapshot(userId, txn.getTransactionDate());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteTransaction(Long userId, Long id) {
        LedgerTransaction old = getOwnedTransaction(userId, id);
        if (old == null) {
            throw new IllegalArgumentException("流水不存在或无权操作");
        }
        if (LedgerTransaction.STATUS_DELETED == old.getStatus()) {
            return; // 幂等：已删除直接返回
        }
        // 1. 反向冲正余额
        reverseBalanceEffect(old);
        // 2. 逻辑删除（快照保留在流水行，历史报表可追溯）
        LambdaUpdateWrapper<LedgerTransaction> uw = new LambdaUpdateWrapper<>();
        uw.eq(LedgerTransaction::getId, id)
                .eq(LedgerTransaction::getUserId, userId)
                .set(LedgerTransaction::getStatus, LedgerTransaction.STATUS_DELETED);
        update(uw);
        // 3. 刷新净资产快照
        refreshNetWorthSnapshot(userId, old.getTransactionDate());
    }

    @Override
    public Map<String, Object> pageTransactions(Long userId, TransactionQuery query) {
        LambdaQueryWrapper<LedgerTransaction> qw = new LambdaQueryWrapper<>();
        qw.eq(LedgerTransaction::getUserId, userId)
                .eq(LedgerTransaction::getStatus, LedgerTransaction.STATUS_NORMAL);
        if (query.getType() != null && !query.getType().isEmpty()) {
            qw.eq(LedgerTransaction::getType, query.getType());
        }
        // 账户筛选：作为转出方或转入方出现都算
        if (query.getAccountId() != null) {
            Long aid = query.getAccountId();
            qw.and(w -> w.eq(LedgerTransaction::getAccountId, aid)
                    .or().eq(LedgerTransaction::getTargetAccountId, aid));
        }
        if (query.getLiabilityId() != null) {
            qw.eq(LedgerTransaction::getLiabilityId, query.getLiabilityId());
        }
        if (query.getCategoryId() != null) {
            qw.eq(LedgerTransaction::getCategoryId, query.getCategoryId());
        }
        if (query.getStartDate() != null && !query.getStartDate().isEmpty()) {
            qw.ge(LedgerTransaction::getTransactionDate, LocalDate.parse(query.getStartDate()));
        }
        if (query.getEndDate() != null && !query.getEndDate().isEmpty()) {
            qw.le(LedgerTransaction::getTransactionDate, LocalDate.parse(query.getEndDate()));
        }
        qw.orderByDesc(LedgerTransaction::getTransactionDate)
                .orderByDesc(LedgerTransaction::getId);
        int pageNum = query.getPageNum() == null || query.getPageNum() < 1 ? 1 : query.getPageNum();
        int pageSize = query.getPageSize() == null || query.getPageSize() < 1 ? 20
                : Math.min(query.getPageSize(), 100);
        Page<LedgerTransaction> page = page(new Page<>(pageNum, pageSize), qw);
        fillDisplayNames(page.getRecords());
        Map<String, Object> result = new HashMap<>();
        result.put("records", page.getRecords());
        result.put("total", page.getTotal());
        return result;
    }

    /** 填充列表展示用名称（账户/负债/分类），避免前端二次查询 */
    private void fillDisplayNames(java.util.List<LedgerTransaction> records) {
        if (records == null || records.isEmpty()) {
            return;
        }
        java.util.Set<Long> assetIds = new java.util.HashSet<>();
        java.util.Set<Long> liabilityIds = new java.util.HashSet<>();
        java.util.Set<Long> categoryIds = new java.util.HashSet<>();
        for (LedgerTransaction t : records) {
            if (t.getAccountId() != null) assetIds.add(t.getAccountId());
            if (t.getTargetAccountId() != null) assetIds.add(t.getTargetAccountId());
            if (t.getLiabilityId() != null) liabilityIds.add(t.getLiabilityId());
            if (t.getCategoryId() != null) categoryIds.add(t.getCategoryId());
        }
        Map<Long, String> assetNames = new HashMap<>();
        if (!assetIds.isEmpty()) {
            for (LedgerAssetAccount a : assetAccountMapper.selectBatchIds(assetIds)) {
                assetNames.put(a.getId(), a.getName());
            }
        }
        Map<Long, String> liabilityNames = new HashMap<>();
        if (!liabilityIds.isEmpty()) {
            for (LedgerLiabilityAccount l : liabilityAccountMapper.selectBatchIds(liabilityIds)) {
                liabilityNames.put(l.getId(), l.getName());
            }
        }
        Map<Long, String> categoryNames = new HashMap<>();
        if (!categoryIds.isEmpty()) {
            for (com.moyun.ledger.domain.entity.LedgerCategory c : categoryMapper.selectBatchIds(categoryIds)) {
                categoryNames.put(c.getId(), c.getName());
            }
        }
        for (LedgerTransaction t : records) {
            if (t.getAccountId() != null) t.setAccountName(assetNames.get(t.getAccountId()));
            if (t.getTargetAccountId() != null) t.setTargetAccountName(assetNames.get(t.getTargetAccountId()));
            if (t.getLiabilityId() != null) t.setLiabilityName(liabilityNames.get(t.getLiabilityId()));
            if (t.getCategoryId() != null) t.setCategoryName(categoryNames.get(t.getCategoryId()));
        }
    }

    // ------------------------------------------------------------------
    // 私有方法：校验 / DTO 应用 / 余额联动核心
    // ------------------------------------------------------------------

    /** 入参校验（金额守恒与业务边界规则） */
    private void validate(TransactionCreateDTO dto) {
        String type = dto.getType();
        boolean typeOk = LedgerTransaction.TYPE_INCOME.equals(type)
                || LedgerTransaction.TYPE_EXPENSE.equals(type)
                || LedgerTransaction.TYPE_TRANSFER.equals(type)
                || LedgerTransaction.TYPE_REPAYMENT.equals(type)
                || LedgerTransaction.TYPE_BORROW.equals(type)
                || LedgerTransaction.TYPE_ADJUST.equals(type);
        if (!typeOk) {
            throw new IllegalArgumentException("非法记账类型：" + type);
        }
        Long amount = dto.getAmount();
        if (amount == null) {
            throw new IllegalArgumentException("金额不能为空");
        }
        if (LedgerTransaction.TYPE_ADJUST.equals(type)) {
            if (amount.compareTo(BigDecimal.ZERO.longValue()) == 0) {
                throw new IllegalArgumentException("校准差额不能为0");
            }
        } else if (amount <= 0) {
            throw new IllegalArgumentException("金额必须大于0");
        }
        if (LedgerTransaction.TYPE_TRANSFER.equals(type)) {
            if (dto.getTargetAccountId() == null) {
                throw new IllegalArgumentException("转账必须指定目标账户");
            }
            if (dto.getAccountId() != null && dto.getAccountId().equals(dto.getTargetAccountId())) {
                throw new IllegalArgumentException("转账两个账户不能相同");
            }
        }
        if (LedgerTransaction.TYPE_REPAYMENT.equals(type) || LedgerTransaction.TYPE_BORROW.equals(type)) {
            if (dto.getLiabilityId() == null) {
                throw new IllegalArgumentException("还款/借款必须指定负债账户");
            }
        }
        if (LedgerTransaction.TYPE_INCOME.equals(type) || LedgerTransaction.TYPE_EXPENSE.equals(type)
                || LedgerTransaction.TYPE_ADJUST.equals(type)) {
            if (dto.getAccountId() == null) {
                throw new IllegalArgumentException("必须指定资产账户");
            }
        }
    }

    /** DTO 应用到流水实体（金额/类型已在 validate 校验） */
    private void applyDto(LedgerTransaction txn, TransactionCreateDTO dto) {
        txn.setType(dto.getType());
        txn.setAmount(dto.getAmount());
        txn.setCategoryId(dto.getCategoryId());
        txn.setAccountId(dto.getAccountId());
        txn.setLiabilityId(dto.getLiabilityId());
        txn.setTargetAccountId(dto.getTargetAccountId());
        txn.setDescription(dto.getDescription());
        txn.setMerchant(dto.getMerchant());
        txn.setVoucherUrl(dto.getVoucherUrl());
        txn.setClientUuid(dto.getClientUuid());
        txn.setTransactionDate(dto.getTransactionDate() != null ? dto.getTransactionDate() : LocalDate.now());
        // adjust 不计预算，其余默认计入
        txn.setIsBudget(LedgerTransaction.TYPE_ADJUST.equals(dto.getType()) ? 0
                : (dto.getIsBudget() != null ? dto.getIsBudget() : 1));
        txn.setStatus(LedgerTransaction.STATUS_NORMAL);
    }

    /**
     * 正向应用余额变动并写入快照列（金额守恒与边界校验的核心）
     *
     * @param txn        新流水（写入 balanceAfter 等快照列）
     * @param oldBalance 修改场景下旧流水的快照余额（冲正后重放时用于校验基准），创建时为 null
     */
    private void applyBalanceEffect(LedgerTransaction txn, LedgerTransaction oldBalance) {
        String type = txn.getType();
        long amount = txn.getAmount();

        switch (type) {
            case LedgerTransaction.TYPE_INCOME: {
                long after = applyAssetDelta(txn.getAccountId(), amount, txn.getUserId());
                txn.setBalanceAfter(after);
                break;
            }
            case LedgerTransaction.TYPE_EXPENSE: {
                long after = applyAssetDelta(txn.getAccountId(), -amount, txn.getUserId());
                txn.setBalanceAfter(after);
                break;
            }
            case LedgerTransaction.TYPE_TRANSFER: {
                long fromAfter = applyAssetDelta(txn.getAccountId(), -amount, txn.getUserId());
                long toAfter = applyAssetDelta(txn.getTargetAccountId(), amount, txn.getUserId());
                txn.setBalanceAfter(fromAfter);
                txn.setTargetBalanceAfter(toAfter);
                break;
            }
            case LedgerTransaction.TYPE_REPAYMENT: {
                if (txn.getAccountId() != null) {
                    long assetAfter = applyAssetDelta(txn.getAccountId(), -amount, txn.getUserId());
                    txn.setBalanceAfter(assetAfter);
                }
                long liabilityAfter = applyLiabilityDelta(txn.getLiabilityId(), -amount, txn.getUserId());
                txn.setLiabilityBalanceAfter(liabilityAfter);
                break;
            }
            case LedgerTransaction.TYPE_BORROW: {
                if (txn.getAccountId() != null) {
                    long assetAfter = applyAssetDelta(txn.getAccountId(), amount, txn.getUserId());
                    txn.setBalanceAfter(assetAfter);
                }
                long liabilityAfter = applyLiabilityDelta(txn.getLiabilityId(), amount, txn.getUserId());
                txn.setLiabilityBalanceAfter(liabilityAfter);
                break;
            }
            case LedgerTransaction.TYPE_ADJUST: {
                long after = applyAssetDelta(txn.getAccountId(), amount, txn.getUserId());
                txn.setBalanceAfter(after);
                break;
            }
            default:
                throw new IllegalArgumentException("非法记账类型：" + type);
        }
    }

    /**
     * 反向冲正旧流水的余额变动（按旧金额逆向执行联动矩阵）
     */
    private void reverseBalanceEffect(LedgerTransaction old) {
        String type = old.getType();
        long amount = old.getAmount();
        switch (type) {
            case LedgerTransaction.TYPE_INCOME:
                applyAssetDelta(old.getAccountId(), -amount, old.getUserId());
                break;
            case LedgerTransaction.TYPE_EXPENSE:
                applyAssetDelta(old.getAccountId(), amount, old.getUserId());
                break;
            case LedgerTransaction.TYPE_TRANSFER:
                applyAssetDelta(old.getAccountId(), amount, old.getUserId());
                applyAssetDelta(old.getTargetAccountId(), -amount, old.getUserId());
                break;
            case LedgerTransaction.TYPE_REPAYMENT:
                if (old.getAccountId() != null) {
                    applyAssetDelta(old.getAccountId(), amount, old.getUserId());
                }
                applyLiabilityDelta(old.getLiabilityId(), amount, old.getUserId());
                break;
            case LedgerTransaction.TYPE_BORROW:
                if (old.getAccountId() != null) {
                    applyAssetDelta(old.getAccountId(), -amount, old.getUserId());
                }
                applyLiabilityDelta(old.getLiabilityId(), -amount, old.getUserId());
                break;
            case LedgerTransaction.TYPE_ADJUST:
                applyAssetDelta(old.getAccountId(), -amount, old.getUserId());
                break;
            default:
                log.warn("冲正遇到未知流水类型 type={} id={}", type, old.getId());
        }
    }

    /**
     * 资产账户余额原子增减（乐观锁），返回交易后余额
     */
    private long applyAssetDelta(Long accountId, long delta, Long userId) {
        LedgerAssetAccount account = assetAccountMapper.selectById(accountId);
        if (account == null || !account.getUserId().equals(userId)) {
            throw new IllegalArgumentException("资产账户不存在或无权操作");
        }
        if (account.getStatus() != LedgerAssetAccount.STATUS_ENABLED) {
            throw new IllegalArgumentException("资产账户已停用归档，不能记账");
        }
        LambdaUpdateWrapper<LedgerAssetAccount> uw = new LambdaUpdateWrapper<>();
        uw.eq(LedgerAssetAccount::getId, accountId)
                .eq(LedgerAssetAccount::getUserId, userId)
                .eq(LedgerAssetAccount::getVersion, account.getVersion())
                .setSql("balance = balance + (" + delta + ")")
                .setSql("version = version + 1");
        int rows = assetAccountMapper.update(null, uw);
        if (rows == 0) {
            throw new IllegalStateException("余额更新冲突（乐观锁），请重试");
        }
        return account.getBalance() + delta;
    }

    /**
     * 负债账户欠款原子增减（乐观锁 + 超额拒绝 + 结清判定），返回交易后欠款
     */
    private long applyLiabilityDelta(Long liabilityId, long delta, Long userId) {
        LedgerLiabilityAccount liability = liabilityAccountMapper.selectById(liabilityId);
        if (liability == null || !liability.getUserId().equals(userId)) {
            throw new IllegalArgumentException("负债账户不存在或无权操作");
        }
        if (liability.getStatus() != LedgerLiabilityAccount.STATUS_ENABLED) {
            throw new IllegalArgumentException("负债账户已停用归档，不能记账");
        }
        long after = liability.getBalance() + delta;
        if (after < 0) {
            throw new IllegalArgumentException("还款金额不能超过当前欠款 " + toYuan(liability.getBalance()) + " 元");
        }
        LambdaUpdateWrapper<LedgerLiabilityAccount> uw = new LambdaUpdateWrapper<>();
        uw.eq(LedgerLiabilityAccount::getId, liabilityId)
                .eq(LedgerLiabilityAccount::getUserId, userId)
                .eq(LedgerLiabilityAccount::getVersion, liability.getVersion())
                .setSql("balance = balance + (" + delta + ")")
                .setSql("version = version + 1");
        // 结清判定：冲正导致余额回到 >0 时需重置结清标记
        if (after == 0) {
            uw.set(LedgerLiabilityAccount::getSettleFlag, 1);
        } else {
            uw.set(LedgerLiabilityAccount::getSettleFlag, 0);
        }
        int rows = liabilityAccountMapper.update(null, uw);
        if (rows == 0) {
            throw new IllegalStateException("欠款更新冲突（乐观锁），请重试");
        }
        return after;
    }

    /** 获取归属当前用户的流水 */
    private LedgerTransaction getOwnedTransaction(Long userId, Long id) {
        LambdaQueryWrapper<LedgerTransaction> qw = new LambdaQueryWrapper<>();
        qw.eq(LedgerTransaction::getId, id).eq(LedgerTransaction::getUserId, userId);
        return getOne(qw);
    }

    /**
     * 刷新指定日期的净资产快照（事务内 upsert）
     */
    private void refreshNetWorthSnapshot(Long userId, LocalDate date) {
        // 聚合启用且计入合计的账户现值
        LambdaQueryWrapper<LedgerAssetAccount> aq = new LambdaQueryWrapper<>();
        aq.eq(LedgerAssetAccount::getUserId, userId)
                .eq(LedgerAssetAccount::getStatus, LedgerAssetAccount.STATUS_ENABLED)
                .eq(LedgerAssetAccount::getIncludeInTotal, 1);
        long totalAsset = 0;
        for (LedgerAssetAccount a : assetAccountMapper.selectList(aq)) {
            totalAsset += a.getBalance();
        }
        LambdaQueryWrapper<LedgerLiabilityAccount> lq = new LambdaQueryWrapper<>();
        lq.eq(LedgerLiabilityAccount::getUserId, userId)
                .eq(LedgerLiabilityAccount::getStatus, LedgerLiabilityAccount.STATUS_ENABLED)
                .eq(LedgerLiabilityAccount::getIncludeInTotal, 1);
        long totalLiability = 0;
        for (LedgerLiabilityAccount l : liabilityAccountMapper.selectList(lq)) {
            totalLiability += l.getBalance();
        }
        long netWorth = totalAsset - totalLiability;

        LambdaQueryWrapper<LedgerNetWorthSnapshot> sq = new LambdaQueryWrapper<>();
        sq.eq(LedgerNetWorthSnapshot::getUserId, userId)
                .eq(LedgerNetWorthSnapshot::getSnapDate, date);
        LedgerNetWorthSnapshot exist = snapshotMapper.selectOne(sq);
        if (exist == null) {
            LedgerNetWorthSnapshot snap = new LedgerNetWorthSnapshot();
            snap.setUserId(userId);
            snap.setSnapDate(date);
            snap.setTotalAsset(totalAsset);
            snap.setTotalLiability(totalLiability);
            snap.setNetWorth(netWorth);
            snapshotMapper.insert(snap);
        } else {
            exist.setTotalAsset(totalAsset);
            exist.setTotalLiability(totalLiability);
            exist.setNetWorth(netWorth);
            snapshotMapper.updateById(exist);
        }
    }

    /** 分转元展示（错误提示用） */
    private String toYuan(long cents) {
        return BigDecimal.valueOf(cents).divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP).toPlainString();
    }

    /** 日期格式化（日志用） */
    private static final DateTimeFormatter DTF = DateTimeFormatter.ofPattern("yyyy-MM-dd");
}
