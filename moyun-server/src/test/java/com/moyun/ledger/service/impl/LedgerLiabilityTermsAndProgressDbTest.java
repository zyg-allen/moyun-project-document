package com.moyun.ledger.service.impl;

import com.moyun.ledger.domain.dto.TransactionCreateDTO;
import com.moyun.ledger.domain.entity.LedgerLiabilityAccount;
import com.moyun.ledger.domain.entity.LedgerTransaction;
import com.moyun.ledger.mapper.LedgerLiabilityAccountMapper;
import com.moyun.ledger.service.ILedgerLiabilityAccountService;
import com.moyun.ledger.service.ILedgerTransactionService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

/**
 * 负债账户：已还期数（paid_terms）联动 + 可空字段"显式清空" —— 真库测试
 *
 * <h3>本类覆盖两处用户可见缺陷（v13.2）</h3>
 * <ol>
 *   <li><b>还款进度永远是 0 期</b>：App 负债卡片显示 {@code {{paidTerms}}/{{totalTerms}}期}
 *       （{@code pages/portfolio/index.vue:70}），但 {@code paid_terms} 全项目**从无写入方** ——
 *       用户还了 N 期仍显示 0/N。现改为在还款的**同一条原子 UPDATE** 里 {@code paid_terms +1}
 *       （冲正一笔还款 −1），避免"读旧值 +1 写回"的丢更新。</li>
 *   <li><b>清空静默失效</b>：App 清空"每期还款额/还款日/总期数"时显式发 {@code null}，
 *       而旧的 {@code updateById}（null 则跳过）让清空不生效。现按"请求体显式提供的字段名"
 *       判定：显式提供 → 以传入值为准（含 null=清空）；未提供 → 保持原值。</li>
 * </ol>
 *
 * <p>类上 {@code @Transactional} → 默认回滚；测试数据用 {@code userId=999980002}。</p>
 *
 * @author moyun
 */
@SpringBootTest
@Transactional
class LedgerLiabilityTermsAndProgressDbTest {

    private static final long UID = 999_980_002L;
    private static final BigDecimal THOUSAND = new BigDecimal("1000.00");
    private static final BigDecimal TWO_HUNDRED = new BigDecimal("200.00");
    private static final BigDecimal ONE_HUNDRED = new BigDecimal("100.00");

    @Autowired private ILedgerLiabilityAccountService liabilityService;
    @Autowired private ILedgerTransactionService transactionService;
    @Autowired private LedgerLiabilityAccountMapper liabilityMapper;

    // ==================== ① 已还期数联动 ====================

    @Test
    @DisplayName("记一笔还款 → paid_terms=1；再记一笔 → 2；删除（冲正）一笔 → 回到 1")
    void repayment_maintainsPaidTerms() {
        Long id = liabilityService.createAccount(UID, liability("信用卡"), THOUSAND).getId();
        assertNull(liabilityMapper.selectById(id).getPaidTerms(),
                "新建时未还期数为 null（列可空，按 0 展示）");

        Long firstTxn = transactionService.createTransaction(UID, repayment(id, TWO_HUNDRED));
        assertEquals(1, liabilityMapper.selectById(id).getPaidTerms(), "还 1 笔应为 1 期");
        assertBalance(id, "800.00");

        transactionService.createTransaction(UID, repayment(id, ONE_HUNDRED));
        assertEquals(2, liabilityMapper.selectById(id).getPaidTerms(), "还 2 笔应为 2 期");
        assertBalance(id, "700.00");

        // 冲正（删除）一笔还款：欠款回退，已还期数同步 -1
        transactionService.deleteTransaction(UID, firstTxn);
        assertEquals(1, liabilityMapper.selectById(id).getPaidTerms(),
                "冲正一笔还款后已还期数必须回退");
        assertBalance(id, "900.00");
    }

    @Test
    @DisplayName("借款不增加已还期数（只有还款算一期）")
    void borrow_doesNotTouchPaidTerms() {
        Long id = liabilityService.createAccount(UID, liability("信用贷"), THOUSAND).getId();
        transactionService.createTransaction(UID, borrow(id, TWO_HUNDRED));
        assertNull(liabilityMapper.selectById(id).getPaidTerms(), "借款不应计入已还期数");
    }

    // ==================== ② 可空字段：显式提供 → 可清空 ====================

    @Test
    @DisplayName("清空月供/还款日/总期数（显式传 null）必须真正落库——原实现静默失效")
    void updateAccount_clearsOptionalFieldsWhenProvided() {
        LedgerLiabilityAccount acc = liability("信用卡");
        acc.setMonthlyPayment(new BigDecimal("500.00"));
        acc.setRepaymentDay(10);
        acc.setTotalTerms(12);
        Long id = liabilityService.createAccount(UID, acc, THOUSAND).getId();

        LedgerLiabilityAccount payload = new LedgerLiabilityAccount();
        payload.setId(id);
        payload.setName("信用卡");
        payload.setMonthlyPayment(null);
        payload.setRepaymentDay(null);
        payload.setTotalTerms(null);
        liabilityService.updateAccount(UID, payload,
                Set.of("name", "monthlyPayment", "repaymentDay", "totalTerms"));

        LedgerLiabilityAccount after = liabilityMapper.selectById(id);
        assertNull(after.getMonthlyPayment(), "显式传 null 必须清空月供");
        assertNull(after.getRepaymentDay(), "显式传 null 必须清空还款日");
        assertNull(after.getTotalTerms(), "显式传 null 必须清空总期数");
    }

    @Test
    @DisplayName("未提供（未出现在请求体）的可空字段必须保持原值——防误清空")
    void updateAccount_keepsOptionalFieldsWhenNotProvided() {
        LedgerLiabilityAccount acc = liability("信用卡");
        acc.setMonthlyPayment(new BigDecimal("500.00"));
        acc.setRepaymentDay(10);
        acc.setTotalTerms(12);
        Long id = liabilityService.createAccount(UID, acc, THOUSAND).getId();

        LedgerLiabilityAccount payload = new LedgerLiabilityAccount();
        payload.setId(id);
        payload.setName("信用卡（改名）");
        // 只提供 name：月供/还款日/总期数不在 provided 里
        liabilityService.updateAccount(UID, payload, Set.of("name"));

        LedgerLiabilityAccount after = liabilityMapper.selectById(id);
        assertEquals("信用卡（改名）", after.getName());
        assertEquals(0, new BigDecimal("500.00").compareTo(after.getMonthlyPayment()), "未提供不应清空月供");
        assertEquals(10, after.getRepaymentDay(), "未提供不应清空还款日");
        assertEquals(12, after.getTotalTerms(), "未提供不应清空总期数");
    }

    // ==================== ③ 派生列/状态列不可由更新接口改写 ====================

    @Test
    @DisplayName("balance / paid_terms / settle_flag / status 传了也不生效（由记账联动与归档接口维护）")
    void updateAccount_cannotOverwriteDerivedOrStateColumns() {
        Long id = liabilityService.createAccount(UID, liability("信用卡"), THOUSAND).getId();

        LedgerLiabilityAccount payload = new LedgerLiabilityAccount();
        payload.setId(id);
        payload.setName("信用卡");
        payload.setBalance(new BigDecimal("999999.00"));   // 客户端越权
        payload.setPaidTerms(99);
        payload.setSettleFlag(1);
        payload.setStatus(LedgerLiabilityAccount.STATUS_ARCHIVED);
        liabilityService.updateAccount(UID, payload,
                Set.of("name", "balance", "paidTerms", "settleFlag", "status"));

        LedgerLiabilityAccount after = liabilityMapper.selectById(id);
        assertEquals(0, THOUSAND.compareTo(after.getBalance()), "balance 不得由改属性接口写入");
        assertNull(after.getPaidTerms(), "paid_terms 由记账联动维护，不得由改属性接口写入");
        assertEquals(0, after.getSettleFlag(), "settle_flag 不得由改属性接口改写");
        assertEquals(1, after.getStatus(), "status（归档）只能走 deleteAccount");
        assertEquals("信用卡", after.getName(), "合法字段仍应更新");
    }

    // ==================== 工具方法 ====================

    private void assertBalance(Long id, String expectedYuan) {
        assertEquals(0, new BigDecimal(expectedYuan).compareTo(liabilityMapper.selectById(id).getBalance()),
                "欠款应变为 " + expectedYuan + "，实际 " + liabilityMapper.selectById(id).getBalance());
    }

    private static LedgerLiabilityAccount liability(String name) {
        LedgerLiabilityAccount l = new LedgerLiabilityAccount();
        l.setName(name);
        l.setType(LedgerLiabilityAccount.TYPE_CREDIT_CARD);
        return l;
    }

    private static TransactionCreateDTO repayment(Long liabilityId, BigDecimal amount) {
        TransactionCreateDTO dto = new TransactionCreateDTO();
        dto.setType(LedgerTransaction.TYPE_REPAYMENT);
        dto.setAmount(amount);
        dto.setLiabilityId(liabilityId);
        dto.setTransactionDate(LocalDate.now());
        dto.setDescription("还款");
        return dto;
    }

    private static TransactionCreateDTO borrow(Long liabilityId, BigDecimal amount) {
        TransactionCreateDTO dto = new TransactionCreateDTO();
        dto.setType(LedgerTransaction.TYPE_BORROW);
        dto.setAmount(amount);
        dto.setLiabilityId(liabilityId);
        dto.setTransactionDate(LocalDate.now());
        dto.setDescription("借款");
        return dto;
    }
}
