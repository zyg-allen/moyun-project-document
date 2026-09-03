package com.moyun.ledger.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.moyun.core.base.AjaxResult;
import com.moyun.core.base.BaseController;
import com.moyun.ledger.domain.entity.LedgerAssetAccount;
import com.moyun.ledger.domain.entity.LedgerLiabilityAccount;
import com.moyun.ledger.domain.entity.LedgerTransaction;
import com.moyun.ledger.mapper.LedgerAssetAccountMapper;
import com.moyun.ledger.mapper.LedgerLiabilityAccountMapper;
import com.moyun.ledger.mapper.LedgerTransactionMapper;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.Map;

/**
 * CMS 记账运营统计 Controller
 *
 * <p>脱敏红线：仅返回聚合指标（用户数/流水数/类型分布/近30日活跃），
 * 不返回任何用户个体数据、金额明细、账户名称。
 *
 * @author moyun
 */
@Tag(name = "CMS记账运营统计", description = "脱敏聚合运营指标")
@RestController
@RequestMapping("/cms/ledger/stats")
public class CmsLedgerStatsController extends BaseController {

    @Autowired
    private LedgerAssetAccountMapper assetAccountMapper;

    @Autowired
    private LedgerLiabilityAccountMapper liabilityAccountMapper;

    @Autowired
    private LedgerTransactionMapper transactionMapper;

    @Operation(summary = "运营统计总览（脱敏聚合）")
    @PreAuthorize("@ss.hasPermi('cms:ledgerStats:list')")
    @GetMapping("/overview")
    public AjaxResult overview() {
        Map<String, Object> data = new HashMap<>();
        LocalDate now = LocalDate.now();
        LocalDate monthAgo = now.minusDays(30);

        // 用户规模：持有任一启用账户的用户数（资产∪负债，内存去重）
        java.util.Set<Long> userIds = new java.util.HashSet<>();
        LambdaQueryWrapper<LedgerAssetAccount> aq = new LambdaQueryWrapper<>();
        aq.eq(LedgerAssetAccount::getStatus, LedgerAssetAccount.STATUS_ENABLED);
        assetAccountMapper.selectList(aq).forEach(a -> userIds.add(a.getUserId()));
        LambdaQueryWrapper<LedgerLiabilityAccount> lq = new LambdaQueryWrapper<>();
        lq.eq(LedgerLiabilityAccount::getStatus, LedgerLiabilityAccount.STATUS_ENABLED);
        liabilityAccountMapper.selectList(lq).forEach(l -> userIds.add(l.getUserId()));
        data.put("userCount", userIds.size());

        // 流水规模（含已删除=历史记账行为量）
        data.put("transactionCount", transactionMapper.selectCount(null));

        // 近30日活跃记账用户数（近30日有流水记录的去重用户）
        java.util.Set<Long> activeUsers = new java.util.HashSet<>();
        LambdaQueryWrapper<LedgerTransaction> tq = new LambdaQueryWrapper<>();
        tq.ge(LedgerTransaction::getTransactionDate, monthAgo);
        transactionMapper.selectList(tq).forEach(t -> activeUsers.add(t.getUserId()));
        data.put("activeUserCount30d", activeUsers.size());

        // 记账类型分布（status=1 有效流水）
        Map<String, Long> typeDist = new HashMap<>();
        LambdaQueryWrapper<LedgerTransaction> dq = new LambdaQueryWrapper<>();
        dq.eq(LedgerTransaction::getStatus, LedgerTransaction.STATUS_NORMAL)
                .select(LedgerTransaction::getType);
        for (LedgerTransaction t : transactionMapper.selectList(dq)) {
            typeDist.merge(t.getType(), 1L, Long::sum);
        }
        data.put("typeDistribution", typeDist);

        return success(data);
    }
}
