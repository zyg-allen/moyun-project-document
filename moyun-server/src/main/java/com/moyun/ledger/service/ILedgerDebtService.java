package com.moyun.ledger.service;

import com.moyun.ledger.domain.entity.LedgerDebtRepayment;
import com.moyun.ledger.domain.entity.LedgerObligation;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Map;

/**
 * 债权债务服务：应收/应付统一走 ledger_obligation 一张表，type 区分。
 *
 * <p>设计要点：核销**复用** {@link ILedgerTransactionService#createTransaction} 生成资金流水，
 * 不另写一套余额变动逻辑 —— 否则资产联动口径会分叉成两份，早晚对不上账。</p>
 *
 * @author moyun
 */
public interface ILedgerDebtService {

    // ---------------- 应收 ----------------

    Map<String, Object> listReceivables(Long userId, String keyword, String settleStatus,
                                       Integer pageNum, Integer pageSize);

    LedgerObligation getReceivable(Long userId, Long id);

    Long createReceivable(Long userId, LedgerObligation bo);

    void updateReceivable(Long userId, Long id, LedgerObligation bo);

    void deleteReceivable(Long userId, Long id);

    // ---------------- 应付 ----------------

    Map<String, Object> listPayables(Long userId, String keyword, String settleStatus,
                                    Integer pageNum, Integer pageSize);

    LedgerObligation getPayable(Long userId, Long id);

    Long createPayable(Long userId, LedgerObligation bo);

    void updatePayable(Long userId, Long id, LedgerObligation bo);

    void deletePayable(Long userId, Long id);

    // ---------------- 核销 ----------------

    /**
     * 核销一笔债务（收款 / 还款）。
     *
     * @param debtType  receivable=对方还我钱（资金账户+）；payable=我还对方钱（资金账户-）
     * @param debtId    债务记录ID
     * @param amount    本次核销金额，必须 &gt; 0 且不超过未结余额
     * @param accountId 资金账户（钱进/出哪个账户）
     * @param repayDate 核销日期
     * @param remark    备注
     * @return 包含核销流水ID与最新结算状态的 Map
     */
    Map<String, Object> settle(Long userId, String debtType, Long debtId, BigDecimal amount,
                               Long accountId, LocalDate repayDate, String remark);

    /** 某笔债务的全部核销记录（按日期倒序） */
    java.util.List<LedgerDebtRepayment> listRepayments(Long userId, String debtType, Long debtId);

    /**
     * 债务汇总（资产总览口径用）：应收未结、应付未结。
     * <p>口径说明：应收计入总资产、应付计入总负债，各自只算**未结余额**（total - paid），
     * 已结清部分早已变成现金或已从现金支出，重复计入会双重计算。</p>
     */
    Map<String, Object> summary(Long userId);
}
