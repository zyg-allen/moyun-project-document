package com.moyun.pay.domain.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.annotation.Version;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 用户资金账户（V11.0）
 *
 * <p>余额单位：元（人民币，DECIMAL(18,2)，v11.31 统一）。并发安全：乐观锁 version +
 * 条件更新（balance >= 扣减额），全部走 Mapper 原子 SQL，不使用先读后写。
 *
 * @author moyun
 */
@TableName("pay_user_account")
public class UserAccount {

    /** 用户 ID（主键，一对一账户） */
    @TableId(type = IdType.INPUT)
    private Long userId;

    /** 可用余额（元） */
    private BigDecimal balance;

    /** 累计收入（元，含分账所得） */
    private BigDecimal totalIncome;

    /** 累计提现（元） */
    private BigDecimal totalWithdraw;

    /** 乐观锁版本号 */
    @Version
    private Integer version;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;

    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }
    public BigDecimal getBalance() { return balance; }
    public void setBalance(BigDecimal balance) { this.balance = balance; }
    public BigDecimal getTotalIncome() { return totalIncome; }
    public void setTotalIncome(BigDecimal totalIncome) { this.totalIncome = totalIncome; }
    public BigDecimal getTotalWithdraw() { return totalWithdraw; }
    public void setTotalWithdraw(BigDecimal totalWithdraw) { this.totalWithdraw = totalWithdraw; }
    public Integer getVersion() { return version; }
    public void setVersion(Integer version) { this.version = version; }
    public LocalDateTime getCreateTime() { return createTime; }
    public void setCreateTime(LocalDateTime createTime) { this.createTime = createTime; }
    public LocalDateTime getUpdateTime() { return updateTime; }
    public void setUpdateTime(LocalDateTime updateTime) { this.updateTime = updateTime; }
}
