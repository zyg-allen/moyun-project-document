package com.moyun.ledger.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.moyun.core.base.entity.SysDictData;
import com.moyun.ext.ai.entity.ModelConfig;
import com.moyun.ext.ai.service.ModelConfigService;
import com.moyun.ledger.domain.entity.LedgerAssetAccount;
import com.moyun.ledger.domain.entity.LedgerCategory;
import com.moyun.ledger.domain.entity.LedgerLiabilityAccount;
import com.moyun.ledger.domain.entity.LedgerTransaction;
import com.moyun.ledger.mapper.LedgerAssetAccountMapper;
import com.moyun.ledger.mapper.LedgerCategoryMapper;
import com.moyun.ledger.mapper.LedgerLiabilityAccountMapper;
import com.moyun.ledger.mapper.LedgerTransactionMapper;
import com.moyun.ledger.service.ILedgerAiAnalysisService;
import com.moyun.portal.domain.entity.PortalUser;
import com.moyun.portal.mapper.PortalUserMapper;
import com.moyun.portal.service.IPortalUserService;
import com.moyun.system.service.ISysDictTypeService;
import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.model.chat.ChatLanguageModel;
import dev.langchain4j.model.chat.response.ChatResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 记账 AI 财务分析服务实现
 *
 * 规则引擎计算硬指标（资产负债率/还款压力/入不敷出等），
 * LLM 生成个性化综述与建议（AI 未启用或调用失败时降级为模板文案）。
 *
 * @author moyun
 */
@Service
public class LedgerAiAnalysisServiceImpl implements ILedgerAiAnalysisService {

    private static final Logger log = LoggerFactory.getLogger(LedgerAiAnalysisServiceImpl.class);

    private static final String DICT_IDENTITY_TAG = "ledger_identity_tag";

    @Autowired
    private LedgerTransactionMapper transactionMapper;
    @Autowired
    private LedgerCategoryMapper categoryMapper;
    @Autowired
    private LedgerAssetAccountMapper assetAccountMapper;
    @Autowired
    private LedgerLiabilityAccountMapper liabilityAccountMapper;
    @Autowired
    private IPortalUserService portalUserService;
    @Autowired
    private PortalUserMapper portalUserMapper;
    @Autowired
    private ISysDictTypeService dictTypeService;
    /** LLM 能力由 ext-ai 模块提供，未启用时为空（降级模板） */
    @Autowired(required = false)
    private ModelConfigService modelConfigService;

    @Override
    public Map<String, Object> analyze(Long userId) {
        PortalUser user = portalUserService.selectPortalUserById(userId);
        LocalDate today = LocalDate.now();
        LocalDate sixMonthsAgo = today.minusMonths(6).withDayOfMonth(1);

        // ===== 数据聚合 =====
        List<LedgerTransaction> recent = transactionMapper.selectList(new LambdaQueryWrapper<LedgerTransaction>()
                .eq(LedgerTransaction::getUserId, userId)
                .eq(LedgerTransaction::getStatus, 1)
                .ge(LedgerTransaction::getTransactionDate, sixMonthsAgo)
                .le(LedgerTransaction::getTransactionDate, today));
        List<LedgerLiabilityAccount> debts = liabilityAccountMapper.selectList(
                new LambdaQueryWrapper<LedgerLiabilityAccount>()
                        .eq(LedgerLiabilityAccount::getUserId, userId)
                        .eq(LedgerLiabilityAccount::getStatus, 1)
                        .and(w -> w.isNull(LedgerLiabilityAccount::getSettleFlag)
                                .or().ne(LedgerLiabilityAccount::getSettleFlag, 1)));
        Map<Long, String> catNames = loadCategoryNames(userId);

        // ===== 核心指标 =====
        BigDecimal totalIncome = recent.stream()
                .filter(t -> "income".equals(t.getType()))
                .map(t -> t.getAmount() == null ? BigDecimal.ZERO : t.getAmount())
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal totalExpense = recent.stream()
                .filter(t -> "expense".equals(t.getType()))
                .map(t -> t.getAmount() == null ? BigDecimal.ZERO : t.getAmount())
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal monthlyRepayment = debts.stream()
                .filter(d -> d.getMonthlyPayment() != null)
                .map(d -> d.getMonthlyPayment() == null ? BigDecimal.ZERO : d.getMonthlyPayment())
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal totalLiability = debts.stream()
                .filter(d -> d.getIncludeInTotal() == null || d.getIncludeInTotal() == 1)
                .map(d -> d.getBalance() == null ? BigDecimal.ZERO : d.getBalance())
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal totalAsset = assetAccountMapper.selectList(new LambdaQueryWrapper<LedgerAssetAccount>()
                        .eq(LedgerAssetAccount::getUserId, userId)
                        .eq(LedgerAssetAccount::getStatus, 1)
                        .eq(LedgerAssetAccount::getIncludeInTotal, 1))
                .stream()
                .map(a -> a.getBalance() == null ? BigDecimal.ZERO : a.getBalance())
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        double months = Math.max(1, recent.stream().map(LedgerTransaction::getTransactionDate)
                .map(YearMonth::from).distinct().count());
        BigDecimal avgMonthlyIncome = totalIncome.divide(BigDecimal.valueOf(months), 2, RoundingMode.HALF_UP);
        BigDecimal avgMonthlyExpense = totalExpense.divide(BigDecimal.valueOf(months), 2, RoundingMode.HALF_UP);

        // 近3月逐月判断是否入不敷出
        Map<YearMonth, BigDecimal[]> monthly = new LinkedHashMap<>();
        for (LedgerTransaction t : recent) {
            if (!"income".equals(t.getType()) && !"expense".equals(t.getType())) continue;
            BigDecimal[] arr = monthly.computeIfAbsent(YearMonth.from(t.getTransactionDate()), k -> new BigDecimal[2]);
            if (arr[0] == null) arr[0] = BigDecimal.ZERO;
            if (arr[1] == null) arr[1] = BigDecimal.ZERO;
            if ("income".equals(t.getType())) {
                arr[0] = arr[0].add(t.getAmount() == null ? BigDecimal.ZERO : t.getAmount());
            } else {
                arr[1] = arr[1].add(t.getAmount() == null ? BigDecimal.ZERO : t.getAmount());
            }
        }
        int deficitMonths = 0;
        List<YearMonth> keys = new ArrayList<>(monthly.keySet());
        for (int i = Math.max(0, keys.size() - 3); i < keys.size(); i++) {
            BigDecimal[] arr = monthly.get(keys.get(i));
            if (arr[1].compareTo(arr[0]) > 0) deficitMonths++;
        }

        double debtRatio = totalAsset.compareTo(BigDecimal.ZERO) > 0 
                ? totalLiability.divide(totalAsset, 4, RoundingMode.HALF_UP).doubleValue() 
                : (totalLiability.compareTo(BigDecimal.ZERO) > 0 ? 1.0 : 0.0);
        double repaymentPressure = avgMonthlyIncome.compareTo(BigDecimal.ZERO) > 0 
                ? monthlyRepayment.divide(avgMonthlyIncome, 4, RoundingMode.HALF_UP).doubleValue()
                : (monthlyRepayment.compareTo(BigDecimal.ZERO) > 0 ? 1.0 : 0.0);
        double savingRate = avgMonthlyIncome.compareTo(BigDecimal.ZERO) > 0
                ? Math.max(0, avgMonthlyIncome.subtract(avgMonthlyExpense)
                        .divide(avgMonthlyIncome, 4, RoundingMode.HALF_UP).doubleValue()) : 0;

        // ===== 收入来源（近6月 income 分类占比） =====
        Map<Long, BigDecimal> incomeByCat = new LinkedHashMap<>();
        for (LedgerTransaction t : recent) {
            if (!"income".equals(t.getType()) || t.getCategoryId() == null) continue;
            BigDecimal amount = t.getAmount() == null ? BigDecimal.ZERO : t.getAmount();
            incomeByCat.merge(t.getCategoryId(), amount, BigDecimal::add);
        }
        List<Map<String, Object>> incomeSources = new ArrayList<>();
        BigDecimal incomeTotal = incomeByCat.values().stream().reduce(BigDecimal.ZERO, BigDecimal::add);
        incomeByCat.entrySet().stream()
                .sorted((a, b) -> b.getValue().compareTo(a.getValue()))
                .forEach(e -> {
                    Map<String, Object> src = new HashMap<>();
                    src.put("name", catNames.getOrDefault(e.getKey(), "其他收入"));
                    src.put("amount", e.getValue());
                    src.put("ratio", incomeTotal.compareTo(BigDecimal.ZERO) > 0 
                            ? e.getValue().multiply(BigDecimal.TEN)
                                    .divide(incomeTotal, 1, RoundingMode.HALF_UP)
                                    .doubleValue()
                            : 0);
                    incomeSources.add(src);
                });

        // ===== 债务风险（规则引擎） =====
        List<Map<String, Object>> debtRisks = new ArrayList<>();
        int todayDay = today.getDayOfMonth();
        for (LedgerLiabilityAccount d : debts) {
            boolean isCard = "credit_card".equals(d.getType());
            boolean isCarLoan = "bank_loan".equals(d.getType())
                    || (d.getName() != null && d.getName().contains("车贷"));
            if ((isCard || isCarLoan) && d.getRepaymentDay() != null) {
                int diff = d.getRepaymentDay() - todayDay;
                if (diff >= 0 && diff <= 5) {
                    debtRisks.add(risk("high", (isCard ? "信用卡" : "车贷") + "还款日临近",
                            "「" + d.getName() + "」" + d.getRepaymentDay() + "日还款"
                                    + (d.getMonthlyPayment() != null ? "，月供 ¥" + yuan(d.getMonthlyPayment()) : "")
                                    + "，请确保账户余额充足。"));
                }
            }
            if (isCard && d.getBalance() != null && avgMonthlyIncome.compareTo(BigDecimal.ZERO) > 0 
                    && d.getBalance().compareTo(avgMonthlyIncome.multiply(BigDecimal.valueOf(3))) > 0) {
                debtRisks.add(risk("mid", "信用卡负债偏高",
                        "「" + d.getName() + "」欠款已达月收入的 3 倍以上，建议优先偿还、避免循环利息。"));
            }
        }
        if (debtRatio >= 0.8 && totalLiability.compareTo(BigDecimal.ZERO) > 0) {
            debtRisks.add(risk("high", "资产负债率过高",
                    "当前资产负债率 " + pct(debtRatio) + "（负债 ¥" + yuan(totalLiability)
                            + " / 资产 ¥" + yuan(totalAsset) + "），建议控制新增负债、优先偿还高息债务。"));
        } else if (debtRatio >= 0.5 && totalLiability.compareTo(BigDecimal.ZERO) > 0) {
            debtRisks.add(risk("mid", "负债水平中等偏高",
                    "当前资产负债率 " + pct(debtRatio) + "，处于 50%-80% 区间，注意留足应急资金。"));
        }
        if (repaymentPressure >= 0.5 && monthlyRepayment.compareTo(BigDecimal.ZERO) > 0) {
            debtRisks.add(risk(avgMonthlyIncome.compareTo(BigDecimal.ZERO) > 0 ? "high" : "mid", "还款压力过大",
                    "月供合计 ¥" + yuan(monthlyRepayment) + "，占月均收入的 " + pct(repaymentPressure)
                            + "，超过 50% 警戒线，需警惕断供风险。"));
        } else if (repaymentPressure >= 0.3 && monthlyRepayment.compareTo(BigDecimal.ZERO) > 0) {
            debtRisks.add(risk("low", "还款压力中等",
                    "月供占月均收入的 " + pct(repaymentPressure) + "，建议保持在 30% 以内更稳健。"));
        }
        if (deficitMonths >= 2) {
            debtRisks.add(risk("high", "连续入不敷出",
                    "近 " + deficitMonths + " 个月支出超过收入，长期将侵蚀资产，建议立即复盘支出结构。"));
        }

        // ===== 建议（规则引擎） =====
        List<Map<String, Object>> suggestions = new ArrayList<>();
        if (avgMonthlyIncome.compareTo(BigDecimal.ZERO) == 0
                && totalExpense.compareTo(BigDecimal.ZERO) > 0) {
            suggestions.add(suggest("📝", "补记收入来源",
                    "近期只有支出记录、没有收入记录。若实际有收入（工资/兼职等），请及时补记，分析才准确；若确实无收入，请重点控制支出。"));
        }
        if (!incomeSources.isEmpty() && incomeTotal.compareTo(BigDecimal.ZERO) > 0
                && ((BigDecimal) incomeSources.get(0).get("amount"))
                        .compareTo(incomeTotal.multiply(BigDecimal.valueOf(0.8))) >= 0) {
            BigDecimal topAmount = (BigDecimal) incomeSources.get(0).get("amount");
            suggestions.add(suggest("💼", "拓展收入来源",
                    "收入 " + pct(topAmount.divide(incomeTotal, 4, RoundingMode.HALF_UP).doubleValue()) + " 来自「"
                            + incomeSources.get(0).get("name") + "」，过于单一。可结合职业（"
                            + profileText(user) + "）评估兼职、技能变现等第二收入曲线。"));
        }
        if (deficitMonths >= 2) {
            suggestions.add(suggest("🧾", "控制非必要支出",
                    "月均支出 ¥" + yuan(avgMonthlyExpense) + " 已超月均收入 ¥" + yuan(avgMonthlyIncome)
                            + "，建议按「必要 - 想要 - 储蓄」三账户法拆分，先砍「想要」。"));
        }
        if (savingRate < 0.1 && avgMonthlyIncome.compareTo(BigDecimal.ZERO) > 0 && deficitMonths == 0) {
            suggestions.add(suggest("🐷", "提升储蓄率",
                    "当前月储蓄率 " + pct(savingRate) + "，低于 10%。建议发薪日先转存 10%-20% 再消费。"));
        }
        if (totalLiability.compareTo(BigDecimal.ZERO) == 0 && avgMonthlyIncome.compareTo(BigDecimal.ZERO) > 0) {
            BigDecimal surplus = avgMonthlyIncome.subtract(avgMonthlyExpense);
            suggestions.add(suggest("📈", "无债一身轻，可开始理财",
                    "当前无在还负债，月结余 ¥" + yuan(surplus.compareTo(BigDecimal.ZERO) > 0 ? surplus : BigDecimal.ZERO)
                            + " 可考虑「货币基金打底 + 指数定投增值」的组合。"));
        }
        if (monthlyRepayment.compareTo(BigDecimal.ZERO) > 0 && totalLiability.compareTo(BigDecimal.ZERO) > 0 
                && avgMonthlyIncome.compareTo(BigDecimal.ZERO) > 0) {
            suggestions.add(suggest("🏦", "优化债务结构",
                    "若存在多笔债务，优先偿还利率最高的（通常是信用卡）；有余力时可考虑低息置换。"));
        }
        if (suggestions.isEmpty()) {
            boolean hasHighRisk = debtRisks.stream().anyMatch(r -> "high".equals(r.get("level")));
            suggestions.add(hasHighRisk
                    ? suggest("⚠️", "优先处理高风险项",
                            "存在需要立即关注的高风险债务问题，请先处理上方风险提示，再逐步优化收支结构。")
                    : suggest("✅", "财务状况稳健",
                            "收支平衡、负债可控。继续保持记账习惯，让每一分钱都有迹可循。"));
        }

        // ===== 指标汇总 =====
        Map<String, Object> indicators = new LinkedHashMap<>();
        indicators.put("totalAsset", totalAsset);
        indicators.put("totalLiability", totalLiability);
        indicators.put("netWorth", totalAsset.subtract(totalLiability));
        indicators.put("debtRatio", Math.round(debtRatio * 1000) / 10.0);
        indicators.put("avgMonthlyIncome", avgMonthlyIncome);
        indicators.put("avgMonthlyExpense", avgMonthlyExpense);
        indicators.put("monthlyRepayment", monthlyRepayment);
        indicators.put("repaymentPressure", Math.round(repaymentPressure * 1000) / 10.0);
        indicators.put("savingRate", Math.round(savingRate * 1000) / 10.0);
        indicators.put("deficitMonths", deficitMonths);
        indicators.put("sampleMonths", (int) months);

        Map<String, Object> profile = buildProfile(user);

        // ===== LLM 综述（失败降级模板） =====
        String aiSummary = generateSummary(indicators, incomeSources, debtRisks, user);

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("profile", profile);
        result.put("indicators", indicators);
        result.put("incomeSources", incomeSources);
        result.put("debtRisks", debtRisks);
        result.put("suggestions", suggestions);
        result.put("aiSummary", aiSummary);
        result.put("aiEnabled", !aiSummary.startsWith("（模板"));
        return result;
    }

    @Override
    public Map<String, Object> getProfile(Long userId) {
        PortalUser user = portalUserService.selectPortalUserById(userId);
        Map<String, Object> profile = buildProfile(user);
        List<Map<String, String>> options = new ArrayList<>();
        try {
            for (SysDictData d : dictTypeService.selectDictDataByType(DICT_IDENTITY_TAG)) {
                Map<String, String> opt = new HashMap<>();
                opt.put("value", d.getDictValue());
                opt.put("label", d.getDictLabel());
                options.add(opt);
            }
        } catch (Exception e) {
            log.warn("查询身份标签字典失败", e);
        }
        profile.put("identityOptions", options);
        return profile;
    }

    @Override
    public Map<String, Object> updateProfile(Long userId, Map<String, String> body) {
        PortalUser user = portalUserService.selectPortalUserById(userId);
        if (user == null) {
            throw new IllegalArgumentException("用户不存在");
        }
        if (body.containsKey("position")) {
            String position = body.get("position");
            if (position != null && position.length() > 100) {
                throw new IllegalArgumentException("职位长度不能超过100个字符");
            }
            user.setPosition(position);
        }
        if (body.containsKey("company")) {
            String company = body.get("company");
            if (company != null && company.length() > 200) {
                throw new IllegalArgumentException("公司长度不能超过200个字符");
            }
            user.setCompany(company);
        }
        if (body.containsKey("identityTag")) {
            String tag = body.get("identityTag");
            if (tag != null && tag.length() > 32) {
                throw new IllegalArgumentException("身份标签长度不能超过32个字符");
            }
            // 白名单校验：必须是字典内的合法值（空值视为清除）
            boolean valid = tag == null || tag.isEmpty();
            if (!valid) {
                valid = dictTypeService.selectDictDataByType(DICT_IDENTITY_TAG).stream()
                        .anyMatch(d -> tag.equals(d.getDictValue()));
            }
            if (!valid) {
                throw new IllegalArgumentException("非法身份标签");
            }
            user.setIdentityTag(tag);
        }
        portalUserMapper.updateById(user);
        return getProfile(userId);
    }

    // ==================== 私有方法 ====================

    private Map<Long, String> loadCategoryNames(Long userId) {
        return categoryMapper.selectList(new LambdaQueryWrapper<LedgerCategory>()
                        .eq(LedgerCategory::getUserId, userId)
                        .or()
                        .isNull(LedgerCategory::getUserId))
                .stream().collect(Collectors.toMap(LedgerCategory::getId, LedgerCategory::getName, (a, b) -> a));
    }

    private Map<String, Object> buildProfile(PortalUser user) {
        Map<String, Object> p = new LinkedHashMap<>();
        if (user != null) {
            p.put("position", user.getPosition());
            p.put("company", user.getCompany());
            p.put("identityTag", user.getIdentityTag());
            p.put("identityTagLabel", identityTagLabel(user.getIdentityTag()));
        }
        return p;
    }

    private String identityTagLabel(String tag) {
        if (tag == null || tag.isEmpty()) return null;
        try {
            return dictTypeService.selectDictDataByType(DICT_IDENTITY_TAG).stream()
                    .filter(d -> tag.equals(d.getDictValue()))
                    .map(SysDictData::getDictLabel).findFirst().orElse(null);
        } catch (Exception e) {
            return null;
        }
    }

    private String profileText(PortalUser user) {
        if (user == null) return "未填写";
        StringBuilder sb = new StringBuilder();
        String tagLabel = identityTagLabel(user.getIdentityTag());
        if (tagLabel != null) sb.append(tagLabel);
        if (user.getPosition() != null && !user.getPosition().isEmpty()) {
            if (sb.length() > 0) sb.append(" / ");
            sb.append(user.getPosition());
        }
        if (user.getCompany() != null && !user.getCompany().isEmpty()) {
            if (sb.length() > 0) sb.append(" @ ");
            sb.append(user.getCompany());
        }
        return sb.length() > 0 ? sb.toString() : "未填写";
    }

    /** LLM 生成综述（失败降级模板） */
    private String generateSummary(Map<String, Object> indicators,
                                   List<Map<String, Object>> incomeSources,
                                   List<Map<String, Object>> debtRisks, PortalUser user) {
        String prompt = buildPrompt(indicators, incomeSources, debtRisks, user);
        String llm = callLlm(prompt);
        if (llm != null && !llm.isEmpty()) {
            return llm.trim();
        }
        StringBuilder sb = new StringBuilder("（模板分析）根据你的记账数据：");
        sb.append("近 ").append(indicators.get("sampleMonths")).append(" 个月月均收入 ¥")
                .append(yuan((BigDecimal) indicators.get("avgMonthlyIncome")))
                .append("、月均支出 ¥").append(yuan((BigDecimal) indicators.get("avgMonthlyExpense")))
                .append("，资产负债率 ").append(indicators.get("debtRatio")).append("%。");
        if (!debtRisks.isEmpty()) {
            sb.append("存在 ").append(debtRisks.size()).append(" 项债务风险需关注，");
        }
        if ((Integer) indicators.get("deficitMonths") >= 2) {
            sb.append("已连续入不敷出，请重点复盘支出。");
        } else {
            sb.append("整体收支可控。");
        }
        return sb.toString();
    }

    private String buildPrompt(Map<String, Object> indicators,
                               List<Map<String, Object>> incomeSources,
                               List<Map<String, Object>> debtRisks, PortalUser user) {
        StringBuilder sb = new StringBuilder();
        sb.append("你是一位专业、友善的个人财务顾问。请根据以下用户记账数据，输出一份中文财务综述，")
                .append("包含：1）财务现状概述（2-3 句）；2）核心问题或亮点（如有债务风险请明确点出）；")
                .append("3）3 条以内的具体行动建议。\n")
                .append("要求：口语化、具体、引用数据；总字数 300 字以内；不要使用 Markdown 标记。\n\n");
        sb.append("【用户画像】").append(profileText(user)).append('\n');
        sb.append("【核心指标】近").append(indicators.get("sampleMonths")).append("个月：月均收入 ¥")
                .append(yuan((BigDecimal) indicators.get("avgMonthlyIncome")))
                .append("，月均支出 ¥").append(yuan((BigDecimal) indicators.get("avgMonthlyExpense")))
                .append("，月储蓄率 ").append(indicators.get("savingRate")).append("%，")
                .append("月供合计 ¥").append(yuan((BigDecimal) indicators.get("monthlyRepayment")))
                .append("（占收入 ").append(indicators.get("repaymentPressure")).append("%），")
                .append("总资产 ¥").append(yuan((BigDecimal) indicators.get("totalAsset")))
                .append("，总负债 ¥").append(yuan(toBigDecimal(indicators.get("totalLiability"))))
                .append("，资产负债率 ").append(indicators.get("debtRatio")).append("%，")
                .append("连续入不敷出月数 ").append(indicators.get("deficitMonths")).append("。\n");
        sb.append("【收入来源】");
        if (incomeSources.isEmpty()) {
            sb.append("暂无收入记录。");
        } else {
            incomeSources.forEach(s -> sb.append(s.get("name")).append(" ¥")
                    .append(yuan(toBigDecimal(s.get("amount")))).append("（").append(s.get("ratio")).append("%）"));
        }
        sb.append('\n');
        sb.append("【已识别风险】");
        if (debtRisks.isEmpty()) {
            sb.append("无。");
        } else {
            debtRisks.forEach(r -> sb.append(r.get("title")).append("；"));
        }
        sb.append('\n');
        return sb.toString();
    }

    /**
     * 金额归一化：Map 中金额字段类型不可控（BigDecimal/Long/Integer/Number），
     * 统一转 BigDecimal，避免 ClassCastException。
     */
    private BigDecimal toBigDecimal(Object v) {
        if (v == null) return BigDecimal.ZERO;
        if (v instanceof BigDecimal) return (BigDecimal) v;
        if (v instanceof Long || v instanceof Integer) return BigDecimal.valueOf(((Number) v).longValue());
        if (v instanceof Number) return BigDecimal.valueOf(((Number) v).doubleValue());
        try {
            return new BigDecimal(v.toString());
        } catch (NumberFormatException e) {
            return BigDecimal.ZERO;
        }
    }

    /** 调用 LLM（复用 ext-ai 模块模型配置；未配置/失败返回 null） */
    private String callLlm(String prompt) {
        if (modelConfigService == null) {
            return null;
        }
        try {
            ModelConfig config = modelConfigService.getDefaultChatConfig();
            if (config == null) {
                return null;
            }
            ChatLanguageModel model = modelConfigService.createChatModel(config.getId());
            ChatResponse response = model.chat(UserMessage.from(prompt));
            return response.aiMessage().text();
        } catch (Exception e) {
            log.warn("AI 财务综述生成失败，降级模板: {}", e.getMessage());
            return null;
        }
    }

    private Map<String, Object> risk(String level, String title, String detail) {
        Map<String, Object> r = new LinkedHashMap<>();
        r.put("level", level);
        r.put("title", title);
        r.put("detail", detail);
        return r;
    }

    private Map<String, Object> suggest(String icon, String title, String detail) {
        Map<String, Object> s = new LinkedHashMap<>();
        s.put("icon", icon);
        s.put("title", title);
        s.put("detail", detail);
        return s;
    }

    private String yuan(BigDecimal amount) {
        if (amount == null) {
            return "0.00";
        }
        return amount.setScale(2, RoundingMode.HALF_UP).toPlainString();
    }

    private String pct(double v) {
        return String.format("%.1f%%", v * 100);
    }
}
