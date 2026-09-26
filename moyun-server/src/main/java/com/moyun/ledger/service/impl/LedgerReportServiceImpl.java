package com.moyun.ledger.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.moyun.ledger.domain.entity.LedgerAssetAccount;
import com.moyun.ledger.domain.entity.LedgerBudget;
import com.moyun.ledger.domain.entity.LedgerCategory;
import com.moyun.ledger.domain.entity.LedgerLiabilityAccount;
import com.moyun.ledger.domain.entity.LedgerNetWorthSnapshot;
import com.moyun.ledger.domain.entity.LedgerTransaction;
import com.moyun.ledger.domain.vo.FinancialReportVO;
import com.moyun.ledger.domain.vo.RatiosVO;
import com.moyun.ledger.domain.vo.ScoreVO;
import com.moyun.ledger.mapper.LedgerAssetAccountMapper;
import com.moyun.ledger.mapper.LedgerBudgetMapper;
import com.moyun.ledger.mapper.LedgerCategoryMapper;
import com.moyun.ledger.mapper.LedgerLiabilityAccountMapper;
import com.moyun.ledger.mapper.LedgerNetWorthSnapshotMapper;
import com.moyun.ledger.mapper.LedgerTransactionMapper;
import com.moyun.ledger.service.ILedgerReportService;
import com.moyun.portal.domain.entity.PortalUser;
import com.moyun.portal.mapper.PortalUserMapper;
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

    @Autowired
    private PortalUserMapper portalUserMapper;

    // ==================== 财务分析报告（v2 整改：确定性计算 + 规则引擎兜底） ====================

    @Override
    public FinancialReportVO financialReport(Long userId, String period) {
        YearMonth ym = parsePeriod(period);
        LocalDate today = LocalDate.now();
        LocalDate start = ym.atDay(1);
        // 当月/未来月：截止今天；历史月：整月
        LocalDate end = ym.isAfter(YearMonth.from(today)) ? today : ym.atEndOfMonth();

        FinancialReportVO vo = new FinancialReportVO();
        vo.setPeriod(ym.toString());
        vo.setAsOfDate(today.toString());
        vo.setProfile(loadFinanceProfile(userId));

        // 1. 资产负债表（时点：启用且计入统计的账户）
        buildBalanceSheet(userId, vo.getBalanceSheet());

        // 2. 收支报表（期间：income/expense 流水，按分类语义拆主动/被动、必要/弹性）
        buildIncomeStatement(userId, start, end, vo.getIncomeStatement());

        // 3. 关键比率（确定性）
        calcRatios(vo);

        // 4. 规则引擎评分（兜底核心）
        calcScore(vo);
        return vo;
    }

    /** 期间解析（yyyy-MM；非法/为空回落当月） */
    private YearMonth parsePeriod(String period) {
        if (period != null && !period.isBlank()) {
            try {
                return YearMonth.parse(period.trim());
            } catch (Exception ignore) {
                // 非法格式回落当月
            }
        }
        return YearMonth.now();
    }

    /** 资产按类型汇总：流动(cash/savings/ewallet/stored_value)、投资(investment)、自用(fixed_asset)、其他(receivable/other) */
    private void buildBalanceSheet(Long userId, FinancialReportVO.BalanceSheetVO bs) {
        List<LedgerAssetAccount> assets = assetAccountMapper.selectList(new LambdaQueryWrapper<LedgerAssetAccount>()
                .eq(LedgerAssetAccount::getUserId, userId)
                .eq(LedgerAssetAccount::getStatus, 1)
                .eq(LedgerAssetAccount::getIncludeInTotal, 1));
        for (LedgerAssetAccount a : assets) {
            BigDecimal amount = a.getBalance() == null ? BigDecimal.ZERO : a.getBalance();
            switch (a.getType() == null ? "" : a.getType()) {
                case LedgerAssetAccount.TYPE_CASH:
                case LedgerAssetAccount.TYPE_SAVINGS:
                case LedgerAssetAccount.TYPE_EWALLET:
                case LedgerAssetAccount.TYPE_STORED_VALUE:
                    bs.setCurrentAssets(bs.getCurrentAssets().add(amount));
                    break;
                case LedgerAssetAccount.TYPE_INVESTMENT:
                    bs.setInvestmentAssets(bs.getInvestmentAssets().add(amount));
                    break;
                case LedgerAssetAccount.TYPE_FIXED_ASSET:
                    bs.setSelfUseAssets(bs.getSelfUseAssets().add(amount));
                    break;
                default:
                    bs.setOtherAssets(bs.getOtherAssets().add(amount));
                    break;
            }
        }
        bs.setTotalAssets(bs.getCurrentAssets().add(bs.getInvestmentAssets())
                .add(bs.getSelfUseAssets()).add(bs.getOtherAssets()));

        // 负债：在还且计入统计；短期(credit_card/consumer_loan/other) vs 长期(bank_loan/personal_loan≥24期)
        List<LedgerLiabilityAccount> debts = liabilityAccountMapper.selectList(
                new LambdaQueryWrapper<LedgerLiabilityAccount>()
                        .eq(LedgerLiabilityAccount::getUserId, userId)
                        .eq(LedgerLiabilityAccount::getStatus, 1)
                        .and(w -> w.isNull(LedgerLiabilityAccount::getSettleFlag)
                                .or().ne(LedgerLiabilityAccount::getSettleFlag, 1)));
        for (LedgerLiabilityAccount d : debts) {
            if (d.getIncludeInTotal() != null && d.getIncludeInTotal() != 1) continue;
            BigDecimal amount = d.getBalance() == null ? BigDecimal.ZERO : d.getBalance();
            if (isLongTermDebt(d)) {
                bs.setLongTermLiabilities(bs.getLongTermLiabilities().add(amount));
            } else {
                bs.setShortTermLiabilities(bs.getShortTermLiabilities().add(amount));
            }
        }
        bs.setTotalLiabilities(bs.getShortTermLiabilities().add(bs.getLongTermLiabilities()));
        bs.setNetAssets(bs.getTotalAssets().subtract(bs.getTotalLiabilities()));
    }

    /** 长期负债判定：银行贷款恒为长期；消费贷/个人借款按总期数≥24期为长期；信用卡/其他为短期 */
    private boolean isLongTermDebt(LedgerLiabilityAccount d) {
        String type = d.getType() == null ? "" : d.getType();
        if (LedgerLiabilityAccount.TYPE_BANK_LOAN.equals(type)) return true;
        if (LedgerLiabilityAccount.TYPE_CREDIT_CARD.equals(type)
                || LedgerLiabilityAccount.TYPE_OTHER.equals(type)) return false;
        return d.getTotalTerms() != null && d.getTotalTerms() >= 24;
    }

    /** 收支报表：主动/被动收入、必要/弹性支出（分类语义 + 关键词兜底） */
    private void buildIncomeStatement(Long userId, LocalDate start, LocalDate end,
                                      FinancialReportVO.IncomeStatementVO is) {
        List<LedgerTransaction> txns = list(new LambdaQueryWrapper<LedgerTransaction>()
                .eq(LedgerTransaction::getUserId, userId)
                .eq(LedgerTransaction::getStatus, LedgerTransaction.STATUS_NORMAL)
                .between(LedgerTransaction::getTransactionDate, start, end)
                .in(LedgerTransaction::getType,
                        LedgerTransaction.TYPE_INCOME, LedgerTransaction.TYPE_EXPENSE));
        // 分类语义（groupName 优先于名称关键词）
        Map<Long, LedgerCategory> categories = loadCategoryMap(userId);
        for (LedgerTransaction t : txns) {
            BigDecimal amount = t.getAmount() == null ? BigDecimal.ZERO : t.getAmount();
            LedgerCategory c = t.getCategoryId() == null ? null : categories.get(t.getCategoryId());
            String name = c == null ? "" : c.getName();
            String group = c == null || c.getGroupName() == null ? "" : c.getGroupName();
            if (LedgerTransaction.TYPE_INCOME.equals(t.getType())) {
                if (isPassiveIncome(name, group)) {
                    is.setPassiveIncome(is.getPassiveIncome().add(amount));
                } else {
                    is.setActiveIncome(is.getActiveIncome().add(amount));
                }
            } else {
                if (isNecessaryExpense(name, group)) {
                    is.setNecessaryExpense(is.getNecessaryExpense().add(amount));
                } else {
                    is.setFlexibleExpense(is.getFlexibleExpense().add(amount));
                }
            }
        }
        is.setTotalIncome(is.getActiveIncome().add(is.getPassiveIncome()));
        is.setTotalExpense(is.getNecessaryExpense().add(is.getFlexibleExpense()));
        is.setSurplus(is.getTotalIncome().subtract(is.getTotalExpense()));

        // 月供合计（负债收入比口径：在还负债的每月还款额）
        List<LedgerLiabilityAccount> debts = liabilityAccountMapper.selectList(
                new LambdaQueryWrapper<LedgerLiabilityAccount>()
                        .eq(LedgerLiabilityAccount::getUserId, userId)
                        .eq(LedgerLiabilityAccount::getStatus, 1)
                        .and(w -> w.isNull(LedgerLiabilityAccount::getSettleFlag)
                                .or().ne(LedgerLiabilityAccount::getSettleFlag, 1)));
        for (LedgerLiabilityAccount d : debts) {
            if (d.getMonthlyPayment() != null) {
                is.setMonthlyDebtPayment(is.getMonthlyDebtPayment().add(d.getMonthlyPayment()));
            }
        }
    }

    /** 分类映射（系统预设 user_id=0 + 本人自定义） */
    private Map<Long, LedgerCategory> loadCategoryMap(Long userId) {
        Map<Long, LedgerCategory> map = new HashMap<>();
        for (LedgerCategory c : categoryMapper.selectList(new LambdaQueryWrapper<LedgerCategory>()
                .and(w -> w.eq(LedgerCategory::getUserId, 0L).or().eq(LedgerCategory::getUserId, userId)))) {
            map.put(c.getId(), c);
        }
        return map;
    }

    /** 被动收入：非劳动所得（利息/租金/分红/投资收益等） */
    private boolean isPassiveIncome(String name, String group) {
        String text = group + name;
        return text.contains("利息") || text.contains("租金") || text.contains("分红")
                || text.contains("股息") || text.contains("投资") || text.contains("理财")
                || text.contains("版税") || text.contains("被动");
    }

    /** 必要支出：生活刚需（语义分组优先，名称关键词兜底） */
    private boolean isNecessaryExpense(String name, String group) {
        if (group.contains("刚需") || group.contains("必需") || group.contains("固定")
                || group.contains("生活")) return true;
        if (group.contains("弹性") || group.contains("享受") || group.contains("娱乐")
                || group.contains("人情")) return false;
        String[] keywords = {"餐", "食", "粮油", "菜", "房", "租", "水电", "燃气", "物业",
                "交通", "加油", "通讯", "话费", "网费", "日用", "医疗", "药", "教育",
                "学费", "保险", "还款", "贷", "孩子", "母婴", "宠物粮"};
        for (String k : keywords) {
            if (name.contains(k)) return true;
        }
        return false;
    }

    /** 6 个关键比率（确定性；分母为 0 时置 null 表示无数据） */
    private void calcRatios(FinancialReportVO vo) {
        RatiosVO r = vo.getRatios();
        FinancialReportVO.BalanceSheetVO bs = vo.getBalanceSheet();
        FinancialReportVO.IncomeStatementVO is = vo.getIncomeStatement();

        // 1. 资产负债率 = 总负债 / 总资产
        if (bs.getTotalAssets().compareTo(BigDecimal.ZERO) > 0) {
            r.setDebtToAsset(pct(bs.getTotalLiabilities(), bs.getTotalAssets()));
            r.setDebtToAssetLevel(debtToAssetLevel(r.getDebtToAsset()));
        }
        // 2. 紧急预备金月数 = 流动资产 / 月支出
        if (is.getTotalExpense().compareTo(BigDecimal.ZERO) > 0) {
            r.setEmergencyFundMonths(round1(bs.getCurrentAssets()
                    .divide(is.getTotalExpense(), 2, RoundingMode.HALF_UP)));
            r.setEmergencyFundLevel(emergencyFundLevel(r.getEmergencyFundMonths()));
        }
        // 3. 结余率 = 结余 / 总收入
        if (is.getTotalIncome().compareTo(BigDecimal.ZERO) > 0) {
            r.setSurplusRatio(pct(is.getSurplus(), is.getTotalIncome()));
            r.setSurplusLevel(surplusLevel(r.getSurplusRatio()));
        }
        // 4. 负债收入比 = 月供 / 总收入
        if (is.getTotalIncome().compareTo(BigDecimal.ZERO) > 0) {
            r.setDebtToIncome(pct(is.getMonthlyDebtPayment(), is.getTotalIncome()));
            r.setDebtToIncomeLevel(debtToIncomeLevel(r.getDebtToIncome()));
        }
        // 5. 被动收入占比 = 被动收入 / 总收入
        if (is.getTotalIncome().compareTo(BigDecimal.ZERO) > 0) {
            r.setPassiveIncomeRatio(pct(is.getPassiveIncome(), is.getTotalIncome()));
            r.setPassiveIncomeLevel(passiveIncomeLevel(r.getPassiveIncomeRatio()));
        }
        // 6. 投资资产占比 = 投资资产 / 总资产（辅助指标，不参与评分）
        if (bs.getTotalAssets().compareTo(BigDecimal.ZERO) > 0) {
            r.setInvestmentAssetRatio(pct(bs.getInvestmentAssets(), bs.getTotalAssets()));
            r.setInvestmentAssetLevel(investmentAssetLevel(r.getInvestmentAssetRatio()));
        }
    }

    /** 规则引擎评分：5 项加权（30/25/20/15/10），A=满分/B=0.6/C=0.1；无数据项按剩余权重归一化 */
    private void calcScore(FinancialReportVO vo) {
        RatiosVO r = vo.getRatios();
        ScoreVO s = vo.getScore();
        double earned = 0;
        double available = 0;
        earned += addScore(s, "资产负债率", 30, r.getDebtToAssetLevel(), r.getDebtToAsset(), "%");
        available += weightOf(r.getDebtToAssetLevel(), 30);
        earned += addScore(s, "紧急预备金", 25, r.getEmergencyFundLevel(), r.getEmergencyFundMonths(), "个月");
        available += weightOf(r.getEmergencyFundLevel(), 25);
        earned += addScore(s, "结余率", 20, r.getSurplusLevel(), r.getSurplusRatio(), "%");
        available += weightOf(r.getSurplusLevel(), 20);
        earned += addScore(s, "负债收入比", 15, r.getDebtToIncomeLevel(), r.getDebtToIncome(), "%");
        available += weightOf(r.getDebtToIncomeLevel(), 15);
        earned += addScore(s, "被动收入占比", 10, r.getPassiveIncomeLevel(), r.getPassiveIncomeRatio(), "%");
        available += weightOf(r.getPassiveIncomeLevel(), 10);

        int total = available > 0
                ? (int) Math.round(earned / available * 100) : 0;
        s.setTotal(total);
        if (total >= 85) { s.setGrade("A"); s.setGradeLabel("优秀"); }
        else if (total >= 70) { s.setGrade("B"); s.setGradeLabel("良好"); }
        else if (total >= 60) { s.setGrade("C"); s.setGradeLabel("一般"); }
        else { s.setGrade("D"); s.setGradeLabel("预警"); }
        s.setDesc(gradeDesc(s.getGrade(), total));
    }

    /** 单项计分 + 明细（level=null 表示无数据，计 0 分且不参与归一化分母） */
    private double addScore(ScoreVO s, String name, int weight, String level, Double value, String unit) {
        Map<String, Object> item = new LinkedHashMap<>();
        item.put("name", name);
        item.put("weight", weight);
        item.put("level", level);
        item.put("ratioValue", value);
        item.put("ratioLabel", value == null ? "无数据" : stripTrailingZero(value) + unit);
        if (level == null) {
            item.put("score", 0);
            s.getDetail().add(item);
            return 0;
        }
        double factor = "A".equals(level) ? 1.0 : "B".equals(level) ? 0.6 : 0.1;
        double score = weight * factor;
        item.put("score", (int) Math.round(score));
        s.getDetail().add(item);
        return score;
    }

    private double weightOf(String level, int weight) {
        return level == null ? 0 : weight;
    }

    private String gradeDesc(String grade, int total) {
        switch (grade) {
            case "A": return "财务状况优秀，资产负债结构合理，流动性充足，建议适度投资。";
            case "B": return "财务状况良好，结余能力较强，但部分指标有提升空间，建议补充应急储备。";
            case "C": return "财务状况一般，存在一定风险，建议优先偿债并控制支出。";
            default: return total == 0
                    ? "暂无足够记账数据，先去记几笔账，即可生成财务健康评估。"
                    : "财务状况预警，负债过高或流动性严重不足，建议立即制定改善计划。";
        }
    }

    // ---- 比率评级阈值（与评分卡一致） ----
    private String debtToAssetLevel(Double v) {
        if (v == null) return null;
        if (v < 50) return "A";
        return v <= 70 ? "B" : "C";
    }

    private String emergencyFundLevel(Double v) {
        if (v == null) return null;
        if (v >= 6) return "A";
        return v >= 3 ? "B" : "C";
    }

    private String surplusLevel(Double v) {
        if (v == null) return null;
        if (v >= 30) return "A";
        return v >= 10 ? "B" : "C";
    }

    private String debtToIncomeLevel(Double v) {
        if (v == null) return null;
        if (v < 30) return "A";
        return v <= 50 ? "B" : "C";
    }

    private String passiveIncomeLevel(Double v) {
        if (v == null) return null;
        if (v >= 20) return "A";
        return v >= 10 ? "B" : "C";
    }

    private String investmentAssetLevel(Double v) {
        if (v == null) return null;
        if (v >= 20) return "A";
        return v >= 10 ? "B" : "C";
    }

    /** 除法转百分比（1 位小数） */
    private Double pct(BigDecimal part, BigDecimal whole) {
        if (whole.compareTo(BigDecimal.ZERO) <= 0) return null;
        return part.multiply(BigDecimal.valueOf(100))
                .divide(whole, 1, RoundingMode.HALF_UP).doubleValue();
    }

    private Double round1(BigDecimal v) {
        return v.setScale(1, RoundingMode.HALF_UP).doubleValue();
    }

    private String stripTrailingZero(Double v) {
        return v % 1 == 0 ? String.valueOf(v.intValue()) : String.valueOf(v);
    }

    /** 用户画像（AI 财务分析基础数据；缺失字段不上送） */
    private Map<String, Object> loadFinanceProfile(Long userId) {
        Map<String, Object> p = new LinkedHashMap<>();
        try {
            PortalUser user = portalUserMapper.selectById(userId);
            if (user == null) return p;
            Integer age = ageOf(user.getBirthday());
            if (age != null) p.put("age", age);
            if (notBlank(user.getIndustry())) p.put("industry", user.getIndustry());
            if (notBlank(user.getPosition())) p.put("position", user.getPosition());
            if (notBlank(user.getMaritalStatus())) p.put("maritalStatus", user.getMaritalStatus());
            if (notBlank(user.getMaritalStatus())) p.put("maritalStatusLabel", maritalLabel(user.getMaritalStatus()));
            if (user.getHasMortgage() != null) p.put("hasMortgage", user.getHasMortgage() == 1);
            if (user.getHasSideIncome() != null) p.put("hasSideIncome", user.getHasSideIncome() == 1);
            if (notBlank(user.getIncomeTypes())) p.put("incomeTypes", user.getIncomeTypes().split(","));
        } catch (Exception ignore) {
            // 画像缺失不影响报表主体
        }
        return p;
    }

    /** 生日（yyyy-MM-dd）→ 年龄 */
    private Integer ageOf(String birthday) {
        if (birthday == null || birthday.isBlank()) return null;
        try {
            LocalDate birth = LocalDate.parse(birthday.trim());
            return java.time.Period.between(birth, LocalDate.now()).getYears();
        } catch (Exception e) {
            return null;
        }
    }

    private String maritalLabel(String code) {
        switch (code == null ? "" : code) {
            case "single": return "单身";
            case "married": return "已婚";
            case "other": return "其他";
            default: return code;
        }
    }

    private boolean notBlank(String s) {
        return s != null && !s.isBlank();
    }


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
