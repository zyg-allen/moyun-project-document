package com.moyun.ledger.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.moyun.ledger.domain.entity.LedgerAssetAccount;
import com.moyun.ledger.domain.entity.LedgerBudget;
import com.moyun.ledger.domain.entity.LedgerLiabilityAccount;
import com.moyun.ledger.domain.entity.LedgerNetWorthSnapshot;
import com.moyun.ledger.domain.entity.LedgerTransaction;
import com.moyun.ledger.mapper.LedgerAssetAccountMapper;
import com.moyun.ledger.mapper.LedgerBudgetMapper;
import com.moyun.ledger.mapper.LedgerLiabilityAccountMapper;
import com.moyun.ledger.mapper.LedgerNetWorthSnapshotMapper;
import com.moyun.ledger.mapper.LedgerTransactionMapper;
import com.moyun.ledger.service.ILedgerDashboardService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 记账总览服务实现
 *
 * <p>统计口径（设计方案 V1.2）：
 * 净资产/总资产/总负债 = 账户现值聚合；涨跌 = 与昨日快照对比；
 * 本月收支 = 流水表 status=1 且 is_budget=1 按月实时聚合。
 *
 * @author moyun
 */
@Service
public class LedgerDashboardServiceImpl implements ILedgerDashboardService {

    @Autowired
    private LedgerAssetAccountMapper assetAccountMapper;

    @Autowired
    private LedgerLiabilityAccountMapper liabilityAccountMapper;

    @Autowired
    private LedgerTransactionMapper transactionMapper;

    @Autowired
    private LedgerBudgetMapper budgetMapper;

    @Autowired
    private LedgerNetWorthSnapshotMapper snapshotMapper;

    @Override
    public Map<String, Object> dashboard(Long userId) {
        Map<String, Object> data = new HashMap<>();
        LocalDate today = LocalDate.now();
        LocalDate monthStart = today.withDayOfMonth(1);

        // 1. 总资产 / 总负债 / 净资产（启用且计入合计的账户现值）
        LambdaQueryWrapper<LedgerAssetAccount> aq = new LambdaQueryWrapper<>();
        aq.eq(LedgerAssetAccount::getUserId, userId)
                .eq(LedgerAssetAccount::getStatus, LedgerAssetAccount.STATUS_ENABLED)
                .eq(LedgerAssetAccount::getIncludeInTotal, 1);
        BigDecimal totalAsset = BigDecimal.ZERO;
        List<LedgerAssetAccount> assets = assetAccountMapper.selectList(aq);
        for (LedgerAssetAccount a : assets) {
            if (a.getBalance() != null) {
                totalAsset = totalAsset.add(a.getBalance());
            }
        }

        LambdaQueryWrapper<LedgerLiabilityAccount> lq = new LambdaQueryWrapper<>();
        lq.eq(LedgerLiabilityAccount::getUserId, userId)
                .eq(LedgerLiabilityAccount::getStatus, LedgerLiabilityAccount.STATUS_ENABLED)
                .eq(LedgerLiabilityAccount::getIncludeInTotal, 1);
        BigDecimal totalLiability = BigDecimal.ZERO;
        List<LedgerLiabilityAccount> liabilities = liabilityAccountMapper.selectList(lq);
        for (LedgerLiabilityAccount l : liabilities) {
            if (l.getBalance() != null) {
                totalLiability = totalLiability.add(l.getBalance());
            }
        }
        BigDecimal netWorth = totalAsset.subtract(totalLiability);

        // 资产/负债账户数（前端新手引导判断：均为 0 时显示引导卡片）
        LambdaQueryWrapper<LedgerAssetAccount> aqAll = new LambdaQueryWrapper<>();
        aqAll.eq(LedgerAssetAccount::getUserId, userId)
                .eq(LedgerAssetAccount::getStatus, LedgerAssetAccount.STATUS_ENABLED);
        LambdaQueryWrapper<LedgerLiabilityAccount> lqAll = new LambdaQueryWrapper<>();
        lqAll.eq(LedgerLiabilityAccount::getUserId, userId)
                .eq(LedgerLiabilityAccount::getStatus, LedgerLiabilityAccount.STATUS_ENABLED);
        data.put("assetAccountCount", assetAccountMapper.selectCount(aqAll));
        data.put("liabilityAccountCount", liabilityAccountMapper.selectCount(lqAll));

        data.put("totalAsset", totalAsset);
        data.put("totalLiability", totalLiability);
        data.put("netWorth", netWorth);

        // 2. 涨跌标识（对比昨日快照；无昨日快照时与当日快照对比，即涨跌为0）
        LambdaQueryWrapper<LedgerNetWorthSnapshot> yq = new LambdaQueryWrapper<>();
        yq.eq(LedgerNetWorthSnapshot::getUserId, userId)
                .lt(LedgerNetWorthSnapshot::getSnapDate, today)
                .orderByDesc(LedgerNetWorthSnapshot::getSnapDate)
                .last("LIMIT 1");
        LedgerNetWorthSnapshot yesterday = snapshotMapper.selectOne(yq);
        BigDecimal netWorthChange = yesterday == null ? BigDecimal.ZERO : netWorth.subtract(yesterday.getNetWorth());
        data.put("netWorthChange", netWorthChange);

        // 3. 本月收支（status=1 且计入预算的流水实时聚合）
        BigDecimal monthIncome = sumAmount(userId, monthStart, today, LedgerTransaction.TYPE_INCOME);
        BigDecimal monthExpense = sumAmount(userId, monthStart, today, LedgerTransaction.TYPE_EXPENSE);
        data.put("monthIncome", monthIncome);
        data.put("monthExpense", monthExpense);

        // 4. 本月预算进度（总预算）
        LambdaQueryWrapper<LedgerBudget> bq = new LambdaQueryWrapper<>();
        bq.eq(LedgerBudget::getUserId, userId)
                .eq(LedgerBudget::getYear, today.getYear())
                .eq(LedgerBudget::getMonth, today.getMonthValue())
                .isNull(LedgerBudget::getCategoryId);
        LedgerBudget totalBudget = budgetMapper.selectOne(bq);
        Map<String, Object> budgetInfo = new HashMap<>();
        if (totalBudget != null) {
            long rate = totalBudget.getAmount().compareTo(BigDecimal.ZERO) == 0 ? 100
                    : monthExpense.multiply(BigDecimal.valueOf(100))
                            .divide(totalBudget.getAmount(), 0, RoundingMode.HALF_UP)
                            .longValue();
            budgetInfo.put("amount", totalBudget.getAmount());
            budgetInfo.put("used", monthExpense);
            budgetInfo.put("rate", rate);
        }
        data.put("budget", budgetInfo);

        // 5. 最近 20 条流水（索引 idx_user_status_id 命中）
        LambdaQueryWrapper<LedgerTransaction> tq = new LambdaQueryWrapper<>();
        tq.eq(LedgerTransaction::getUserId, userId)
                .eq(LedgerTransaction::getStatus, LedgerTransaction.STATUS_NORMAL)
                .orderByDesc(LedgerTransaction::getId)
                .last("LIMIT 20");
        data.put("recentTransactions", transactionMapper.selectList(tq));

        return data;
    }

    /** 按类型汇总区间内金额（计入预算口径） */
    private BigDecimal sumAmount(Long userId, LocalDate start, LocalDate end, String type) {
        LambdaQueryWrapper<LedgerTransaction> qw = new LambdaQueryWrapper<>();
        qw.eq(LedgerTransaction::getUserId, userId)
                .eq(LedgerTransaction::getType, type)
                .eq(LedgerTransaction::getStatus, LedgerTransaction.STATUS_NORMAL)
                .eq(LedgerTransaction::getIsBudget, 1)
                .ge(LedgerTransaction::getTransactionDate, start)
                .le(LedgerTransaction::getTransactionDate, end);
        BigDecimal sum = BigDecimal.ZERO;
        for (LedgerTransaction t : transactionMapper.selectList(qw)) {
            if (t.getAmount() != null) {
                sum = sum.add(t.getAmount());
            }
        }
        return sum;
    }
}
