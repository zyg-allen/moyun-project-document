package com.moyun.ledger.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.moyun.common.annotation.Log;
import com.moyun.common.enums.BusinessType;
import com.moyun.common.exception.system.ServiceException;
import com.moyun.core.base.BaseController;
import com.moyun.core.base.TableDataInfo;
import com.moyun.ext.aigateway.entity.AiExecuteLog;
import com.moyun.ext.aigateway.mapper.AiExecuteLogMapper;
import com.moyun.ledger.domain.entity.LedgerAiAnalysisReport;
import com.moyun.ledger.domain.entity.LedgerCategory;
import com.moyun.ledger.domain.entity.LedgerTransaction;
import com.moyun.ledger.domain.entity.PortalDataAuth;
import com.moyun.ledger.domain.entity.SysDataAccessLog;
import com.moyun.ledger.mapper.CmsLedgerUserQueryMapper;
import com.moyun.ledger.mapper.LedgerAiAnalysisReportMapper;
import com.moyun.ledger.mapper.LedgerCategoryMapper;
import com.moyun.ledger.mapper.LedgerTransactionMapper;
import com.moyun.ledger.service.ILedgerDataAuthService;
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
 * 后台-记账用户维度（合规版）
 *
 * <p><b>合规红线</b>：
 * <ul>
 *   <li>列表只返回脱敏身份 + 聚合指标；</li>
 *   <li>查看个体流水必须先取得用户在 App 内的授权（范围含 transaction 且未过期/未撤回）；</li>
 *   <li>无论允许还是拒绝，都写入 {@code sys_data_access_log}，可事后追溯；</li>
 *   <li>流水只投影非敏感列（类型/金额/分类/日期/状态），备注、商户、凭证、账户名一律不返回。</li>
 * </ul>
 *
 * @author moyun
 */
@Tag(name = "CMS记账用户管理", description = "用户维度聚合 + 授权后流水（全程留痕）")
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

    @Autowired
    private CmsLedgerUserQueryMapper userQueryMapper;

    @Autowired
    private ILedgerDataAuthService dataAuthService;

    /** 记账类型中文标签（含借出/收款，避免后台显示英文枚举） */
    private static final Map<String, String> TYPE_LABELS = Map.of(
            "income", "收入", "expense", "支出", "transfer", "转账",
            "lend", "借出", "collect", "收款",
            "repayment", "还款", "borrow", "借款", "adjust", "余额校准");

    @Operation(summary = "记账用户列表（脱敏聚合 + 授权状态，可按授权状态筛选）")
    @PreAuthorize("@ss.hasPermi('cms:ledgerUsers:list')")
    @GetMapping("/list")
    public TableDataInfo list(@RequestParam(defaultValue = "1") Integer pageNum,
                              @RequestParam(defaultValue = "10") Integer pageSize,
                              @RequestParam(required = false) Long userId,
                              @RequestParam(required = false) String authStatus) {
        // 1. 记账域全部用户（流水 ∪ 账户 ∪ 预算 ∪ 存钱 ∪ 定时 ∪ 备忘 ∪ 仓库 ∪ 商品）
        List<Long> allIds = userQueryMapper.selectLedgerUserIds();
        if (userId != null) {
            allIds = allIds.stream().filter(id -> Objects.equals(id, userId)).toList();
        }

        // 2. 授权状态过滤：只有过滤时才需要全量判定授权
        boolean filterByAuth = authStatus != null && !authStatus.isBlank();
        Map<Long, PortalDataAuth> allAuthMap = filterByAuth
                ? dataAuthService.findActiveBatch(allIds) : null;
        if (allAuthMap != null) {
            boolean wantGranted = "granted".equals(authStatus);
            allIds = allIds.stream()
                    .filter(id -> allAuthMap.containsKey(id) == wantGranted)
                    .toList();
        }

        long total = allIds.size();
        int from = Math.max(0, (pageNum - 1) * pageSize);
        int to = Math.min(allIds.size(), from + pageSize);
        List<Long> pageIds = from >= allIds.size() ? List.of() : allIds.subList(from, to);
        if (pageIds.isEmpty()) {
            return getDataTable(new ArrayList<>(), total);
        }

        final Map<Long, PortalDataAuth> authOfPage = allAuthMap != null
                ? allAuthMap : dataAuthService.findActiveBatch(pageIds);

        // 3. 用户基础信息（脱敏输出；ledger.user_id = portal_user.id 主键）
        Map<Long, PortalUser> userMap = portalUserMapper.selectList(
                        new LambdaQueryWrapper<PortalUser>()
                                .in(PortalUser::getId, pageIds)
                                .select(PortalUser::getId, PortalUser::getUsername,
                                        PortalUser::getNickname, PortalUser::getPhone))
                .stream().collect(Collectors.toMap(PortalUser::getId, u -> u, (a, b) -> a));

        // 4. 三类聚合（页内 userId 一次 IN 聚合，避免 N+1）
        Map<Long, Map<String, Object>> txnAgg = aggregate(transactionMapper.selectMaps(
                new QueryWrapper<LedgerTransaction>()
                        .select("user_id", "count(*) as cnt", "max(transaction_date) as last_date")
                        .in("user_id", pageIds)
                        .eq("status", LedgerTransaction.STATUS_NORMAL)
                        .groupBy("user_id")));
        Map<Long, Map<String, Object>> reportAgg = aggregate(reportMapper.selectMaps(
                new QueryWrapper<LedgerAiAnalysisReport>()
                        .select("user_id", "count(*) as cnt")
                        .in("user_id", pageIds)
                        .groupBy("user_id")));
        Map<Long, Map<String, Object>> aiAgg = aggregate(aiExecuteLogMapper.selectMaps(
                new QueryWrapper<AiExecuteLog>()
                        .select("user_id", "count(*) as calls", "sum(token_used) as tokens", "sum(cost_yuan) as cost")
                        .in("user_id", pageIds)
                        .isNotNull("user_id")
                        .groupBy("user_id")));

        // 5. 组装行
        List<Map<String, Object>> rows = new ArrayList<>();
        for (Long uid : pageIds) {
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("userId", uid);
            PortalUser u = userMap.get(uid);
            row.put("nickname", mask(u == null ? String.valueOf(uid) : fallback(u.getNickname(), u.getUsername())));
            row.put("phone", u == null ? null : maskPhone(u.getPhone()));

            PortalDataAuth auth = authOfPage.get(uid);
            row.put("authStatus", auth == null ? "none" : "granted");
            row.put("authScopes", auth == null ? null : auth.getScopes());
            row.put("authExpireAt", auth == null || auth.getExpireAt() == null
                    ? null : auth.getExpireAt().toString().replace('T', ' '));

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
        return getDataTable(rows, total);
    }

    /**
     * 用户流水明细（需用户授权 transaction 范围）
     *
     * <p>未授权 → 403 + 落一条 denied 审计；已授权 → 返回最小字段集并落 allowed 审计。</p>
     */
    @Operation(summary = "用户流水明细（需授权；全程留痕）")
    @PreAuthorize("@ss.hasPermi('cms:ledgerUsers:query')")
    @Log(title = "记账-用户数据访问", businessType = BusinessType.OTHER)
    @GetMapping("/{userId}/transactions")
    public TableDataInfo transactions(@PathVariable Long userId,
                                      @RequestParam(defaultValue = "1") Integer pageNum,
                                      @RequestParam(defaultValue = "10") Integer pageSize) {
        PortalDataAuth auth = dataAuthService.findActive(userId);
        boolean allowed = auth != null && auth.covers(PortalDataAuth.SCOPE_TRANSACTION);
        String desc = "pageNum=" + pageNum + ", pageSize=" + pageSize;

        if (!allowed) {
            dataAuthService.recordAccess(userId, PortalDataAuth.SCOPE_TRANSACTION,
                    SysDataAccessLog.ACTION_QUERY, desc, SysDataAccessLog.RESULT_DENIED, null);
            throw new ServiceException("该用户未授权查看流水明细，请让其在 App「我的 → 隐私与授权」中开启", 403);
        }

        startPage();
        List<LedgerTransaction> txns = transactionMapper.selectList(
                new LambdaQueryWrapper<LedgerTransaction>()
                        .eq(LedgerTransaction::getUserId, userId)
                        .select(LedgerTransaction::getId, LedgerTransaction::getType,
                                LedgerTransaction::getAmount, LedgerTransaction::getCategoryId,
                                LedgerTransaction::getTransactionDate, LedgerTransaction::getStatus)
                        .orderByDesc(LedgerTransaction::getTransactionDate)
                        .orderByDesc(LedgerTransaction::getId));

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
        TableDataInfo data = getDataTable(rows);
        dataAccessServiceRecord(userId, auth.getId(), desc, rows.size());
        return data;
    }

    /** 成功查看后落一条 allowed 审计（条数不含字段值） */
    private void dataAccessServiceRecord(Long userId, Long authId, String desc, int rows) {
        dataAuthService.recordAccess(userId, PortalDataAuth.SCOPE_TRANSACTION,
                SysDataAccessLog.ACTION_QUERY, desc + ", rows=" + rows,
                SysDataAccessLog.RESULT_ALLOWED, authId);
    }

    /** 聚合结果按 user_id 映射 */
    private Map<Long, Map<String, Object>> aggregate(List<Map<String, Object>> rows) {
        Map<Long, Map<String, Object>> map = new HashMap<>();
        for (Map<String, Object> row : rows) {
            Long uid = toLong(row.get("user_id"));
            if (uid != null) {
                map.put(uid, row);
            }
        }
        return map;
    }

    // ==================== 脱敏工具 ====================

    private String mask(String text) {
        if (text == null || text.isBlank()) {
            return null;
        }
        if (text.length() <= 2) {
            return "**";
        }
        return text.charAt(0) + "*".repeat(text.length() - 2) + text.charAt(text.length() - 1);
    }

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
