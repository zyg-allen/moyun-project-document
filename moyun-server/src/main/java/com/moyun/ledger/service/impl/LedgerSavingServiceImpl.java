package com.moyun.ledger.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.moyun.common.exception.system.ServiceException;
import com.moyun.ledger.domain.entity.LedgerSavingPlan;
import com.moyun.ledger.domain.entity.LedgerSavingRecord;
import com.moyun.ledger.mapper.LedgerSavingPlanMapper;
import com.moyun.ledger.mapper.LedgerSavingRecordMapper;
import com.moyun.ledger.service.ILedgerSavingService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 存钱计划服务实现
 *
 * <p>金额单位：元（BigDecimal，统一口径）。
 * 期次生成规则：
 * 52week → 52期，第n期 10n 元；
 * fixed  → periodAmount 一期，直至凑满 targetAmount（末期为差额）；
 * monthly → periodAmount 一期 × 12 个月（一年期，按 startDate 起算）；
 * custom → periodCount 期，首期 periodAmount，每期递增 increaseStep（最后一期为凑整差额）。
 *
 * @author moyun
 */
@Service
public class LedgerSavingServiceImpl extends ServiceImpl<LedgerSavingPlanMapper, LedgerSavingPlan>
        implements ILedgerSavingService {

    @Autowired
    private LedgerSavingRecordMapper recordMapper;

    @Autowired
    private com.moyun.ledger.mapper.LedgerTransactionMapper transactionMapper;

    @Autowired
    private com.moyun.ledger.service.ILedgerTransactionService transactionService;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long createPlan(Long userId, LedgerSavingPlan plan) {
        if (plan.getName() == null || plan.getName().trim().isEmpty()) {
            throw new ServiceException("请输入计划名称");
        }
        if (plan.getTargetAmount() == null || plan.getTargetAmount().compareTo(BigDecimal.ZERO) <= 0) {
            throw new ServiceException("目标金额必须大于0");
        }
        String method = plan.getMethod() == null ? LedgerSavingPlan.METHOD_52WEEK : plan.getMethod();
        plan.setUserId(userId);
        plan.setMethod(method);
        plan.setCurrentAmount(BigDecimal.ZERO);
        plan.setStatus(LedgerSavingPlan.STATUS_RUNNING);
        if (plan.getStartDate() == null) {
            plan.setStartDate(LocalDate.now());
        }
        this.save(plan);

        // 按方式生成期次流水
        List<LedgerSavingRecord> records = buildRecords(plan);
        for (LedgerSavingRecord r : records) {
            r.setPlanId(plan.getId());
            r.setUserId(userId);
            r.setStatus(LedgerSavingRecord.STATUS_PENDING);
            // 计划日期：按月或按周从开始日期推算，供日历/年份分组使用
            LocalDate planned;
            if ("monthly".equals(method)) {
                planned = plan.getStartDate().plusMonths(r.getPeriodIndex() - 1);
            } else {
                planned = plan.getStartDate().plusWeeks(r.getPeriodIndex() - 1);
            }
            r.setRecordDate(planned);
            recordMapper.insert(r);
        }
        return plan.getId();
    }

    /** 按存钱方式生成期次（不落库，仅构造） */
    private List<LedgerSavingRecord> buildRecords(LedgerSavingPlan plan) {
        List<LedgerSavingRecord> list = new ArrayList<>();
        String method = plan.getMethod();
        BigDecimal target = plan.getTargetAmount();
        switch (method) {
            case LedgerSavingPlan.METHOD_52WEEK -> {
                for (int w = 1; w <= 52; w++) {
                    list.add(record(w, BigDecimal.TEN.multiply(BigDecimal.valueOf(w))));
                }
            }
            case LedgerSavingPlan.METHOD_FIXED, LedgerSavingPlan.METHOD_MONTHLY -> {
                BigDecimal period = plan.getPeriodAmount();
                if (period == null || period.compareTo(BigDecimal.ZERO) <= 0) {
                    throw new ServiceException("请输入每期存入金额");
                }
                int count = target.divide(period, 0, java.math.RoundingMode.CEILING).intValue();
                BigDecimal accumulated = BigDecimal.ZERO;
                for (int i = 1; i <= count; i++) {
                    BigDecimal cur = target.subtract(accumulated).min(period);
                    list.add(record(i, cur));
                    accumulated = accumulated.add(cur);
                }
            }
            case LedgerSavingPlan.METHOD_CUSTOM -> {
                Integer periodCount = plan.getPeriodCount();
                BigDecimal step = plan.getIncreaseStep() == null ? BigDecimal.ZERO : plan.getIncreaseStep();
                BigDecimal first = plan.getPeriodAmount();
                if (periodCount == null || periodCount <= 0) {
                    throw new ServiceException("自定义方式请填写总期数");
                }
                if (first == null || first.compareTo(BigDecimal.ZERO) <= 0) {
                    throw new ServiceException("自定义方式请填写首期金额");
                }
                BigDecimal accumulated = BigDecimal.ZERO;
                for (int i = 1; i <= periodCount; i++) {
                    BigDecimal cur = first.add(step.multiply(BigDecimal.valueOf(i - 1L)));
                    if (i == periodCount && accumulated.add(cur).compareTo(target) > 0) {
                        cur = target.subtract(accumulated); // 末期凑整
                    }
                    if (cur.compareTo(BigDecimal.ZERO) > 0) {
                        list.add(record(i, cur));
                        accumulated = accumulated.add(cur);
                    }
                }
            }
            default -> throw new ServiceException("不支持的存钱方式：" + method);
        }
        return list;
    }

    private LedgerSavingRecord record(int index, BigDecimal targetAmount) {
        LedgerSavingRecord r = new LedgerSavingRecord();
        r.setPeriodIndex(index);
        r.setTargetAmount(targetAmount);
        return r;
    }

    @Override
    public Map<String, Object> listPlans(Long userId) {
        LambdaQueryWrapper<LedgerSavingPlan> q = new LambdaQueryWrapper<>();
        q.eq(LedgerSavingPlan::getUserId, userId)
                .ne(LedgerSavingPlan::getStatus, LedgerSavingPlan.STATUS_DELETED)
                .orderByDesc(LedgerSavingPlan::getCreateTime);
        List<LedgerSavingPlan> plans = this.list(q);

        BigDecimal totalTarget = BigDecimal.ZERO;
        BigDecimal totalSaved = BigDecimal.ZERO;
        for (LedgerSavingPlan p : plans) {
            if (p.getTargetAmount() != null) totalTarget = totalTarget.add(p.getTargetAmount());
            if (p.getCurrentAmount() != null) totalSaved = totalSaved.add(p.getCurrentAmount());
        }
        Map<String, Object> result = new HashMap<>();
        result.put("plans", plans);
        result.put("totalTarget", totalTarget);
        result.put("totalSaved", totalSaved);
        result.put("totalRemaining", totalTarget.subtract(totalSaved).max(BigDecimal.ZERO));
        return result;
    }

    @Override
    public com.moyun.ledger.domain.vo.SavingPlanDetailVO planDetail(Long userId, Long planId) {
        LedgerSavingPlan plan = getOwnedPlan(userId, planId);
        LambdaQueryWrapper<LedgerSavingRecord> q = new LambdaQueryWrapper<>();
        q.eq(LedgerSavingRecord::getPlanId, planId)
                .eq(LedgerSavingRecord::getUserId, userId)
                .orderByAsc(LedgerSavingRecord::getPeriodIndex);
        java.util.List<LedgerSavingRecord> records = recordMapper.selectList(q);
        com.moyun.ledger.domain.vo.SavingPlanDetailVO vo = new com.moyun.ledger.domain.vo.SavingPlanDetailVO();
        com.moyun.ledger.domain.vo.SavingPlanDetailVO.Plan pv = new com.moyun.ledger.domain.vo.SavingPlanDetailVO.Plan();
        org.springframework.beans.BeanUtils.copyProperties(plan, pv);
        java.util.List<com.moyun.ledger.domain.vo.SavingPlanDetailVO.Record> rvs = new ArrayList<>();
        for (LedgerSavingRecord r : records) {
            com.moyun.ledger.domain.vo.SavingPlanDetailVO.Record rv = new com.moyun.ledger.domain.vo.SavingPlanDetailVO.Record();
            org.springframework.beans.BeanUtils.copyProperties(r, rv);
            rvs.add(rv);
        }
        vo.setPlan(pv);
        vo.setRecords(rvs);
        return vo;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deposit(Long userId, Long planId, Long recordId, BigDecimal actualAmount) {
        LedgerSavingPlan plan = getOwnedPlan(userId, planId);
        LedgerSavingRecord r = getOwnedRecord(userId, recordId, planId);
        if (LedgerSavingRecord.STATUS_SUCCESS == r.getStatus()) {
            throw new ServiceException("该期已存入，请勿重复操作");
        }
        BigDecimal amount = actualAmount != null ? actualAmount : r.getTargetAmount();
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new ServiceException("存入金额必须大于0");
        }
        r.setStatus(LedgerSavingRecord.STATUS_SUCCESS);
        r.setAmount(amount);
        r.setRecordDate(LocalDate.now());
        recordMapper.updateById(r);

        plan.setCurrentAmount(plan.getCurrentAmount() == null ? amount : plan.getCurrentAmount().add(amount));
        // 全部期次成功 或 已存金额达到目标 → 计划成功
        LambdaQueryWrapper<LedgerSavingRecord> q = new LambdaQueryWrapper<>();
        q.eq(LedgerSavingRecord::getPlanId, planId)
                .eq(LedgerSavingRecord::getStatus, LedgerSavingRecord.STATUS_PENDING);
        boolean allDone = recordMapper.selectCount(q) == 0
                || plan.getCurrentAmount().compareTo(plan.getTargetAmount()) >= 0;
        if (allDone && plan.getStatus() == LedgerSavingPlan.STATUS_RUNNING) {
            plan.setStatus(LedgerSavingPlan.STATUS_SUCCESS);
        }
        this.updateById(plan);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void failRecord(Long userId, Long planId, Long recordId, String reason) {
        getOwnedPlan(userId, planId);
        LedgerSavingRecord r = getOwnedRecord(userId, recordId, planId);
        if (LedgerSavingRecord.STATUS_SUCCESS == r.getStatus()) {
            throw new ServiceException("该期已成功，不能标记失败");
        }
        r.setStatus(LedgerSavingRecord.STATUS_FAILED);
        r.setFailReason(reason == null || reason.isBlank() ? "主动放弃" : reason);
        r.setRecordDate(LocalDate.now());
        recordMapper.updateById(r);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deletePlan(Long userId, Long planId) {
        LedgerSavingPlan plan = getOwnedPlan(userId, planId);
        plan.setStatus(LedgerSavingPlan.STATUS_DELETED);
        this.updateById(plan);
    }

    @Override
    public List<LedgerSavingRecord> listRecords(Long userId, Long planId) {
        LambdaQueryWrapper<LedgerSavingRecord> q = new LambdaQueryWrapper<>();
        q.eq(LedgerSavingRecord::getPlanId, planId)
                .eq(LedgerSavingRecord::getUserId, userId)
                .orderByAsc(LedgerSavingRecord::getPeriodIndex);
        return recordMapper.selectList(q);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void markRecord(Long userId, Long planId, Long recordId, Integer status, BigDecimal amount, String reason) {
        LedgerSavingPlan plan = getOwnedPlan(userId, planId);
        LedgerSavingRecord r = getOwnedRecord(userId, recordId, planId);
        int st = status == null ? 1 : status;
        if (st == 0) {
            r.setStatus(LedgerSavingRecord.STATUS_PENDING);
            r.setAmount(null);
            r.setFailReason(null);
            r.setTransactionId(null);
        } else if (st == 1) {
            BigDecimal amt = amount != null ? amount : r.getTargetAmount();
            if (amt == null || amt.compareTo(BigDecimal.ZERO) <= 0) {
                throw new ServiceException("金额必须大于0");
            }
            r.setStatus(LedgerSavingRecord.STATUS_SUCCESS);
            r.setAmount(amt);
            r.setFailReason(null);
        } else if (st == 2) {
            r.setStatus(LedgerSavingRecord.STATUS_FAILED);
            r.setFailReason(reason == null || reason.isBlank() ? "未完成" : reason);
        } else {
            throw new ServiceException("状态不合法");
        }
        if (r.getRecordDate() == null) {
            r.setRecordDate(LocalDate.now());
        }
        recordMapper.updateById(r);
        recalcPlan(plan);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void linkTransaction(Long userId, Long planId, Long recordId, Long transactionId) {
        LedgerSavingPlan plan = getOwnedPlan(userId, planId);
        LedgerSavingRecord r = getOwnedRecord(userId, recordId, planId);
        com.moyun.ledger.domain.entity.LedgerTransaction txn = transactionMapper.selectById(transactionId);
        if (txn == null || !userId.equals(txn.getUserId())) {
            throw new ServiceException("流水不存在");
        }
        r.setTransactionId(txn.getId());
        r.setStatus(LedgerSavingRecord.STATUS_SUCCESS);
        r.setAmount(txn.getAmount());
        if (txn.getTransactionDate() != null) {
            r.setRecordDate(txn.getTransactionDate());
        }
        recordMapper.updateById(r);
        recalcPlan(plan);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void unlinkTransaction(Long userId, Long planId, Long recordId, boolean deleteTransaction) {
        LedgerSavingPlan plan = getOwnedPlan(userId, planId);
        LedgerSavingRecord r = getOwnedRecord(userId, recordId, planId);
        if (deleteTransaction && r.getTransactionId() != null) {
            transactionMapper.deleteById(r.getTransactionId());
        }
        r.setTransactionId(null);
        if (r.getStatus() == LedgerSavingRecord.STATUS_SUCCESS) {
            r.setStatus(LedgerSavingRecord.STATUS_PENDING);
            r.setAmount(null);
        }
        recordMapper.updateById(r);
        recalcPlan(plan);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long createTransferAndLink(Long userId, Long planId, Long recordId, com.moyun.ledger.domain.dto.TransactionCreateDTO dto) {
        LedgerSavingPlan plan = getOwnedPlan(userId, planId);
        LedgerSavingRecord r = getOwnedRecord(userId, recordId, planId);
        com.moyun.ledger.domain.dto.TransactionCreateDTO t = new com.moyun.ledger.domain.dto.TransactionCreateDTO();
        t.setType("transfer");
        t.setAccountId(dto.getAccountId() != null ? dto.getAccountId() : plan.getSourceAccountId());
        t.setTargetAccountId(dto.getTargetAccountId() != null ? dto.getTargetAccountId() : plan.getSavingsAccountId());
        t.setAmount(dto.getAmount() != null ? dto.getAmount() : r.getTargetAmount());
        t.setTransactionDate(dto.getTransactionDate() != null ? dto.getTransactionDate() : LocalDate.now());
        t.setDescription(dto.getDescription() != null ? dto.getDescription() : ("存钱计划：" + plan.getName()));
        t.setClientUuid("saving:" + recordId);
        t.setBizType("saving");
        t.setDescription(plan.getName() + "-第" + r.getPeriodIndex() + "期");
        if (t.getAccountId() == null || t.getTargetAccountId() == null) {
            throw new ServiceException("请先绑定转出与存钱账户");
        }
        if (t.getAccountId().equals(t.getTargetAccountId())) {
            throw new ServiceException("转出与存入账户不能相同");
        }
        Long txnId = transactionService.createTransaction(userId, t);
        linkTransaction(userId, planId, recordId, txnId);
        return txnId;
    }

    @Override
    public List<com.moyun.ledger.domain.vo.LedgerTransactionVO> listLinkableTransactions(Long userId, LocalDate date, BigDecimal amount) {
        LambdaQueryWrapper<com.moyun.ledger.domain.entity.LedgerTransaction> q = new LambdaQueryWrapper<>();
        q.eq(com.moyun.ledger.domain.entity.LedgerTransaction::getUserId, userId)
                .eq(com.moyun.ledger.domain.entity.LedgerTransaction::getStatus,
                        com.moyun.ledger.domain.entity.LedgerTransaction.STATUS_NORMAL);
        if (date != null) {
            q.between(com.moyun.ledger.domain.entity.LedgerTransaction::getTransactionDate,
                    date.minusDays(7), date.plusDays(7));
        } else {
            q.ge(com.moyun.ledger.domain.entity.LedgerTransaction::getTransactionDate, LocalDate.now().minusDays(15));
        }
        if (amount != null) {
            q.eq(com.moyun.ledger.domain.entity.LedgerTransaction::getAmount, amount);
        }
        q.orderByDesc(com.moyun.ledger.domain.entity.LedgerTransaction::getTransactionDate).last("LIMIT 50");
        java.util.List<com.moyun.ledger.domain.entity.LedgerTransaction> list = transactionMapper.selectList(q);
        List<com.moyun.ledger.domain.vo.LedgerTransactionVO> vos = new ArrayList<>();
        for (com.moyun.ledger.domain.entity.LedgerTransaction txn : list) {
            com.moyun.ledger.domain.vo.LedgerTransactionVO v = new com.moyun.ledger.domain.vo.LedgerTransactionVO();
            org.springframework.beans.BeanUtils.copyProperties(txn, v);
            vos.add(v);
        }
        return vos;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateAccounts(Long userId, Long planId, Long sourceAccountId, Long savingsAccountId) {
        LedgerSavingPlan plan = getOwnedPlan(userId, planId);
        plan.setSourceAccountId(sourceAccountId);
        plan.setSavingsAccountId(savingsAccountId);
        this.updateById(plan);
    }

    /** 重算已存金额与计划状态 */
    private void recalcPlan(LedgerSavingPlan plan) {
        LambdaQueryWrapper<LedgerSavingRecord> done = new LambdaQueryWrapper<>();
        done.eq(LedgerSavingRecord::getPlanId, plan.getId())
                .eq(LedgerSavingRecord::getStatus, LedgerSavingRecord.STATUS_SUCCESS);
        java.math.BigDecimal sum = recordMapper.selectList(done).stream()
                .map(LedgerSavingRecord::getAmount)
                .filter(java.util.Objects::nonNull)
                .reduce(java.math.BigDecimal.ZERO, java.math.BigDecimal::add);
        plan.setCurrentAmount(sum);
        LambdaQueryWrapper<LedgerSavingRecord> pending = new LambdaQueryWrapper<>();
        pending.eq(LedgerSavingRecord::getPlanId, plan.getId())
                .eq(LedgerSavingRecord::getStatus, LedgerSavingRecord.STATUS_PENDING);
        long left = recordMapper.selectCount(pending);
        boolean reached = plan.getTargetAmount() != null && sum.compareTo(plan.getTargetAmount()) >= 0;
        if (left == 0 || reached) {
            plan.setStatus(LedgerSavingPlan.STATUS_SUCCESS);
        } else if (plan.getStatus() != null && plan.getStatus() == LedgerSavingPlan.STATUS_SUCCESS) {
            plan.setStatus(LedgerSavingPlan.STATUS_RUNNING);
        }
        this.updateById(plan);
    }

    private LedgerSavingPlan getOwnedPlan(Long userId, Long planId) {
        LedgerSavingPlan plan = this.getById(planId);
        if (plan == null || !userId.equals(plan.getUserId())
                || plan.getStatus() == LedgerSavingPlan.STATUS_DELETED) {
            throw new ServiceException("存钱计划不存在");
        }
        return plan;
    }

    private LedgerSavingRecord getOwnedRecord(Long userId, Long recordId, Long planId) {
        LedgerSavingRecord r = recordMapper.selectById(recordId);
        if (r == null || !userId.equals(r.getUserId()) || !planId.equals(r.getPlanId())) {
            throw new ServiceException("存钱期次不存在");
        }
        return r;
    }
}
