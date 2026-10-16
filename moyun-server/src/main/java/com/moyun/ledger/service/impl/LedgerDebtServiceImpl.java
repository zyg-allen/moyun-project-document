package com.moyun.ledger.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.moyun.common.exception.system.ServiceException;
import com.moyun.ledger.domain.entity.LedgerDebtRepayment;
import com.moyun.ledger.domain.dto.TransactionCreateDTO;
import com.moyun.ledger.domain.entity.LedgerTransaction;
import com.moyun.ledger.domain.entity.LedgerPayable;
import com.moyun.ledger.domain.entity.LedgerReceivable;
import com.moyun.ledger.mapper.LedgerDebtRepaymentMapper;
import com.moyun.ledger.mapper.LedgerDebtSummaryMapper;
import com.moyun.ledger.mapper.LedgerPayableMapper;
import com.moyun.ledger.mapper.LedgerReceivableMapper;
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
 * 债权债务服务实现。
 *
 * @author moyun
 */
@Service
public class LedgerDebtServiceImpl implements ILedgerDebtService {

    @Autowired
    private LedgerReceivableMapper receivableMapper;
    @Autowired
    private LedgerPayableMapper payableMapper;
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
        LambdaQueryWrapper<LedgerReceivable> q = new LambdaQueryWrapper<>();
        // 始终限定 userId：债务是强私有数据，任何筛选都不能绕过它
        q.eq(LedgerReceivable::getUserId, userId)
                .eq(LedgerReceivable::getStatus, LedgerReceivable.STATUS_NORMAL);
        if (keyword != null && !keyword.isBlank()) {
            q.like(LedgerReceivable::getCounterparty, keyword.trim());
        }
        if (settleStatus != null && !settleStatus.isBlank()) {
            q.eq(LedgerReceivable::getSettleStatus, settleStatus.trim());
        }
        q.orderByAsc(LedgerReceivable::getSettleStatus)
                .orderByAsc(LedgerReceivable::getDueDate)
                .orderByDesc(LedgerReceivable::getId);

        int pn = (pageNum == null || pageNum < 1) ? 1 : pageNum;
        int ps = (pageSize == null || pageSize < 1) ? 20 : Math.min(pageSize, 100);
        Page<LedgerReceivable> page = receivableMapper.selectPage(new Page<>(pn, ps), q);
        return pageResult(page.getRecords(), page.getTotal(), pn, ps, page.getPages());
    }

    @Override
    public LedgerReceivable getReceivable(Long userId, Long id) {
        return getOwnedReceivable(userId, id);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long createReceivable(Long userId, LedgerReceivable bo) {
        validateDebt(bo.getCounterparty(), bo.getTotalAmount());
        LedgerReceivable r = new LedgerReceivable();
        r.setUserId(userId);
        r.setCounterparty(bo.getCounterparty().trim());
        r.setCounterpartyId(bo.getCounterpartyId());
        r.setWarehouseId(bo.getWarehouseId());
        r.setSourceType(bo.getSourceType() == null ? LedgerReceivable.SOURCE_MANUAL : bo.getSourceType());
        r.setSourceId(bo.getSourceId());
        r.setTotalAmount(bo.getTotalAmount());
        r.setPaidAmount(ZERO);
        r.setSettleStatus(LedgerReceivable.SETTLE_PENDING);
        r.setDueDate(bo.getDueDate());
        r.setRemark(bo.getRemark());
        r.setStatus(LedgerReceivable.STATUS_NORMAL);
        r.setCreateBy(bo.getCreateBy());
        receivableMapper.insert(r);
        return r.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateReceivable(Long userId, Long id, LedgerReceivable bo) {
        LedgerReceivable db = getOwnedReceivable(userId, id);
        if (bo.getCounterparty() != null) {
            db.setCounterparty(bo.getCounterparty().trim());
        }
        if (bo.getTotalAmount() != null) {
            // 总额不得低于已收金额，否则账上会出现"负的未结余额"
            if (bo.getTotalAmount().compareTo(nz(db.getPaidAmount())) < 0) {
                throw new ServiceException("应收总额不能小于已收金额 " + nz(db.getPaidAmount()).toPlainString() + " 元");
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
        receivableMapper.updateById(db);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteReceivable(Long userId, Long id) {
        LedgerReceivable db = getOwnedReceivable(userId, id);
        db.setStatus(LedgerReceivable.STATUS_DELETED);
        receivableMapper.updateById(db);
    }

    // ---------------- 应付 ----------------

    @Override
    public Map<String, Object> listPayables(Long userId, String keyword, String settleStatus,
                                           Integer pageNum, Integer pageSize) {
        LambdaQueryWrapper<LedgerPayable> q = new LambdaQueryWrapper<>();
        q.eq(LedgerPayable::getUserId, userId)
                .eq(LedgerPayable::getStatus, LedgerPayable.STATUS_NORMAL);
        if (keyword != null && !keyword.isBlank()) {
            q.like(LedgerPayable::getCounterparty, keyword.trim());
        }
        if (settleStatus != null && !settleStatus.isBlank()) {
            q.eq(LedgerPayable::getSettleStatus, settleStatus.trim());
        }
        q.orderByAsc(LedgerPayable::getSettleStatus)
                .orderByAsc(LedgerPayable::getDueDate)
                .orderByDesc(LedgerPayable::getId);

        int pn = (pageNum == null || pageNum < 1) ? 1 : pageNum;
        int ps = (pageSize == null || pageSize < 1) ? 20 : Math.min(pageSize, 100);
        Page<LedgerPayable> page = payableMapper.selectPage(new Page<>(pn, ps), q);
        return pageResult(page.getRecords(), page.getTotal(), pn, ps, page.getPages());
    }

    @Override
    public LedgerPayable getPayable(Long userId, Long id) {
        return getOwnedPayable(userId, id);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long createPayable(Long userId, LedgerPayable bo) {
        validateDebt(bo.getCounterparty(), bo.getTotalAmount());
        LedgerPayable p = new LedgerPayable();
        p.setUserId(userId);
        p.setCounterparty(bo.getCounterparty().trim());
        p.setCounterpartyId(bo.getCounterpartyId());
        p.setWarehouseId(bo.getWarehouseId());
        p.setSourceType(bo.getSourceType() == null ? LedgerPayable.SOURCE_MANUAL : bo.getSourceType());
        p.setSourceId(bo.getSourceId());
        p.setTotalAmount(bo.getTotalAmount());
        p.setPaidAmount(ZERO);
        p.setSettleStatus(LedgerPayable.SETTLE_PENDING);
        p.setDueDate(bo.getDueDate());
        p.setRemark(bo.getRemark());
        p.setStatus(LedgerPayable.STATUS_NORMAL);
        p.setCreateBy(bo.getCreateBy());
        payableMapper.insert(p);
        return p.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updatePayable(Long userId, Long id, LedgerPayable bo) {
        LedgerPayable db = getOwnedPayable(userId, id);
        if (bo.getCounterparty() != null) {
            db.setCounterparty(bo.getCounterparty().trim());
        }
        if (bo.getTotalAmount() != null) {
            if (bo.getTotalAmount().compareTo(nz(db.getPaidAmount())) < 0) {
                throw new ServiceException("应付总额不能小于已还金额 " + nz(db.getPaidAmount()).toPlainString() + " 元");
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
        payableMapper.updateById(db);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deletePayable(Long userId, Long id) {
        LedgerPayable db = getOwnedPayable(userId, id);
        db.setStatus(LedgerPayable.STATUS_DELETED);
        payableMapper.updateById(db);
    }

    // ---------------- 核销 ----------------

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Map<String, Object> settle(Long userId, String debtType, Long debtId, BigDecimal amount,
                                      Long accountId, LocalDate repayDate, String remark) {
        if (!LedgerDebtRepayment.DEBT_RECEIVABLE.equals(debtType)
                && !LedgerDebtRepayment.DEBT_PAYABLE.equals(debtType)) {
            throw new ServiceException("非法债务类型");
        }
        if (amount == null || amount.compareTo(ZERO) <= 0) {
            throw new ServiceException("核销金额必须大于0");
        }
        LocalDate date = repayDate == null ? LocalDate.now() : repayDate;

        Long txId;
        String settledStatus;
        if (LedgerDebtRepayment.DEBT_RECEIVABLE.equals(debtType)) {
            LedgerReceivable d = getOwnedReceivable(userId, debtId);
            BigDecimal outstanding = nz(d.getTotalAmount()).subtract(nz(d.getPaidAmount()));
            if (amount.compareTo(outstanding) > 0) {
                throw new ServiceException("核销金额超过未结余额（当前 " + outstanding.toPlainString() + " 元）");
            }
            // 收款 = 资金账户增加。走独立的 collect 类型而非 income ——
            // 收债不是收入（钱本来就是我的），计入收入报表会让当月收入虚高。
            txId = createSettleTransaction(userId, LedgerTransaction.TYPE_COLLECT, amount, accountId,
                    date, d.getCounterparty(), remark, d.getId(), null);
            d.setPaidAmount(nz(d.getPaidAmount()).add(amount));
            d.setSettleStatus(resolveSettleStatus(d.getTotalAmount(), d.getPaidAmount()));
            receivableMapper.updateById(d);
            settledStatus = d.getSettleStatus();
        } else {
            LedgerPayable d = getOwnedPayable(userId, debtId);
            BigDecimal outstanding = nz(d.getTotalAmount()).subtract(nz(d.getPaidAmount()));
            if (amount.compareTo(outstanding) > 0) {
                throw new ServiceException("核销金额超过未结余额（当前 " + outstanding.toPlainString() + " 元）");
            }
            // 还款 = 资金账户减少，带 payableId 走 repayment。
            // validate 已允许「负债账户 or 应付款」二选一，余额分支也已判空，
            // 故这里不会去动负债账户；应付余额由本方法扣减。
            txId = createSettleTransaction(userId, LedgerTransaction.TYPE_REPAYMENT, amount, accountId,
                    date, d.getCounterparty(), remark, null, d.getId());
            d.setPaidAmount(nz(d.getPaidAmount()).add(amount));
            d.setSettleStatus(resolveSettleStatus(d.getTotalAmount(), d.getPaidAmount()));
            payableMapper.updateById(d);
            settledStatus = d.getSettleStatus();
        }

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
        result.put("settleStatus", settledStatus);
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

    // ---------------- 内部 ----------------

    private void validateDebt(String counterparty, BigDecimal total) {
        if (counterparty == null || counterparty.isBlank()) {
            throw new ServiceException("请填写对方名称");
        }
        if (total == null || total.compareTo(ZERO) <= 0) {
            throw new ServiceException("金额必须大于0");
        }
    }

    private LedgerReceivable getOwnedReceivable(Long userId, Long id) {
        LedgerReceivable r = receivableMapper.selectById(id);
        if (r == null || !userId.equals(r.getUserId())
                || LedgerReceivable.STATUS_DELETED == r.getStatus()) {
            throw new ServiceException("应收款不存在");
        }
        return r;
    }

    private LedgerPayable getOwnedPayable(Long userId, Long id) {
        LedgerPayable p = payableMapper.selectById(id);
        if (p == null || !userId.equals(p.getUserId())
                || LedgerPayable.STATUS_DELETED == p.getStatus()) {
            throw new ServiceException("应付款不存在");
        }
        return p;
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