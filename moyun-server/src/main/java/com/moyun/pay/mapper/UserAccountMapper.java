package com.moyun.pay.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.moyun.pay.domain.entity.UserAccount;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Update;

/**
 * 用户资金账户 Mapper（V11.0）
 *
 * <p>余额变动一律走这里的原子 SQL（带 balance >= 条件），配合乐观锁双保险，
 * 禁止先读后写。
 *
 * @author moyun
 */
@Mapper
public interface UserAccountMapper extends BaseMapper<UserAccount> {

    /**
     * 原子入账（分账所得/充值）：balance + totalIncome 同增
     *
     * @return 影响行数（0=账户不存在）
     */
    @Update("UPDATE pay_user_account SET balance = balance + #{amount}, total_income = total_income + #{amount}, "
            + "version = version + 1, update_time = NOW() WHERE user_id = #{userId}")
    int creditBalance(@Param("userId") Long userId, @Param("amount") long amount);

    /**
     * 原子扣减（提现冻结）：带 balance >= 条件，天然防超扣
     *
     * @return 影响行数（0=余额不足或账户不存在）
     */
    @Update("UPDATE pay_user_account SET balance = balance - #{amount}, total_withdraw = total_withdraw + #{amount}, "
            + "version = version + 1, update_time = NOW() WHERE user_id = #{userId} AND balance >= #{amount}")
    int debitBalance(@Param("userId") Long userId, @Param("amount") long amount);
}
