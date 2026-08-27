package com.moyun.pay.domain.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.annotation.Version;

import java.time.LocalDateTime;

/**
 * 用户资金账户（V11.0）
 *
 * <p>余额单位：分（long）。并发安全：乐观锁 version + 条件更新（balance >= 扣减额），
 * 不使用浮点，不使用先读后写。
 *
 * @author moyun
 */
@TableName("user_account")
public class UserAccount {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 用户 ID（唯一） */
    private Long userId;

    /** 可用余额（分） */
    private Long balance;

    /** 累计收入（分，含分账所得） */
    private Long totalIncome;

    /** 累计提现（分） */
    private Long totalWithdraw;

    /** 乐观锁版本号 */
    @Version
    private Integer version;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }
    public Long getBalance() { return balance; }
    public void setBalance(Long balance) { this.balance = balance; }
    public Long getTotalIncome() { return totalIncome; }
    public void setTotalIncome(Long totalIncome) { this.totalIncome = totalIncome; }
    public Long getTotalWithdraw() { return totalWithdraw; }
    public void setTotalWithdraw(Long totalWithdraw) { this.totalWithdraw = totalWithdraw; }
    public Integer getVersion() { return version; }
    public void setVersion(Integer version) { this.version = version; }
    public LocalDateTime getCreateTime() { return createTime; }
    public void setCreateTime(LocalDateTime createTime) { this.createTime = createTime; }
    public LocalDateTime getUpdateTime() { return updateTime; }
    public void setUpdateTime(LocalDateTime updateTime) { this.updateTime = updateTime; }
}
