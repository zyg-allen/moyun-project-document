package com.moyun.ledger.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.moyun.core.base.BaseController;
import com.moyun.core.base.TableDataInfo;
import com.moyun.ext.aigateway.entity.AiExecuteLog;
import com.moyun.ext.aigateway.mapper.AiExecuteLogMapper;
import com.moyun.ledger.domain.entity.LedgerAiAnalysisReport;
import com.moyun.ledger.domain.entity.LedgerCategory;
import com.moyun.ledger.domain.entity.LedgerTransaction;
import com.moyun.ledger.mapper.LedgerAiAnalysisReportMapper;
import com.moyun.ledger.mapper.LedgerCategoryMapper;
import com.moyun.ledger.mapper.LedgerTransactionMapper;
import com.moyun.portal.domain.entity.PortalUser;
import com.moyun.portal.mapper.PortalUserMapper;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * CMS 记账用户维度管理 Controller
 *
 * <p>隐私保护口径（用户个体数据展示的最小必要原则）：
 * <ul>
 *   <li>用户身份三要素脱敏：昵称/用户名/手机号（首尾保留、中间打码）</li>
 *   <li>流水仅返回非敏感列：类型/金额/分类/日期（不返回备注、商户、凭证、账户名、余额快照）</li>
 *   <li>AI 使用为聚合指标：调用次数/Token 消耗/成本（不返回对话内容）</li>
 * </ul>
 *
 * @author moyun
 */
@Tag(name = "CMS记账用户管理", description = "用户维度流水/使用情况（脱敏）")
@RestController
@RequestMapping("/cms/ledger/users")
public class CmsLedgerUserController extends BaseController {

    @Autowired
    private LedgerTransactionMapper transactionMapper;

    @Autowired
    private LedgerAiAnalysisReportMapper reportMapper;

    @Autowired
    private AiExecuteLogMapper aiExecuteLogMapper;

    @Autowired
    private LedgerCategoryMapper categoryMapper;

    @Autowired
    private PortalUserMapper portalUserMapper;

    /** 记账类型中文标签 */
    private static final Map<String, String> TYPE_LABELS = Map.of(
            "income", "收入", "expense", "支出", "transfer", "转账",
            "repayment", "还款", "borrow", "借款", "adjust", "余额校准");

    @Operation(summary = "记账用户列表（含流水/AI 使用/token 消费聚合，脱敏）")
    @PreAuthorize("@ss.hasPermi('cms:ledgerUsers:list')")
    @GetMapping("/list")
    public TableDataInfo list(@RequestParam(defaultValue = "1") Long pageNum,
                              @RequestParam(defaultValue = "10") Long pageSize,
                              @RequestParam(required = false) Long userId) {
        // 1. 有记账行为的用户分页（流水表去重；可按 userId 精确定位）
        QueryWrapper<LedgerTransaction> pw = new QueryWrapper<>();
        pw.select("DISTINCT user_id");
        if (userId != null) {
            pw.eq("user_id", userId);
        }
        Page<Map<String, Object>> page = new Page<>(pageNum, pageSize);
        List<Long> userIds = transactionMapper.selectMapsPage(page, pw).getRecords().stream()
                .map(m -> toLong(m.get("user_id")))
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
        if (userIds.isEmpty()) {
            return getDataTable(new ArrayList<>(), page.getTotal());
        }

        // 2. 用户基础信息（脱敏输出；ledger.user_id = portal_user.id 主键）
        Map<Long, PortalUser> userMap = portalUserMapper.selectList(
                        new LambdaQueryWrapper<PortalUser>()
                                .in(PortalUser::getId, userIds)
                                .select(PortalUser::getId, PortalUser::getUsername,
                                        PortalUser::getNickname, PortalUser::getPhone))
                .stream().collect(Collectors.toMap(PortalUser::getId, u -> u, (a, b) -> a));

        // 3. 三类聚合（页内 userId 一次 IN 聚合，避免 N+1）
        // 3.1 流水聚合：有效笔数 + 最近记账日期
        Map<Long, Map<String, Object>> txnAgg = transactionMapper.selectMaps(new QueryWrapper<LedgerTransaction>()
                        .select("user_id", "count(*) as cnt", "max(transaction_date) as last_date")
                        .in("user_id", userIds)
                        .eq("status", LedgerTransaction.STATUS_NORMAL)
                        .groupBy("user_id"))
                .stream().collect(Collectors.toMap(m -> toLong(m.get("user_id")), m -> m));

        // 3.2 AI 分析报告数
        Map<Long, Map<String, Object>> reportAgg = reportMapper.selectMaps(new QueryWrapper<LedgerAiAnalysisReport>()
                        .select("user_id", "count(*) as cnt")
                        .in("user_id", userIds)
                        .groupBy("user_id"))
                .stream().collect(Collectors.toMap(m -> toLong(m.get("user_id")), m -> m));

        // 3.3 AI 调用与 token 消费（网关执行日志按用户聚合）
        Map<Long, Map<String, Object>> aiAgg = aiExecuteLogMapper.selectMaps(new QueryWrapper<AiExecuteLog>()
                        .select("user_id", "count(*) as calls", "sum(token_used) as tokens", "sum(cost_yuan) as cost")
                        .in("user_id", userIds)
                        .isNotNull("user_id")
                        .groupBy("user_id"))
                .stream().collect(Collectors.toMap(m -> toLong(m.get("user_id")), m -> m));

        // 4. 组装行（按分页顺序）
        List<Map<String, Object>> rows = new ArrayList<>();
        for (Long uid : userIds) {
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("userId", uid);
            PortalUser u = userMap.get(uid);
            row.put("nickname", mask(u == null ? String.valueOf(uid) : fallback(u.getNickname(), u.getUsername())));
            row.put("phone", u == null ? null : maskPhone(u.getPhone()));
            Map<String, Object> t = txnAgg.getOrDefault(uid, Map.of());
            row.put("txnCount", toLong(t.get("cnt"), 0L));
            row.put("lastTxnDate", t.get("last_date"));
            Map<String, Object> r = reportAgg.getOrDefault(uid, Map.of());
            row.put("aiReportCount", toLong(r.get("cnt"), 0L));
            Map<String, Object> a = aiAgg.getOrDefault(uid, Map.of());
            row.put("aiCallCount", toLong(a.get("calls"), 0L));
            row.put("aiTokenUsed", toLong(a.get("tokens"), 0L));
            row.put("aiCostYuan", a.get("cost"));
            rows.add(row);
        }
        return getDataTable(rows, page.getTotal());
    }

    /**
     * 用户流水（简易版·隐私保护）：仅类型/金额/分类/日期，
     * 不返回备注、商户、凭证、账户名、余额快照等敏感明细
     */
    @Operation(summary = "用户流水明细（脱敏简易版）")
    @PreAuthorize("@ss.hasPermi('cms:ledgerUsers:query')")
    @GetMapping("/{userId}/transactions")
    public TableDataInfo transactions(@PathVariable Long userId,
                                      @RequestParam(defaultValue = "1") Long pageNum,
                                      @RequestParam(defaultValue = "10") Long pageSize) {
        startPage();
        List<LedgerTransaction> txns = transactionMapper.selectList(
                new LambdaQueryWrapper<LedgerTransaction>()
                        .eq(LedgerTransaction::getUserId, userId)
                        .select(LedgerTransaction::getId, LedgerTransaction::getType,
                                LedgerTransaction::getAmount, LedgerTransaction::getCategoryId,
                                LedgerTransaction::getTransactionDate, LedgerTransaction::getStatus)
                        .orderByDesc(LedgerTransaction::getTransactionDate)
                        .orderByDesc(LedgerTransaction::getId));

        // 分类名映射（批量一次查询）
        List<Long> categoryIds = txns.stream()
                .map(LedgerTransaction::getCategoryId)
                .filter(Objects::nonNull).distinct().collect(Collectors.toList());
        Map<Long, String> catNames = categoryIds.isEmpty() ? Map.of()
                : categoryMapper.selectList(new LambdaQueryWrapper<LedgerCategory>()
                        .in(LedgerCategory::getId, categoryIds)
                        .select(LedgerCategory::getId, LedgerCategory::getName))
                .stream().collect(Collectors.toMap(LedgerCategory::getId, LedgerCategory::getName));

        List<Map<String, Object>> rows = new ArrayList<>();
        for (LedgerTransaction t : txns) {
            Map<String, Object> row = new HashMap<>();
            row.put("type", t.getType());
            row.put("typeLabel", TYPE_LABELS.getOrDefault(t.getType(), t.getType()));
            row.put("amount", t.getAmount());
            row.put("categoryName", t.getCategoryId() == null ? null : catNames.get(t.getCategoryId()));
            row.put("transactionDate", t.getTransactionDate());
            row.put("status", t.getStatus());
            rows.add(row);
        }
        return getDataTable(rows);
    }

    // ==================== 脱敏工具 ====================

    /** 通用脱敏：首尾各留 1 字符，中间 *（长度≤2 全 *） */
    private String mask(String text) {
        if (text == null || text.isBlank()) {
            return null;
        }
        if (text.length() <= 2) {
            return "**";
        }
        return text.charAt(0) + "*".repeat(text.length() - 2) + text.charAt(text.length() - 1);
    }

    /** 手机号脱敏：138****5678 */
    private String maskPhone(String phone) {
        if (phone == null || phone.length() < 7) {
            return mask(phone);
        }
        return phone.substring(0, 3) + "****" + phone.substring(phone.length() - 4);
    }

    private String fallback(String primary, String secondary) {
        return primary != null && !primary.isBlank() ? primary : (secondary != null ? secondary : "");
    }

    private Long toLong(Object v) {
        return v == null ? null : Long.valueOf(v.toString());
    }

    private Long toLong(Object v, Long def) {
        Long r = toLong(v);
        return r == null ? def : r;
    }
}
