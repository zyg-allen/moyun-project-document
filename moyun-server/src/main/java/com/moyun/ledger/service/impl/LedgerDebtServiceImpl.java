package com.moyun.ledger.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.moyun.common.exception.system.ServiceException;
import com.moyun.ledger.domain.dto.TransactionCreateDTO;
import com.moyun.ledger.domain.entity.LedgerDebtRepayment;
import com.moyun.ledger.domain.entity.LedgerObligation;
import com.moyun.ledger.domain.entity.LedgerTransaction;
import com.moyun.ledger.mapper.LedgerDebtRepaymentMapper;
import com.moyun.ledger.mapper.LedgerDebtSummaryMapper;
import com.moyun.ledger.mapper.LedgerObligationMapper;
import com.moyun.ledger.service.ILedgerDebtService;
import com.moyun.ledger.service.ILedgerTransactionService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 债权债务服务实现：应收/应付统一走 ledger_obligation 一张表，type 区分。
 *
 * @author moyun
 */
@Service
public class LedgerDebtServiceImpl implements ILedgerDebtService {

    @Autowired
    private LedgerObligationMapper obligationMapper;
    @Autowired
    private LedgerDebtRepaymentMapper repaymentMapper;
    @Autowired
    private LedgerDebtSummaryMapper summaryMapper;
    @Autowired
    private ILedgerTransactionService transactionService;

    private static final BigDecimal ZERO = BigDecimal.ZERO;

    private static BigDecimal nz(BigDecimal v) {
        return v == null ? ZERO : v;
    }

    /**
     * 由「总额 / 已结」推导结算状态。
     *
     * <p>**唯一的推导点**：写入与核销都走这里，避免两处各判一次而口径分叉。</p>
     */
    private static String resolveSettleStatus(BigDecimal total, BigDecimal paid) {
        BigDecimal t = nz(total);
        BigDecimal p = nz(paid);
        if (p.compareTo(ZERO) <= 0) {
            return "pending";
        }
        return p.compareTo(t) >= 0 ? "settled" : "partial";
    }

    // ---------------- 应收 ----------------

    @Override
    public Map<String, Object> listReceivables(Long userId, String keyword, String settleStatus,
                                              Integer pageNum, Integer pageSize) {
        return listByType(userId, LedgerObligation.TYPE_RECEIVABLE, keyword, settleStatus, pageNum, pageSize);
    }

    @Override
    public LedgerObligation getReceivable(Long userId, Long id) {
        return getOwned(userId, id, LedgerObligation.TYPE_RECEIVABLE);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long createReceivable(Long userId, LedgerObligation bo) {
        return create(userId, LedgerObligation.TYPE_RECEIVABLE, bo);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateReceivable(Long userId, Long id, LedgerObligation bo) {
        update(userId, id, LedgerObligation.TYPE_RECEIVABLE, bo, "应收");
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteReceivable(Long userId, Long id) {
        delete(userId, id, LedgerObligation.TYPE_RECEIVABLE);
    }

    // ---------------- 应付 ----------------

    @Override
    public Map<String, Object> listPayables(Long userId, String keyword, String settleStatus,
                                            Integer pageNum, Integer pageSize) {
        return listByType(userId, LedgerObligation.TYPE_PAYABLE, keyword, settleStatus, pageNum, pageSize);
    }

    @Override
    public LedgerObligation getPayable(Long userId, Long id) {
        return getOwned(userId, id, LedgerObligation.TYPE_PAYABLE);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long createPayable(Long userId, LedgerObligation bo) {
        return create(userId, LedgerObligation.TYPE_PAYABLE, bo);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updatePayable(Long userId, Long id, LedgerObligation bo) {
        update(userId, id, LedgerObligation.TYPE_PAYABLE, bo, "应付");
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deletePayable(Long userId, Long id) {
        delete(userId, id, LedgerObligation.TYPE_PAYABLE);
    }

    // ---------------- 核销 ----------------

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Map<String, Object> settle(Long userId, String debtType, Long debtId, BigDecimal amount,
                                      Long accountId, LocalDate repayDate, String remark) {
        if (!LedgerObligation.TYPE_RECEIVABLE.equals(debtType)
                && !LedgerObligation.TYPE_PAYABLE.equals(debtType)) {
            throw new ServiceException("非法债务类型");
        }
        if (amount == null || amount.compareTo(ZERO) <= 0) {
            throw new ServiceException("核销金额必须大于0");
        }
        LocalDate date = repayDate == null ? LocalDate.now() : repayDate;

        LedgerObligation d = getOwned(userId, debtId, debtType);
        BigDecimal outstanding = nz(d.getTotalAmount()).subtract(nz(d.getPaidAmount()));
        if (amount.compareTo(outstanding) > 0) {
            throw new ServiceException("核销金额超过未结余额（当前 " + outstanding.toPlainString() + " 元）");
        }

        Long txId;
        if (LedgerObligation.TYPE_RECEIVABLE.equals(debtType)) {
            // 收款 = 资金账户增加。走独立的 collect 类型而非 income ——
            // 收债不是收入（钱本来就是我的），计入收入报表会让当月收入虚高。
            txId = createSettleTransaction(userId, LedgerTransaction.TYPE_COLLECT, amount, accountId,
                    date, d.getCounterparty(), remark, d.getId(), null);
        } else {
            // 还款 = 资金账户减少，带 payableId 走 repayment。
            txId = createSettleTransaction(userId, LedgerTransaction.TYPE_REPAYMENT, amount, accountId,
                    date, d.getCounterparty(), remark, null, d.getId());
        }
        d.setPaidAmount(nz(d.getPaidAmount()).add(amount));
        d.setSettleStatus(resolveSettleStatus(d.getTotalAmount(), d.getPaidAmount()));
        obligationMapper.updateById(d);

        // 核销流水留痕：它是「这笔债分几次还的」唯一凭据，也是撤销时的回滚依据
        LedgerDebtRepayment r = new LedgerDebtRepayment();
        r.setUserId(userId);
        r.setDebtType(debtType);
        r.setDebtId(debtId);
        r.setAmount(amount);
        r.setAccountId(accountId);
        r.setTransactionId(txId);
        r.setRepayDate(date);
        r.setOperatorId(userId);
        r.setRemark(remark);
        r.setStatus(LedgerDebtRepayment.STATUS_NORMAL);
        repaymentMapper.insert(r);

        Map<String, Object> result = new HashMap<>();
        result.put("repaymentId", r.getId());
        result.put("transactionId", txId);
        result.put("settleStatus", d.getSettleStatus());
        return result;
    }

    /**
     * 生成核销的资金流水。
     *
     * <p>复用 {@link ILedgerTransactionService#createTransaction} ——
     * 绝不在这里另写一套余额变动，否则资产联动口径会分叉成两份，早晚对不上账。</p>
     */
    private Long createSettleTransaction(Long userId, String type, BigDecimal amount, Long accountId,
                                        LocalDate date, String counterparty, String remark,
                                        Long receivableId, Long payableId) {
        TransactionCreateDTO dto = new TransactionCreateDTO();
        dto.setType(type);
        dto.setAmount(amount);
        dto.setAccountId(accountId);
        dto.setTransactionDate(date);
        String desc = (remark != null && !remark.isBlank() ? remark : "债务核销") + " - " + counterparty;
        dto.setDescription(desc.length() > 200 ? desc.substring(0, 200) : desc);
        dto.setReceivableId(receivableId);
        dto.setPayableId(payableId);
        // 债权转换不是当期收支行为，不计预算（与 lend/collect 同口径）
        dto.setIsBudget(0);
        return transactionService.createTransaction(userId, dto);
    }

    @Override
    public List<LedgerDebtRepayment> listRepayments(Long userId, String debtType, Long debtId) {
        LambdaQueryWrapper<LedgerDebtRepayment> q = new LambdaQueryWrapper<>();
        q.eq(LedgerDebtRepayment::getUserId, userId)
                .eq(LedgerDebtRepayment::getStatus, LedgerDebtRepayment.STATUS_NORMAL);
        if (debtType != null && !debtType.isBlank()) {
            q.eq(LedgerDebtRepayment::getDebtType, debtType);
        }
        if (debtId != null && debtId > 0) {
            q.eq(LedgerDebtRepayment::getDebtId, debtId);
        }
        q.orderByDesc(LedgerDebtRepayment::getRepayDate).orderByDesc(LedgerDebtRepayment::getId);
        return repaymentMapper.selectList(q);
    }

    @Override
    public Map<String, Object> summary(Long userId) {
        BigDecimal receivable = nz(summaryMapper.sumReceivableOutstanding(userId));
        BigDecimal payable = nz(summaryMapper.sumPayableOutstanding(userId));
        Map<String, Object> m = new HashMap<>();
        m.put("receivableOutstanding", receivable);
        m.put("payableOutstanding", payable);
        return m;
    }

    // ---------------- 内部：统一按 type 走一张表 ----------------

    private Map<String, Object> listByType(Long userId, String type, String keyword, String settleStatus,
                                           Integer pageNum, Integer pageSize) {
        LambdaQueryWrapper<LedgerObligation> q = new LambdaQueryWrapper<>();
        // 始终限定 userId + type：债务是强私有数据，任何筛选都不能绕过它
        q.eq(LedgerObligation::getUserId, userId)
                .eq(LedgerObligation::getType, type)
                .eq(LedgerObligation::getStatus, LedgerObligation.STATUS_NORMAL);
        if (keyword != null && !keyword.isBlank()) {
            q.like(LedgerObligation::getCounterparty, keyword.trim());
        }
        if (settleStatus != null && !settleStatus.isBlank()) {
            q.eq(LedgerObligation::getSettleStatus, settleStatus.trim());
        }
        q.orderByAsc(LedgerObligation::getSettleStatus)
                .orderByAsc(LedgerObligation::getDueDate)
                .orderByDesc(LedgerObligation::getId);

        int pn = (pageNum == null || pageNum < 1) ? 1 : pageNum;
        int ps = (pageSize == null || pageSize < 1) ? 20 : Math.min(pageSize, 100);
        Page<LedgerObligation> page = obligationMapper.selectPage(new Page<>(pn, ps), q);
        return pageResult(page.getRecords(), page.getTotal(), pn, ps, page.getPages());
    }

    private Long create(Long userId, String type, LedgerObligation bo) {
        validateDebt(bo.getCounterparty(), bo.getTotalAmount());
        LedgerObligation o = new LedgerObligation();
        o.setUserId(userId);
        o.setType(type);
        o.setCounterparty(bo.getCounterparty().trim());
        o.setCounterpartyId(bo.getCounterpartyId());
        o.setWarehouseId(bo.getWarehouseId());
        o.setSourceType(bo.getSourceType() == null ? LedgerObligation.SOURCE_MANUAL : bo.getSourceType());
        o.setSourceId(bo.getSourceId());
        o.setTotalAmount(bo.getTotalAmount());
        o.setPaidAmount(ZERO);
        o.setSettleStatus(LedgerObligation.SETTLE_PENDING);
        o.setDueDate(bo.getDueDate());
        o.setRemark(bo.getRemark());
        o.setStatus(LedgerObligation.STATUS_NORMAL);
        o.setCreateBy(bo.getCreateBy());
        obligationMapper.insert(o);
        return o.getId();
    }

    private void update(Long userId, Long id, String type, LedgerObligation bo, String label) {
        LedgerObligation db = getOwned(userId, id, type);
        if (bo.getCounterparty() != null) {
            db.setCounterparty(bo.getCounterparty().trim());
        }
        if (bo.getTotalAmount() != null) {
            // 总额不得低于已结金额，否则账上会出现"负的未结余额"
            if (bo.getTotalAmount().compareTo(nz(db.getPaidAmount())) < 0) {
                throw new ServiceException(label + "总额不能小于已结金额 " + nz(db.getPaidAmount()).toPlainString() + " 元");
            }
            db.setTotalAmount(bo.getTotalAmount());
            db.setSettleStatus(resolveSettleStatus(db.getTotalAmount(), db.getPaidAmount()));
        }
        if (bo.getDueDate() != null) {
            db.setDueDate(bo.getDueDate());
        }
        if (bo.getRemark() != null) {
            db.setRemark(bo.getRemark());
        }
        obligationMapper.updateById(db);
    }

    private void delete(Long userId, Long id, String type) {
        LedgerObligation db = getOwned(userId, id, type);
        db.setStatus(LedgerObligation.STATUS_DELETED);
        obligationMapper.updateById(db);
    }

    private void validateDebt(String counterparty, BigDecimal total) {
        if (counterparty == null || counterparty.isBlank()) {
            throw new ServiceException("请填写对方名称");
        }
        if (total == null || total.compareTo(ZERO) <= 0) {
            throw new ServiceException("金额必须大于0");
        }
    }

    private LedgerObligation getOwned(Long userId, Long id, String type) {
        LedgerObligation o = obligationMapper.selectById(id);
        if (o == null || !userId.equals(o.getUserId())
                || !type.equals(o.getType())
                || LedgerObligation.STATUS_DELETED == o.getStatus()) {
            throw new ServiceException("债务记录不存在");
        }
        return o;
    }

    private Map<String, Object> pageResult(List<?> records, long total, int pn, int ps, long pages) {
        Map<String, Object> m = new HashMap<>();
        m.put("records", records);
        m.put("total", total);
        m.put("pageNum", pn);
        m.put("pageSize", ps);
        m.put("pages", pages);
        return m;
    }
}
