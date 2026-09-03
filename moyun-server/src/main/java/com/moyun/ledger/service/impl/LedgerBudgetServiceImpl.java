package com.moyun.ledger.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.moyun.ledger.domain.entity.LedgerBudget;
import com.moyun.ledger.mapper.LedgerBudgetMapper;
import com.moyun.ledger.service.ILedgerBudgetService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

/**
 * 记账预算服务实现
 *
 * @author moyun
 */
@Service
public class LedgerBudgetServiceImpl extends ServiceImpl<LedgerBudgetMapper, LedgerBudget>
        implements ILedgerBudgetService {

    @Override
    public List<LedgerBudget> listByMonth(Long userId, Integer year, Integer month) {
        LocalDate now = LocalDate.now();
        int y = year == null ? now.getYear() : year;
        int m = month == null ? now.getMonthValue() : month;
        LambdaQueryWrapper<LedgerBudget> qw = new LambdaQueryWrapper<>();
        qw.eq(LedgerBudget::getUserId, userId)
                .eq(LedgerBudget::getYear, y)
                .eq(LedgerBudget::getMonth, m);
        return list(qw);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void saveBudget(Long userId, LedgerBudget budget) {
        LocalDate now = LocalDate.now();
        if (budget.getYear() == null) {
            budget.setYear(now.getYear());
        }
        if (budget.getMonth() == null) {
            budget.setMonth(now.getMonthValue());
        }
        if (budget.getAmount() == null || budget.getAmount() < 0) {
            throw new IllegalArgumentException("预算金额不合法");
        }
        if (budget.getYear() < 2000 || budget.getYear() > 2100
                || budget.getMonth() < 1 || budget.getMonth() > 12) {
            throw new IllegalArgumentException("预算年月不合法");
        }
        LambdaQueryWrapper<LedgerBudget> qw = new LambdaQueryWrapper<>();
        qw.eq(LedgerBudget::getUserId, userId)
                .eq(LedgerBudget::getYear, budget.getYear())
                .eq(LedgerBudget::getMonth, budget.getMonth());
        if (budget.getCategoryId() == null) {
            qw.isNull(LedgerBudget::getCategoryId);
        } else {
            qw.eq(LedgerBudget::getCategoryId, budget.getCategoryId());
        }
        LedgerBudget exist = getOne(qw);
        if (exist == null) {
            budget.setId(null);
            budget.setUserId(userId);
            save(budget);
        } else {
            exist.setAmount(budget.getAmount());
            updateById(exist);
        }
    }
}
