package com.moyun.ledger.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.moyun.ledger.domain.entity.LedgerAssetAccount;
import com.moyun.ledger.domain.entity.LedgerBudget;
import com.moyun.ledger.domain.entity.LedgerCategory;
import com.moyun.ledger.domain.entity.LedgerLiabilityAccount;
import com.moyun.ledger.domain.entity.LedgerNetWorthSnapshot;
import com.moyun.ledger.domain.entity.LedgerTransaction;
import com.moyun.ledger.mapper.LedgerAssetAccountMapper;
import com.moyun.ledger.mapper.LedgerBudgetMapper;
import com.moyun.ledger.mapper.LedgerCategoryMapper;
import com.moyun.ledger.mapper.LedgerLiabilityAccountMapper;
import com.moyun.ledger.mapper.LedgerNetWorthSnapshotMapper;
import com.moyun.ledger.mapper.LedgerTransactionMapper;
import com.moyun.ledger.service.ILedgerReportService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 记账报表服务（Phase 3：报表中心 + CSV 导出）
 *
 * <p>数据量口径：个人账本年度流水千级以内，直接内存聚合（避免 GROUP BY SQL 维护成本）。
 *
 * @author moyun
 */
@Service
public class LedgerReportServiceImpl extends ServiceImpl<LedgerTransactionMapper, LedgerTransaction>
        implements ILedgerReportService {

    @Autowired
    private LedgerAssetAccountMapper assetAccountMapper;

    @Autowired
    private LedgerLiabilityAccountMapper liabilityAccountMapper;

    @Autowired
    private LedgerCategoryMapper categoryMapper;

    @Autowired
    private LedgerBudgetMapper budgetMapper;

    @Autowired
    private LedgerNetWorthSnapshotMapper snapshotMapper;

    @Override
    public Map<String, Object> overview(Long userId, int year) {
        Map<String, Object> result = new HashMap<>();

        // 1. 年度流水（正常状态），内存聚合
        LocalDate start = LocalDate.of(year, 1, 1);
        LocalDate end = LocalDate.of(year, 12, 31);
        LambdaQueryWrapper<LedgerTransaction> qw = new LambdaQueryWrapper<>();
        qw.eq(LedgerTransaction::getUserId, userId)
                .eq(LedgerTransaction::getStatus, LedgerTransaction.STATUS_NORMAL)
                .between(LedgerTransaction::getTransactionDate, start, end);
        List<LedgerTransaction> yearTxns = list(qw);

        // 1.1 月度收支趋势（计入预算的部分）
        List<Map<String, Object>> monthlyTrend = new ArrayList<>();
        BigDecimal[] incomeByMonth = new BigDecimal[13];
        BigDecimal[] expenseByMonth = new BigDecimal[13];
        for (int i = 0; i <= 12; i++) {
            incomeByMonth[i] = BigDecimal.ZERO;
            expenseByMonth[i] = BigDecimal.ZERO;
        }
        for (LedgerTransaction t : yearTxns) {
            if (t.getIsBudget() == null || t.getIsBudget() != 1) continue;
            int m = t.getTransactionDate().getMonthValue();
            if (LedgerTransaction.TYPE_INCOME.equals(t.getType())) {
                incomeByMonth[m] = incomeByMonth[m].add(t.getAmount() == null ? BigDecimal.ZERO : t.getAmount());
            }
            if (LedgerTransaction.TYPE_EXPENSE.equals(t.getType())) {
                expenseByMonth[m] = expenseByMonth[m].add(t.getAmount() == null ? BigDecimal.ZERO : t.getAmount());
            }
        }
        for (int m = 1; m <= 12; m++) {
            Map<String, Object> row = new HashMap<>();
            row.put("month", m);
            row.put("income", incomeByMonth[m]);
            row.put("expense", expenseByMonth[m]);
            monthlyTrend.add(row);
        }
        result.put("monthlyTrend", monthlyTrend);
        result.put("yearIncome", sumOf(incomeByMonth));
        result.put("yearExpense", sumOf(expenseByMonth));

        // 1.2 分类收支占比（当年汇总）
        Map<Long, BigDecimal[]> byCategory = new HashMap<>(); // [income, expense]
        java.util.Set<Long> categoryIds = new java.util.HashSet<>();
        for (LedgerTransaction t : yearTxns) {
            if (t.getCategoryId() == null) continue;
            BigDecimal[] arr = byCategory.computeIfAbsent(t.getCategoryId(), k -> new BigDecimal[]{BigDecimal.ZERO, BigDecimal.ZERO});
            if (LedgerTransaction.TYPE_INCOME.equals(t.getType())) {
                arr[0] = arr[0].add(t.getAmount() == null ? BigDecimal.ZERO : t.getAmount());
            }
            if (LedgerTransaction.TYPE_EXPENSE.equals(t.getType())) {
                arr[1] = arr[1].add(t.getAmount() == null ? BigDecimal.ZERO : t.getAmount());
            }
        }
        Map<Long, String> categoryNames = loadCategoryNames(userId, byCategory.keySet());
        result.put("categoryIncome", buildCategoryRank(byCategory, categoryNames, 0));
        result.put("categoryExpense", buildCategoryRank(byCategory, categoryNames, 1));

        // 2. 净资产趋势（近 30 天快照）
        LambdaQueryWrapper<LedgerNetWorthSnapshot> sw = new LambdaQueryWrapper<>();
        sw.eq(LedgerNetWorthSnapshot::getUserId, userId)
                .ge(LedgerNetWorthSnapshot::getSnapDate, LocalDate.now().minusDays(30))
                .orderByAsc(LedgerNetWorthSnapshot::getSnapDate);
        List<Map<String, Object>> netWorthTrend = new ArrayList<>();
        for (LedgerNetWorthSnapshot s : snapshotMapper.selectList(sw)) {
            Map<String, Object> row = new HashMap<>();
            row.put("date", s.getSnapDate().toString());
            row.put("netWorth", s.getNetWorth());
            row.put("totalAsset", s.getTotalAsset());
            row.put("totalLiability", s.getTotalLiability());
            netWorthTrend.add(row);
        }
        result.put("netWorthTrend", netWorthTrend);

        // 3. 账户余额分布（参与统计的活跃账户）
        LambdaQueryWrapper<LedgerAssetAccount> aw = new LambdaQueryWrapper<>();
        aw.eq(LedgerAssetAccount::getUserId, userId)
                .eq(LedgerAssetAccount::getStatus, 1)
                .orderByDesc(LedgerAssetAccount::getBalance);
        List<Map<String, Object>> accountDistribution = new ArrayList<>();
        for (LedgerAssetAccount a : assetAccountMapper.selectList(aw)) {
            Map<String, Object> row = new HashMap<>();
            row.put("name", a.getName());
            row.put("type", a.getType());
            row.put("balance", a.getBalance());
            accountDistribution.add(row);
        }
        result.put("accountDistribution", accountDistribution);

        // 4. 在还负债一览
        LambdaQueryWrapper<LedgerLiabilityAccount> lw = new LambdaQueryWrapper<>();
        lw.eq(LedgerLiabilityAccount::getUserId, userId)
                .eq(LedgerLiabilityAccount::getStatus, 1)
                .eq(LedgerLiabilityAccount::getSettleFlag, 0)
                .orderByAsc(LedgerLiabilityAccount::getDueDate);
        List<Map<String, Object>> liabilityOverview = new ArrayList<>();
        for (LedgerLiabilityAccount l : liabilityAccountMapper.selectList(lw)) {
            Map<String, Object> row = new HashMap<>();
            row.put("name", l.getName());
            row.put("type", l.getType());
            row.put("balance", l.getBalance());
            row.put("monthlyPayment", l.getMonthlyPayment());
            row.put("repaymentDay", l.getRepaymentDay());
            row.put("dueDate", l.getDueDate() == null ? null : l.getDueDate().toString());
            liabilityOverview.add(row);
        }
        result.put("liabilityOverview", liabilityOverview);

        // 5. 当月预算执行（月度总预算，category_id IS NULL）
        YearMonth now = YearMonth.now();
        LambdaQueryWrapper<LedgerBudget> bw = new LambdaQueryWrapper<>();
        bw.eq(LedgerBudget::getUserId, userId)
                .eq(LedgerBudget::getYear, year)
                .eq(LedgerBudget::getMonth, now.getMonthValue())
                .isNull(LedgerBudget::getCategoryId);
        LedgerBudget budget = budgetMapper.selectOne(bw);
        Map<String, Object> budgetExec = new HashMap<>();
        budgetExec.put("amount", budget != null ? budget.getAmount() : 0);
        budgetExec.put("used", expenseByMonth[now.getMonthValue()]);
        result.put("budgetExec", budgetExec);

        return result;
    }

    @Override
    public String buildCsv(Long userId, LocalDate startDate, LocalDate endDate) {
        LambdaQueryWrapper<LedgerTransaction> qw = new LambdaQueryWrapper<>();
        qw.eq(LedgerTransaction::getUserId, userId)
                .eq(LedgerTransaction::getStatus, LedgerTransaction.STATUS_NORMAL)
                .ge(LedgerTransaction::getTransactionDate, startDate != null ? startDate : LocalDate.of(2000, 1, 1))
                .le(LedgerTransaction::getTransactionDate, endDate != null ? endDate : LocalDate.now())
                .orderByDesc(LedgerTransaction::getTransactionDate)
                .orderByDesc(LedgerTransaction::getId);
        List<LedgerTransaction> txns = list(qw);

        // 名称回填（与流水列表展示字段同口径）
        java.util.Set<Long> assetIds = new java.util.HashSet<>();
        java.util.Set<Long> liabilityIds = new java.util.HashSet<>();
        java.util.Set<Long> categoryIds = new java.util.HashSet<>();
        for (LedgerTransaction t : txns) {
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
        Map<Long, String> categoryNames = loadCategoryNames(userId, categoryIds);

        StringBuilder sb = new StringBuilder();
        sb.append('\uFEFF'); // BOM：Excel 中文兼容
        sb.append("日期,类型,金额(元),分类,账户,转入账户,负债,备注,商户,凭证\n");
        for (LedgerTransaction t : txns) {
            sb.append(csv(t.getTransactionDate().toString())).append(',')
                    .append(csv(TYPE_TEXT.getOrDefault(t.getType(), t.getType()))).append(',')
                    .append(t.getAmount() != null ? t.getAmount().setScale(2, RoundingMode.HALF_UP).toPlainString() : "0.00").append(',')
                    .append(csv(categoryNames.get(t.getCategoryId()))).append(',')
                    .append(csv(assetNames.get(t.getAccountId()))).append(',')
                    .append(csv(assetNames.get(t.getTargetAccountId()))).append(',')
                    .append(csv(liabilityNames.get(t.getLiabilityId()))).append(',')
                    .append(csv(t.getDescription())).append(',')
                    .append(csv(t.getMerchant())).append(',')
                    .append(csv(t.getVoucherUrl())).append('\n');
        }
        return sb.toString();
    }

    // ------------------------------------------------------------------
    // 私有工具
    // ------------------------------------------------------------------

    private static final Map<String, String> TYPE_TEXT = Map.of(
            "income", "收入", "expense", "支出", "transfer", "转账",
            "repayment", "还款", "borrow", "借款", "adjust", "校准");

    private BigDecimal sumOf(BigDecimal[] arr) {
        BigDecimal s = BigDecimal.ZERO;
        for (int i = 1; i < arr.length; i++) s = s.add(arr[i]);
        return s;
    }

    private Map<Long, String> loadCategoryNames(Long userId, java.util.Set<Long> categoryIds) {
        Map<Long, String> names = new HashMap<>();
        if (categoryIds == null || categoryIds.isEmpty()) return names;
        // 系统预设(user_id=0) + 本人自定义
        LambdaQueryWrapper<LedgerCategory> cw = new LambdaQueryWrapper<>();
        cw.in(LedgerCategory::getId, categoryIds)
                .and(w -> w.eq(LedgerCategory::getUserId, 0L).or().eq(LedgerCategory::getUserId, userId));
        for (LedgerCategory c : categoryMapper.selectList(cw)) {
            names.put(c.getId(), c.getName());
        }
        return names;
    }

    /** 分类占比排名（idx 0=收入 1=支出，按金额降序） */
    private List<Map<String, Object>> buildCategoryRank(Map<Long, BigDecimal[]> byCategory,
                                                         Map<Long, String> names, int idx) {
        List<Map.Entry<Long, BigDecimal[]>> entries = new ArrayList<>(byCategory.entrySet());
        entries.sort((a, b) -> b.getValue()[idx].compareTo(a.getValue()[idx]));
        List<Map<String, Object>> rank = new ArrayList<>();
        for (Map.Entry<Long, BigDecimal[]> e : entries) {
            BigDecimal amount = e.getValue()[idx];
            if (amount.compareTo(BigDecimal.ZERO) <= 0) continue;
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("name", names.getOrDefault(e.getKey(), "未分类"));
            row.put("amount", amount);
            rank.add(row);
        }
        return rank;
    }

    private String centToYuan(long cent) {
        return BigDecimal.valueOf(cent, 2).setScale(2, RoundingMode.HALF_UP).toPlainString();
    }

    /** CSV 单元格转义：含逗号/引号/换行时加引号并转义内部引号 */
    private String csv(String v) {
        if (v == null || v.isEmpty()) return "";
        if (v.contains(",") || v.contains("\"") || v.contains("\n")) {
            return "\"" + v.replace("\"", "\"\"") + "\"";
        }
        return v;
    }
}
