package com.moyun.ledger.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.moyun.ledger.domain.dto.TransactionCreateDTO;
import com.moyun.ledger.domain.entity.LedgerAssetAccount;
import com.moyun.ledger.domain.entity.LedgerLiabilityAccount;
import com.moyun.ledger.domain.entity.LedgerNetWorthSnapshot;
import com.moyun.ledger.domain.entity.LedgerTransaction;
import com.moyun.ledger.mapper.LedgerAssetAccountMapper;
import com.moyun.ledger.mapper.LedgerLiabilityAccountMapper;
import com.moyun.ledger.mapper.LedgerNetWorthSnapshotMapper;
import com.moyun.ledger.mapper.LedgerTransactionMapper;
import com.moyun.ledger.service.ILedgerAssetAccountService;
import com.moyun.ledger.service.ILedgerLiabilityAccountService;
import com.moyun.ledger.service.ILedgerTransactionService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.test.context.transaction.TestTransaction;
import org.springframework.transaction.support.TransactionTemplate;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.function.Supplier;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * 记账账户"改属性"与"记账联动"的隔离性 —— 真库回归测试
 *
 * <h3>被验证的缺陷（P0 · 资金）</h3>
 * <p>{@code LedgerAssetAccountServiceImpl.updateAccount} / {@code LedgerLiabilityAccountServiceImpl.updateAccount}
 * 原来是 <b>读整行 → 改字段 → {@code updateById} 写回整行</b>：</p>
 * <pre>
 * exist = getOwned(...);                       // ① 快照读（含 balance / version）
 * account.setBalance(exist.getBalance());      // ② 把"读到的"余额塞回待写实体
 * account.setVersion(exist.getVersion());
 * updateById(account);                         // ③ 写回整行 → balance / version 一起被覆盖
 * </pre>
 * <p>而余额的真正维护方是 {@code LedgerTransactionServiceImpl.applyAssetDelta/applyLiabilityDelta}
 * （{@code balance = balance + delta}、{@code version = version + 1}、{@code WHERE version = ?} 乐观锁）。
 * 两者叠加的后果：</p>
 * <ol>
 *   <li><b>抹账</b>：用户改账户名期间并发记了一笔账，改属性会把余额<strong>写回记账前的旧值</strong>
 *       —— 流水记着 +50、余额却没变，账实不符；</li>
 *   <li><b>乐观锁 ABA</b>：把 {@code version} 写回旧值后，"读到的 version"重新可用，
 *       两个并发记账都可能命中 {@code WHERE version = ?} → 重复叠加或丢更新。</li>
 * </ol>
 *
 * <h3>为什么这样测（确定性复现，不用 Mock、不用 sleep）</h3>
 * <p>竞态窗口在 service 内部的"读之后、写之前"。这里用 MySQL <b>REPEATABLE READ 的一致性快照</b>
 * 把这个窗口变成确定性的：</p>
 * <ol>
 *   <li>T1（本测试事务）：建账户并在 T1 内读一次 → <b>固定 T1 的 read view</b>；</li>
 *   <li>T2（{@code REQUIRES_NEW} 独立事务）：通过<b>真实记账链路</b>
 *       {@code ILedgerTransactionService.createTransaction} 落一笔账并提交；</li>
 *   <li>T1：调用 {@code updateAccount}——其内部 {@code getOwned} 仍看到 T2 提交前的快照值（100）；</li>
 *   <li>用<b>新事务</b>读最终落库值：修复后余额应为 150（未被抹），修复前为 100（被抹）。</li>
 * </ol>
 * <p>即"旧值"不是猜的：T1 的快照天然就是旧的，T2 是真实提交的生产路径写入。</p>
 *
 * <p>⚠️ 因用到 {@code REQUIRES_NEW}，T2 的写入<b>不会</b>随测试事务回滚，故 {@link #cleanup()}
 * 在独立事务里显式清理测试数据（固定 {@code userId=999980001}，不触碰真实数据）。</p>
 *
 * @author moyun
 */
@SpringBootTest
@Transactional
class LedgerAccountMetaUpdateIsolationDbTest {

    /** 测试专用 userId（远离真实数据） */
    private static final long UID = 999_980_001L;
    private static final BigDecimal HUNDRED = new BigDecimal("100.00");
    private static final BigDecimal FIFTY = new BigDecimal("50.00");
    private static final BigDecimal HUNDRED_FIFTY = new BigDecimal("150.00");

    @Autowired private ILedgerAssetAccountService assetService;
    @Autowired private ILedgerLiabilityAccountService liabilityService;
    @Autowired private ILedgerTransactionService transactionService;

    @Autowired private LedgerAssetAccountMapper assetMapper;
    @Autowired private LedgerLiabilityAccountMapper liabilityMapper;
    @Autowired private LedgerTransactionMapper transactionMapper;
    @Autowired private LedgerNetWorthSnapshotMapper snapshotMapper;

    @Autowired private PlatformTransactionManager txManager;

    @AfterEach
    void cleanup() {
        // ① 先结束测试事务（回滚）。否则 T1 持有的行锁会让下面的清理事务一直等锁（实测 Lock wait timeout）
        if (TestTransaction.isActive()) {
            TestTransaction.flagForRollback();
            TestTransaction.end();
        }
        // ② 显式清理 REQUIRES_NEW 的写入（它们不受测试事务回滚影响）
        inNewTx(() -> {
            assetMapper.delete(new LambdaQueryWrapper<LedgerAssetAccount>()
                    .eq(LedgerAssetAccount::getUserId, UID));
            liabilityMapper.delete(new LambdaQueryWrapper<LedgerLiabilityAccount>()
                    .eq(LedgerLiabilityAccount::getUserId, UID));
            transactionMapper.delete(new LambdaQueryWrapper<LedgerTransaction>()
                    .eq(LedgerTransaction::getUserId, UID));
            snapshotMapper.delete(new LambdaQueryWrapper<LedgerNetWorthSnapshot>()
                    .eq(LedgerNetWorthSnapshot::getUserId, UID));
            return null;
        });
    }

    // ==================== ① 资产：改属性不得抹掉并发记账结果 ====================

    @Test
    @DisplayName("资产改属性：并发记一笔收入 +50 后，balance/version 必须保持记账结果（不得写回旧值）")
    void assetMetaUpdate_doesNotOverwriteConcurrentIncome() {
        // 账户必须在**已提交事务**中创建：T2 是独立事务，看不到 T1 未提交的 INSERT
        Long id = inNewTx(() -> assetService.createAccount(UID, asset("现金"), HUNDRED).getId());
        // 固定 T1 的 read view（此后 T1 内所有一致性读都停在 T2 提交之前）
        assertEquals(0, HUNDRED.compareTo(assetMapper.selectById(id).getBalance()));

        // T2：真实记账链路，+50 并提交
        inNewTx(() -> transactionService.createTransaction(UID, income(id, FIFTY)));
        assertEquals(0, HUNDRED_FIFTY.compareTo(currentAsset(id).getBalance()),
                "前置条件：并发记账应已落库为 150");

        // T1：改属性（客户端只送可编辑字段，与 moyun-ledger-app portfolio/index.vue 一致）
        LedgerAssetAccount payload = new LedgerAssetAccount();
        payload.setId(id);
        payload.setName("现金（改名）");
        payload.setType("cash");
        payload.setIncludeInTotal(0);
        assetService.updateAccount(UID, payload);

        // 在 T1 内读回：InnoDB 的 UPDATE 会基于"最新已提交行"生成 T1 自己的行版本，
        // 因此这里读到的就是"T1 若提交将落库的值"，也正是抹账在真实环境中的后果。
        LedgerAssetAccount after = assetMapper.selectById(id);
        assertEquals("现金（改名）", after.getName(), "功能不得回退");
        assertEquals(0, after.getIncludeInTotal());
        assertEquals(0, HUNDRED_FIFTY.compareTo(after.getBalance()),
                "改属性把并发记账的结果抹掉了：流水记了 +50，余额却回到旧值（账实不符）。实际 balance="
                        + after.getBalance());
        assertEquals(1, after.getVersion(),
                "version 被写回旧值 → 乐观锁 WHERE version=? 的 ABA 窗口被重新打开。实际 version="
                        + after.getVersion());
    }

    @Test
    @DisplayName("资产归档（删除）：并发记账 +50 后归档，balance/version 必须保持不变")
    void assetArchive_doesNotOverwriteConcurrentIncome() {
        Long id = inNewTx(() -> assetService.createAccount(UID, asset("待归档"), HUNDRED).getId());
        assertEquals(0, HUNDRED.compareTo(assetMapper.selectById(id).getBalance()));

        inNewTx(() -> transactionService.createTransaction(UID, income(id, FIFTY)));

        assetService.deleteAccount(UID, id);

        // T1 内读回 = "T1 若提交将落库的值"
        LedgerAssetAccount after = assetMapper.selectById(id);
        assertEquals(LedgerAssetAccount.STATUS_ARCHIVED, after.getStatus(), "归档状态应生效");
        assertEquals(0, HUNDRED_FIFTY.compareTo(after.getBalance()),
                "归档把并发记账的余额抹掉了。实际 balance=" + after.getBalance());
        assertEquals(1, after.getVersion(), "归档把 version 写回旧值。实际 version=" + after.getVersion());
    }

    // ==================== ② 负债：同上 ====================

    @Test
    @DisplayName("负债改属性：并发借入 +50 后，balance/version 必须保持记账结果")
    void liabilityMetaUpdate_doesNotOverwriteConcurrentBorrow() {
        Long id = inNewTx(() -> liabilityService.createAccount(UID, liability("信用卡"), HUNDRED).getId());
        assertEquals(0, HUNDRED.compareTo(liabilityMapper.selectById(id).getBalance()));

        inNewTx(() -> transactionService.createTransaction(UID, borrow(id, FIFTY)));
        assertEquals(0, HUNDRED_FIFTY.compareTo(currentLiability(id).getBalance()),
                "前置条件：并发借入应已落库为 150");

        LedgerLiabilityAccount payload = new LedgerLiabilityAccount();
        payload.setId(id);
        payload.setName("信用卡（改名）");
        payload.setType("credit_card");
        payload.setIncludeInTotal(0);
        liabilityService.updateAccount(UID, payload);

        // T1 内读回 = "T1 若提交将落库的值"
        LedgerLiabilityAccount after = liabilityMapper.selectById(id);
        assertEquals("信用卡（改名）", after.getName());
        assertEquals(0, after.getIncludeInTotal());
        assertEquals(0, HUNDRED_FIFTY.compareTo(after.getBalance()),
                "改属性把并发借入的结果抹掉了（欠款账实不符）。实际 balance=" + after.getBalance());
        assertEquals(1, after.getVersion(), "version 被写回旧值。实际 version=" + after.getVersion());
    }

    // ==================== ③ 列范围：status 不属可编辑列 ====================

    @Test
    @DisplayName("改属性不得复活已归档账户（status 不在可编辑列内）")
    void assetMetaUpdate_cannotResurrectArchivedAccount() {
        Long id = inNewTx(() -> assetService.createAccount(UID, asset("已归档"), HUNDRED).getId());
        assetService.deleteAccount(UID, id);
        assertEquals(LedgerAssetAccount.STATUS_ARCHIVED, assetMapper.selectById(id).getStatus());

        LedgerAssetAccount payload = new LedgerAssetAccount();
        payload.setId(id);
        payload.setName("已归档（改名）");
        payload.setStatus(LedgerAssetAccount.STATUS_ENABLED);   // 客户端越权尝试
        assetService.updateAccount(UID, payload);

        assertEquals(LedgerAssetAccount.STATUS_ARCHIVED, assetMapper.selectById(id).getStatus(),
                "status 属归档语义，只能走 deleteAccount，不得由改属性接口改写");
    }

    // ==================== ④ 不清空未提供的列（防列级 UPDATE 引入数据丢失） ====================

    @Test
    @DisplayName("部分字段更新：未提供的列（icon/valuation/hideBalance/sortOrder）必须保留原值")
    void assetMetaUpdate_keepsColumnsNotProvided() {
        LedgerAssetAccount acc = asset("投资账户");
        acc.setIcon("icon-invest");
        acc.setValuation(new BigDecimal("8888.00"));
        acc.setHideBalance(1);
        acc.setSortOrder(7);
        Long id = inNewTx(() -> assetService.createAccount(UID, acc, HUNDRED).getId());

        LedgerAssetAccount payload = new LedgerAssetAccount();
        payload.setId(id);
        payload.setName("投资账户（改名）");
        assetService.updateAccount(UID, payload);

        LedgerAssetAccount after = assetMapper.selectById(id);
        assertEquals("icon-invest", after.getIcon(), "未提供的 icon 被置空了");
        assertEquals(0, new BigDecimal("8888.00").compareTo(after.getValuation()), "未提供的 valuation 被置空了");
        assertEquals(1, after.getHideBalance(), "未提供的 hideBalance 被置空了");
        assertEquals(7, after.getSortOrder(), "未提供的 sortOrder 被置空了");
        assertEquals(0, HUNDRED.compareTo(after.getBalance()), "未改余额时余额必须不变");
    }

    // ==================== 工具方法 ====================

    /** 在独立事务（REQUIRES_NEW）中执行并提交；用于制造"并发"和生产最终落库值 */
    private <T> T inNewTx(Supplier<T> work) {
        TransactionTemplate t = new TransactionTemplate(txManager);
        t.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
        return t.execute(status -> work.get());
    }

    private LedgerAssetAccount currentAsset(Long id) {
        return inNewTx(() -> assetMapper.selectById(id));
    }

    private LedgerLiabilityAccount currentLiability(Long id) {
        return inNewTx(() -> liabilityMapper.selectById(id));
    }

    private static LedgerAssetAccount asset(String name) {
        LedgerAssetAccount a = new LedgerAssetAccount();
        a.setName(name);
        a.setType(LedgerAssetAccount.TYPE_CASH);
        return a;
    }

    private static LedgerLiabilityAccount liability(String name) {
        LedgerLiabilityAccount l = new LedgerLiabilityAccount();
        l.setName(name);
        l.setType(LedgerLiabilityAccount.TYPE_CREDIT_CARD);
        return l;
    }

    private static TransactionCreateDTO income(Long accountId, BigDecimal amount) {
        TransactionCreateDTO dto = new TransactionCreateDTO();
        dto.setType(LedgerTransaction.TYPE_INCOME);
        dto.setAmount(amount);
        dto.setAccountId(accountId);
        dto.setTransactionDate(LocalDate.now());
        dto.setDescription("并发记账-收入");
        return dto;
    }

    private static TransactionCreateDTO borrow(Long liabilityId, BigDecimal amount) {
        TransactionCreateDTO dto = new TransactionCreateDTO();
        dto.setType(LedgerTransaction.TYPE_BORROW);
        dto.setAmount(amount);
        dto.setLiabilityId(liabilityId);
        dto.setTransactionDate(LocalDate.now());
        dto.setDescription("并发记账-借入");
        return dto;
    }
}
