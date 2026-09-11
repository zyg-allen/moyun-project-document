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
 * <p>金额单位：元（BigDecimal，v11.31 统一口径）。
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
    public Map<String, Object> planDetail(Long userId, Long planId) {
        LedgerSavingPlan plan = getOwnedPlan(userId, planId);
        LambdaQueryWrapper<LedgerSavingRecord> q = new LambdaQueryWrapper<>();
        q.eq(LedgerSavingRecord::getPlanId, planId)
                .eq(LedgerSavingRecord::getUserId, userId)
                .orderByAsc(LedgerSavingRecord::getPeriodIndex);
        Map<String, Object> result = new HashMap<>();
        result.put("plan", plan);
        result.put("records", recordMapper.selectList(q));
        return result;
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