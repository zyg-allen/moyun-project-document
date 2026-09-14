package com.moyun.ext.cms.controller;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.moyun.core.base.AjaxResult;
import com.moyun.core.base.BaseController;
import com.moyun.ledger.domain.entity.LedgerTipOrder;
import com.moyun.ledger.domain.entity.LedgerVipOrder;
import com.moyun.ledger.mapper.LedgerTipOrderMapper;
import com.moyun.ledger.mapper.LedgerVipOrderMapper;
import com.moyun.pay.domain.entity.LedgerEntry;
import com.moyun.pay.domain.entity.UserAccount;
import com.moyun.pay.domain.entity.WithdrawOrder;
import com.moyun.pay.mapper.LedgerEntryMapper;
import com.moyun.pay.mapper.UserAccountMapper;
import com.moyun.pay.mapper.WithdrawOrderMapper;
import com.moyun.portal.domain.entity.PortalTipOrder;
import com.moyun.portal.mapper.PortalTipOrderMapper;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * CMS 平台收入总览 Controller（v11.78）
 *
 * <p>全平台支付汇集视角：按 平台（记账App / 墨韵门户）→ 渠道（App打赏 / 门户文章打赏 / 付费阅读 / 规划中渠道）两级划分。
 *
 * <p>口径说明（前后台一致，金额单位：元）：
 * <ul>
 *   <li>全平台成交总额（GMV）= 各渠道成功订单金额合计：
 *       App打赏（ledger_tip_order status='paid'，V11.80 接公共通道后为真实资金）+ 门户打赏/付费阅读（portal_tip_order status='paid'）</li>
 *   <li>平台直接所得 = 公共通道 PLATFORM/credit 分录合计（V11.80：含 App 打赏全额 + 门户通道抽成，分录为准不双算）</li>
 *   <li>用户所得 = 公共通道 USER/credit 分录合计（门户作者分账累计，含钱包余额）</li>
 *   <li>pay_order 为通道单据，不重复计入 GMV（业务订单为准，避免与打赏订单双算）</li>
 *   <li>面试会员 / 简历优化 / 记账VIP 为规划中渠道，暂无订单表，展示 0 值占位</li>
 * </ul>
 *
 * <p>聚合方式：全部 SQL SUM / COUNT / GROUP BY（遵守"禁止全表 selectList 内存聚合"铁律）。
 *
 * @author moyun
 */
@Tag(name = "CMS平台收入总览", description = "按平台×渠道汇聚全平台收入")
@RestController
@RequestMapping("/cms/pay/revenue")
public class CmsPayRevenueController extends BaseController {

    @Autowired
    private LedgerTipOrderMapper ledgerTipOrderMapper;

    @Autowired
    private PortalTipOrderMapper portalTipOrderMapper;

    @Autowired
    private LedgerEntryMapper ledgerEntryMapper;

    @Autowired
    private UserAccountMapper userAccountMapper;

    @Autowired
    private WithdrawOrderMapper withdrawOrderMapper;

    @Autowired
    private LedgerVipOrderMapper ledgerVipOrderMapper;

    @Operation(summary = "收入总览", description = "平台×渠道两级收入聚合 + 平台直接所得 + 近6月趋势 + 支付方式分布")
    @PreAuthorize("@ss.hasPermi('cms:payRevenue:view')")
    @GetMapping("/overview")
    public AjaxResult overview() {
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("caliberNote", "口径：GMV=各渠道成功订单合计（App打赏+记账VIP+门户 status=paid）；平台直接所得=App打赏(平台对象)+通道抽成；pay_order 通道单据不重复计入；金额单位元");

        // ===== 1. 渠道聚合 =====
        // 1.1 App 记账打赏：总 + 按打赏对象 + 按支付方式（v11.79 状态统一 'paid'，字段统一 pay_channel）
        Map<String, Object> appTip = aggregateOne(ledgerTipOrderMapper.selectMaps(new QueryWrapper<LedgerTipOrder>()
                .select("COUNT(*) AS cnt", "COALESCE(SUM(amount), 0) AS total")
                .eq("status", LedgerTipOrder.STATUS_PAID)));
        Map<String, Map<String, Object>> appTipByTarget = groupToMap(ledgerTipOrderMapper.selectMaps(new QueryWrapper<LedgerTipOrder>()
                .select("target", "COUNT(*) AS cnt", "COALESCE(SUM(amount), 0) AS total")
                .eq("status", LedgerTipOrder.STATUS_PAID)
                .groupBy("target")), "target");
        Map<String, Map<String, Object>> appTipByPayWay = groupToMap(ledgerTipOrderMapper.selectMaps(new QueryWrapper<LedgerTipOrder>()
                .select("pay_channel", "COUNT(*) AS cnt", "COALESCE(SUM(amount), 0) AS total")
                .eq("status", LedgerTipOrder.STATUS_PAID)
                .groupBy("pay_channel")), "pay_channel");

        // 1.2 门户打赏/付费阅读：按 target_type × pay_channel
        Map<String, Map<String, Object>> portalByType = groupToMap(portalTipOrderMapper.selectMaps(new QueryWrapper<PortalTipOrder>()
                .select("target_type", "COUNT(*) AS cnt", "COALESCE(SUM(amount), 0) AS total")
                .eq("status", "paid")
                .groupBy("target_type")), "target_type");
        Map<String, Map<String, Object>> portalByTypeWay = groupToMap(portalTipOrderMapper.selectMaps(new QueryWrapper<PortalTipOrder>()
                .select("target_type", "pay_channel", "COUNT(*) AS cnt", "COALESCE(SUM(amount), 0) AS total")
                .eq("status", "paid")
                .groupBy("target_type", "pay_channel")), "target_type", "pay_channel");

        // 1.3 记账VIP订阅总（v11.81 新增渠道，平台直收类）
        Map<String, Object> ledgerVipTotal = aggregateOne(ledgerVipOrderMapper.selectMaps(new QueryWrapper<LedgerVipOrder>()
                .select("COUNT(*) AS cnt", "COALESCE(SUM(amount), 0) AS total")
                .eq("status", LedgerVipOrder.STATUS_PAID)));

        // 1.4 通道平台抽成（pay_ledger_entry PLATFORM/credit，SQL SUM）
        BigDecimal gatewayPlatformFee = sumLedgerAmount(LedgerEntry.ROLE_PLATFORM);
        BigDecimal gatewayUserShare = sumLedgerAmount(LedgerEntry.ROLE_USER);

        // ===== 2. 组装平台→渠道 =====
        // 记账App（v11.81：记账VIP 规划占位 → 真实聚合渠道）
        Map<String, Map<String, Object>> vipByPayWay = groupToMap(ledgerVipOrderMapper.selectMaps(new QueryWrapper<LedgerVipOrder>()
                .select("pay_channel", "COUNT(*) AS cnt", "COALESCE(SUM(amount), 0) AS total")
                .eq("status", LedgerVipOrder.STATUS_PAID)
                .groupBy("pay_channel")), "pay_channel");
        Map<String, Object> appTipChannel = channel("app_tip", "App记账打赏", appTip, payWays(appTipByPayWay, Map.of("wechat", "微信支付", "alipay", "支付宝"), null));
        Map<String, Object> ledgerVipChannel = channel("ledger_vip", "记账VIP订阅", ledgerVipTotal,
                payWays(vipByPayWay, Map.of("wechat", "微信支付", "alipay", "支付宝"), null));
        Map<String, Object> ledgerApp = platform("ledger_app", "记账App", new ArrayList<>(List.of(
                appTipChannel,
                ledgerVipChannel)));

        // 门户
        Map<String, Object> portalTipTotal = sumGroups(portalByType, "article", "column");
        Map<String, Object> paidReadingTotal = portalByType.getOrDefault("article_paid", zeroRow());
        Map<String, Object> portal = platform("portal", "墨韵门户", new ArrayList<>(List.of(
                channel("portal_tip", "门户文章打赏", portalTipTotal,
                        payWays(portalByTypeWay, Map.of("points", "积分", "alipay", "支付宝", "wechat", "微信支付"), new String[]{"article", "column"})),
                channel("paid_reading", "付费阅读", paidReadingTotal,
                        payWays(portalByTypeWay, Map.of("points", "积分", "alipay", "支付宝", "wechat", "微信支付"), new String[]{"article_paid"})),
                plannedChannel("interview_member", "面试会员（规划中）"),
                plannedChannel("resume_optimize", "简历优化（规划中）"))));

        List<Map<String, Object>> platforms = new ArrayList<>(List.of(ledgerApp, portal));
        data.put("platforms", platforms);

        // ===== 3. 顶部指标（口径见类注释） =====
        BigDecimal gmv = dec(appTip).add(dec(portalTipTotal)).add(dec(paidReadingTotal)).add(dec(ledgerVipTotal));
        long orderCount = cnt(appTip) + cnt(portalTipTotal) + cnt(paidReadingTotal) + cnt(ledgerVipTotal);
        // V11.80 口径（App打赏接入公共通道后）：平台所得 = PLATFORM/credit 分录合计
        // （已含 App 打赏全额 + 门户通道抽成，不再叠加 appTipPlatformPart 避免双算）；
        // 用户所得 = USER/credit 分录合计（门户作者分账累计）。
        BigDecimal platformDirect = gatewayPlatformFee;
        BigDecimal userShare = gatewayUserShare;

        // v11.79：退款指标（门户 refunded，未计入 GMV）+ 提现 + 守恒对账
        Map<String, Object> refundedRow = aggregateOne(portalTipOrderMapper.selectMaps(new QueryWrapper<PortalTipOrder>()
                .select("COUNT(*) AS cnt", "COALESCE(SUM(amount), 0) AS total")
                .eq("status", "refunded")));
        Map<String, Object> withdrawRow = aggregateOne(withdrawOrderMapper.selectMaps(new QueryWrapper<WithdrawOrder>()
                .select("COUNT(*) AS cnt", "COALESCE(SUM(amount), 0) AS total")
                .eq("status", WithdrawOrder.STATUS_PAID)));
        // 守恒对账：理论公账余额 = 平台抽成累计 + Σ用户余额（提现已从余额扣除）
        Map<String, Object> accountRow = aggregateOne(userAccountMapper.selectMaps(new QueryWrapper<UserAccount>()
                .select("COALESCE(SUM(balance), 0) AS total_balance", "COALESCE(SUM(total_income), 0) AS total_income",
                        "COALESCE(SUM(total_withdraw), 0) AS total_withdraw", "COUNT(*) AS cnt")));
        Object userBalanceSumObj = ((Map<String, Object>) accountRow).get("total_balance");
        BigDecimal userBalanceSum = userBalanceSumObj == null ? BigDecimal.ZERO : new BigDecimal(userBalanceSumObj.toString());
        BigDecimal theoreticalAccount = gatewayPlatformFee.add(userBalanceSum);

        Map<String, Object> totals = new LinkedHashMap<>();
        totals.put("grandTotal", gmv);
        totals.put("orderCount", orderCount);
        totals.put("platformDirect", platformDirect);
        totals.put("userShare", userShare);
        totals.put("refundedAmount", dec(refundedRow));
        totals.put("refundedCount", cnt(refundedRow));
        totals.put("withdrawnAmount", dec(withdrawRow));
        totals.put("withdrawCount", cnt(withdrawRow));
        totals.put("userBalanceSum", userBalanceSum);
        totals.put("theoreticalAccount", theoreticalAccount);
        data.put("totals", totals);

        // ===== 4. 近6月趋势（SQL GROUP BY，按月合并两渠道） =====
        LocalDateTime trendStart = LocalDate.now().minusMonths(5).withDayOfMonth(1).atStartOfDay();
        Map<String, Map<String, Object>> appMonthly = groupToMap(ledgerTipOrderMapper.selectMaps(new QueryWrapper<LedgerTipOrder>()
                .select("DATE_FORMAT(COALESCE(paid_time, create_time), '%Y-%m') AS ym", "COUNT(*) AS cnt", "COALESCE(SUM(amount), 0) AS total")
                .eq("status", LedgerTipOrder.STATUS_PAID)
                .ge("create_time", trendStart)
                .groupBy("DATE_FORMAT(COALESCE(paid_time, create_time), '%Y-%m')")), "ym");
        Map<String, Map<String, Object>> portalMonthly = groupToMap(portalTipOrderMapper.selectMaps(new QueryWrapper<PortalTipOrder>()
                .select("DATE_FORMAT(COALESCE(paid_time, created_time), '%Y-%m') AS ym", "COUNT(*) AS cnt", "COALESCE(SUM(amount), 0) AS total")
                .eq("status", "paid")
                .ge("created_time", trendStart)
                .groupBy("DATE_FORMAT(COALESCE(paid_time, created_time), '%Y-%m')")), "ym");
        List<Map<String, Object>> trend = new ArrayList<>();
        for (int i = 5; i >= 0; i--) {
            String ym = LocalDate.now().minusMonths(i).withDayOfMonth(1).toString().substring(0, 7);
            Map<String, Object> appRow = appMonthly.getOrDefault(ym, zeroRow());
            Map<String, Object> portalRow = portalMonthly.getOrDefault(ym, zeroRow());
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("ym", ym);
            row.put("amount", dec(appRow).add(dec(portalRow)));
            row.put("orderCount", cnt(appRow) + cnt(portalRow));
            trend.add(row);
        }
        data.put("monthlyTrend", trend);

        return success(data);
    }

    // ==================== 组装辅助 ====================

    private Map<String, Object> platform(String code, String name, List<Map<String, Object>> channels) {
        BigDecimal total = channels.stream().map(c -> dec(c)).reduce(BigDecimal.ZERO, BigDecimal::add);
        long count = channels.stream().mapToLong(CmsPayRevenueController::cnt).sum();
        Map<String, Object> p = new LinkedHashMap<>();
        p.put("code", code);
        p.put("name", name);
        p.put("totalAmount", total);
        p.put("orderCount", count);
        p.put("channels", channels);
        return p;
    }

    private Map<String, Object> channel(String code, String name, Map<String, Object> agg, List<Map<String, Object>> payWays) {
        Map<String, Object> c = new LinkedHashMap<>();
        c.put("code", code);
        c.put("name", name);
        c.put("amount", dec(agg));
        c.put("orderCount", cnt(agg));
        c.put("status", "active");
        c.put("payWays", payWays);
        return c;
    }

    private Map<String, Object> plannedChannel(String code, String name) {
        Map<String, Object> c = new LinkedHashMap<>();
        c.put("code", code);
        c.put("name", name);
        c.put("amount", BigDecimal.ZERO);
        c.put("orderCount", 0L);
        c.put("status", "planned");
        c.put("payWays", new ArrayList<>());
        return c;
    }

    /** 支付方式分布：rows 按（可选 typeKey +）wayKey 分组的结果 → 指定 type 集合内的 way 明细 */
    private List<Map<String, Object>> payWays(Map<String, Map<String, Object>> groupedByTypeWay,
                                              Map<String, String> wayNames, String[] onlyTypes) {
        List<Map<String, Object>> result = new ArrayList<>();
        Map<String, Map<String, Object>> merged = new LinkedHashMap<>();
        for (Map.Entry<String, Map<String, Object>> e : groupedByTypeWay.entrySet()) {
            // key 形如 "article|wechat"（双键）或 "wechat"（单键）
            String[] parts = e.getKey().split("\\|");
            String way = parts.length > 1 ? parts[1] : parts[0];
            if (onlyTypes != null) {
                boolean match = false;
                for (String t : onlyTypes) {
                    if (parts[0].equals(t)) { match = true; break; }
                }
                if (!match) { continue; }
            }
            Map<String, Object> acc = merged.getOrDefault(way, zeroRow());
            Map<String, Object> add = new HashMap<>(acc);
            add.put("amount", dec(acc).add(dec(e.getValue())));
            add.put("cnt", cnt(acc) + cnt(e.getValue()));
            merged.put(way, add);
        }
        for (Map.Entry<String, Map<String, Object>> e : merged.entrySet()) {
            Map<String, Object> w = new LinkedHashMap<>();
            w.put("way", e.getKey());
            w.put("wayName", wayNames.getOrDefault(e.getKey(), e.getKey()));
            w.put("amount", dec(e.getValue()));
            w.put("orderCount", cnt(e.getValue()));
            result.add(w);
        }
        return result;
    }

    private Map<String, Object> sumGroups(Map<String, Map<String, Object>> grouped, String... keys) {
        Map<String, Object> result = zeroRow();
        for (String key : keys) {
            Map<String, Object> row = grouped.get(key);
            if (row != null) {
                result.put("total", dec(result).add(dec(row)));
                result.put("cnt", cnt(result) + cnt(row));
            }
        }
        return result;
    }

    /** 单行聚合（无 GROUP BY）结果 */
    private Map<String, Object> aggregateOne(List<Map<String, Object>> rows) {
        if (rows == null || rows.isEmpty() || rows.get(0) == null) {
            return zeroRow();
        }
        Map<String, Object> row = new HashMap<>(rows.get(0));
        row.putIfAbsent("total", BigDecimal.ZERO);
        row.putIfAbsent("cnt", 0L);
        return row;
    }

    /** selectMaps 分组结果 → 以 key 列值为 Map 键（单键或双键 "a|b"） */
    @SafeVarargs
    private final Map<String, Map<String, Object>> groupToMap(List<Map<String, Object>> rows, String... keyCols) {
        Map<String, Map<String, Object>> result = new LinkedHashMap<>();
        if (rows == null) {
            return result;
        }
        for (Map<String, Object> row : rows) {
            if (row == null) {
                continue;
            }
            StringBuilder key = new StringBuilder();
            for (String col : keyCols) {
                Object v = row.get(col);
                if (key.length() > 0) {
                    key.append('|');
                }
                key.append(v == null ? "" : v.toString());
            }
            Map<String, Object> normalized = new HashMap<>(row);
            normalized.putIfAbsent("total", BigDecimal.ZERO);
            normalized.putIfAbsent("cnt", 0L);
            result.put(key.toString(), normalized);
        }
        return result;
    }

    private BigDecimal sumLedgerAmount(String accountRole) {
        List<Map<String, Object>> rows = ledgerEntryMapper.selectMaps(new QueryWrapper<LedgerEntry>()
                .select("COALESCE(SUM(amount), 0) AS total")
                .eq("account_role", accountRole)
                .eq("direction", LedgerEntry.DIRECTION_CREDIT));
        if (rows == null || rows.isEmpty() || rows.get(0) == null) {
            return BigDecimal.ZERO;
        }
        Object total = rows.get(0).get("total");
        return total == null ? BigDecimal.ZERO : new BigDecimal(total.toString());
    }

    private static Map<String, Object> zeroRow() {
        Map<String, Object> row = new HashMap<>();
        row.put("total", BigDecimal.ZERO);
        row.put("cnt", 0L);
        return row;
    }

    private static BigDecimal dec(Map<String, Object> row) {
        if (row == null) {
            return BigDecimal.ZERO;
        }
        Object v = row.get("total") == null ? row.get("amount") : row.get("total");
        return v == null ? BigDecimal.ZERO : new BigDecimal(v.toString());
    }

    private static long cnt(Map<String, Object> row) {
        if (row == null) {
            return 0L;
        }
        Object v = row.get("cnt") == null ? row.get("orderCount") : row.get("cnt");
        if (v == null) {
            return 0L;
        }
        try {
            return Long.parseLong(v.toString());
        } catch (NumberFormatException e) {
            return 0L;
        }
    }
}
