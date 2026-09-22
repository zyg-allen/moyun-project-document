package com.moyun.pay.service;

import com.moyun.pay.domain.entity.UserAccount;

import java.math.BigDecimal;

/**
 * 用户资金账户服务
 *
 * <p>金额单位：元（人民币，统一）。
 *
 * @author moyun
 */
public interface IUserAccountService {

    /** 查询账户（不存在自动开户：balance=0） */
    UserAccount getOrCreate(Long userId);

    /** 入账（分账所得，原子）：返回变动后余额（元） */
    BigDecimal credit(Long userId, BigDecimal amount);

    /** 扣减（提现，原子，防超扣）：成功返回 true */
    boolean debit(Long userId, BigDecimal amount);

    /** 冻结（提现申请，原子，可用余额 = balance - frozen_amount 条件防超提）：成功返回 true */
    boolean freeze(Long userId, BigDecimal amount);

    /** 解冻（提现驳回/撤回，原子）：成功返回 true */
    boolean unfreeze(Long userId, BigDecimal amount);

    /** 扣减冻结（提现审核通过打款，原子：balance/frozen/totalWithdraw 三联动）：成功返回 true */
    boolean debitFrozen(Long userId, BigDecimal amount);
}
