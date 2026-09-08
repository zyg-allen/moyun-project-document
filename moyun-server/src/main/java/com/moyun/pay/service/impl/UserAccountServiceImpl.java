package com.moyun.pay.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.moyun.pay.domain.entity.UserAccount;
import com.moyun.pay.mapper.UserAccountMapper;
import com.moyun.pay.service.IUserAccountService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 用户资金账户服务实现（V11.0）
 *
 * <p>余额变动全部走 {@code UserAccountMapper} 的原子 SQL（balance 条件更新 + 乐观锁），
 * 杜绝读改写竞态。金额单位：元（v11.31 统一）。
 *
 * @author moyun
 */
@Service
public class UserAccountServiceImpl implements IUserAccountService {

    private static final Logger log = LoggerFactory.getLogger(UserAccountServiceImpl.class);

    @Autowired
    private UserAccountMapper userAccountMapper;

    @Override
    public UserAccount getOrCreate(Long userId) {
        UserAccount account = userAccountMapper.selectOne(new LambdaQueryWrapper<UserAccount>()
                .eq(UserAccount::getUserId, userId)
                .last("LIMIT 1"));
        if (account != null) {
            return account;
        }
        // 开户（并发兜底：唯一索引 user_id 冲突时回读）
        account = new UserAccount();
        account.setUserId(userId);
        account.setBalance(BigDecimal.ZERO);
        account.setTotalIncome(BigDecimal.ZERO);
        account.setTotalWithdraw(BigDecimal.ZERO);
        account.setVersion(0);
        account.setCreateTime(LocalDateTime.now());
        account.setUpdateTime(LocalDateTime.now());
        try {
            userAccountMapper.insert(account);
            log.info("[user-account] 开户成功 userId={}", userId);
            return account;
        } catch (org.springframework.dao.DuplicateKeyException e) {
            return userAccountMapper.selectOne(new LambdaQueryWrapper<UserAccount>()
                    .eq(UserAccount::getUserId, userId)
                    .last("LIMIT 1"));
        }
    }

    @Override
    public BigDecimal credit(Long userId, BigDecimal amount) {
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("入账金额必须大于 0");
        }
        getOrCreate(userId);
        int rows = userAccountMapper.creditBalance(userId, amount);
        if (rows == 0) {
            throw new IllegalStateException("入账失败 userId=" + userId);
        }
        UserAccount after = userAccountMapper.selectOne(new LambdaQueryWrapper<UserAccount>()
                .eq(UserAccount::getUserId, userId).last("LIMIT 1"));
        log.info("[user-account] 入账 userId={} amount={}元 balanceAfter={}元", userId, amount, after.getBalance());
        return after.getBalance();
    }

    @Override
    public boolean debit(Long userId, BigDecimal amount) {
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("扣减金额必须大于 0");
        }
        getOrCreate(userId);
        int rows = userAccountMapper.debitBalance(userId, amount);
        if (rows == 0) {
            log.warn("[user-account] 扣减失败（余额不足或账户不存在）userId={} amount={}元", userId, amount);
            return false;
        }
        log.info("[user-account] 扣减成功 userId={} amount={}元", userId, amount);
        return true;
    }
}
