package com.moyun.pay.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.moyun.pay.domain.entity.UserAccount;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.math.BigDecimal;

/**
 * 用户资金账户 Mapper
 *
 * <p>余额变动一律走这里的原子 SQL（带 balance >= 条件），配合乐观锁双保险，
 * 禁止先读后写。金额单位：元（DECIMAL(18,2)）。
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

    int creditBalance(@Param("userId") Long userId, @Param("amount") BigDecimal amount);

    /**
     * 原子扣减（提现冻结）：带 balance >= 条件，天然防超扣
     *
     * @return 影响行数（0=余额不足或账户不存在）
     */

    int debitBalance(@Param("userId") Long userId, @Param("amount") BigDecimal amount);

    /**
     * 原子冻结（提现申请）：frozen_amount += amount，带可用余额条件（balance - frozen_amount >= amount），
     * 天然防并发重复申请超提
     *
     * @return 影响行数（0=可用余额不足或账户不存在）
     */
    int freezeBalance(@Param("userId") Long userId, @Param("amount") BigDecimal amount);

    /**
     * 原子解冻（提现驳回/撤回）：frozen_amount -= amount，带 frozen_amount >= amount 条件
     *
     * @return 影响行数（0=冻结金额不足或账户不存在）
     */
    int unfreezeBalance(@Param("userId") Long userId, @Param("amount") BigDecimal amount);

    /**
     * 原子扣减冻结（提现审核通过打款）：balance -= amount 且 frozen_amount -= amount 且
     * total_withdraw += amount 三联动，条件 frozen_amount >= amount AND balance >= amount
     *
     * @return 影响行数（0=冻结/余额不足或账户不存在）
     */
    int debitFrozenBalance(@Param("userId") Long userId, @Param("amount") BigDecimal amount);
}
