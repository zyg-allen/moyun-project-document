package com.moyun.pay.channel;

import java.time.LocalDateTime;

/**
 * 渠道预下单请求（网关 → 渠道）
 *
 * @author moyun
 */
public class PayChannelRequest {

    /** 本系统支付单号（幂等键） */
    private String payNo;
    /** 金额（分） */
    private long amount;
    /** 商品描述 */
    private String subject;
    /** 过期时间 */
    private LocalDateTime expireTime;

    public String getPayNo() { return payNo; }
    public void setPayNo(String payNo) { this.payNo = payNo; }
    public long getAmount() { return amount; }
    public void setAmount(long amount) { this.amount = amount; }
    public String getSubject() { return subject; }
    public void setSubject(String subject) { this.subject = subject; }
    public LocalDateTime getExpireTime() { return expireTime; }
    public void setExpireTime(LocalDateTime expireTime) { this.expireTime = expireTime; }
}
