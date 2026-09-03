package com.moyun.ledger.job;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.moyun.ledger.domain.entity.LedgerAssetAccount;
import com.moyun.ledger.domain.entity.LedgerLiabilityAccount;
import com.moyun.ledger.domain.entity.LedgerNetWorthSnapshot;
import com.moyun.ledger.mapper.LedgerAssetAccountMapper;
import com.moyun.ledger.mapper.LedgerLiabilityAccountMapper;
import com.moyun.ledger.mapper.LedgerNetWorthSnapshotMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * 净资产每日快照定时任务
 *
 * <p>每日 00:10 为所有活跃记账用户生成前一日快照补漏 + 当日基线；
 * 当日有记账的用户由事务内实时 upsert 维护，本任务兜底全量。
 *
 * @author moyun
 */
@Component("ledgerNetWorthSnapshotTask")
public class LedgerNetWorthSnapshotTask {

    private static final Logger log = LoggerFactory.getLogger(LedgerNetWorthSnapshotTask.class);

    @Autowired
    private LedgerAssetAccountMapper assetAccountMapper;

    @Autowired
    private LedgerLiabilityAccountMapper liabilityAccountMapper;

    @Autowired
    private LedgerNetWorthSnapshotMapper snapshotMapper;

    /** 每日 00:10 执行 */
    @Scheduled(cron = "0 10 0 * * ?")
    public void snapshot() {
        LocalDate today = LocalDate.now();
        // 收集所有有账户的用户（资产或负债任一存在即视为记账用户）
        Set<Long> userIds = new HashSet<>();
        LambdaQueryWrapper<LedgerAssetAccount> aq = new LambdaQueryWrapper<>();
        aq.eq(LedgerAssetAccount::getStatus, LedgerAssetAccount.STATUS_ENABLED);
        for (LedgerAssetAccount a : assetAccountMapper.selectList(aq)) {
            userIds.add(a.getUserId());
        }
        LambdaQueryWrapper<LedgerLiabilityAccount> lq = new LambdaQueryWrapper<>();
        lq.eq(LedgerLiabilityAccount::getStatus, LedgerLiabilityAccount.STATUS_ENABLED);
        for (LedgerLiabilityAccount l : liabilityAccountMapper.selectList(lq)) {
            userIds.add(l.getUserId());
        }

        int created = 0;
        for (Long userId : userIds) {
            try {
                created += upsertSnapshot(userId, today);
            } catch (Exception e) {
                log.error("净资产快照生成失败 userId={}", userId, e);
            }
        }
        log.info("净资产每日快照任务完成：用户数={} 新增快照={}", userIds.size(), created);
    }

    /** 单用户快照 upsert（已存在则跳过，当日实时值以事务内 upsert 为准） */
    private int upsertSnapshot(Long userId, LocalDate date) {
        LambdaQueryWrapper<LedgerNetWorthSnapshot> sq = new LambdaQueryWrapper<>();
        sq.eq(LedgerNetWorthSnapshot::getUserId, userId)
                .eq(LedgerNetWorthSnapshot::getSnapDate, date);
        if (snapshotMapper.selectCount(sq) > 0) {
            return 0;
        }
        LambdaQueryWrapper<LedgerAssetAccount> aq = new LambdaQueryWrapper<>();
        aq.eq(LedgerAssetAccount::getUserId, userId)
                .eq(LedgerAssetAccount::getStatus, LedgerAssetAccount.STATUS_ENABLED)
                .eq(LedgerAssetAccount::getIncludeInTotal, 1);
        long totalAsset = 0;
        List<LedgerAssetAccount> assets = assetAccountMapper.selectList(aq);
        for (LedgerAssetAccount a : assets) {
            totalAsset += a.getBalance();
        }
        LambdaQueryWrapper<LedgerLiabilityAccount> lq = new LambdaQueryWrapper<>();
        lq.eq(LedgerLiabilityAccount::getUserId, userId)
                .eq(LedgerLiabilityAccount::getStatus, LedgerLiabilityAccount.STATUS_ENABLED)
                .eq(LedgerLiabilityAccount::getIncludeInTotal, 1);
        long totalLiability = 0;
        List<LedgerLiabilityAccount> liabilities = liabilityAccountMapper.selectList(lq);
        for (LedgerLiabilityAccount l : liabilities) {
            totalLiability += l.getBalance();
        }
        LedgerNetWorthSnapshot snap = new LedgerNetWorthSnapshot();
        snap.setUserId(userId);
        snap.setSnapDate(date);
        snap.setTotalAsset(totalAsset);
        snap.setTotalLiability(totalLiability);
        snap.setNetWorth(totalAsset - totalLiability);
        snapshotMapper.insert(snap);
        return 1;
    }
}
